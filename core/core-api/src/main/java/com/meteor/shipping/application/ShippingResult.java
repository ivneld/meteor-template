package com.meteor.shipping.application;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.domain.ShippingStatus;

public record ShippingResult(Long id, Long orderId, Address address, ShippingStatus status) {

    static ShippingResult from(Shipping shipping) {
        return new ShippingResult(shipping.getId(), shipping.getOrderId(), shipping.getAddress(), shipping.getStatus());
    }

}
