package com.meteor.payment.storage;

import java.util.Optional;

import com.meteor.payment.domain.Payment;

import org.springframework.stereotype.Repository;

/**
 * 결제는 승인 후 바뀌지 않으므로 신규 저장만 지원한다.
 */
@Repository
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpa.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        if (!payment.isNew()) {
            throw new IllegalStateException("payment is immutable once approved: " + payment.getId());
        }
        return jpa.save(PaymentEntity.from(payment)).toDomain();
    }

}
