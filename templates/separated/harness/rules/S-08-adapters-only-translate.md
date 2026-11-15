---
id: S-08
title: 저장소 어댑터와 client 는 변환만 한다
kind: 의미
applies_to: com.meteor.domain 의 *Repository, com.meteor.client
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [S-04, R-05]
human_doc: docs/architecture/domain-services.md#S-08
---

# S-08 저장소 어댑터와 client 는 변환만 한다

## 규칙

`*Repository` 어댑터와 client 는 엔티티·외부 형식과 도메인 사이 변환만 한다. 도메인 상태에 따른 분기, 계산, 거절을 하지 않는다.

## 왜

어댑터에 규칙이 들어가면 저장소를 바꾸거나 외부 계약이 바뀔 때 규칙이 함께 흔들리고, 도메인 단위 테스트로는 보이지 않는다.

## 위반 신호

- `toDomain`·`save` 안에서 상태에 따라 값을 바꾸거나 예외를 던진다(NOT_FOUND 제외).
- client 가 응답 코드에 따라 도메인 판단(승인 금액 비교 등)을 한다.

## 위반 아님 (오탐 주의)

- null 처리, 타입 변환, 없는 행에 대한 NOT_FOUND 예외.
- client 가 외부 오류를 자기 예외로 감싸는 것.

## 이렇게 한다

```java
private static Order toDomain(OrderEntity entity) {
    return Order.restore(entity.getId(), ..., entity.getStatus());   // 변환만
}
```

## 이렇게 하지 않는다

```java
public Order save(Order order) {
    if (order.getStatus() == OrderStatus.PAID && order.totalAmount().isZero()) throw ...;   // 어댑터의 규칙
}
```

## 고치는 법

- 규칙을 애그리거트·`*Policy` 로 옮기고, 어댑터·쿼리는 값을 인자로 받게 바꾼다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
