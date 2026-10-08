package com.meteor.flow;

import com.meteor.ContextTest;
import com.meteor.member.application.MemberRegisterCommand;
import com.meteor.member.application.MemberUseCase;
import com.meteor.member.domain.Email;
import com.meteor.order.application.OrderPaidEvent;
import com.meteor.order.application.OrderPlaceCommand;
import com.meteor.order.application.OrderUseCase;
import com.meteor.order.domain.OrderStatus;
import com.meteor.payment.application.PaymentPayCommand;
import com.meteor.payment.application.PaymentUseCase;
import com.meteor.payment.domain.PaymentMethod;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import com.meteor.shipping.application.ShippingUseCase;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 결제 승인과 주문 PAID 는 같은 트랜잭션이다(S-05). 주문이 PAID 로 바뀐 뒤 같은 트랜잭션 안의 처리가 실패하면 결제도, 주문 상태도 남지
 * 않아야 하고, 커밋 이후 부가 처리(배송 준비)는 시작되지 않아야 한다.
 */
@Import(PaymentOrderAtomicityTest.FailAfterOrderPaid.class)
class PaymentOrderAtomicityTest extends ContextTest {

    private final MemberUseCase memberUseCase;

    private final OrderUseCase orderUseCase;

    private final PaymentUseCase paymentUseCase;

    private final ShippingUseCase shippingUseCase;

    private final EntityManager entityManager;

    PaymentOrderAtomicityTest(MemberUseCase memberUseCase, OrderUseCase orderUseCase, PaymentUseCase paymentUseCase,
            ShippingUseCase shippingUseCase, EntityManager entityManager) {
        this.memberUseCase = memberUseCase;
        this.orderUseCase = orderUseCase;
        this.paymentUseCase = paymentUseCase;
        this.shippingUseCase = shippingUseCase;
        this.entityManager = entityManager;
    }

    @Test
    void failureAfterOrderPaidRollsBackPaymentToo() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("atomic@example.com"), "kim"))
            .getId();
        Long orderId = orderUseCase
            .place(new OrderPlaceCommand(memberId, "keyboard", 1, Money.of(10_000),
                    new Address("Seoul", "Teheran-ro 1", "06000")))
            .getId();

        assertThatThrownBy(() -> paymentUseCase.pay(new PaymentPayCommand(orderId, PaymentMethod.CARD)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(orderUseCase.find(orderId).getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(paymentCount(orderId)).isZero();
        assertThatThrownBy(() -> shippingUseCase.findByOrder(orderId)).isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.SHIPPING_NOT_FOUND));
    }

    private long paymentCount(Long orderId) {
        return ((Number) entityManager.createNativeQuery("select count(*) from payment where order_id = ?1")
            .setParameter(1, orderId)
            .getSingleResult()).longValue();
    }

    /** 주문 PAID 직후, 결제 트랜잭션 안에서 실패하는 후속 처리를 흉내 낸다. */
    static class FailAfterOrderPaid {

        @EventListener
        public void on(OrderPaidEvent event) {
            throw new IllegalStateException("failure inside the payment transaction after the order became PAID");
        }

    }

}
