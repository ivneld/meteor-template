# api 계층

api 계층은 HTTP 요청을 Command 나 도메인 서비스 인자로 바꾸고, 돌려받은 애그리거트를 응답 DTO 로 바꾼다. 그 외에는 아무것도 하지 않는다.
트랜잭션도, 규칙 판단도, 예외 처리도 하지 않는다. 예외는 `ApiControllerAdvice` 가 받는다([에러 처리](../error-handling.md)).

```java
@PostMapping("/{orderId}/cancel")
public OrderResponse cancel(@PathVariable Long orderId) {
    return OrderResponse.from(orderWriter.cancel(orderId));
}
```

<a id="R-10"></a>
### R-10 api 는 usecase 와 도메인 서비스만 부르고, 애그리거트는 읽기만 한다

api 는 `*Repository`, `*Policy`, 저장소 인프라(`support.storage`), client 를 쓰지 않는다. 돌려받은 애그리거트에서는 접근자(`get*`, `is*`, `has*`, `*Amount`)만
읽는다.

컨트롤러가 받은 애그리거트는 도메인 서비스의 트랜잭션이 끝난 준영속 엔티티다. 상태 변경 메서드를 부르면 트랜잭션 밖에서 상태가 바뀌고
저장되지 않는다. 연관관계가 없으므로(R-14) 접근자를 읽는 것은 안전하다. 저장소나 Policy 를 직접 쓰면 도메인 서비스와
usecase 를 거치지 않는 두 번째 경로가 생긴다.

강제: ArchUnit `R10_api_depends_on_use_cases_and_domain_services_only`, `R10_api_only_reads_aggregates`.

<a id="R-11"></a>
### R-11 엔드포인트 하나는 호출 하나만 한다

엔드포인트 메서드 하나는 `*UseCase` 나 `*Reader`/`*Writer` 의 메서드를 하나만 부른다.

여러 개를 부르면 호출마다 트랜잭션이 따로 열려 부분 커밋이 생기고, 흐름을 컨트롤러가 조율하게 된다. 묶어야 하면 usecase 메서드 하나로 만든다.
`OrderController.place` 가 회원 확인과 주문 생성을 직접 부르지 않고 `OrderUseCase.place` 하나를 부르는 이유다.

강제: ArchUnit `R11_endpoint_calls_one_use_case_or_domain_service`.

<a id="R-12"></a>
### R-12 API 계약에 도메인 타입을 쓰지 않는다

`api.request`, `api.response` 의 필드는 domain 타입을 갖지 않는다. 값 객체는 원시 타입이나 api 전용 record 로 푼다. enum 은 core-enum 의
것이라 허용한다.

API 계약이 도메인 모델에 묶이면 둘 중 하나를 못 바꾼다. 애그리거트가 엔티티이므로 엔티티를 그대로 응답하면 컬럼 하나를 더할 때마다 API
응답이 바뀌고, 감사 컬럼(`createdAt`) 같은 영속 정보가 새어 나간다. 통합형에서 매핑 비용이 사라지지 않고 이 자리로 옮겨 온 것이다.

```java
public record OrderResponse(Long id, ..., long unitPrice, long totalAmount, AddressResponse shippingAddress, OrderStatus status) {
    public static OrderResponse from(Order order) { ... }
    public record AddressResponse(String city, String street, String zipCode) { ... }
}
```

강제: ArchUnit `R12_api_dtos_carry_no_domain_types`.

<a id="R-19"></a>
### R-19 api 는 값 객체를 만들고 읽기만 한다

api 는 값 객체에서 정적 팩토리(`of`, `from`), 생성자, 구성 요소 접근자(`amount()`, `city()`)만 쓴다. `plus`, `times` 같은 연산은 부르지
않는다.

금액 계산 같은 연산이 컨트롤러로 새면 규칙이 두 곳이 된다. 계산이 필요한 값은 애그리거트가 메서드로 내준다(`Order.totalAmount()`). 요청을
Command 로 바꾸며 `Money.of(unitPrice)`, `new Address(...)` 로 값 객체를 만드는 것은 변환이라 허용한다. 이전에는 리뷰 규칙 S-03 이었다.

강제: ArchUnit `R19_api_does_not_compute_with_value_objects`.
