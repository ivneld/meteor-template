package com.meteor.order.api.response;

import com.meteor.order.domain.Order;
import com.meteor.order.enums.OrderStatus;
import com.meteor.shared.Address;

/**
 * 응답 DTO. 도메인 타입은 enum 만 쓰고 값 객체(Money, Address)는 원시 타입과 api 전용 record 로 푼다(R-11).
 */
public record OrderResponse(Long id, Long memberId, String productName, int quantity, long unitPrice, long totalAmount,
        AddressResponse shippingAddress, OrderStatus status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getMemberId(), order.getProductName(), order.getQuantity(),
                order.getUnitPrice().amount(), order.totalAmount().amount(),
                AddressResponse.from(order.getShippingAddress()), order.getStatus());
    }

    public record AddressResponse(String city, String street, String zipCode) {

        static AddressResponse from(Address address) {
            return new AddressResponse(address.city(), address.street(), address.zipCode());
        }

    }

}
