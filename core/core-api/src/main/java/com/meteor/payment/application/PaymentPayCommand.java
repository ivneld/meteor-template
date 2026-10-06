package com.meteor.payment.application;

import com.meteor.payment.domain.PaymentMethod;

public record PaymentPayCommand(Long orderId, PaymentMethod method) {
}
