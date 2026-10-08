package com.meteor.payment.api.response;

import com.meteor.payment.domain.Payment;
import com.meteor.payment.enums.PaymentMethod;

public record PaymentResponse(Long id, Long orderId, long amount, PaymentMethod method) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount().amount(),
                payment.getMethod());
    }

}
