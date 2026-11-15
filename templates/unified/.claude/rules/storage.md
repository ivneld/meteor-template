---
paths:
  - "storage/db-core/src/main/java/**"
---

# storage 경로 규칙 (통합형)

- 이 모듈은 저장소 인프라(DataSource, JPA 설정, `BaseEntity`)뿐이다. 엔티티와 Spring Data 인터페이스는 core-api 의 domain 에 둔다.
- 도메인 패키지를 참조하지 않는다.

고치기 전에 읽는다:

- [R-02 의존은 바깥에서 안쪽으로만 흐른다](../../harness/rules/R-02-dependency-direction.md)
- [R-05 JPA 엔티티는 domain 의 애그리거트이고 영속화는 *Repository 로만 한다](../../harness/rules/R-05-persistence-model-placement.md)
