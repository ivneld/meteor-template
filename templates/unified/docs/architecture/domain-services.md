# 도메인 서비스와 usecase

도메인 서비스(`*Reader`, `*Writer`)는 애그리거트 하나의 조회와 변경을 맡는다. 여러 도메인 서비스를 엮거나 외부 시스템을 부르는 흐름은
usecase(`*UseCase`)가 맡는다.

## 이름

domain 의 클래스 종류는 닫혀 있다.

| 종류 | 이름 | 트랜잭션 | 예 |
|---|---|---|---|
| 조회 | `*Reader` | 클래스에 `@Transactional(readOnly = true)` | `OrderReader.find`, `MemberReader.ensureActive` |
| 생성·변경 | `*Writer` | 클래스에 `@Transactional` | `OrderWriter.place`, `OrderWriter.cancel` |
| 저장소 | `*Repository` (Spring Data 인터페이스) | 선언하지 않는다 (호출자의 트랜잭션에 참여) | `OrderRepository` |

`*Writer` 가 커지면 `OrderCanceller` 처럼 동사 단위로 쪼개지 말고 먼저 규칙이 Writer 에 새어 있지 않은지(S-04) 본다. 그래도 크면
`*Writer` 를 둘로 나누되 이름은 `*Writer` 로 끝낸다.

## 범위

도메인 서비스는 꺼내고, 애그리거트·Policy 에 넘기고, 저장한다. 판단은 하지 않는다.

```java
public Order cancel(Long orderId) {
    Order order = load(orderId);      // 꺼낸다
    order.cancel();                   // 애그리거트가 판단한다
    return orderRepository.save(order); // 저장한다
}
```

<a id="R-04"></a>
### R-04 도메인 서비스는 자기 *Repository 만 쓴다

`*Reader` 와 `*Writer` 는 같은 이름의 `*Repository` 만 쓴다(`OrderWriter` → `OrderRepository`). 다른 `*Repository`, 다른 `*Reader`·
`*Writer` 를 쓰지 않는다.

도메인 서비스가 커지는 길은 대부분 다른 애그리거트의 저장소나 다른 도메인 서비스를 부르는 데서 열린다. 서비스끼리 엮이기 시작하면 흐름
전체를 떠안고 비대해진다. 엮는 일은 usecase 가 한다.

다른 애그리거트의 값 하나만 필요할 때도 리포지토리를 직접 읽지 않는다. usecase 가 상대 도메인 서비스에서 값을 받아 인자로 넘긴다. 결제 금액을
`OrderReader.payableAmount()` 에서 받아 `PaymentWriter.request(...)` 에 넘기는 것이 본보기다.

강제: ArchUnit `R04_domain_services_touch_only_their_own_repository`.

<a id="R-16"></a>
### R-16 상태를 바꾼 *Writer 메서드는 save 한다

`*Writer` 메서드가 애그리거트의 상태를 바꾸는 메서드를 부르면, 같은 메서드에서 `*Repository.save` 를 부른다.

애그리거트가 엔티티라 영속성 컨텍스트 안에서는 `save` 를 빠뜨려도 더티 체킹으로 저장된다. 그래도 명시한다. 저장 시점이 코드에 보이고,
트랜잭션 밖에서 받은 준영속 객체를 바꾼 경우나 분리형으로 옮긴 뒤에는 `save` 가 없으면 변경이 조용히 사라진다. 영속성 컨텍스트 안의
엔티티에 대한 `save` 는 거의 아무 일도 하지 않으므로 비용도 없다. 이전에는 리뷰 규칙 S-05 였다.

```java
public Order markPaid(Long orderId) {
    Order order = load(orderId);
    order.markPaid();
    return orderRepository.save(order);   // 빠뜨리면 ArchUnit 이 실패한다
}
```

ArchUnit 은 메서드 하나 안의 호출만 본다. 변경을 private 헬퍼로 나눴다면 그 헬퍼가 바꾸고 저장한다.

강제: ArchUnit `R16_writers_save_what_they_change`.

<a id="R-17"></a>
### R-17 *Reader 는 쓰지 않는다

