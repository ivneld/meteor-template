package com.meteor.payment.api.response;

import com.meteor.payment.application.PaymentResult;
import com.meteor.shared.PaymentMethod;

public record PaymentResponse(Long id, Long orderId, long amount, PaymentMethod method) {

    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(result.id(), result.orderId(), result.amount().amount(), result.method());
    }

}
