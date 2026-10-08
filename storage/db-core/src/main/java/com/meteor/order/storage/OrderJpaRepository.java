package com.meteor.order.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    long countByMemberIdAndStatus(Long memberId, String status);

}
