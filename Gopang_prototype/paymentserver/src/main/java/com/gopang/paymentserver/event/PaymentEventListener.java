package com.gopang.paymentserver.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final StreamBridge streamBridge;

    @EventListener
    public void handleTransactionStartCommit(PaymentEvent event) {
        log.info("Received message to paymentRequest-topic: " + event.getMessage());
        streamBridge.send("paymentStatus-topic", MessageBuilder
                .withPayload(event.getOrderRequest())
                .build()
        );
    }

    @EventListener
    public void handleTransactionCancelCommit(PaymentCancelEvent cancelEvent) {
        log.info("Received message to paymentCancelRequest-topic: " + cancelEvent.getMessage());
        streamBridge.send("paymentCancelStatus-topic", MessageBuilder
                .withPayload(cancelEvent.getCancelStatus())
                .build()
        );
    }
}