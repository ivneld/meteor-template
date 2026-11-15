---
id: S-07
title: 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다
kind: 의미
applies_to: @Transactional *UseCase 메서드, TransactionTemplate 람다
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: BLOCK
related: [S-06, R-08]
human_doc: docs/architecture/transactions.md#S-07
---

# S-07 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다

## 규칙

`@Transactional` usecase 메서드나 `TransactionTemplate` 람다 안에서 도메인 서비스 호출을 try-catch 로 감싸 예외를 삼키고 계속 진행하지 않는다.

## 왜

바깥 트랜잭션에 참여한 도메인 서비스가 예외를 던지면 트랜잭션 전체에 rollback-only 표시가 붙는다. 그 예외를 잡고 계속하면 커밋 시점에 `UnexpectedRollbackException` 이 나서, 원래 실패와 무관해 보이는 오류가 난다. 오탐이 거의 없어 차단(BLOCK)한다.

## 위반 신호

- `@Transactional` 메서드 안: `try { xxxWriter.something(); } catch (CoreException e) { log...; }` 후 다음 호출로 진행.
- `transaction.execute(status -> { try { ... } catch (...) { return null; } })`.

## 위반 아님 (오탐 주의)

- 트랜잭션 밖의 catch(외부 호출 실패 처리, `PaymentUseCase.pay` 의 `PgApprovalException`).
- 잡아서 다른 예외로 다시 던지는 것.
- 도메인 서비스가 아닌 순수 계산의 예외를 잡는 것.

## 이렇게 한다

```java
// 실패해도 계속해야 하면 그 호출을 별도 구간으로 나눈다
transaction.execute(status -> orderWriter.markPaid(orderId));
```

## 이렇게 하지 않는다

```java
@Transactional
public void settle(Long orderId) {
    try {
        orderWriter.markPaid(orderId);
    } catch (CoreException e) {
        log.warn("skip", e);        // rollback-only 인 채로 계속
    }
    paymentWriter.approve(...);
}
```

## 고치는 법

- 예외를 다시 던지거나, 실패를 허용해야 하는 호출을 별도 트랜잭션 구간(`TransactionTemplate`)으로 나눈다.
- 실패 가능성을 미리 묻는 애그리거트 질의 메서드를 만들어 예외 없이 분기하는 방법도 있다(이때 분기는 애그리거트 결과로, S-01).

## 리뷰 판정

위반(violation)이면 `BLOCK`. 머지 전에 고치거나 ADR 로 예외를 남긴다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
