package com.meteor.domain;

import com.meteor.enums.OrderStatus;
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
 * 주문 애그리거트이자 JPA 엔티티. 회원은 memberId 로만 참조하고 연관관계를 걸지 않는다(R-14). 상태 전이 규칙은 모두 여기 있다.
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false))
    private Money unitPrice;

    @Embedded
    private Address shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    protected Order() {
    }

    private Order(Long memberId, String productName, int quantity, Money unitPrice, Address shippingAddress,
            OrderStatus status) {
        this.memberId = memberId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.shippingAddress = shippingAddress;
        this.status = status;
    }

    public static Order place(Long memberId, String productName, int quantity, Money unitPrice,
            Address shippingAddress) {
        if (quantity < 1) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "quantity must be at least 1");
        }
        return new Order(memberId, productName, quantity, unitPrice, shippingAddress, OrderStatus.CREATED);
    }

    public Money totalAmount() {
        return unitPrice.times(quantity);
    }

    /** 결제할 금액. 결제 가능한 상태가 아니면 거절한다. */
    public Money payableAmount() {
        ensurePayable();
        return totalAmount();
    }

    public void markPaid() {
        ensurePayable();
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", getId())
                .property("orderStatus", status);
        }
        status = OrderStatus.CANCELLED;
    }

    private void ensurePayable() {
        if (status != OrderStatus.CREATED) {
            throw new CoreException(ErrorCode.ORDER_NOT_PAYABLE).property("orderId", getId())
                .property("orderStatus", status);
        }
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

}
