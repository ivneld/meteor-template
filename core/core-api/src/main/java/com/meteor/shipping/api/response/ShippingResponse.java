package com.meteor.shipping.api.response;

import com.meteor.shared.Address;
import com.meteor.shared.ShippingStatus;
import com.meteor.shipping.application.ShippingResult;

public record ShippingResponse(Long id, Long orderId, Address address, ShippingStatus status) {

    public static ShippingResponse from(ShippingResult result) {
        return new ShippingResponse(result.id(), result.orderId(), result.address(), result.status());
    }

}
