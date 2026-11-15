---
id: R-14
title: 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다
kind: 구조
applies_to: com.meteor.domain 의 애그리거트 필드, com.meteor.storage 의 엔티티 필드
enforced_by: ArchUnit (core/core-api/src/test/java/com/meteor/architecture/ArchitectureRules.java)
archunit_fields: [R14_aggregates_reference_others_by_id, R14_no_jpa_associations]
related: [R-04, R-05, R-10]
human_doc: docs/architecture/domain-model.md#R-14
---

# R-14 애그리거트끼리는 ID 로만 참조하고 JPA 연관관계를 걸지 않는다

## 규칙

- 애그리거트는 다른 애그리거트를 필드로 갖지 않는다. `List<Payment>` 처럼 제네릭 인자로 갖는 것도 같다. `memberId`, `orderId` 처럼 ID 로 참조한다.
- 어떤 클래스도 `@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany` 를 쓰지 않는다.
- 애그리거트 내부 값의 컬렉션(`@ElementCollection`)은 이 규칙 대상이 아니다.

## 왜

애그리거트는 통째로 로딩·저장되는 단위다. 연관관계를 따라가면 그 단위가 무너지고, 한 `*Writer` 가 두 애그리거트를 바꾸게 되어 R-04 가 무의미해진다. 엔티티 사이 연관관계는 어댑터 변환을 그래프 변환으로 만든다.

## 이렇게 한다

```java
public class Order {
    private Long memberId;          // 회원은 ID 로
}
```

## 이렇게 하지 않는다

```java
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;          // 다른 애그리거트를 필드로, 연관관계까지
}
```

## 고치는 법

- 필드를 ID 로 바꾼다.
- 두 애그리거트가 함께 필요한 흐름은 usecase 가 각 `*Reader` 에서 받아 조합한다.
- 화면용 조합 조회는 조회 전용 `*Reader` 메서드와 JOIN 쿼리(필요한 컬럼만)로 만든다.
