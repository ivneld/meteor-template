package com.meteor.payment.application;

import com.meteor.shared.PaymentMethod;

public record PaymentPayCommand(Long orderId, PaymentMethod method) {
}
