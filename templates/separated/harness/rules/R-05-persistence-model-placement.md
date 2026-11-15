---
id: R-05
title: JPA 엔티티는 storage 에 있고 *Repository 어댑터 밖으로 나가지 않는다
kind: 구조
applies_to: com.meteor.storage, com.meteor.domain 의 *Repository
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R05_entities_live_in_storage_only, R05_only_repository_adapters_touch_storage, R05_repository_adapters_expose_only_aggregates]
related: [R-01, R-14, R-21]
human_doc: docs/architecture/persistence.md#R-05
---

# R-05 JPA 엔티티는 storage 에 있고 *Repository 어댑터 밖으로 나가지 않는다

## 규칙

- `@Entity` 는 `com.meteor.storage`(storage/db-core 모듈)에만 있다. 엔티티는 도메인을 모르고 core-enum 의 enum 만 안다.
- storage(엔티티, `*JpaRepository`)를 참조하는 것은 domain 의 `*Repository` 어댑터뿐이다.
- `*Repository` 의 public 메서드는 엔티티나 Spring Data 타입을 주고받지 않는다. 애그리거트와 원시 값만 오간다.

## 왜

영속 모델이 API 계약이나 도메인이 되는 것을 막는다. 엔티티는 기본 생성자, 가변 필드, 프록시, 지연 로딩 같은 JPA 요구를 따르고, 애그리거트는 `final` 필드와 정적 팩토리로 규칙에만 집중한다. 변환이 어댑터 안에서 끝나므로 순수 애그리거트를 트랜잭션 밖으로 그대로 돌려줄 수 있다.

## 이렇게 한다

```java
@Repository
public class OrderRepository {
    private final OrderJpaRepository jpa;
    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderRepository::toDomain);   // 엔티티 → 애그리거트
    }
}
```

## 이렇게 하지 않는다

```java
public OrderEntity findEntity(Long id) { ... }        // 엔티티를 어댑터 밖으로
private final OrderJpaRepository jpa;                 // OrderWriter 가 storage 를 직접
```

## 고치는 법

- 새 애그리거트는 `storage` 에 `XxxEntity`·`XxxJpaRepository`, domain 에 `XxxRepository` 어댑터(save, findById, toDomain)를 둔다. 왕복 테스트 `XxxRepositoryIT` 를 같이 쓴다.
- 조회 조건이 필요하면 `*JpaRepository` 에 메서드를 만들고 어댑터가 감싸 애그리거트나 원시 값으로 돌려준다.
