---
id: S-06
title: 트랜잭션을 나눈 usecase 는 중간 상태와 재실행 안전성을 설계한다
kind: 의미
applies_to: TransactionTemplate 을 쓰는 *UseCase 메서드
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [R-08, R-18, S-07]
human_doc: docs/architecture/transactions.md#S-06
---

# S-06 트랜잭션을 나눈 usecase 는 중간 상태와 재실행 안전성을 설계한다

## 규칙

트랜잭션을 나누면 앞 구간은 커밋됐는데 뒤 구간이 실패하는 상태를 받아들인다는 뜻이다. 그래서 (1) 쓰기는 모두 `TransactionTemplate` 구간 안에 있고, (2) 앞 구간이 남긴 상태가 의미를 가져 실패를 정리·대사할 수 있고, (3) 뒤 구간을 다시 실행해도 결과가 바뀌지 않는다.

## 왜

나눈 경계 사이의 실패는 테스트에서 잘 드러나지 않고 운영에서 데이터 불일치로 나타난다. 중간 상태가 의미 없으면 실패한 건을 찾을 수도, 고칠 수도 없다.

## 위반 신호

- `TransactionTemplate` 을 쓰는 메서드에서 람다 밖에서 `*Writer` 를 둘 이상 엮는다.
- 람다 안에서 client 를 부른다(R-18 이 못 보는 경우).
- 앞 구간이 남긴 상태로 실패 건을 식별할 수 없다(예: PG 호출 전에 아무것도 저장하지 않음).
- 뒤 구간 재실행이 이중 처리를 만든다(상태로 거절하지 않는 승인 등).

## 위반 아님 (오탐 주의)

- 트랜잭션 밖에서 외부 호출 실패를 잡아 단일 `*Writer` 로 실패 상태를 남기는 것(`PaymentUseCase.pay` 의 `paymentWriter.fail`). 도메인 서비스 하나라 자기 트랜잭션으로 충분하다.

## 이렇게 한다

```java
Payment requested = transaction.execute(status -> paymentWriter.request(...));   // REQUESTED 로 남는다
PgApproval approval = pgClient.approve(requested.paymentKey(), ...);              // 멱등 키
return transaction.execute(status -> {
    Payment approved = paymentWriter.approve(requested.getId(), approval.transactionKey());
    orderWriter.markPaid(command.orderId());                                       // 함께 커밋·롤백
    return approved;
});
```

## 이렇게 하지 않는다

```java
PgApproval approval = pgClient.approve(...);                 // 아무것도 남기지 않고 외부 호출
Payment approved = paymentWriter.approve(...);               // 람다 밖에서 Writer 둘
orderWriter.markPaid(command.orderId());
```

## 고치는 법

- 외부 호출 전에 의미 있는 중간 상태(REQUESTED)를 저장하고, 함께 바뀌어야 하는 쓰기는 같은 구간에 넣는다.
- 뒤 구간의 애그리거트 메서드가 상태로 거절하게 해(`Payment.approve` 는 REQUESTED 에서만) 재실행을 안전하게 한다.
- 흐름을 `PaymentTransactionBoundaryIT` 처럼 실패 지점마다 통합 테스트한다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
