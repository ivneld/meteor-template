package com.meteor.shipping.application;

import com.meteor.shared.Address;
import com.meteor.shared.ShippingStatus;
import com.meteor.shipping.domain.Shipping;

public record ShippingResult(Long id, Long orderId, Address address, ShippingStatus status) {

    static ShippingResult from(Shipping shipping) {
        return new ShippingResult(shipping.getId(), shipping.getOrderId(), shipping.getAddress(), shipping.getStatus());
    }

}
