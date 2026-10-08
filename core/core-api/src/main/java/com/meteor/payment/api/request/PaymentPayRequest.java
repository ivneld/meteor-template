package com.meteor.payment.api.request;

import com.meteor.payment.application.PaymentPayCommand;
import com.meteor.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentPayRequest(@NotNull Long orderId, @NotNull PaymentMethod method) {

    public PaymentPayCommand toCommand() {
        return new PaymentPayCommand(orderId, method);
    }

}
