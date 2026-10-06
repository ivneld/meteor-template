package com.meteor.flow;

import com.meteor.ContextTest;
import com.meteor.member.application.MemberRegisterCommand;
import com.meteor.member.application.MemberUseCase;
import com.meteor.member.domain.Email;
import com.meteor.order.application.OrderPlaceCommand;
import com.meteor.order.application.OrderResult;
import com.meteor.order.application.OrderUseCase;
import com.meteor.order.domain.OrderLimitPolicy;
import com.meteor.order.domain.OrderStatus;
import com.meteor.payment.application.PaymentPayCommand;
import com.meteor.payment.application.PaymentResult;
import com.meteor.payment.application.PaymentUseCase;
import com.meteor.payment.domain.PaymentMethod;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import com.meteor.shipping.application.ShippingResult;
import com.meteor.shipping.application.ShippingUseCase;
import com.meteor.shipping.domain.ShippingStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 네 컨텍스트가 Facade 와 Event 로만 협력해 주문 한 건이 끝까지 흐르는지 확인한다.
 *
 * <pre>
 * 회원 가입 → 주문(MemberFacade 로 회원 확인, OrderLimitPolicy 로 한도 확인) → 결제(OrderFacade 로 금액 조회)
 *   → PaymentCompletedEvent → 주문 PAID (결제와 같은 트랜잭션)
 *   → 커밋 → OrderPaidEvent → 배송 READY (커밋 이후 부가 처리) → SHIPPED → DELIVERED
 * </pre>
 *
 * 결제와 주문 PAID 가 함께 롤백되는지는 {@link PaymentOrderAtomicityTest} 가 확인한다.
 */
class OrderFlowTest extends ContextTest {

    private final MemberUseCase memberUseCase;

    private final OrderUseCase orderUseCase;

    private final PaymentUseCase paymentUseCase;

    private final ShippingUseCase shippingUseCase;

    OrderFlowTest(MemberUseCase memberUseCase, OrderUseCase orderUseCase, PaymentUseCase paymentUseCase,
            ShippingUseCase shippingUseCase) {
        this.memberUseCase = memberUseCase;
        this.orderUseCase = orderUseCase;
        this.paymentUseCase = paymentUseCase;
        this.shippingUseCase = shippingUseCase;
    }

    @Test
    void paymentDrivesOrderAndShippingThroughEvents() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("flow@example.com"), "kim")).id();
        Address address = new Address("Seoul", "Teheran-ro 1", "06000");
        OrderResult order = orderUseCase
            .place(new OrderPlaceCommand(memberId, "keyboard", 2, Money.of(50_000), address));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);

        PaymentResult payment = paymentUseCase.pay(new PaymentPayCommand(order.id(), PaymentMethod.CARD));

        assertThat(payment.amount()).isEqualTo(Money.of(100_000));
        assertThat(orderUseCase.find(order.id()).status()).isEqualTo(OrderStatus.PAID);

        ShippingResult shipping = shippingUseCase.findByOrder(order.id());
        assertThat(shipping.status()).isEqualTo(ShippingStatus.READY);
        assertThat(shipping.address()).isEqualTo(address);

        shippingUseCase.ship(shipping.id());
        assertThat(shippingUseCase.deliver(shipping.id()).status()).isEqualTo(ShippingStatus.DELIVERED);
    }

    @Test
    void paidOrderCannotBePaidAgain() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("twice@example.com"), "kim")).id();
        OrderResult order = orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000),
                new Address("Seoul", "Teheran-ro 1", "06000")));
        paymentUseCase.pay(new PaymentPayCommand(order.id(), PaymentMethod.CARD));

        assertThatThrownBy(() -> paymentUseCase.pay(new PaymentPayCommand(order.id(), PaymentMethod.CARD)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_PAYABLE));
    }

    @Test
    void memberCannotHoldMoreAwaitingPaymentOrdersThanTheLimit() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("limit@example.com"), "kim")).id();
        Address address = new Address("Seoul", "Teheran-ro 1", "06000");
        for (int i = 0; i < OrderLimitPolicy.MAX_AWAITING_PAYMENT; i++) {
            orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000), address));
        }

        assertThatThrownBy(
                () -> orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000), address)))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.ORDER_LIMIT_EXCEEDED));
    }

    @Test
    void withdrawnMemberCannotOrder() {
        Long memberId = memberUseCase.register(new MemberRegisterCommand(Email.of("gone@example.com"), "kim")).id();
        memberUseCase.withdraw(memberId);

        assertThatThrownBy(() -> orderUseCase.place(new OrderPlaceCommand(memberId, "mouse", 1, Money.of(10_000),
                new Address("Seoul", "Teheran-ro 1", "06000"))))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_ACTIVE));
    }

}
