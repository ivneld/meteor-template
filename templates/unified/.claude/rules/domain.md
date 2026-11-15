---
paths:
  - "core/core-api/src/main/java/com/meteor/domain/**"
---

# domain 경로 규칙 (통합형)

- 판단은 애그리거트와 `*Policy` 에 둔다. `*Reader`/`*Writer` 는 꺼내고 → 넘기고 → 저장한다.
- 애그리거트는 `@Entity` 지만 JPA 매핑 어노테이션과 `BaseEntity` 외의 프레임워크를 모른다. `*Policy` 는 JPA 도 모른다.
- `XxxWriter` 는 `XxxRepository` 만 쓰고, 애그리거트를 바꿨으면 같은 메서드에서 `save` 한다(더티 체킹에 기대지 않는다).
- setter, public 생성자, 연관관계(`@ManyToOne` 등), `EntityManager` 를 쓰지 않는다.

고치기 전에 읽는다:

- [R-01 도메인 모델은 JPA 매핑 외의 프레임워크를 모른다](../../harness/rules/R-01-domain-model-is-pure.md)
- [R-03 클래스 이름은 닫힌 어휘에서 고른다](../../harness/rules/R-03-closed-naming.md)
- [R-04 도메인 서비스는 자기 애그리거트의 *Repository 만 쓴다](../../harness/rules/R-04-domain-service-scope.md)
- [R-05 JPA 엔티티는 domain 의 애그리거트이고 영속화는 *Repository 로만 한다](../../harness/rules/R-05-persistence-model-placement.md)
- [R-07 *Reader 는 readOnly, *Writer 는 쓰기 트랜잭션이고 기본 전파만 쓴다](../../harness/rules/R-07-domain-service-transactions.md)
- [R-14 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다](../../harness/rules/R-14-reference-aggregates-by-id.md)
- [R-15 도메인 모델은 public setter 를 갖지 않는다](../../harness/rules/R-15-no-setters-in-domain-model.md)
- [R-16 애그리거트를 바꾼 *Writer 메서드는 save 를 부른다](../../harness/rules/R-16-explicit-save.md)
- [R-17 *Reader 는 쓰지 않는다](../../harness/rules/R-17-readers-do-not-write.md)
- [R-20 애그리거트는 자기 정적 팩토리로만 만든다](../../harness/rules/R-20-aggregates-created-by-factories.md)
- [S-01 usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다](../../harness/rules/S-01-no-branching-on-domain-state.md)
- [S-02 규칙이 있는 애그리거트는 빈약하지 않다](../../harness/rules/S-02-aggregates-are-not-anemic.md)
- [S-04 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다](../../harness/rules/S-04-cross-aggregate-rules-in-policy.md)
- [S-08 저장소 쿼리와 client 에 규칙을 넣지 않는다](../../harness/rules/S-08-adapters-only-translate.md)
- [S-09 JPA 가 도메인 설계를 정하게 두지 않는다](../../harness/rules/S-09-jpa-does-not-shape-the-model.md)
