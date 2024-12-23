package com.gopang.orderservice.message.payment;

import com.gopang.orderservice.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResPayOrder {
    // 주문 고유 번호
    public Long order_id;

    // 결제 상태
    public PaymentStatus paymentStatus;
}
