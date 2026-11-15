package com.meteor.domain;

import com.meteor.enums.PaymentMethod;
import com.meteor.enums.PaymentStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 결제 애그리거트이자 JPA 엔티티. 주문은 orderId 로만 참조한다. 상태 기계: REQUESTED → APPROVED | FAILED.
 *
 * <p>
 * PG 호출 전에 REQUESTED 로 먼저 저장하므로, PG 승인 뒤 처리가 실패해도 "요청은 했지만 확정되지 않은 결제"가 남아 대사(reconcile)할
 * 수 있다. 트랜잭션을 나누는 유스케이스는 이렇게 중간 상태가 의미를 갖도록 설계한다.
 */
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long orderId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String pgTransactionKey;

    protected Payment() {
    }

    private Payment(Long orderId, Money amount, PaymentMethod method, PaymentStatus status) {
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public static Payment request(Long orderId, Money amount, PaymentMethod method) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        return new Payment(orderId, amount, method, PaymentStatus.REQUESTED);
    }

    public void approve(String pgTransactionKey) {
        ensureRequested();
        this.status = PaymentStatus.APPROVED;
        this.pgTransactionKey = pgTransactionKey;
    }

    public void fail() {
        ensureRequested();
        this.status = PaymentStatus.FAILED;
    }

    /** PG 에 넘기는 멱등 키. 같은 결제는 같은 키로 요청한다. 저장된 뒤에만 의미가 있다. */
    public String paymentKey() {
        return String.valueOf(getId());
    }

    private void ensureRequested() {
        if (status != PaymentStatus.REQUESTED) {
            throw new CoreException(ErrorCode.PAYMENT_NOT_REQUESTED).property("paymentId", getId())
                .property("paymentStatus", status);
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPgTransactionKey() {
        return pgTransactionKey;
    }

}
