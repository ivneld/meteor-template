package com.meteor.order.application;

import com.meteor.member.application.MemberFacade;
import com.meteor.order.domain.Order;
import com.meteor.order.storage.OrderRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 유스케이스. 회원 컨텍스트는 MemberFacade 로만, 결제·배송 컨텍스트는 이벤트로만 만난다.
 */
@Service
public class OrderUseCase {

    private final OrderRepository orderRepository;

    private final MemberFacade memberFacade;

    private final ApplicationEventPublisher eventPublisher;

    public OrderUseCase(OrderRepository orderRepository, MemberFacade memberFacade,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.memberFacade = memberFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResult place(OrderPlaceCommand command) {
        memberFacade.ensureActive(command.memberId());
        Order order = Order.place(command.memberId(), command.productName(), command.quantity(), command.unitPrice(),
                command.shippingAddress());
        return OrderResult.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResult find(Long orderId) {
        return OrderResult.from(load(orderId));
    }

    @Transactional
    public OrderResult cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        return OrderResult.from(orderRepository.save(order));
    }

    /**
     * 결제 완료 이벤트 리스너가 호출한다. 리스너는 결제 트랜잭션이 커밋된 뒤 실행되므로 새 트랜잭션을 열어야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderResult markPaid(Long orderId) {
        Order order = load(orderId);
        order.markPaid();
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderPaidEvent(saved.getId(), saved.getShippingAddress()));
        return OrderResult.from(saved);
    }

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
    }

}
