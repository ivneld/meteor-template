package com.meteor.shipping.storage;

import java.util.Optional;

import com.meteor.shipping.domain.Shipping;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 배송 저장소. 도메인 객체만 주고받고, JPA 엔티티와의 변환은 이 클래스 안에서 끝난다. 엔티티와 Spring Data 인터페이스는
 * package-private 이라 밖에서 보이지 않는다(R-08).
 */
@Repository
public class ShippingRepository {

    private final ShippingJpaRepository jpa;

    ShippingRepository(ShippingJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Shipping> findById(Long id) {
        return jpa.findById(id).map(ShippingEntity::toDomain);
    }

    public Optional<Shipping> findByOrderId(Long orderId) {
        return jpa.findByOrderId(orderId).map(ShippingEntity::toDomain);
    }

    public Shipping save(Shipping shipping) {
        ShippingEntity entity;
        if (shipping.isNew()) {
            entity = ShippingEntity.from(shipping);
        }
        else {
            entity = jpa.findById(shipping.getId())
                .orElseThrow(
                        () -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("shippingId", shipping.getId()));
            entity.apply(shipping);
        }
        return jpa.save(entity).toDomain();
    }

}
