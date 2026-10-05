package com.meteor.order.application;

import com.meteor.shared.Address;

/**
 * 주문이 결제 완료 상태가 됐음을 알리는 이벤트. 배송 컨텍스트가 받는다. 수신 측이 필요한 값(배송지)만 싣는다. 주문 전체를 실어 나르면 경계가 흐려진다.
 */
public record OrderPaidEvent(Long orderId, Address shippingAddress) {
}
