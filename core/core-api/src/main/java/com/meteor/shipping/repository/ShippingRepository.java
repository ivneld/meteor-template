package com.meteor.shipping.repository;

import java.util.Optional;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.storage.ShippingEntity;
import com.meteor.shipping.storage.ShippingJpaRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 배송 저장소 어댑터. storage 모듈의 JPA 엔티티를 이 모듈의 애그리거트로 바꾸고, 그 반대도 한다.
 */
@Repository
public class ShippingRepository {

    private final ShippingJpaRepository jpa;

    ShippingRepository(ShippingJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Shipping> findById(Long id) {
        return jpa.findById(id).map(ShippingRepository::toDomain);
    }

    public Optional<Shipping> findByOrderId(Long orderId) {
        return jpa.findByOrderId(orderId).map(ShippingRepository::toDomain);
    }

    public Shipping save(Shipping shipping) {
        ShippingEntity entity;
        if (shipping.isNew()) {
            Address address = shipping.getAddress();
            entity = new ShippingEntity(shipping.getOrderId(), address.city(), address.street(), address.zipCode(),
                    shipping.getStatus());
        }
        else {
            entity = jpa.findById(shipping.getId())
                .orElseThrow(
                        () -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("shippingId", shipping.getId()));
            entity.changeStatus(shipping.getStatus());
        }
        return toDomain(jpa.save(entity));
    }

    private static Shipping toDomain(ShippingEntity entity) {
        return Shipping.restore(entity.getId(), entity.getOrderId(),
                new Address(entity.getCity(), entity.getStreet(), entity.getZipCode()), entity.getStatus());
    }

}
