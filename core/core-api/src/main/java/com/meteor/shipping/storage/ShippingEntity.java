package com.meteor.shipping.storage;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.domain.ShippingStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipping")
class ShippingEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShippingStatus status;

    protected ShippingEntity() {
    }

    static ShippingEntity from(Shipping shipping) {
        ShippingEntity entity = new ShippingEntity();
        entity.orderId = shipping.getOrderId();
        entity.city = shipping.getAddress().city();
        entity.street = shipping.getAddress().street();
        entity.zipCode = shipping.getAddress().zipCode();
        entity.status = shipping.getStatus();
        return entity;
    }

    void apply(Shipping shipping) {
        this.status = shipping.getStatus();
    }

    Shipping toDomain() {
        return Shipping.restore(getId(), orderId, new Address(city, street, zipCode), status);
    }

}
