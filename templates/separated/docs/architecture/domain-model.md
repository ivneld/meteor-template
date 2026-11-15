# 도메인 모델

규칙이 사는 곳이다. 도메인 모델은 애그리거트, 값 객체(record), `*Policy` 세 종류이고 모두 `com.meteor.domain` 에 있다.

| 종류 | 이름 | 예 | 하는 일 |
|---|---|---|---|
| 애그리거트 | 도메인 명사 | `Order`, `Member`, `Payment` | 식별자와 상태를 갖고, 상태 전이 규칙을 가진다 |
| 값 객체 | 도메인 명사 (record) | `Money`, `Address`, `Email` | 값의 성립 조건과 값 연산을 가진다 |
| 순수 규칙 | `*Policy` | `OrderLimitPolicy` | 애그리거트 하나가 알 수 없는, 여러 애그리거트에 걸친 규칙 |

규칙을 바꿀 때 고칠 곳이 하나여야 한다. 그래서 판단은 이 세 종류에만 두고, 도메인 서비스와 usecase 는 사실을 꺼내 넘기고 저장만 한다.

<a id="R-01"></a>
### R-01 도메인 모델은 프레임워크를 모른다

도메인 모델(애그리거트, 값 객체, `*Policy`)은 Spring, JPA, storage, client, usecase, api, `*Repository`, `*Reader`, `*Writer` 에 의존하지
않는다. domain 전체는 usecase, api, client 를 모르고, support 에서는 에러 어휘(`support.error`)만 쓴다. 에러 어휘도 프레임워크를 모른다.

규칙이 프레임워크와 분리되어야 목 없이 단위 테스트할 수 있다. `OrderTest` 는 Spring 컨텍스트도 DB 도 없이 주문의 상태 전이를 확인한다.
도메인 서비스가 외부 시스템과 상위 계층 타입(`*Command`, request)을 모르게 해 도메인 연산이 화면·API 모양을 따라가지 않게 한다. 두 번째
실행 모듈이 생겨 `domain` 을 core-domain 모듈로 추출할 때 파일 이동으로 끝나는 것도 이 규칙 덕분이다.

```java
public class Order {                       // import 는 com.meteor.enums, support.error 뿐
    private final Long memberId;
    private final Money unitPrice;
    private OrderStatus status;
    ...
}
```

강제: ArchUnit `R01_domain_model_is_pure`, `R01_domain_knows_no_outer_layers`, `R01_domain_uses_only_error_vocabulary_from_support`,
`R01_error_vocabulary_knows_no_framework`.

## 애그리거트

애그리거트는 통째로 로딩되고 통째로 저장되는 단위다. 상태를 바꾸는 메서드는 규칙을 함께 가진다.

```java
public void cancel() {
    if (status != OrderStatus.CREATED) {
        throw new CoreException(ErrorCode.ORDER_NOT_CANCELLABLE).property("orderId", id)
            .property("orderStatus", status);
    }
    status = OrderStatus.CANCELLED;
}
```

<a id="R-14"></a>
### R-14 애그리거트끼리는 ID 로만 참조한다

애그리거트는 다른 애그리거트를 필드(컬렉션의 원소 포함)로 갖지 않는다. 어떤 클래스도 `@OneToMany`, `@ManyToOne`, `@OneToOne`,
`@ManyToMany` 를 쓰지 않는다.

애그리거트가 통째로 로딩·저장되는 단위이고, 연관관계를 따라가면 그 단위가 무너진다. 지연 로딩, cascade, N+1 이 설계에 들어오고 어디까지가
한 번에 저장되는지 알 수 없게 된다. `Order` 는 `Member` 대신 `memberId` 를, `Payment` 는 `Order` 대신 `orderId` 를 갖는다. 두 애그리거트가
함께 필요하면 usecase 가 각 `*Reader` 에서 받아 조합하고, 화면용 조합 조회는 조회 전용 `*Reader` 메서드와 쿼리로 만든다.

`@ElementCollection` 은 애그리거트 안의 값 컬렉션이라 금지하지 않는다.

강제: ArchUnit `R14_aggregates_reference_others_by_id`, `R14_no_jpa_associations`.

<a id="R-15"></a>
### R-15 도메인 모델에는 public setter 가 없다

애그리거트, 값 객체, `*Policy` 는 public `set*` 메서드를 갖지 않는다.

상태는 규칙을 가진 메서드(`cancel`, `markPaid`, `approve`)로만 바뀌어야 규칙을 바꿀 때 고칠 곳이 하나다. setter 가 있으면 규칙을 우회해
상태를 바꾸는 길이 열리고, 판단이 setter 를 부르는 쪽으로 흩어진다.

강제: ArchUnit `R15_domain_model_has_no_setters`.

<a id="R-20"></a>
### R-20 애그리거트는 자기 정적 팩토리로만 만든다

