package com.meteor.payment.domain;

import com.meteor.shared.Money;

/**
 * 결제 애그리거트. 주문은 orderId 로만 참조한다. 승인 금액은 주문 컨텍스트가 OrderFacade 로 알려준 값이다.
 */
public class Payment {

    private final Long id;

    private final Long orderId;

    private final Money amount;

    private final PaymentMethod method;

    private Payment(Long id, Long orderId, Money amount, PaymentMethod method) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
    }

    public static Payment approve(Long orderId, Money amount, PaymentMethod method) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        return new Payment(null, orderId, amount, method);
    }

    /** 저장소에서 복원한다. storage 어댑터만 호출한다. */
    public static Payment restore(Long id, Long orderId, Money amount, PaymentMethod method) {
        return new Payment(id, orderId, amount, method);
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
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

}
