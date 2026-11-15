---
id: S-04
title: 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다
kind: 의미
applies_to: *Writer, *UseCase, *Policy
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [S-01, R-04, R-01]
human_doc: docs/architecture/domain-model.md#S-04
---

# S-04 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다

## 규칙

애그리거트 하나가 알 수 없는 규칙(다른 주문의 건수, 다른 애그리거트의 합계)은 순수한 `*Policy` 에 둔다. 도메인 서비스와 usecase 는 저장소에서 사실을 꺼내 Policy 에 넘기기만 한다. 무엇을 셀지와 한도도 Policy 가 정한다.

## 왜

저장소 조회 결과를 서비스가 직접 비교하면 규칙이 서비스에 숨고, 테스트에 DB 나 목이 필요해진다. Policy 는 순수 객체라 사실만 넘겨 단위 테스트한다(`OrderLimitPolicyTest`).

## 위반 신호

- `*Writer` 나 `*UseCase` 가 건수·목록·합계를 받아 직접 비교하고 거절하거나 분기한다(`if (count >= 3) throw`).
- 한도 숫자, 세는 상태 같은 규칙 상수가 서비스나 쿼리 메서드 호출부에 있다.
- Policy 가 저장소나 도메인 서비스를 주입받는다(R-01 이 빌드에서 막는다).

## 위반 아님 (오탐 주의)

- 사실을 꺼내 `*Policy` 나 애그리거트에 넘기기만 하는 코드(`OrderWriter.place`).
- 존재 여부만 보고 NOT_FOUND 를 던지는 것.

## 이렇게 한다

```java
long awaiting = orderRepository.countByMemberIdAndStatus(memberId, OrderLimitPolicy.COUNTED_STATUS);
OrderLimitPolicy.ensureCanPlace(memberId, awaiting);
```

## 이렇게 하지 않는다

```java
long awaiting = orderRepository.countByMemberIdAndStatus(memberId, OrderStatus.CREATED);
if (awaiting >= 3) {
    throw new CoreException(ErrorCode.ORDER_LIMIT_EXCEEDED);
}
```

## 고치는 법

- 비교와 상수를 `XxxPolicy` 의 정적 메서드·상수로 옮기고, 서비스는 사실을 꺼내 넘긴다. Policy 단위 테스트를 같이 쓴다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
