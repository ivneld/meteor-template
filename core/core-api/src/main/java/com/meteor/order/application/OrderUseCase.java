package com.meteor.order.application;

import com.meteor.member.application.MemberFacade;
import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderLimitPolicy;
import com.meteor.order.repository.OrderRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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

    /** 애그리거트와 Policy 의 계산 결과를 함께 돌려줘야 하므로 전용 결과 모델을 쓴다. */
    @Transactional
    public OrderPlacementResult place(OrderPlaceCommand command) {
        memberFacade.ensureActive(command.memberId());
        long awaiting = orderRepository.countByMemberIdAndStatus(command.memberId(), OrderLimitPolicy.COUNTED_STATUS);
        OrderLimitPolicy.ensureCanPlace(command.memberId(), awaiting);
        Order order = Order.place(command.memberId(), command.productName(), command.quantity(), command.unitPrice(),
                command.shippingAddress());
        Order saved = orderRepository.save(order);
        return new OrderPlacementResult(saved, OrderLimitPolicy.remainingSlots(awaiting + 1));
    }

    @Transactional(readOnly = true)
    public Order find(Long orderId) {
        return load(orderId);
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    /**
     * 결제 완료 이벤트 리스너가 결제 트랜잭션 안에서 호출한다. 새 트랜잭션을 열지 않고 결제 트랜잭션에 참여하므로 결제와 주문 PAID 는 함께
     * 커밋되거나 함께 롤백된다(S-05).
     */
    @Transactional
    public Order markPaid(Long orderId) {
        Order order = load(orderId);
        order.markPaid();
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderPaidEvent(saved.getId(), saved.getShippingAddress()));
        return saved;
    }

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId));
    }

}
