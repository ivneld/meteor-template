package com.meteor.shipping.api.response;

import com.meteor.shared.Address;
import com.meteor.shipping.application.ShippingResult;
import com.meteor.shipping.domain.ShippingStatus;

public record ShippingResponse(Long id, Long orderId, Address address, ShippingStatus status) {

    public static ShippingResponse from(ShippingResult result) {
        return new ShippingResponse(result.id(), result.orderId(), result.address(), result.status());
    }

}
