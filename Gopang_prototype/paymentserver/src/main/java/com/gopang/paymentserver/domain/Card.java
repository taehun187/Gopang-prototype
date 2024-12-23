package com.gopang.paymentserver.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Card {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ORDER_ID")
    public Long order_id;
    @Column(nullable = false)
    private String merchant_uid;
    @Column(nullable = false)
    private int amount;
    @Column(nullable = false)
    private String card_number;
    @Column(nullable = false)
    private String expiry;
    @Column(nullable = false)
    private String birth;
    @Column(nullable = false)
    private String pwd_2digit;
    @Column(nullable = false)
    private String cvc;
    @CreatedDate
    private LocalDateTime createdAt;

    public Card(String kafkaMerchantUid, int kafkaAmount, String kafkaCardNumber, String kafkaExpiry, String kafkaBirth, String kafkaPwd2digit, String kafkaCvc,
                LocalDateTime now) {
        this.merchant_uid = kafkaMerchantUid;
        this.amount = kafkaAmount;
        this.card_number = kafkaCardNumber;
        this.expiry = kafkaExpiry;
        this.birth = kafkaBirth;
        this.pwd_2digit = kafkaPwd2digit;
        this.cvc = kafkaCvc;
        this.createdAt = now;
    }

}
