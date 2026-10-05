# 리뷰 에이전트 하네스

코드 리뷰 에이전트가 [ARCHITECTURE.md](ARCHITECTURE.md) 의 의미 규칙(S-xx)을 PR 마다 확인하도록 하는 설정이다.
구조 규칙(R-xx)은 ArchUnit 이 맡으므로 에이전트에게 다시 묻지 않는다. 도구에 독립적으로 썼으니 사용하는 에이전트의
프롬프트·스킬 형식에 맞춰 옮긴다.

## 역할 분담

| 수단 | 맡는 규칙 | 성격 |
|---|---|---|
| ArchUnit (`./gradlew test`) | R-01 ~ R-09 | 결정적. 전체 코드 그래프를 본다. 위반이면 머지 불가 |
| 리뷰 에이전트 | S-01 ~ S-09 | 의미 판단. PR diff 와 주변 코드를 본다. 규칙별로 차단/권고를 정한다 |
| 사람 | 설계 판단 | 경계가 맞는지, 규칙 자체를 바꿔야 하는지 |

## 에이전트 입력

1. PR diff 전체
2. diff 가 건드린 패키지의 나머지 파일 (컨텍스트 파악용. 최소한 같은 애그리거트의 domain, application, storage)
3. `docs/ARCHITECTURE.md` 전문
4. ArchUnit 실행 결과 (실패한 규칙 ID 목록). 실패가 있으면 그 맥락에서 의미 위반을 찾는다

## 프롬프트 골격

```
당신은 이 저장소의 아키텍처 규칙 리뷰어다. docs/ARCHITECTURE.md 의 의미 규칙 S-01 ~ S-09 만 검사한다.
R-xx 규칙은 ArchUnit 이 이미 검사했으므로 다루지 않는다.

각 규칙에 대해 diff 와 주변 코드를 읽고 위반이 있으면 아래 형식으로만 보고한다. 위반이 없는 규칙은 적지 않는다.
확신이 낮으면 verdict 를 "suspect" 로 표시한다. 규칙에 없는 지적은 하지 않는다.

출력 형식 (규칙 위반 하나당 하나):
- rule: S-05
  file: core/core-api/src/main/java/com/meteor/order/application/OrderUseCase.java
  line: 42
  verdict: violation | suspect
  evidence: |
    private final PaymentRepository paymentRepository;   // 다른 컨텍스트의 Repository 주입
  why: 주문 UseCase 가 결제 Repository 를 주입받아 한 트랜잭션에서 두 컨텍스트를 수정한다.
       분리 시점에 이 트랜잭션은 사가가 되어야 한다.
  fix: PaymentCompleted 이벤트를 발행하고 주문 컨텍스트가 AFTER_COMMIT 리스너로 받는다.

위반이 하나도 없으면 "no findings" 만 출력한다.
```

`why` 에는 규칙의 이유를 ARCHITECTURE.md 의 표현으로 쓴다. 코멘트가 쌓이면 규칙이 팀의 언어가 된다.

## 규칙별 판단 가이드

에이전트가 각 규칙을 어떻게 확인하는지. 골든 세트의 기대 결과도 이 기준으로 만든다.

| 규칙 | 확인 방법 | 오탐 주의 |
|---|---|---|
| S-01 | `*UseCase` 본문에서 도메인 객체의 getter 결과로 분기하는 `if`/`switch` | 입력 검증(null, 빈 값)은 위반 아님 |
| S-02 | `domain` 의 애그리거트에 public setter, 상태 변경 메서드 부재, UseCase 가 `restore(...)` 로 새 상태를 조립 | record VO 는 대상 아님 |
| S-03 | `*Controller` 에서 `of`/`from`/getter 외의 VO 메서드 호출 | Request → Command 변환은 허용 |
| S-04 | `domain` 클래스 필드 타입이 다른 컨텍스트의 애그리거트. `storage` 엔티티의 `@ManyToOne` 등이 다른 컨텍스트 엔티티 | ID 타입(VO) 보유는 허용 |
| S-05 | `*UseCase` 생성자가 두 컨텍스트 이상의 `*Repository`/`*Facade` 를 주입 | 조회 전용 Facade 하나를 읽는 것은 권고 수준 |
| S-06 | `@Query`, 네이티브 쿼리, JPQL 에 다른 컨텍스트 테이블·엔티티 등장 | 같은 컨텍스트 내부 JOIN 은 허용 |
| S-07 | `@TransactionalEventListener` 의 phase 가 `BEFORE_COMMIT`, 또는 리스너가 반환값을 호출자에게 돌려줌 | `@EventListener` 동기 리스너는 같은 컨텍스트 내부라면 허용 |
| S-08 | `storage`/`clients` 클래스 안의 도메인 상태 분기, 계산 | null 처리와 타입 변환은 허용 |
| S-09 | UseCase 가 애그리거트 메서드를 호출한 뒤 `save()` 없이 반환 | 읽기 전용 UseCase 는 대상 아님 |

## 차단 범위

- 차단(머지 불가): S-04, S-05, S-06. 분리 비용을 직접 만드는 규칙이고 오탐이 거의 없다.
- 권고(코멘트만): 나머지. 골든 세트에서 정밀도가 90% 를 넘으면 차단으로 올린다.

오탐이 차단하기 시작하면 팀은 에이전트를 무시하는 법을 배운다. 차단 범위는 측정 결과로만 넓힌다.

## 골든 세트

프롬프트를 바꿀 때마다 재현율과 정밀도를 재기 위한 고정 입력.

1. `docs/review/golden/` 아래에 케이스별 디렉터리를 둔다. 각 케이스는 `diff.patch` 와 `expected.yaml`(기대 findings, 위 출력 형식).
2. 규칙마다 위반 케이스 1개 이상, 위반처럼 보이지만 아닌 케이스 1개 이상. 처음엔 20개면 충분하다.
3. 실제 PR 에서 에이전트가 놓치거나 오탐한 사례는 케이스로 추가한다. 골든 세트는 자란다.
4. 측정은 규칙별 재현율·정밀도. 차단 규칙은 정밀도 우선, 권고 규칙은 재현율 우선.

## 운영

- 규칙 변경은 `ARCHITECTURE.md` PR 하나로 하고, 같은 PR 에서 ArchUnit 테스트와 이 문서를 고친다.
- 에이전트 코멘트에 반박이 있으면 사람이 판단하고, 결과를 골든 세트에 넣는다. 에이전트와 논쟁하지 않는다.
- 분기마다 ARCHITECTURE.md 의 측정 항목과 함께 골든 세트 점수를 본다.
