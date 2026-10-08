package com.meteor.payment.storage;

import com.meteor.payment.enums.PaymentMethod;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class PaymentEntity extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    protected PaymentEntity() {
    }

    public PaymentEntity(Long orderId, long amount, PaymentMethod method) {
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

}
