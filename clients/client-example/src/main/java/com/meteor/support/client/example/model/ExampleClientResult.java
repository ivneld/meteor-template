package com.meteor.support.client.example.model;

/**
 * 외부 응답 DTO 를 그대로 노출하지 않고, 호출 측이 필요로 하는 형태로 변환한 결과.
 */
public record ExampleClientResult(String id, String message) {
}
