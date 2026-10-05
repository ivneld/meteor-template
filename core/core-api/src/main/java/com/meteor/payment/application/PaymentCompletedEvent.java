package com.meteor.payment.application;

/**
 * 결제가 승인됐음을 알리는 이벤트. 주문 컨텍스트가 받아 주문을 PAID 로 바꾼다.
 */
public record PaymentCompletedEvent(Long orderId, Long paymentId) {
}
