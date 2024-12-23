package com.gopang.paymentserver.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gopang.paymentserver.authentication.TokenService;
import com.gopang.paymentserver.domain.Cancel;
import com.gopang.paymentserver.dto.CancelRequest;
import com.gopang.paymentserver.event.PaymentCancelEvent;
import com.gopang.paymentserver.repository.CancelRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class Payment_Cancel {

    @Value("${portone.apiBaseUrl}")
    private String API_BASE_URL;
    @Value("${portone.paymentCancel}")
    private String PAYMENT_CANCEL_ENDPOINT;

    @Autowired
    private CancelRepository cancelRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    public Consumer<String> cancelBinding() {
        return jsonString -> {
            try {
                PaymentCancel(jsonString);
            } catch (org.json.JSONException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Transactional
    public String PaymentCancel(String jsonString) throws org.json.JSONException {

        JSONObject jsonBody = new JSONObject(jsonString);
        System.err.println(" KafkaCancelReceived: " + jsonString);

        String kafkaMerchant_uid = jsonBody.getString("order_id");
        int kafkaAmount = jsonBody.getInt("amount");

        try {
            TokenService tokenService = new TokenService();
            String accessToken = tokenService.getToken();

            // RestTemplate 인스턴스 생성
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("merchant_uid", kafkaMerchant_uid);
            requestBody.put("amount", kafkaAmount);

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            // 요청 바디와 헤더를 이용하여 요청 엔터티 생성
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // API의 URL 정의
            String apiUrl = API_BASE_URL + PAYMENT_CANCEL_ENDPOINT;

            // HTTP POST 요청을 보내고 응답을 받음
            ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

            // HTTP 상태 코드 확인
            HttpStatus statusCode = (HttpStatus) responseEntity.getStatusCode();

            if (statusCode == HttpStatus.OK) {
                String responseBody = responseEntity.getBody();

                // JSON 응답 파싱
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonObject = jsonParser.parse(responseBody).getAsJsonObject();

                String status = jsonObject.get("response").getAsJsonObject().get("status").getAsString();

                int cancelAmount = jsonObject.get("response").getAsJsonObject().get("cancel_amount").getAsInt();
                int amount = jsonObject.get("response").getAsJsonObject().get("amount").getAsInt();



                if (cancelAmount > amount) {
                    System.err.println("환불 가능한 금액보다 높은 금액을 입력했습니다.");
                } else {
                    int remainingBalance = amount - cancelAmount;

                    CancelRequest paymentCancelbuil = CancelRequest.builder()
                            .order_id(kafkaMerchant_uid)
                            .cancelAmount(cancelAmount)
                            .amount(amount)
                            .remainingBalance(remainingBalance)
                            .status(status)
                            .build();

                    Cancel paymentCancel = new Cancel(kafkaMerchant_uid, cancelAmount, kafkaAmount, status);
                    cancelRepository.save(paymentCancel);

                    // 로깅을 위한 메시지 생성 및 이벤트 발행
                    String message = paymentCancel.getOrder_id() + "번 취소됨.";

//                     로깅을 위한 메시지 생성 및 이벤트 발행
                    eventPublisher.publishEvent(new PaymentCancelEvent(paymentCancelbuil, message));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}