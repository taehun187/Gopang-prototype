package com.gopang.paymentserver.dto;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CancelRequest {
    String order_id;
    int cancelAmount;
    int amount;
    int remainingBalance;
    String status;
}
