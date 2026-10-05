package com.meteor.payment.api;

import com.meteor.payment.api.request.PaymentPayRequest;
import com.meteor.payment.api.response.PaymentResponse;
import com.meteor.payment.application.PaymentUseCase;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping
    public PaymentResponse pay(@RequestBody @Valid PaymentPayRequest request) {
        return PaymentResponse.from(paymentUseCase.pay(request.toCommand()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable Long paymentId) {
        return PaymentResponse.from(paymentUseCase.find(paymentId));
    }

}
