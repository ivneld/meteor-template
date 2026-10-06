package com.meteor.payment.storage;

import com.meteor.payment.domain.Payment;
import com.meteor.shared.Money;
import com.meteor.shared.PaymentMethod;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
class PaymentEntity extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    protected PaymentEntity() {
    }

    static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.orderId = payment.getOrderId();
        entity.amount = payment.getAmount().amount();
        entity.method = payment.getMethod();
        return entity;
    }

    Payment toDomain() {
        return Payment.restore(getId(), orderId, Money.of(amount), method);
    }

}
