# 영속화

JPA 를 쓰되 **도메인 모델로 쓰지 않고 영속 엔진으로만 쓴다.** 애그리거트는 순수 자바로 `com.meteor.domain` 에, JPA 엔티티는 storage 모듈의
`com.meteor.storage` 에 있고, 둘 사이 변환은 domain 의 `*Repository` 어댑터가 한다.

```
com.meteor.domain                         com.meteor.storage (storage/db-core)
  Order  ◄── toDomain ──┐                   OrderEntity       @Entity, enum 컬럼, 값 객체는 펼친 컬럼
                        OrderRepository ──► OrderJpaRepository Spring Data
  Order  ─── save ──────┘
```

storage 모듈은 도메인을 모르지만 core-enum 은 알므로 상태는 enum 컬럼으로, 값 객체는 컬럼으로 펼쳐 저장하고 어댑터가 record 로 조립한다.

## 포기하는 것과 유지하는 것

| 포기하는 것 (애그리거트 중심 설계가 원래 피하는 것) | 유지하는 것 (영속 모델에 붙는 인프라 기능) |
|---|---|
| 더티 체킹. 저장은 `save()` 로 명시한다 (R-16) | `@TenantId` 로 모든 쿼리에 테넌트 조건 자동 적용 |
| 지연 로딩과 연관관계 탐색. 애그리거트는 통째로 로딩한다 (R-14) | `@Version` 낙관적 락, Envers 감사 이력, 2차 캐시 |
| cascade. 저장은 애그리거트 단위로 끝난다 | 스키마 매핑과 `validate`, 배치 insert, 방언 추상화 |

## 변환 비용

변환 코드는 애그리거트마다 `*Repository` 의 `save` / `toDomain` 과 애그리거트의 `restore` 로 고정되어 있어 비용이 예측 가능하다.

```java
public Order save(Order order) {
    OrderEntity entity;
    if (order.isNew()) {
        entity = new OrderEntity(order.getMemberId(), ..., order.getStatus());
    }
    else {
        entity = jpa.findById(order.getId()).orElseThrow(...);
        entity.changeStatus(order.getStatus());
    }
    return toDomain(jpa.save(entity));
}
```

`@Version` 을 쓸 때 애그리거트가 트랜잭션을 넘어 전달된다면 `restore()` 에 version 을 포함시켜 `*Repository` 가 복원 시 넘기도록 한다.
변환이 제대로 되는지는 애그리거트마다 왕복 테스트(`OrderRepositoryIT`, `PaymentRepositoryIT`)로 확인한다.

<a id="R-05"></a>
### R-05 영속 모델은 storage 와 저장소 어댑터 안에 있다

JPA 엔티티는 `com.meteor.storage` 에만 있다. storage(엔티티, Spring Data)를 참조하는 것은 domain 의 `*Repository` 어댑터뿐이다.
`*Repository` 의 public 메서드는 엔티티나 Spring Data 타입을 주고받지 않고 애그리거트만 주고받는다.

영속 모델이 API 계약이나 도메인 모델이 되는 것을 막는다. 엔티티가 어댑터 밖으로 한 번 나가면 그것을 받는 쪽이 엔티티에 의존하고, 변환이
여러 곳으로 흩어진다. 변환은 어댑터 안에서 끝난다.

강제: ArchUnit `R05_entities_live_in_storage_only`, `R05_only_repository_adapters_touch_storage`,
`R05_repository_adapters_expose_only_aggregates`.

## 엔티티를 만들 때

- `com.meteor.storage` 에 `*Entity` 와 `*JpaRepository` 를 둔다. 둘 다 public 이고 도메인을 모른다.
- `BaseEntity` 를 상속해 식별자와 감사 컬럼(`createdAt`, `updatedAt`)을 받는다. 감사 컬럼은 영속 모델의 관심사라 애그리거트에는 없다.
- 상태는 `@Enumerated(EnumType.STRING)` 컬럼, 값 객체는 펼친 컬럼으로 저장한다.
- 다른 엔티티와 연관관계를 걸지 않고 ID 컬럼만 둔다(R-14).
- 상태를 바꾸는 메서드(`changeStatus`)는 어댑터가 애그리거트의 상태를 옮겨 적는 용도다. 규칙을 넣지 않는다.
