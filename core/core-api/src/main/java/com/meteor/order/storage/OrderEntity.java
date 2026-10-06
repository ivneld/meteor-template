package com.meteor.order.storage;

import com.meteor.order.domain.Order;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import com.meteor.shared.OrderStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 주문 영속 모델. VO(Money, Address)는 컬럼으로 펼쳐 저장하고 복원 시 다시 조립한다. 회원 테이블과 FK 를 걸지 않는다(S-06).
 */
@Entity
@Table(name = "orders")
class OrderEntity extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    protected OrderEntity() {
    }

    static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.memberId = order.getMemberId();
        entity.productName = order.getProductName();
        entity.quantity = order.getQuantity();
        entity.unitPrice = order.getUnitPrice().amount();
        entity.city = order.getShippingAddress().city();
        entity.street = order.getShippingAddress().street();
        entity.zipCode = order.getShippingAddress().zipCode();
        entity.status = order.getStatus();
        return entity;
    }

    void apply(Order order) {
        this.status = order.getStatus();
    }

    Order toDomain() {
        return Order.restore(getId(), memberId, productName, quantity, Money.of(unitPrice),
                new Address(city, street, zipCode), status);
    }

}
