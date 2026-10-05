package com.meteor.shipping.storage;

import java.util.Optional;

import com.meteor.shipping.domain.Shipping;

public interface ShippingRepository {

    Optional<Shipping> findById(Long id);

    Optional<Shipping> findByOrderId(Long orderId);

    Shipping save(Shipping shipping);

}
