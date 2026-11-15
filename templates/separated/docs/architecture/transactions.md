# 트랜잭션

트랜잭션을 여는 곳은 두 종류뿐이고, 경계를 조절하는 곳은 usecase 하나뿐이다. 그래서 "이 코드가 어느 트랜잭션 안인가"를 클래스 이름과
메서드 하나만 보고 알 수 있다.

## 선언하는 곳

| 위치 | 허용 | 규칙 |
|---|---|---|
| `*Reader` | 클래스에 `@Transactional(readOnly = true)`. 메서드에 다시 붙인다면 기본 전파만 | R-06, R-07 |
| `*Writer` | 클래스에 `@Transactional`. 메서드에 다시 붙인다면 기본 전파만 | R-06, R-07 |
| `*UseCase` | `@Transactional` (모든 속성), `TransactionTemplate` | R-06 |
| 그 외 (api, `*Repository`, `*Policy`, storage, client) | 없음 | R-06 |

<a id="R-06"></a>
### R-06 @Transactional 은 usecase 와 *Reader / *Writer 에만 있다

`@Transactional` 클래스와 메서드는 usecase 와 domain 의 `*Reader`/`*Writer` 에만 있다. `TransactionTemplate` 같은 프로그래밍 방식
트랜잭션은 usecase 에만 있다.

트랜잭션을 여는 곳이 두 종류뿐이어야 경계를 추적할 수 있다. 컨트롤러가 트랜잭션을 열면 엔드포인트마다 경계가 달라지고, 저장소 어댑터가 열면
호출자의 트랜잭션과 어떻게 합쳐지는지 따로 알아야 한다.

강제: ArchUnit `R06_transactional_classes_only_in_usecase_or_domain_services`,
`R06_transactional_methods_only_in_usecase_or_domain_services`, `R06_programmatic_transactions_only_in_usecase`.

<a id="R-07"></a>
### R-07 도메인 서비스는 기본 전파만 쓴다

`*Reader` 는 클래스에 `@Transactional(readOnly = true)`, `*Writer` 는 클래스에 `@Transactional` 을 붙인다. 도메인 서비스는 기본 전파
(`REQUIRED`)만 쓴다.

기본 전파라서 도메인 서비스는 혼자 불리면 독립된 트랜잭션이 되고, usecase 가 연 트랜잭션 안에서 불리면 거기에 참여한다. `REQUIRES_NEW` 같은
경계 조절을 도메인 서비스가 하면 usecase 를 읽어도 실제 경계를 알 수 없다.

```java
@Component
@Transactional(readOnly = true)
public class OrderReader { ... }
```

강제: ArchUnit `R07_readers_are_read_only`, `R07_writers_are_read_write`, `R07_domain_services_use_default_propagation`.

## 묶기

<a id="R-08"></a>
### R-08 쓰기를 엮는 usecase 메서드는 트랜잭션을 연다

도메인 서비스를 둘 이상 부르고 그중 `*Writer` 가 있는 usecase 메서드는 `@Transactional` 이거나 `TransactionTemplate` 을 쓴다. readOnly
트랜잭션의 usecase 메서드는 `*Writer` 를 부르지 않는다.

도메인 서비스는 기본 전파라 바깥 트랜잭션에 참여하므로 함께 커밋되고 함께 롤백된다. 바깥 트랜잭션을 빠뜨리면 서비스마다 따로 커밋해, 두 번째가
실패했을 때 첫 번째만 남는다. 컴파일도 런타임도 알려주지 않으므로 구조 규칙으로 막는다.

```java
@Transactional
public Order place(OrderPlaceCommand command) {
    memberReader.ensureActive(command.memberId());
    return orderWriter.place(command.memberId(), ...);
}
```

읽기 전용 트랜잭션 안에서의 쓰기는 플러시되지 않거나 에러가 난다.

강제: ArchUnit `R08_use_case_combining_writes_opens_a_transaction`, `R08_read_only_use_cases_do_not_write`.

## 나누기

외부 API 호출 동안 DB 커넥션을 잡고 있지 않으려는 것처럼 의도적으로 트랜잭션을 나눠야 하는 usecase 는 메서드에 `@Transactional` 을
붙이지 않고 `TransactionTemplate` 으로 구간을 감싼다. 경계가 한 메서드 안에 그대로 보인다.

```java
public Payment pay(PaymentPayCommand command) {
    Payment requested = transaction.execute(status ->                     // 트랜잭션 1
            paymentWriter.request(command.orderId(), orderReader.payableAmount(command.orderId()), command.method()));

    PgApproval approval = pgClient.approve(requested.paymentKey(), ...);   // 트랜잭션 밖

    return transaction.execute(status -> {                                // 트랜잭션 2
        Payment approved = paymentWriter.approve(requested.getId(), approval.transactionKey());
        orderWriter.markPaid(command.orderId());
        return approved;
    });
}
```

