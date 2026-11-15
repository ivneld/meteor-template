---
id: R-08
title: 쓰기를 엮는 usecase 메서드는 바깥 트랜잭션을 연다
kind: 구조
applies_to: com.meteor.usecase 의 *UseCase 메서드
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R08_use_case_combining_writes_opens_a_transaction, R08_read_only_use_cases_do_not_write]
related: [R-06, R-07, S-06]
human_doc: docs/architecture/transactions.md#R-08
---

# R-08 쓰기를 엮는 usecase 메서드는 바깥 트랜잭션을 연다

## 규칙

- 도메인 서비스를 둘 이상 부르고 그중 `*Writer` 가 있는 usecase 메서드는 `@Transactional`(readOnly 아님)을 붙이거나 `TransactionTemplate` 을 쓴다.
- `@Transactional(readOnly = true)` usecase 메서드는 `*Writer` 를 부르지 않는다.

## 왜

바깥 트랜잭션을 빠뜨리면 도메인 서비스마다 따로 커밋해서, 두 번째가 실패했을 때 첫 번째만 남는다. 컴파일도 런타임도 알려주지 않는 부분 커밋이라 구조 규칙으로 막는다. 읽기 전용 트랜잭션 안의 쓰기는 플러시되지 않거나 에러가 난다.

## 이렇게 한다

```java
@Transactional
public Order place(OrderPlaceCommand command) {
    memberReader.ensureActive(command.memberId());
    return orderWriter.place(...);
}
```

## 이렇게 하지 않는다

```java
public Order place(OrderPlaceCommand command) {         // 트랜잭션 없음: 서비스마다 따로 커밋
    memberReader.ensureActive(command.memberId());
    return orderWriter.place(...);
}
```

## 고치는 법

- 함께 커밋·롤백되어야 하면 `@Transactional` 을 붙인다.
- 일부러 나누는 것이면 `TransactionTemplate` 으로 구간을 감싸고 S-06(중간 상태와 재실행)을 함께 설계한다.

## 판별 한계

ArchUnit 은 람다 본문의 호출도 감싸는 메서드의 것으로 센다. 그래서 `TransactionTemplate` 을 쓰는 메서드에서 쓰기가 모두 템플릿 안에 있는지는 판별하지 못한다. 그 부분은 리뷰(S-06)가 본다.
