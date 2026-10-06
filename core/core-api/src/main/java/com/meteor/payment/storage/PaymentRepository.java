package com.meteor.payment.storage;

import java.util.Optional;

import com.meteor.payment.domain.Payment;

public interface PaymentRepository {

    Optional<Payment> findById(Long id);

    Payment save(Payment payment);

}
