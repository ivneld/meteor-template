---
id: R-19
title: api 는 값 객체를 만들고 읽기만 한다
kind: 구조
applies_to: com.meteor.api (request, response 포함)
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R19_api_does_not_compute_with_value_objects]
related: [R-10, R-12]
human_doc: docs/architecture/api.md#R-19
---

# R-19 api 는 값 객체를 만들고 읽기만 한다

## 규칙

api 가 domain 의 값 객체(record)에서 부를 수 있는 것은 정적 팩토리 `of`·`from`, 생성자, 구성 요소 접근자(`amount()`, `city()`), `equals`/`hashCode`/`toString` 뿐이다. `plus`, `times`, `isZero` 같은 연산은 부르지 않는다. 이전의 의미 규칙 S-03 을 구조 규칙으로 옮긴 것이다.

## 왜

금액 계산 같은 연산이 컨트롤러나 DTO 로 새면 같은 규칙이 도메인과 api 두 곳에 생긴다. 계산은 애그리거트가 하고(`Order.totalAmount()`), api 는 결과를 읽는다.

## 이렇게 한다

```java
// 요청 → 값 객체 생성, 응답 ← 구성 요소 읽기
new OrderPlaceCommand(memberId, productName, quantity, Money.of(unitPrice), shippingAddress.toAddress());
order.totalAmount().amount();
```

## 이렇게 하지 않는다

```java
long total = order.getUnitPrice().times(order.getQuantity()).amount();   // 계산은 Order.totalAmount 에
```

## 고치는 법

- 연산을 애그리거트 메서드로 만들고(`totalAmount()`) api 는 그 결과의 구성 요소만 읽는다.
