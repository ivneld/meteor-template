---
id: S-08
title: 저장소 쿼리와 client 에 규칙을 넣지 않는다
kind: 의미
applies_to: com.meteor.domain 의 *Repository 쿼리 메서드, com.meteor.client
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [S-04, R-05]
human_doc: docs/architecture/domain-services.md#S-08
---

# S-08 저장소 쿼리와 client 에 규칙을 넣지 않는다

## 규칙

`*Repository` 쿼리 메서드 이름과 `@Query`, client 는 규칙을 갖지 않는다. 쿼리 조건에 쓰는 상태·한도 같은 값은 인자로 받고, 그 값은 애그리거트나 `*Policy` 가 정한다.

## 왜

어댑터에 규칙이 들어가면 저장소를 바꾸거나 외부 계약이 바뀔 때 규칙이 함께 흔들리고, 도메인 단위 테스트로는 보이지 않는다.

## 위반 신호

- `findPayableOrdersByMemberId` 처럼 메서드 이름이나 `@Query` 에 상태 값(`status = 'CREATED'`)이 고정되어 규칙을 담는다.
- client 가 응답 코드에 따라 도메인 판단(승인 금액 비교 등)을 한다.

## 위반 아님 (오탐 주의)

- 조건 값을 인자로 받는 쿼리(`countByMemberIdAndStatus(memberId, OrderLimitPolicy.COUNTED_STATUS)`).
- client 가 외부 오류를 자기 예외로 감싸는 것.

## 이렇게 한다

```java
long countByMemberIdAndStatus(Long memberId, OrderStatus status);   // 세는 상태는 Policy 가 정해 넘긴다
```

## 이렇게 하지 않는다

```java
@Query("select count(o) from Order o where o.memberId = :memberId and o.status = 'CREATED'")
long countAwaitingPayment(Long memberId);   // 무엇을 세는지가 쿼리에 숨는다
```

## 고치는 법

- 규칙을 애그리거트·`*Policy` 로 옮기고, 어댑터·쿼리는 값을 인자로 받게 바꾼다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
