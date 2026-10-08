package com.meteor.payment.application;

import com.meteor.order.application.OrderFacade;
import com.meteor.payment.domain.Payment;
import com.meteor.payment.storage.PaymentRepository;
import com.meteor.shared.Money;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 유스케이스. 결제할 금액은 OrderFacade 에 묻고, 승인 결과는 이벤트로 알린다. 주문 테이블은 건드리지 않는다(S-05).
 */
@Service
public class PaymentUseCase {

    private final PaymentRepository paymentRepository;

    private final OrderFacade orderFacade;

    private final ApplicationEventPublisher eventPublisher;

    public PaymentUseCase(PaymentRepository paymentRepository, OrderFacade orderFacade,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderFacade = orderFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment pay(PaymentPayCommand command) {
        Money amount = orderFacade.payableAmount(command.orderId());
        Payment saved = paymentRepository.save(Payment.approve(command.orderId(), amount, command.method()));
        eventPublisher.publishEvent(new PaymentCompletedEvent(saved.getOrderId(), saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Payment find(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_NOT_FOUND).property("paymentId", paymentId));
    }

}
