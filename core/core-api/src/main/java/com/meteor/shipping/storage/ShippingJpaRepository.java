package com.meteor.shipping.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ShippingJpaRepository extends JpaRepository<ShippingEntity, Long> {

    Optional<ShippingEntity> findByOrderId(Long orderId);

}
