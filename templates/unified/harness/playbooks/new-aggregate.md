# 플레이북: 새 애그리거트 추가 (통합형)

상태와 식별자가 있는 새 개념(예: 쿠폰)을 추가할 때. 단계마다 본보기 파일을 열어 같은 모양으로 쓴다.

읽을 규칙: R-01, R-04, R-05, R-14, R-15, R-16, R-17, R-20, S-02, S-04, S-09.

## 단계

1. **enum.** 상태·종류는 `core/core-enum/src/main/java/com/meteor/enums/` 에 둔다. 이름만 가진다(R-13). 본보기 `OrderStatus`.
2. **값 객체.** 식별자 없는 값은 `com.meteor.domain` 의 `@Embeddable` record. 생성자에서 검증하고 실패는 `IllegalArgumentException`. 본보기 `Money`.
3. **애그리거트.** `com.meteor.domain` 에 `@Entity @Table(name = ...)` 클래스, `BaseEntity` 상속. 본보기 `Order`.
   - 매핑은 `@Column`, `@Embedded` + `@AttributeOverride`(값 객체 컬럼 이름), `@Enumerated(EnumType.STRING)`. 다른 애그리거트는 ID 필드로, 연관관계 금지(R-14).
   - JPA 용 기본 생성자는 `protected`, 실제 생성자는 private(R-20). 규칙을 가진 정적 팩토리(도메인 동사: `issue`, `place`).
   - 상태 전이는 의미 있는 메서드로(`use()`, `expire()`), 거절은 `CoreException(ErrorCode.XXX)`, 식별자는 `getId()`. setter 금지(R-15). 상태 변경 메서드에 `get*`/`is*`/`has*` 이름을 붙이지 않는다(R-16 이 이름으로 접근자를 판별한다).
   - 생명주기 콜백, `@Formula`, Hibernate 전용 어노테이션을 쓰지 않는다(R-01, S-09).
4. **단위 테스트.** `core/core-api/src/test/java/com/meteor/domain/XxxTest.java`. 정적 팩토리로 만들어 불변식, 허용 전이, 거절 전이(에러 코드까지)를 확인한다. DB 가 필요하면 규칙이 애그리거트 밖에 있다는 신호다. 본보기 `OrderTest`.
5. **저장소.** `com.meteor.domain.XxxRepository extends JpaRepository<Xxx, Long>`. 조회 조건은 파생 쿼리 메서드로, 조건 값(상태 등)은 인자로 받는다(S-08). 본보기 `OrderRepository`.
6. **매핑 왕복 테스트.** `XxxRepositoryIT extends ContextTest`. 값 객체가 컬럼으로 저장되고 되살아나는지, `save` 가 상태를 반영하는지. 본보기 `OrderRepositoryIT`.
7. **도메인 서비스.** `XxxReader`(`@Component @Transactional(readOnly = true)`, 없으면 NOT_FOUND)와 `XxxWriter`(`@Component @Transactional`, 꺼내고 → 애그리거트 메서드 → `save`, R-16). 자기 `XxxRepository` 만 주입(R-04). `EntityManager` 금지(R-05).
8. **에러 코드.** `support/error/ErrorCode.java` 에 새 접두어나 다음 번호. harness/context/errors.md.
9. **흐름 연결.** playbooks/new-endpoint.md 또는 new-use-case.md. 웹 슬라이스 테스트에서 식별자가 필요하면 `support/fixture/Persisted.withId(...)`.
10. **확인.** `./gradlew test` (ArchUnit 포함).

## 자주 하는 실수

- 화면 때문에 `@ManyToOne` 을 걸고 싶어진다 → R-14. 조회 전용 `Reader` 메서드와 쿼리로.
- 엔티티를 응답으로 그대로 반환한다 → R-12. `XxxResponse.from(...)`.
- 더티 체킹을 믿고 `save` 를 뺀다 → R-16.
