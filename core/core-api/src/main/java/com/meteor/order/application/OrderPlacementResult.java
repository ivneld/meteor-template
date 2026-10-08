package com.meteor.order.application;

import com.meteor.order.domain.Order;

/**
 * 유스케이스 전용 결과 모델의 예. 애그리거트만으로는 표현할 수 없는 계산 결과(남은 주문 가능 건수)를 함께 돌려줄 때 만든다. 애그리거트 하나로 충분한
 * 조회·전이는 애그리거트를 그대로 돌려준다.
 */
public record OrderPlacementResult(Order order, long remainingAwaitingPaymentOrders) {
}