애그리거트는 public 생성자를 갖지 않고, 애그리거트의 생성자는 그 애그리거트 안에서만 부른다. 생성은 규칙을 가진 정적 팩토리로 한다.

생성 시점의 불변식(수량은 1 이상, 결제 금액은 0 보다 큼, 초기 상태)이 팩토리에 있다. 다른 경로로 만들면 그 불변식을 건너뛴다.

```java
public static Order place(Long memberId, String productName, int quantity, Money unitPrice, Address shippingAddress) {
    if (quantity < 1) {
        throw new CoreException(ErrorCode.INVALID_REQUEST, "quantity must be at least 1");
    }
    return new Order(null, memberId, productName, quantity, unitPrice, shippingAddress, OrderStatus.CREATED);
}
```

강제: ArchUnit `R20_aggregates_have_no_public_constructors`, `R20_aggregates_are_constructed_only_by_themselves`.

<a id="R-21"></a>
### R-21 restore 는 같은 이름의 *Repository 만 부른다

애그리거트의 `restore(...)` 는 저장소에서 읽은 상태를 되살리는 통로다. `Order.restore` 는 `OrderRepository` 만 부른다.

`restore` 는 저장된 상태를 그대로 받아야 하므로 불변식을 검사하지 않는다. 도메인 서비스가 `restore` 로 새 상태를 조립하면 애그리거트의
규칙을 우회한다. 상태 변경은 항상 애그리거트 메서드로 한다. 테스트 코드는 특정 상태의 애그리거트를 만들기 위해 `restore` 를 써도 된다.

```java
// OrderRepository 안에서만
private static Order toDomain(OrderEntity entity) {
    return Order.restore(entity.getId(), entity.getMemberId(), ..., entity.getStatus());
}
```

강제: ArchUnit `R21_only_own_repository_restores_aggregates`.

<a id="S-02"></a>
### S-02 규칙이 있는 애그리거트는 빈약하지 않다

상태 전이, 계산, 정책이 있는 애그리거트는 그 규칙을 메서드로 가진다. getter 만 있고 계산과 전이가 도메인 서비스에서 일어나면 위반이다.

빈약한 애그리거트는 규칙을 서비스의 `if` 로 흩어 놓는다. 같은 판단이 여러 서비스에 복제되고 하나만 고쳐진다. setter 와 `restore` 조립은
R-15, R-21 이 구조로 막고, 이 규칙은 그 밖의 의미를 본다. 상태 전이·계산·정책이 없는 설정·코드 관리 기능은 단순한 모델을 허용한다.

강제: 리뷰 에이전트(WARN).

## 값 객체

값 객체는 record 다. 생성자에서 값이 성립하는지 검사하고, 실패하면 `IllegalArgumentException` 을 던진다(`ApiControllerAdvice` 가 `C001`
로 바꾼다). 값 연산(`Money.plus`, `Money.times`)은 값 객체에 둔다. "가입 가능한 이메일 도메인인가" 같은 정책은 값 객체에 두지 않는다.

```java
public record Money(long amount) {
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
    }
    public Money times(int quantity) { ... }
}
```

값 자체가 성립하지 않으면 `IllegalArgumentException`(`Money(-1)`), 값은 맞지만 지금 상태에서 허용되지 않으면 `CoreException`
(`order.cancel()` on PAID)이다.

## *Policy

<a id="S-04"></a>
### S-04 여러 애그리거트에 걸친 규칙은 *Policy 에 둔다

애그리거트 하나가 알 수 없는 규칙(다른 주문이 몇 건 있는가)은 `*Policy` 에 이름을 붙여 순수하게 둔다. 도메인 서비스와 usecase 는 사실을
꺼내 넘기기만 한다. `*Writer` 나 usecase 가 저장소 조회 결과(건수, 목록, 합계)를 직접 비교해 거절하거나 분기하면 위반이다.

규칙이 서비스에 있으면 저장소 없이 판단할 수 없어 단위 테스트가 어렵고, 같은 규칙이 다른 서비스에 복제된다. `OrderLimitPolicy` 는 무엇을
셀지(`COUNTED_STATUS`)와 몇 건까지인지(`MAX_AWAITING_PAYMENT`)를 모두 정하고, `OrderWriter` 는 그 조건으로 건수를 꺼내 넘긴다.

```java
public Order place(Long memberId, String productName, int quantity, Money unitPrice, Address shippingAddress) {
    long awaiting = orderRepository.countByMemberIdAndStatus(memberId, OrderLimitPolicy.COUNTED_STATUS);
    OrderLimitPolicy.ensureCanPlace(memberId, awaiting);
    return orderRepository.save(Order.place(memberId, productName, quantity, unitPrice, shippingAddress));
}
```

Policy 가 리포지토리를 직접 쓰면 편해 보여도 Policy 는 순수하게 둔다. 사실을 꺼내는 일은 서비스가, 판단은 Policy 가 한다.

강제: 리뷰 에이전트(WARN).
