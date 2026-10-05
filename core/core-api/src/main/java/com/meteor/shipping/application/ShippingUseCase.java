package com.meteor.shipping.application;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.storage.ShippingRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingUseCase {

    private final ShippingRepository shippingRepository;

    public ShippingUseCase(ShippingRepository shippingRepository) {
        this.shippingRepository = shippingRepository;
    }

    /** 주문 결제 완료 이벤트 리스너가 호출한다. 커밋 이후에 실행되므로 새 트랜잭션을 연다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShippingResult prepare(Long orderId, Address address) {
        return ShippingResult.from(shippingRepository.save(Shipping.prepare(orderId, address)));
    }

    @Transactional(readOnly = true)
    public ShippingResult find(Long shippingId) {
        return ShippingResult.from(load(shippingId));
    }

    @Transactional(readOnly = true)
    public ShippingResult findByOrder(Long orderId) {
        return ShippingResult.from(shippingRepository.findByOrderId(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("orderId", orderId)));
    }

    @Transactional
    public ShippingResult ship(Long shippingId) {
        Shipping shipping = load(shippingId);
        shipping.ship();
        return ShippingResult.from(shippingRepository.save(shipping));
    }

    @Transactional
    public ShippingResult deliver(Long shippingId) {
        Shipping shipping = load(shippingId);
        shipping.deliver();
        return ShippingResult.from(shippingRepository.save(shipping));
    }

    private Shipping load(Long shippingId) {
        return shippingRepository.findById(shippingId)
            .orElseThrow(() -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("shippingId", shippingId));
    }

}
