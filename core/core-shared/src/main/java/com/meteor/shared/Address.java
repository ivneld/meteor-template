package com.meteor.shared;

/**
 * 배송지. 주문이 받아 배송에 넘기므로 두 컨텍스트가 공유한다.
 */
public record Address(String city, String street, String zipCode) {

    public Address {
        requireText(city, "city");
        requireText(street, "street");
        requireText(zipCode, "zipCode");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

}
