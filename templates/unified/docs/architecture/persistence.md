# 영속화

애그리거트가 곧 JPA 엔티티다. 애그리거트는 `@Entity` 이고 `BaseEntity` 를 상속하며, 값 객체는 `@Embeddable` record 로 애그리거트의 컬럼에
펼쳐 저장한다. `*Repository` 는 Spring Data 인터페이스 그 자체라 변환 코드가 없다.

```
com.meteor.domain
  Order (@Entity) ──── extends ───► BaseEntity (support.storage: id, createdAt, updatedAt)
    ├─ Money   (@Embeddable record)  → unit_price 컬럼
    └─ Address (@Embeddable record)  → city, street, zip_code 컬럼
  OrderRepository extends JpaRepository<Order, Long>
```

## 포기하는 것과 유지하는 것

분리하지 않는 대신 JPA 의 기능 가운데 애그리거트 경계를 흐리는 것은 쓰지 않고, 영속 인프라 기능은 그대로 쓴다.

| 쓰지 않는 것 | 쓰는 것 |
|---|---|
| 연관관계와 지연 로딩. 애그리거트는 ID 로 참조하고 통째로 로딩한다 (R-14) | `@TenantId` 로 모든 쿼리에 테넌트 조건 자동 적용 |
| 더티 체킹에 기댄 저장. 바꾸면 `save()` 를 명시한다 (R-16) | `@Version` 낙관적 락, Envers 감사 이력, 2차 캐시 |
| cascade. 저장은 애그리거트 단위로 끝난다 | 스키마 매핑과 `validate`, 배치 insert, 방언 추상화 |
| `EntityManager` 직접 사용, Hibernate 전용 API (R-05, R-01) | Spring Data 파생 쿼리와 `@Query` |
| 생명주기 콜백의 규칙 (S-09) | `BaseEntity` 의 감사 컬럼(`@CreationTimestamp`, `@UpdateTimestamp`) |

## 분리형과 비교한 비용

| | 통합형 (이 템플릿) | 분리형 |
|---|---|---|
| 애그리거트를 바꿀 때 고칠 곳 | 애그리거트 하나 | 애그리거트, 엔티티, `*Repository` 어댑터 |
| 애그리거트 모양 | JPA 요구를 따른다. `protected` 기본 생성자, 가변 필드 | `final` 필드, 정적 팩토리만 |
| 도메인 규칙 테스트 | 순수 단위 테스트 (어노테이션은 영향 없음) | 순수 단위 테스트 |
| API 응답 변환 | 필요하다 (R-12) | 필요하다 (R-12) |
| 저장 모양이 도메인과 다를 때 | 애그리거트가 휜다. 분리형으로 옮길 신호다 | 엔티티만 바꾼다 |

매핑 비용은 엔티티 ↔ 애그리거트에서 사라지지만 애그리거트 ↔ API 응답에는 그대로 있다. 이 템플릿이 아낀 것은 첫 번째 매핑이다.

<a id="R-05"></a>
### R-05 영속 모델은 도메인 모델 그 자체다

`@Entity` 는 domain 의 애그리거트다. `@Embeddable` 은 domain 의 값 객체(record)다. Spring Data 리포지토리 인터페이스는 domain 의
`*Repository` 뿐이다. 어떤 클래스도 `EntityManager` 나 `EntityManagerFactory` 를 직접 쓰지 않는다.

영속 모델이 곧 도메인 모델이므로 두 번째 영속 모델(조회 전용 엔티티, 애그리거트와 따로 노는 엔티티)이 생기면 무엇이 진짜 모델인지 흐려진다.
`EntityManager` 를 직접 쓰면 영속성 컨텍스트 동작(`flush`, `merge`, `detach`)에 기대는 코드가 퍼진다. 그런 코드는 분리형으로 옮길 때 하나하나
다시 써야 한다. 영속화는 `*Repository` 를 거친다.

강제: ArchUnit `R05_entities_are_domain_aggregates`, `R05_embeddables_are_domain_value_objects`,
`R05_spring_data_repositories_are_domain_repositories`, `R05_no_entity_manager`.

<a id="S-09"></a>
### S-09 JPA 가 도메인 설계를 정하게 두지 않는다

매핑 편의 때문에 도메인 모델을 바꾸지 않는다. 다음은 위반 신호다.

- 매핑을 쉽게 하려고 불변식을 약화한다(nullable 허용, 생성자 검증 제거).
- JPA 생명주기 콜백(`@PrePersist`, `@PreUpdate`, `@PostLoad`)이나 엔티티 리스너에 규칙을 둔다.
- `@Formula`, `@Where` 같은 Hibernate 전용 매핑으로 규칙을 표현한다.
- 값 객체를 원시 필드로 펼쳐 애그리거트에 둔다.
- 엔티티 리스너로 다른 애그리거트를 갱신한다.

영속 기술의 요구가 도메인 모델의 모양을 정하기 시작하면 규칙이 어노테이션과 콜백 속으로 숨는다. 콜백 안의 규칙은 단위 테스트로 확인할 수 없고,
언제 실행되는지 코드를 읽어서는 알 수 없다. 규칙은 애그리거트 메서드로 옮긴다. 같은 신호가 반복되면 그 애그리거트는 [분리형으로 옮길
신호](background.md#분리형으로-옮길-신호)에 해당하는지 검토한다.

강제: 리뷰 에이전트(WARN).

## 애그리거트를 만들 때

- `com.meteor.domain` 에 `@Entity` 클래스로 두고 `BaseEntity` 를 상속해 식별자와 감사 컬럼을 받는다.
- 기본 생성자는 `protected` 로 두고 코드에서 부르지 않는다. 생성은 정적 팩토리로 한다(R-20).
- 필드에 setter 를 두지 않는다(R-15). JPA 는 필드 접근으로 값을 채운다.
- 상태는 `@Enumerated(EnumType.STRING)` 컬럼으로 저장한다.
- 값 객체는 `@Embedded` 로 두고 컬럼 이름은 `@AttributeOverride` 로 정한다. 같은 값 객체를 두 번 쓰면 반드시 정한다.
- 다른 애그리거트는 ID 필드로만 참조한다(R-14).
- `*Repository` 는 `JpaRepository<애그리거트, Long>` 를 상속한 인터페이스로 두고, 조회 조건은 인자로 받는다(S-08).
- 매핑이 제대로 되는지는 왕복 테스트(`OrderRepositoryIT`, `PaymentRepositoryIT`)로 확인한다.
