---
id: R-12
title: API 요청·응답 객체는 도메인 타입을 필드로 갖지 않는다
kind: 구조
applies_to: com.meteor.api.request, com.meteor.api.response
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R12_api_dtos_carry_no_domain_types]
related: [R-10, R-19]
human_doc: docs/architecture/api.md#R-12
---

# R-12 API 요청·응답 객체는 도메인 타입을 필드로 갖지 않는다

## 규칙

- `*Request`·`*Response` 의 필드는 `com.meteor.domain` 타입(애그리거트, 값 객체)이 아니다. 값 객체는 원시 타입이나 api 전용 record(`AddressResponse`)로 푼다.
- enum 은 core-enum(`com.meteor.enums`)의 것이라 그대로 쓴다.
- 변환 메서드(`toCommand()`, `from(Order)`)에서 도메인 타입을 다루는 것은 괜찮다. 필드만 금지다.

## 왜

API 계약이 도메인 모델에 묶이면 둘 중 하나를 바꿀 수 없다. 필드 이름 하나를 바꿔도 클라이언트가 깨지거나, 클라이언트 때문에 도메인 이름을 못 바꾼다.

## 이렇게 한다

```java
public record OrderResponse(Long id, long unitPrice, AddressResponse shippingAddress, OrderStatus status) {
    public static OrderResponse from(Order order) { ... }
}
```

## 이렇게 하지 않는다

```java
public record OrderResponse(Long id, Money unitPrice, Address shippingAddress) { }   // 도메인 타입 필드
// 또는 컨트롤러가 Order 를 그대로 반환
```

## 고치는 법

- 값 객체 필드는 원시 타입(`long unitPrice`)이나 api 전용 중첩 record 로 바꾸고 `from(...)` 에서 접근자로 읽어 채운다.
