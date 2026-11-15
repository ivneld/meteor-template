package com.meteor.domain;

import jakarta.persistence.Embeddable;

/**
 * 배송지. 애그리거트의 컬럼(city, street, zip_code)으로 펼쳐 저장되는 {@code @Embeddable} record 다.
 */
@Embeddable
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
