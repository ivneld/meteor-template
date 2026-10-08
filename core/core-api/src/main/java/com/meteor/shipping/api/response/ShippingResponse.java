package com.meteor.shipping.api.response;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.domain.ShippingStatus;

public record ShippingResponse(Long id, Long orderId, AddressResponse address, ShippingStatus status) {

    public static ShippingResponse from(Shipping shipping) {
        return new ShippingResponse(shipping.getId(), shipping.getOrderId(),
                AddressResponse.from(shipping.getAddress()), shipping.getStatus());
    }

    public record AddressResponse(String city, String street, String zipCode) {

        static AddressResponse from(Address address) {
            return new AddressResponse(address.city(), address.street(), address.zipCode());
        }

    }

}