`*Reader` 는 `*Repository` 의 `save*`, `delete*`, `flush*` 를 부르지 않는다.

`*Reader` 는 읽기 전용 트랜잭션에서 돈다. 그 안의 쓰기는 플러시되지 않거나 에러가 난다. 쓰기가 필요하면 `*Writer` 메서드로 만든다.

강제: ArchUnit `R17_readers_do_not_write`.

<a id="S-01"></a>
### S-01 usecase 와 도메인 서비스는 도메인 상태로 분기하지 않는다

`*UseCase`, `*Reader`, `*Writer` 는 애그리거트의 getter 결과(상태 enum 등)로 `if` / `switch` 하지 않는다. 그 판단은 애그리거트 메서드로
내린다.

```java
// 하지 않는다: Order.cancel() 이 이미 이 규칙을 갖고 있다
if (order.getStatus() == OrderStatus.PAID) {
    throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE);
}
```

같은 판단이 애그리거트와 서비스 두 곳에 있으면 하나만 고쳐진다. 입력 검증(null, 빈 값)이나 `Optional` 의 존재 여부 확인은 도메인 상태 분기가
아니다.

강제: 리뷰 에이전트(WARN).

<a id="S-08"></a>
### S-08 저장소 쿼리와 client 는 규칙을 갖지 않는다

`*Repository` 의 쿼리 메서드 이름과 `@Query` 에 규칙을 박지 않는다. 조건 값은 인자로 받고, 그 값은 `*Policy` 나 애그리거트가 정한다.
client 는 타입 변환만 하고 분기하거나 계산하지 않는다.

```java
// 한다: 무엇을 셀지는 OrderLimitPolicy.COUNTED_STATUS 가 정한다
long countByMemberIdAndStatus(Long memberId, OrderStatus status);

// 하지 않는다: "결제 대기 주문"이라는 규칙이 쿼리에 들어갔다
@Query("select count(o) from Order o where o.memberId = :memberId and o.status = 'CREATED'")
long countAwaitingPayment(Long memberId);
```

쿼리에 규칙이 들어가면 규칙이 도메인 모델 밖에 생기고, 규칙을 바꿀 때 쿼리를 함께 찾아 고쳐야 한다. null 처리, 타입 변환, 정렬·페이징
같은 조회 조건은 규칙이 아니다.

강제: 리뷰 에이전트(WARN).

## usecase 가 필요한 때

| 흐름 | 호출 | 본보기 |
|---|---|---|
| 애그리거트 하나로 끝난다 | api → `*Reader` / `*Writer` | 회원 가입·탈퇴, 주문 조회·취소 |
| 여러 도메인 서비스를 엮는다 | api → `*UseCase` (바깥 트랜잭션) → 도메인 서비스들 | `OrderUseCase.place` (회원 확인 + 주문 생성) |
| 외부 시스템을 부르거나 트랜잭션을 나눈다 | api → `*UseCase` (`TransactionTemplate`) → 도메인 서비스들, client | `PaymentUseCase.pay` |

usecase 의 반환 타입은 상황에 맞게 고른다. 애그리거트 하나면 그대로 돌려주고(`OrderUseCase.place` → `Order`), 여러 애그리거트를 합치거나
계산 결과를 담아야 하면 `*Result` 를 둔다. 식별자 하나처럼 단순한 값은 원시 타입으로 돌려준다(`MemberWriter.register` → `Long`).

<a id="R-09"></a>
### R-09 usecase 는 도메인 서비스를 거친다

usecase 는 `*Repository` 를 쓰지 않고, 다른 `*UseCase` 를 호출하지 않는다.

저장소 접근은 도메인 서비스 한 곳으로 모은다. usecase 끼리 부르면 트랜잭션 경계가 중첩되어 어디서 열리고 합류하는지 호출 체인을 따라가야
알 수 있다. 여러 군데서 반복되는 흐름은 이름을 붙여 usecase 메서드 하나로 만든다.

강제: ArchUnit `R09_use_cases_go_through_domain_services`, `R09_use_cases_do_not_call_each_other`.
