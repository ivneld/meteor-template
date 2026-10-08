package com.meteor.payment.repository;

import java.util.Optional;

import com.meteor.payment.domain.Payment;
import com.meteor.payment.domain.PaymentMethod;
import com.meteor.payment.storage.PaymentEntity;
import com.meteor.payment.storage.PaymentJpaRepository;
import com.meteor.shared.Money;

import org.springframework.stereotype.Repository;

/**
 * 결제 저장소 어댑터. 결제는 승인 후 바뀌지 않으므로 신규 저장만 지원한다.
 */
@Repository
public class PaymentRepository {

    private final PaymentJpaRepository jpa;

    PaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Payment> findById(Long id) {
        return jpa.findById(id).map(PaymentRepository::toDomain);
    }

    public Payment save(Payment payment) {
        if (!payment.isNew()) {
            throw new IllegalStateException("payment is immutable once approved: " + payment.getId());
        }
        PaymentEntity entity = new PaymentEntity(payment.getOrderId(), payment.getAmount().amount(),
                payment.getMethod().name());
        return toDomain(jpa.save(entity));
    }

    private static Payment toDomain(PaymentEntity entity) {
        return Payment.restore(entity.getId(), entity.getOrderId(), Money.of(entity.getAmount()),
                PaymentMethod.valueOf(entity.getMethod()));
    }

}
