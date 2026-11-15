---
id: R-18
title: @Transactional 이 적용된 usecase 메서드는 외부 시스템을 부르지 않는다
kind: 구조
applies_to: com.meteor.usecase 의 *UseCase 메서드
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R18_transactional_use_cases_do_not_call_clients]
related: [R-06, R-08, S-06]
human_doc: docs/architecture/transactions.md#R-18
---

# R-18 @Transactional 이 적용된 usecase 메서드는 외부 시스템을 부르지 않는다

## 규칙

메서드나 클래스에 `@Transactional` 이 적용된 usecase 메서드는 `com.meteor.client` 를 부르지 않는다. 외부 호출이 필요한 흐름은 메서드에 `@Transactional` 을 붙이지 않고 `TransactionTemplate` 구간 밖에서 부른다. 이전 S-06 의 구조로 판별되는 부분을 옮긴 것이다.

## 왜

외부 I/O 동안 DB 커넥션을 잡고 있으면 외부가 느려질 때 커넥션 풀이 마른다. 외부가 성공한 뒤 트랜잭션이 롤백되면 외부 쪽 결과는 되돌릴 수 없어 대사할 근거도 남지 않는다.

## 이렇게 한다

```java
public Payment pay(PaymentPayCommand command) {                 // @Transactional 없음
    Payment requested = transaction.execute(status -> paymentWriter.request(...));
    PgApproval approval = pgClient.approve(...);                  // 트랜잭션 밖
    return transaction.execute(status -> { ... });
}
```

## 이렇게 하지 않는다

```java
@Transactional
public Payment pay(PaymentPayCommand command) {
    Payment requested = paymentWriter.request(...);
    PgApproval approval = pgClient.approve(...);                  // 커넥션을 쥔 채 외부 호출
    return paymentWriter.approve(...);
}
```

## 고치는 법

- `@Transactional` 을 지우고 앞뒤 구간을 `TransactionTemplate` 으로 감싼다. 앞 구간이 남기는 중간 상태를 설계한다(S-06, playbooks/split-transaction.md).

## 판별 한계

`TransactionTemplate` 람다 안에서 client 를 부르는 경우는 감싸는 메서드의 호출로 보이므로 이 규칙이 구분하지 못한다. 리뷰(S-06)가 본다.
