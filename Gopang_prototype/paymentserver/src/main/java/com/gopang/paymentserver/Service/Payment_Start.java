package com.gopang.paymentserver.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gopang.paymentserver.authentication.TokenService;
import com.gopang.paymentserver.domain.Card;
import com.gopang.paymentserver.dto.Paymentdto.PaymentStatus;
import com.gopang.paymentserver.dto.ResPayOrder;
import com.gopang.paymentserver.event.PaymentEvent;
import com.gopang.paymentserver.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class Payment_Start {

    @Value("${portone.apiBaseUrl}")
    private String API_BASE_URL;
    @Value("${portone.paymentstart}")
    private String PAYMENT_ENDPOINT;

    // 이벤트 발행을 위한 ApplicationEventPublisher 주입
    private final ApplicationEventPublisher eventPublisher;
    @Autowired
    private CardRepository cardRepository;

    @Bean
    public Consumer<String> consumerBinding() {
        return jsonString -> { try {Payment(jsonString);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e); }
        };
    }

    @Transactional
    public String Payment(String jsonString) throws IOException, JSONException{

        TokenService tokenService = new TokenService();
        String accessToken = tokenService.getToken();

        RestTemplate restTemplate = new RestTemplate();
        JSONObject kafkajsonBody = new JSONObject(jsonString);

        log.info("KafkaPaymentReceived : " + jsonString);

        String KafkaMerchant_uid = kafkajsonBody.optString("order_id", null);
        int KafkaAmount = kafkajsonBody.optInt("amount", 0);

        String KafkaCard_number = "5465-9699-1234-5678";
        String KafkaExpiry = "2027-05";
        String KafkaBirth = "940123";
        String KafkaPwd_2digit = "12";
        String KafkaCvc = "123";

//        String KafkaCard_number = kafkajsonBody.optString("card_number", null);
//        String KafkaExpiry = kafkajsonBody.optString("expiry", null);
//        String KafkaBirth = kafkajsonBody.optString("birth", null);
//        String KafkaPwd_2digit = kafkajsonBody.optString("pwd_2digit", null);
//        String KafkaCvc = kafkajsonBody.optString("cvc", null);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("merchant_uid", KafkaMerchant_uid);
        jsonBody.put("amount", KafkaAmount);
        jsonBody.put("card_number", KafkaCard_number);
        jsonBody.put("expiry", KafkaExpiry);
        jsonBody.put("birth", KafkaBirth);
        jsonBody.put("pwd_2digit", KafkaPwd_2digit);
        jsonBody.put("cvc", KafkaCvc);

        // JSON 데이터와 적절한 미디어 타입을 설정하여 HttpEntity를 만듭니다.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody.toString(), headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(API_BASE_URL + PAYMENT_ENDPOINT,
                requestEntity, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(responseEntity.getBody());

        String padPaymentStatus = responseJson.path("response").path("status").asText();

        if (Objects.equals(padPaymentStatus, "paid")) {
            PaymentStatus successStatus = PaymentStatus.PAYCOMPLETE;

            ResPayOrder payment = ResPayOrder.builder()
                    .order_id(KafkaMerchant_uid)
                    .paymentStatus(successStatus)
                    .build();

            Card CardEntity = new Card(KafkaMerchant_uid, KafkaAmount, KafkaCard_number, KafkaExpiry, KafkaBirth,
                    KafkaPwd_2digit, KafkaCvc, LocalDateTime.now());
            cardRepository.save(CardEntity);

            // 로깅을 위한 메시지 생성 및 이벤트 발행
            String message = payment.getOrder_id() + "번 결제성공";

            // 로깅을 위한 메시지 생성 및 이벤트 발행
            eventPublisher.publishEvent(new PaymentEvent(payment, message));
        } else {
            // 파싱할 데이터 추출
            PaymentStatus failureStatus = PaymentStatus.PAYFAIL;

            ResPayOrder payment = ResPayOrder.builder()
                    .order_id(KafkaMerchant_uid)
                    .paymentStatus(failureStatus)
                    .build();

            String message = payment.getOrder_id() + "번 결제실패";

            eventPublisher.publishEvent(new PaymentEvent(payment, message));
        }
        return null;
    }
}


