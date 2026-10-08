package com.meteor.query.application;

/**
 * 여러 컨텍스트를 한 화면에 보여주는 읽기 모델. SQL 결과를 그대로 담으므로 다른 컨텍스트의 클래스(상태 enum 포함)를 참조하지 않는다(R-07).
 * 필드가 원시 타입과 문자열뿐이라 api 가 그대로 응답으로 내보낸다.
 */
public record OrderDetailView(Long orderId, String productName, int quantity, long unitPrice, long totalAmount,
        String orderStatus, String memberName, String memberEmail, Long paidAmount, String paymentMethod,
        String shippingStatus) {
}
