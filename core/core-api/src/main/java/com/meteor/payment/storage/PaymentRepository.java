package com.meteor.payment.storage;

import java.util.Optional;

import com.meteor.payment.domain.Payment;

import org.springframework.stereotype.Repository;

/**
 * 결제 저장소. 도메인 객체만 주고받고, JPA 엔티티와의 변환은 이 클래스 안에서 끝난다. 결제는 승인 후 바뀌지 않으므로 신규 저장만 지원한다.
 */
@Repository
public class PaymentRepository {

    private final PaymentJpaRepository jpa;

    PaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Payment> findById(Long id) {
        return jpa.findById(id).map(PaymentEntity::toDomain);
    }

    public Payment save(Payment payment) {
        if (!payment.isNew()) {
            throw new IllegalStateException("payment is immutable once approved: " + payment.getId());
        }
        return jpa.save(PaymentEntity.from(payment)).toDomain();
    }

}
