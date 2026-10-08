package com.meteor.order.domain;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.order.enums.OrderStatus;

/**
 * 회원 한 명이 결제 대기(CREATED) 주문을 몇 건까지 가질 수 있는지 정한다.
 *
 * <p>
 * 주문 한 건(애그리거트)은 다른 주문의 존재를 모르므로 이 규칙을 가질 수 없다. 여러 애그리거트에 걸친 규칙은 UseCase 에 if 로 흩어지기 쉬우므로
 * {@code *Policy} 로 이름을 붙여 domain 에 둔다(S-11). UseCase 는 필요한 사실(건수)을 저장소에서 꺼내 넘기기만 하고, 무엇을
 * 세는지({@link #COUNTED_STATUS})와 몇 건까지인지({@link #MAX_AWAITING_PAYMENT})는 모두 여기서 정한다.
 */
public final class OrderLimitPolicy {

    /** 회원 한 명이 동시에 가질 수 있는 결제 대기 주문 수. */
    public static final int MAX_AWAITING_PAYMENT = 3;

    /** 한도 계산에 세는 주문 상태. 저장소 조회 조건으로 쓴다. */
    public static final OrderStatus COUNTED_STATUS = OrderStatus.CREATED;

    private OrderLimitPolicy() {
    }

    /** 지금 결제 대기 건수가 주어졌을 때 앞으로 더 넣을 수 있는 주문 수. */
    public static long remainingSlots(long awaitingPaymentOrders) {
        return Math.max(0, MAX_AWAITING_PAYMENT - awaitingPaymentOrders);
    }

    public static void ensureCanPlace(Long memberId, long awaitingPaymentOrders) {
        if (awaitingPaymentOrders >= MAX_AWAITING_PAYMENT) {
            throw new CoreException(ErrorCode.ORDER_LIMIT_EXCEEDED).property("memberId", memberId)
                .property("limit", MAX_AWAITING_PAYMENT);
        }
    }

}
