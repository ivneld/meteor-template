package com.meteor.shipping.application;

import com.meteor.shared.Address;
import com.meteor.shipping.domain.Shipping;
import com.meteor.shipping.storage.ShippingRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shipping 컨텍스트의 유스케이스. 트랜잭션 경계와 흐름만 맡고, 결과로는 애그리거트를 그대로 돌려준다. 규칙은 애그리거트와 *Policy 에 있다.
 */
@Service
public class ShippingUseCase {

    private final ShippingRepository shippingRepository;

    public ShippingUseCase(ShippingRepository shippingRepository) {
        this.shippingRepository = shippingRepository;
    }

    /** 주문 결제 완료 이벤트 리스너가 호출한다. 발행 측 트랜잭션이 이미 커밋된 뒤라 새 트랜잭션을 연다(S-07). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Shipping prepare(Long orderId, Address address) {
        return shippingRepository.save(Shipping.prepare(orderId, address));
    }

    @Transactional(readOnly = true)
    public Shipping find(Long shippingId) {
        return load(shippingId);
    }

    @Transactional(readOnly = true)
    public Shipping findByOrder(Long orderId) {
        return shippingRepository.findByOrderId(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("orderId", orderId));
    }

    @Transactional
    public Shipping ship(Long shippingId) {
        Shipping shipping = load(shippingId);
        shipping.ship();
        return shippingRepository.save(shipping);
    }

    @Transactional
    public Shipping deliver(Long shippingId) {
        Shipping shipping = load(shippingId);
        shipping.deliver();
        return shippingRepository.save(shipping);
    }

    private Shipping load(Long shippingId) {
        return shippingRepository.findById(shippingId)
            .orElseThrow(() -> new CoreException(ErrorCode.SHIPPING_NOT_FOUND).property("shippingId", shippingId));
    }

}
