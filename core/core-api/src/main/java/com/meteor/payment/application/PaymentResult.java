package com.meteor.payment.application;

import com.meteor.payment.domain.Payment;
import com.meteor.payment.domain.PaymentMethod;
import com.meteor.shared.Money;

public record PaymentResult(Long id, Long orderId, Money amount, PaymentMethod method) {

    static PaymentResult from(Payment payment) {
        return new PaymentResult(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getMethod());
    }

}