다른 방법은 쓰지 않는다.

- **같은 클래스의 `@Transactional` 메서드를 `this` 로 부르기.** Spring 트랜잭션은 프록시로 동작하므로 자기 호출에는 트랜잭션이 적용되지
  않는다. 컴파일도 테스트도 조용히 통과한다.
- **구간을 별도 빈으로 빼기.** 그 빈이 usecase 면 R-09, 도메인 서비스면 경계가 domain 으로 샌다.
- **바깥 트랜잭션 안에서 `REQUIRES_NEW` 로 열기.** 바깥 커넥션을 쥔 채 하나를 더 써서 부하 시 커넥션 풀이 고갈되고, 어느 구간이 언제
  커밋되는지 메서드만 봐서는 알 수 없다.

<a id="R-18"></a>
### R-18 트랜잭션 안에서 외부 시스템을 부르지 않는다

`@Transactional` 이 적용된(메서드나 클래스에 붙은) usecase 메서드는 client 를 부르지 않는다.

외부 I/O 동안 DB 커넥션을 잡고 있으면 외부 시스템이 느려질 때 커넥션 풀이 마른다. 외부 호출이 성공한 뒤 트랜잭션이 롤백되면 외부의 결과는
되돌릴 수 없다. 외부 호출이 필요하면 `TransactionTemplate` 으로 구간을 나누고 호출은 구간 밖에 둔다. 이전에는 리뷰 규칙 S-06 의 일부였다.

ArchUnit 은 람다 본문의 호출도 감싸는 메서드의 것으로 세므로 `TransactionTemplate` 람다 안의 client 호출은 구별하지 못한다. 그 부분은
S-06 이 본다.

강제: ArchUnit `R18_transactional_use_cases_do_not_call_clients`.

<a id="S-06"></a>
### S-06 트랜잭션을 나눈 usecase 는 중간 상태와 재실행을 설계한다

나눈다는 것은 앞 구간은 커밋됐는데 뒤 구간이 실패하는 상태를 받아들인다는 뜻이다. 그래서 함께 설계한다.

1. **앞 구간이 남긴 상태가 의미를 갖게 한다.** `Payment` 는 PG 호출 전에 `REQUESTED` 로 저장되어, PG 가 거절하면 `FAILED`, 승인 뒤 처리가
   실패하면 `REQUESTED` 로 남아 대사할 수 있다.
2. **뒤 구간을 다시 실행해도 안전하게 한다.** 애그리거트가 상태로 거절하므로(`Payment.approve` 는 `REQUESTED` 에서만) 재시도가 결과를
   바꾸지 않는다.
3. **쓰기는 구간 안에 둔다.** `TransactionTemplate` 을 쓰는 메서드에서 람다 밖에서 `*Writer` 를 둘 이상 엮으면 그 둘은 따로 커밋된다.

트랜잭션 밖에서 외부 호출 실패를 잡아 `*Writer` 하나로 실패 상태를 남기는 것은 정상이다(`PaymentUseCase.pay` 의
`paymentWriter.fail`). 경계 동작은 `PaymentTransactionBoundaryIT` 가 검증한다.

강제: 리뷰 에이전트(WARN). 어디서 나눌지는 설계 판단이 들어가므로 차단하지 않는다.

## 함정

<a id="S-07"></a>
### S-07 트랜잭션 안에서 도메인 서비스의 예외를 삼키지 않는다

`@Transactional` usecase 메서드나 `TransactionTemplate` 람다 안에서 도메인 서비스 호출을 try-catch 로 감싸고 예외를 삼킨 채 진행하지
않는다.

바깥 트랜잭션에 참여한 도메인 서비스가 예외를 던지면 트랜잭션 전체에 rollback-only 표시가 붙는다. usecase 가 그 예외를 잡고 계속 진행하면
커밋 시점에 `UnexpectedRollbackException` 이 나서 원래 실패와 전혀 다른 곳에서 실패한다. 트랜잭션 밖에서 잡아 처리하는 것(외부 호출 실패 등),
잡아서 다른 예외로 다시 던지는 것은 괜찮다.

강제: 리뷰 에이전트(BLOCK). 오탐이 거의 없고 결과가 심각해 리뷰 BLOCK 판정을 내는 유일한 규칙이다.

### readOnly 무시

`*Reader` 의 `readOnly = true` 는 바깥 쓰기 트랜잭션에 참여하면 무시된다. 이것은 의도된 동작이다.
