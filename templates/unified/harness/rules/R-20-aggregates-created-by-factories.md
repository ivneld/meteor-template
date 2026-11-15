---
id: R-20
title: 애그리거트는 자기 정적 팩토리로만 만든다
kind: 구조
applies_to: com.meteor.domain 의 애그리거트 생성자
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R20_aggregates_have_no_public_constructors, R20_aggregates_are_constructed_only_by_themselves]
related: [R-15, R-21]
human_doc: docs/architecture/domain-model.md#R-20
---

# R-20 애그리거트는 자기 정적 팩토리로만 만든다

## 규칙

- 애그리거트는 public 생성자를 갖지 않는다. JPA 가 요구하는 기본 생성자는 `protected` 로 둔다.
- 애그리거트의 생성자는 그 애그리거트 안에서만 부른다. 밖에서는 규칙을 가진 정적 팩토리(`Order.place`, `Member.register`, `Payment.request`)를 쓴다. `protected` 생성자는 같은 패키지(domain)에서 부를 수 있으므로 이 규칙이 막는다.

## 왜

생성 시점의 불변식(수량 1 이상, 금액 0 초과, 초기 상태 CREATED)이 정적 팩토리에 있다. 다른 경로로 만들면 그 불변식을 건너뛴 객체가 생긴다.

## 이렇게 한다

```java
Order order = Order.place(memberId, productName, quantity, unitPrice, shippingAddress);
```

## 이렇게 하지 않는다

```java
public Order() { }                                  // JPA 용이라도 public 이면 안 된다
Order order = new Order();                          // OrderWriter 가 같은 패키지라 부를 수 있다
```

## 고치는 법

- 생성자를 private(통합형의 JPA 기본 생성자는 protected)으로 바꾸고, 불변식을 검사하는 정적 팩토리를 만든다. 팩토리 이름은 도메인 동사(place, register, request)로 짓는다.

## 판별 한계

테스트 코드는 검사 대상이 아니다. 식별자가 필요한 웹 슬라이스 테스트는 `com.meteor.support.fixture.Persisted.withId` 를 쓴다.
