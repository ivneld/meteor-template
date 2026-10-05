package com.meteor.order.application;

import com.meteor.shared.Address;
import com.meteor.shared.Money;

public record OrderPlaceCommand(Long memberId, String productName, int quantity, Money unitPrice,
        Address shippingAddress) {
}
