package com.meteor.shipping.storage;

import java.util.Optional;

import com.meteor.shipping.domain.Shipping;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

@Repository
class ShippingRepositoryAdapter implements ShippingRepository {

    private final ShippingJpaRepository jpa;

    ShippingRepositoryAdapter(ShippingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Shipping> findById(Long id) {
        return jpa.findById(id).map(ShippingEntity::toDomain);
    }

    @Override
    public Optional<Shipping> findByOrderId(Long orderId) {
        return jpa.findByOrderId(orderId).map(ShippingEntity::toDomain);
    }

    @Override
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
