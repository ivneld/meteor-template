---
id: S-09
title: JPA 가 도메인 설계를 정하게 두지 않는다
kind: 의미
applies_to: com.meteor.domain 의 애그리거트와 값 객체
enforced_by: 리뷰 에이전트 (/arch-review)
review_verdict: WARN
related: [R-05, R-14, R-15, R-20, S-02]
human_doc: docs/architecture/persistence.md#S-09
---

# S-09 JPA 가 도메인 설계를 정하게 두지 않는다

## 규칙

애그리거트가 곧 엔티티라도 모델의 모양은 도메인 규칙이 정한다. 매핑 편의 때문에 불변식을 약화하거나, JPA·Hibernate 기능으로 규칙을 표현하지 않는다.

## 왜

통합형은 JPA 요구(기본 생성자, 가변 필드, 프록시)를 도메인 모델이 함께 진다. 그 요구가 규칙의 자리와 모양까지 정하기 시작하면 통합형의 이점(변경 비용이 낮음)보다 비용이 커진다. 이 규칙의 위반이 반복되면 그것이 분리형으로 옮길 신호다(docs/scaling.md).

## 위반 신호

- 매핑을 위해 검증을 지우거나 nullable 을 허용했다(값 객체를 원시 필드로 풀어 검증을 잃음 포함).
- JPA 생명주기 콜백(`@PrePersist`, `@PreUpdate`, `@PostLoad`)이나 엔티티 리스너에 규칙이나 다른 애그리거트 갱신이 있다.
- `@Formula`, `@Where`, `@Filter` 같은 Hibernate 전용 매핑으로 계산·조건을 표현했다.
- 지연 로딩이나 cascade 를 쓰려고 연관관계를 원한다(R-14 가 막지만 우회 설계를 본다).
- 같은 애그리거트에 저장 모양이 둘 이상 필요하다는 요구가 나왔다.

## 위반 아님 (오탐 주의)

- `@Column`, `@Embedded`, `@AttributeOverride`, `@Enumerated` 같은 매핑 메타데이터.
- JPA 용 `protected` 기본 생성자(R-20 이 범위를 지킨다).
- `BaseEntity` 의 감사 컬럼.

## 이렇게 한다

```java
@Embedded
@AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false))
private Money unitPrice;               // 값 객체를 그대로 매핑, 검증은 Money 생성자
```

## 이렇게 하지 않는다

```java
private Long unitPrice;                // Money 를 풀어 검증을 잃음
@PrePersist
void beforeSave() { if (quantity < 1) quantity = 1; }   // 규칙이 콜백에
```

## 고치는 법

- 규칙을 애그리거트 메서드와 정적 팩토리로 옮기고 값 객체를 `@Embeddable` record 로 유지한다.
- 신호가 두 번 이상 반복되면 docs/scaling.md 의 "분리형으로 옮기기" 를 검토하고 ADR 로 남긴다.

## 리뷰 판정

위반(violation)이면 `WARN`. 고치거나 리뷰 요청에 사유를 적는다. 코드만으로 단정할 수 없으면 suspect 로 보고한다.
