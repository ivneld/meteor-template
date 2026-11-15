---
paths:
  - "core/core-api/src/main/java/com/meteor/usecase/**"
---

# usecase 경로 규칙 (통합형)

- usecase 는 도메인 서비스 둘 이상을 엮거나 외부를 부르거나 경계를 조절할 때만 만든다.
- 쓰기를 엮으면 `@Transactional`. 외부 호출이 끼면 `@Transactional` 없이 `TransactionTemplate` 구간 밖에서 부른다.
- `*Repository` 와 다른 `*UseCase` 를 부르지 않는다. 상태로 분기하지 않는다.
- 트랜잭션 안에서 도메인 서비스 예외를 잡아 삼키지 않는다(BLOCK).

고치기 전에 읽는다:

- [R-03 클래스 이름은 닫힌 어휘에서 고른다](../../harness/rules/R-03-closed-naming.md)
- [R-06 @Transactional 은 usecase 와 *Reader / *Writer 에만 있다](../../harness/rules/R-06-where-transactions-are-declared.md)
- [R-08 쓰기를 엮는 usecase 메서드는 바깥 트랜잭션을 연다](../../harness/rules/R-08-use-case-transaction-guard.md)
- [R-09 usecase 는 도메인 서비스를 거치고 다른 usecase 를 부르지 않는다](../../harness/rules/R-09-use-cases-go-through-domain-services.md)
- [R-18 @Transactional 이 적용된 usecase 메서드는 외부 시스템을 부르지 않는다](../../harness/rules/R-18-no-external-calls-in-transactions.md)
- [S-01 usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다](../../harness/rules/S-01-no-branching-on-domain-state.md)
- [S-04 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다](../../harness/rules/S-04-cross-aggregate-rules-in-policy.md)
- [S-06 트랜잭션을 나눈 usecase 는 중간 상태와 재실행 안전성을 설계한다](../../harness/rules/S-06-split-transactions-are-designed.md)
- [S-07 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다](../../harness/rules/S-07-do-not-swallow-inside-transactions.md)
