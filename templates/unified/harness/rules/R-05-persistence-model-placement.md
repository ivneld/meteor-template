---
id: R-05
title: JPA 엔티티는 domain 의 애그리거트이고 영속화는 *Repository 로만 한다
kind: 구조
applies_to: @Entity, @Embeddable, Spring Data 리포지토리, EntityManager
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R05_entities_are_domain_aggregates, R05_embeddables_are_domain_value_objects, R05_spring_data_repositories_are_domain_repositories, R05_no_entity_manager]
related: [R-01, R-14, R-16, S-09]
human_doc: docs/architecture/persistence.md#R-05
---

# R-05 JPA 엔티티는 domain 의 애그리거트이고 영속화는 *Repository 로만 한다

## 규칙

- `@Entity` 는 `com.meteor.domain` 의 애그리거트다. 애그리거트 밖에 별도 엔티티(조회용, 화면용)를 두지 않는다.
- `@Embeddable` 은 domain 의 값 객체 record 다.
- Spring Data 리포지토리 인터페이스는 domain 의 `*Repository` 뿐이다.
- `EntityManager`, `EntityManagerFactory` 를 직접 쓰지 않는다.

## 왜

영속 모델이 곧 도메인 모델이다. 두 번째 영속 모델이 생기면 무엇이 진짜 모델인지 흐려지고 규칙이 둘로 갈라진다. `EntityManager` 를 쓰면 flush, merge, detach 같은 영속성 컨텍스트 동작에 기대는 코드가 퍼져, 나중에 엔티티와 애그리거트를 나누는 구조(분리형)로 옮길 수 없게 된다.

## 이렇게 한다

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByMemberIdAndStatus(Long memberId, OrderStatus status);
}
```

## 이렇게 하지 않는다

```java
@Entity
public class OrderSummary { ... }                     // 화면용 두 번째 엔티티
@PersistenceContext EntityManager em;                 // Writer 가 영속성 컨텍스트를 직접
em.flush();
```

## 고치는 법

- 화면용 조합 조회는 별도 엔티티 대신 `*Reader` 의 조회 메서드와 Spring Data 쿼리(필요한 컬럼만, 프로젝션)로 만든다.
- `EntityManager` 가 필요해 보이면 대부분 명시적 `save`(R-16) 또는 쿼리 메서드로 해결된다. 그래도 필요하면 규칙 예외 절차(playbooks/rule-exception.md)를 따른다.
