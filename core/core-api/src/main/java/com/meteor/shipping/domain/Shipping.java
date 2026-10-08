package com.meteor.shipping.domain;

import com.meteor.shared.Address;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.shipping.enums.ShippingStatus;

/**
 * 배송 애그리거트. 주문은 orderId 로만 참조하고, 배송지는 OrderPaidEvent 로 받은 값이다. 상태 기계: READY → SHIPPED →
 * DELIVERED.
 */
public class Shipping {

    private final Long id;

    private final Long orderId;

    private final Address address;

    private ShippingStatus status;

    private Shipping(Long id, Long orderId, Address address, ShippingStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.address = address;
        this.status = status;
    }

    public static Shipping prepare(Long orderId, Address address) {
        return new Shipping(null, orderId, address, ShippingStatus.READY);
    }

    /** 저장소에서 복원한다. storage 의 *Repository 만 호출한다. */
    public static Shipping restore(Long id, Long orderId, Address address, ShippingStatus status) {
        return new Shipping(id, orderId, address, status);
    }

    public void ship() {
        transition(ShippingStatus.READY, ShippingStatus.SHIPPED);
    }

    public void deliver() {
        transition(ShippingStatus.SHIPPED, ShippingStatus.DELIVERED);
    }

    private void transition(ShippingStatus from, ShippingStatus to) {
        if (status != from) {
            throw new CoreException(ErrorCode.SHIPPING_INVALID_TRANSITION).property("shippingId", id)
                .property("from", status)
                .property("to", to);
        }
        status = to;
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

    public Address getAddress() {
        return address;
    }

    public ShippingStatus getStatus() {
        return status;
    }

}
