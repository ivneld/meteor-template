# 설계된 구조 (통합형)

코드를 어디에 둘지 정할 때 이 문서를 본다. 표의 "허용 의존" 밖으로 나가면 ArchUnit(R 규칙)이 빌드를 깬다.

## 모듈

| 모듈 | 패키지 | 내용 | 의존 |
|---|---|---|---|
| core/core-enum | `com.meteor.enums` | 모든 상태·종류 enum | 없음 (R-13) |
| core/core-api | `com.meteor.domain`, `usecase`, `api`, `support.{error,web,async}` | 유일한 실행 모듈. 애그리거트(엔티티)와 Spring Data 인터페이스도 domain 에 있다 | core-enum, storage/db-core, clients/*, support/* |
| storage/db-core | `com.meteor.support.storage` | 저장소 인프라: DataSource, JPA 설정, `BaseEntity`. 엔티티는 없다 | spring-boot-starter-data-jpa |
| clients/client-pg | `com.meteor.client.pg` | PG 연동(OpenFeign). local 프로필은 PG 없이 승인 | core-api 를 모른다 |
| support/logging, support/monitoring | (리소스) | logback, Actuator | - |
| tests/api-docs | `com.meteor.test.api` | REST Docs 테스트 베이스 `RestDocsTest` | - |

## 계층과 허용 의존

```
api ──► usecase ──► domain(도메인 서비스) ──► domain(*Repository: Spring Data 인터페이스)
 │         │                 │
 │         └──► client       └──► domain(도메인 모델: 애그리거트 엔티티, @Embeddable 값 객체, *Policy)
 └──► domain(*Reader / *Writer)      (애그리거트 하나로 끝나는 흐름)

모든 계층 ──► enums        domain ──► support.error, support.storage.BaseEntity
```

| 패키지 | 클래스 종류 (닫힌 목록, R-03) | 할 수 있는 것 | 할 수 없는 것 |
|---|---|---|---|
| `domain` 도메인 모델 | 애그리거트(`@Entity`, `BaseEntity` 상속), 값 객체(`@Embeddable` record), `*Policy` | 규칙, 상태 전이, 계산, JPA 매핑 어노테이션 | Spring, Hibernate 전용 API, 도메인 빈 의존(R-01). Policy 의 JPA 의존(R-01). setter(R-15). public 생성자(R-20). 다른 애그리거트 필드·연관관계(R-14) |
| `domain` 도메인 서비스 | `*Reader`(`@Transactional(readOnly = true)`), `*Writer`(`@Transactional`) | 자기 `*Repository` 로 꺼내고, 애그리거트·Policy 에 넘기고, `save` | 다른 `*Repository`/도메인 서비스(R-04), client·usecase·api(R-01), 전파 조절(R-07), `EntityManager`(R-05). Reader 의 쓰기(R-17). Writer 의 save 누락(R-16) |
| `domain` 저장소 | `*Repository`(`JpaRepository<Aggregate, Long>` 를 상속한 인터페이스) | 파생 쿼리, `@Query` | 규칙이 박힌 쿼리(S-08). 도메인 서비스 밖에서 호출(R-04, R-09, R-10) |
| `usecase` | `*UseCase`(`@Service`), `*Command`, `*Result` | 여러 도메인 서비스 엮기, client 호출, 트랜잭션 묶기·나누기 | `*Repository`(R-09), 다른 UseCase(R-09), 상태 분기(S-01), `@Transactional` 안 client(R-18) |
| `api` | `*Controller`, `api.request.*Request`, `api.response.*Response` | 요청 → Command/인자 변환, 메서드 하나 호출(R-11), 응답 조립 | 트랜잭션(R-06), `*Repository`·`*Policy`·support.storage·client(R-10), 애그리거트 상태 변경(R-10), 값 객체 연산(R-19), DTO 에 도메인 타입·엔티티(R-12) |
| `support.storage` | `BaseEntity`, DataSource·JPA 설정 | 인프라 | 도메인 참조(R-02) |
| `client.*` | 클라이언트, 자기 요청·응답 record, 자기 예외 | 외부 호출과 변환 | 도메인 참조(R-02), 규칙(S-08) |
| `support.error` | `ErrorCode`, `CoreException`, `LogLevel` | 에러 어휘 | 프레임워크 의존(R-01) |

## 새 코드는 어디에 두나

```
상태가 있고 식별자가 있다 ─────────────► domain 의 @Entity 애그리거트(BaseEntity 상속) + domain 의 *Repository 인터페이스
값이다(식별자 없음, 불변) ─────────────► domain 의 @Embeddable record (컬럼 이름은 쓰는 애그리거트가 @AttributeOverride)
enum 이다 ────────────────────────────► core-enum 의 com.meteor.enums
여러 애그리거트에 걸친 판단이다 ────────► domain 의 *Policy (순수, JPA 도 모름)
애그리거트 하나를 조회한다 ─────────────► 그 애그리거트의 *Reader
애그리거트 하나를 만들거나 바꾼다 ──────► 그 애그리거트의 *Writer (바꾸면 save)
도메인 서비스 둘 이상·외부 호출·경계 조절 ► usecase 의 *UseCase (+ *Command, *Result)
HTTP 입출력 ───────────────────────────► api 의 *Controller, api.request, api.response
외부 시스템 ───────────────────────────► clients/<이름> 모듈의 com.meteor.client.<이름>
```

## 레퍼런스 코드

사람과 에이전트 모두 문서보다 옆 코드를 복사한다. 새 코드는 아래 본보기를 따라 쓴다.

| 본보기 | 파일 |
|---|---|
| 애그리거트 엔티티와 상태 전이 | `core/core-api/src/main/java/com/meteor/domain/Order.java`, `Payment.java`, `Member.java` |
| `@Embeddable` 값 객체 | `domain/Money.java`, `Address.java`, `Email.java` (매핑은 `Order` 의 `@AttributeOverride`) |
| 여러 애그리거트에 걸친 규칙 | `domain/OrderLimitPolicy.java` (사실은 `OrderWriter.place` 가 꺼냄) |
| 저장소 | `domain/OrderRepository.java` (Spring Data 인터페이스) |
| usecase 없이 api 가 도메인 서비스를 바로 부름 | `api/MemberController.java` → `MemberReader`, `MemberWriter` |
| 도메인 서비스 둘을 한 트랜잭션으로 묶는 usecase | `usecase/OrderUseCase.place` |
| 외부 호출을 사이에 두고 트랜잭션을 나누는 usecase | `usecase/PaymentUseCase.pay` |
| 응답 DTO | `api/response/OrderResponse.java` |
| 테스트 | 도메인 단위 `domain/OrderTest.java`, 매핑 왕복 `domain/OrderRepositoryIT.java`, 트랜잭션 경계 `usecase/PaymentTransactionBoundaryIT.java`, 웹 슬라이스 `api/OrderControllerTest.java` (식별자가 필요하면 `support/fixture/Persisted.withId`) |
