# 플레이북: 여러 도메인 서비스를 엮는 usecase

흐름이 도메인 서비스 둘 이상을 엮거나, 외부 시스템을 부르거나, 트랜잭션 경계를 조절해야 할 때.

읽을 규칙: R-03, R-06, R-08, R-09, R-11, R-18, S-01, S-04, S-06, S-07.

## 먼저 판단한다

| 흐름 | 할 일 |
|---|---|
| 애그리거트 하나로 끝난다 | usecase 를 만들지 않는다. new-endpoint.md |
| 도메인 서비스 여럿, 함께 커밋·롤백 | 메서드에 `@Transactional`. 본보기 `OrderUseCase.place` |
| 외부 호출이 끼어 있거나 일부러 나눈다 | 메서드에 `@Transactional` 없이 `TransactionTemplate`. split-transaction.md |

## 단계

1. `usecase/XxxUseCase`(`@Service`)에 흐름 이름의 메서드를 만든다. 기존 usecase 에 같은 흐름이 있으면 그것을 쓴다. usecase 끼리는 부르지 않는다(R-09).
2. 입력은 `usecase/XxxYyyCommand` record. 출력은 애그리거트 하나면 그대로, 여럿을 합치면 `XxxYyyResult` record, 식별자 하나면 원시 타입.
3. 본문은 조회 → 사실 넘기기 → 쓰기 순서의 호출만 둔다. 다른 애그리거트의 값은 상대 `Reader` 에서 받아 인자로 넘긴다(R-04). 상태로 분기하지 않는다(S-01). 건수·합계 비교는 `*Policy` 로(S-04).
4. 쓰기가 섞이면 바깥 트랜잭션을 연다(R-08). 트랜잭션 안에서 도메인 서비스의 예외를 잡아 삼키지 않는다(S-07). `@Transactional` 안에서 client 를 부르지 않는다(R-18).
5. 외부 실패는 트랜잭션 밖에서 잡아 `ErrorCode` 로 바꾼다(harness/context/errors.md).
6. 통합 테스트 `usecase/XxxUseCaseIT extends ContextTest`. 성공, 규칙 거절, 롤백 확인(앞 쓰기가 남지 않는지). 외부는 `@MockitoBean`. 본보기 `OrderUseCaseIT`.
7. 컨트롤러는 이 usecase 메서드 하나만 부른다(R-11).
8. `./gradlew test`.
