---
id: S-01
title: usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다
kind: 의미
applies_to: *UseCase, *Reader, *Writer 의 메서드 본문
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [S-02, S-04, R-15]
human_doc: docs/architecture/domain-services.md#S-01
---

# S-01 usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다

## 규칙

usecase 와 도메인 서비스는 애그리거트의 상태를 꺼내 `if`/`switch` 로 판단하지 않는다. 판단은 애그리거트 메서드에 맡긴다.

## 왜

상태에 따른 판단이 서비스에 있으면 같은 규칙이 여러 서비스에 흩어지고, 애그리거트에 이미 있는 규칙과 어긋난다. 규칙을 바꿀 때 고칠 곳이 하나여야 한다.

## 위반 신호

- `if (order.getStatus() == OrderStatus.PAID)`, `switch (payment.getStatus())` 가 `*UseCase`·`*Reader`·`*Writer` 에 있다.
- 서비스가 상태를 보고 `CoreException` 을 던진다. 애그리거트 메서드가 같은 예외를 이미 던지는 경우가 많다(`Order.cancel` → `ORDER_NOT_CANCELLABLE`).
- 서비스가 `isActive()` 같은 접근자 결과로 거절한다. 애그리거트에 `ensureActive()` 가 있으면 그것을 부른다.

## 위반 아님 (오탐 주의)

- 입력 검증: null, 빈 값, 범위 확인.
- `Optional` 의 존재 여부(없으면 NOT_FOUND).
- 외부 호출 결과(PG 승인 여부)에 따른 흐름 분기. 도메인 상태가 아니다.

## 이렇게 한다

```java
public Order cancel(Long orderId) {
    Order order = load(orderId);
    order.cancel();                    // 거절은 Order 가 한다
    return orderRepository.save(order);
}
```

## 이렇게 하지 않는다

```java
if (order.getStatus() == OrderStatus.PAID) {
    throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE);
}
order.cancel();
```

## 고치는 법

- 분기와 예외를 애그리거트 메서드로 내린다. 이미 있으면 서비스의 분기를 지운다.
- 판단 결과가 값이면 애그리거트에 질의 메서드(`payableAmount()`, `ensureActive()`)를 만든다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
