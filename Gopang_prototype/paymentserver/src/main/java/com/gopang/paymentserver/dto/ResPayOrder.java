package com.gopang.paymentserver.dto;


import com.gopang.paymentserver.dto.Paymentdto.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResPayOrder {
    String order_id;
    PaymentStatus paymentStatus;
}
