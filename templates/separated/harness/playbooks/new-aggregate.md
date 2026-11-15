# 플레이북: 새 애그리거트 추가 (분리형)

상태와 식별자가 있는 새 개념(예: 쿠폰)을 추가할 때. 단계마다 본보기 파일을 열어 같은 모양으로 쓴다.

읽을 규칙: R-01, R-04, R-05, R-14, R-15, R-16, R-17, R-20, R-21, S-02, S-04.

## 단계

1. **enum.** 상태·종류는 `core/core-enum/src/main/java/com/meteor/enums/` 에 둔다. 이름만 가진다(R-13). 본보기 `OrderStatus`.
2. **값 객체.** 식별자 없는 값은 `com.meteor.domain` 의 record. 생성자에서 검증하고 실패는 `IllegalArgumentException`. 본보기 `Money`.
3. **애그리거트.** `com.meteor.domain` 에 순수 자바 클래스. 본보기 `Order`.
   - 필드는 `final` 우선, 상태만 가변. 다른 애그리거트는 ID 필드로(R-14).
   - 생성자는 private(R-20). 규칙을 가진 정적 팩토리(도메인 동사: `issue`, `place`)와 저장소 복원용 `restore(...)` 를 둔다.
   - 상태 전이는 의미 있는 메서드로(`use()`, `expire()`), 거절은 `CoreException(ErrorCode.XXX)`. setter 금지(R-15).
   - `isNew()` 와 접근자 `get*` 를 둔다. 상태 변경 메서드에 `get*`/`is*`/`has*` 이름을 붙이지 않는다(R-16 이 이름으로 접근자를 판별한다).
4. **단위 테스트.** `core/core-api/src/test/java/com/meteor/domain/XxxTest.java`. 팩토리 불변식, 허용 전이, 거절 전이(에러 코드까지). 본보기 `OrderTest`.
5. **엔티티.** `storage/db-core/src/main/java/com/meteor/storage/XxxEntity.java`(`BaseEntity` 상속, 도메인을 모름, enum 은 `@Enumerated(STRING)`, 값 객체는 컬럼으로 펼침, 연관관계 금지)와 `XxxJpaRepository`. 둘 다 public. 본보기 `OrderEntity`, `OrderJpaRepository`.
6. **저장소 어댑터.** `com.meteor.domain.XxxRepository`(`@Repository` 클래스, 생성자 package-private). `findById`, `save`(새것이면 엔티티 생성, 아니면 조회 후 상태 반영), `toDomain`(`Xxx.restore`). public 메서드는 애그리거트와 원시 값만 주고받는다(R-05). 본보기 `OrderRepository`.
7. **왕복 테스트.** `XxxRepositoryIT extends ContextTest`. 값 객체가 살아남는지, 기존 것 저장이 상태를 반영하는지. 본보기 `OrderRepositoryIT`.
8. **도메인 서비스.** `XxxReader`(`@Component @Transactional(readOnly = true)`, 없으면 NOT_FOUND)와 `XxxWriter`(`@Component @Transactional`, 꺼내고 → 애그리거트 메서드 → `save`). 자기 `XxxRepository` 만 주입(R-04).
9. **에러 코드.** `support/error/ErrorCode.java` 에 새 접두어나 다음 번호. harness/context/errors.md.
10. **흐름 연결.** playbooks/new-endpoint.md 또는 new-use-case.md.
11. **확인.** `./gradlew test` (ArchUnit 포함).

## 자주 하는 실수

- 엔티티를 domain 에 두거나 `@Entity` 를 애그리거트에 붙인다 → R-05, R-01.
- `XxxWriter` 가 다른 애그리거트의 `Repository` 로 존재 확인을 한다 → R-04. usecase 에서 상대 `Reader` 를 부른다.
- 한도·건수 판단을 `Writer` 에서 `if` 로 한다 → S-04. `XxxPolicy` 로.
