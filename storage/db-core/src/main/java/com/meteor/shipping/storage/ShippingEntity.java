package com.meteor.shipping.storage;

import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipping")
public class ShippingEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 20)
    private String status;

    protected ShippingEntity() {
    }

    public ShippingEntity(Long orderId, String city, String street, String zipCode, String status) {
        this.orderId = orderId;
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getStatus() {
        return status;
    }

}
