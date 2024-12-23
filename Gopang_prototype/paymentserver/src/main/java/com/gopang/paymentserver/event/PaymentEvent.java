package com.gopang.paymentserver.event;

import com.gopang.paymentserver.dto.ResPayOrder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentEvent {

    private final ResPayOrder orderRequest;
    private final String message;

}
