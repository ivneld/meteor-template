---
paths:
  - "storage/db-core/src/main/java/**"
---

# storage 경로 규칙 (분리형)

- 엔티티와 `*JpaRepository` 는 도메인을 모른다. enum 은 `@Enumerated(STRING)`, 값 객체는 컬럼으로 펼친다.
- JPA 연관관계를 걸지 않는다. 다른 애그리거트는 ID 컬럼으로.

고치기 전에 읽는다:

- [R-02 의존은 바깥에서 안쪽으로만 흐른다](../../harness/rules/R-02-dependency-direction.md)
- [R-05 JPA 엔티티는 storage 에 있고 *Repository 어댑터 밖으로 나가지 않는다](../../harness/rules/R-05-persistence-model-placement.md)
- [R-14 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다](../../harness/rules/R-14-reference-aggregates-by-id.md)
