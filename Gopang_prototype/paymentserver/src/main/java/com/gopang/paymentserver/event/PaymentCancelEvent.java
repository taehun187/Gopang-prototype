package com.gopang.paymentserver.event;

import com.gopang.paymentserver.dto.CancelRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentCancelEvent {

    private final CancelRequest cancelStatus;
    private final String message;

}
