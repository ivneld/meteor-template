---
id: S-02
title: 규칙이 있는 애그리거트는 빈약하지 않다
kind: 의미
applies_to: com.meteor.domain 의 애그리거트
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [S-01, R-15, R-20]
human_doc: docs/architecture/domain-model.md#S-02
---

# S-02 규칙이 있는 애그리거트는 빈약하지 않다

## 규칙

상태 전이, 계산, 검증 규칙이 있는 애그리거트는 그 규칙을 메서드로 가진다. 필드와 접근자만 있고 규칙은 서비스에 있는 모양을 만들지 않는다.

## 왜

규칙이 서비스에 있으면 순수 단위 테스트가 어렵고, 같은 애그리거트를 다루는 서비스마다 규칙이 갈라진다. setter(R-15)와 생성 경로(R-20)는 빌드가 막지만, 규칙이 어디 있는가는 코드 의미를 봐야 안다.

## 위반 신호

- 애그리거트에 상태 변경 메서드가 없고, 서비스가 접근자로 값을 꺼내 계산하거나 전이를 결정한다.
- 새 상태나 새 계산이 추가됐는데 애그리거트는 바뀌지 않고 `*Writer`·`*UseCase` 만 바뀌었다.
- 매핑 편의를 위해 규칙 메서드 대신 필드만 추가했다(S-09 도 함께 본다).

## 위반 아님 (오탐 주의)

- record 값 객체. 값 연산만 있으면 된다.
- 상태 전이·계산·정책이 없는 설정·코드 관리 기능의 단순 모델(이름과 값만 저장하는 코드 테이블 등).

## 이렇게 한다

```java
public Money payableAmount() {
    ensurePayable();
    return totalAmount();
}
```

## 이렇게 하지 않는다

```java
// OrderReader
if (order.getStatus() != OrderStatus.CREATED) throw ...;
return order.getUnitPrice().times(order.getQuantity());
```

## 고치는 법

- 서비스의 계산과 판단을 애그리거트 메서드로 옮기고 애그리거트 단위 테스트를 추가한다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
