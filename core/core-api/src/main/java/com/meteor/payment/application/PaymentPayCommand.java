package com.meteor.payment.application;

import com.meteor.payment.enums.PaymentMethod;

public record PaymentPayCommand(Long orderId, PaymentMethod method) {
}
