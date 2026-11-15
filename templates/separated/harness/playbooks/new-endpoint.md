# 플레이북: 애그리거트 하나로 끝나는 엔드포인트

조회·생성·상태 변경이 애그리거트 하나로 끝날 때. usecase 를 만들지 않고 api 가 `*Reader`/`*Writer` 를 바로 부른다.

읽을 규칙: R-03, R-10, R-11, R-12, R-19, R-16.

## 단계

1. 도메인 서비스 메서드를 만든다. 조회는 `XxxReader`, 생성·변경은 `XxxWriter`. 판단은 애그리거트 메서드에 두고 서비스는 꺼내고 → 넘기고 → 저장한다(S-01).
2. 요청 DTO `api/request/XxxYyyRequest`(record, Bean Validation). 도메인 인자나 Command 로 바꾸는 메서드(`toEmail()`, `toCommand()`)를 둔다. 필드에는 원시 타입만(R-12). 값 객체는 `of`/생성자로 만든다(R-19).
3. 응답 DTO `api/response/XxxResponse`(record, `static from(Xxx)`). 애그리거트에서 접근자만 읽는다(R-10). 값 객체는 원시 타입이나 중첩 record 로 푼다(R-12).
4. 컨트롤러 `api/XxxController`. 엔드포인트 메서드 하나는 도메인 서비스 메서드 하나만 부른다(R-11). try/catch, `@Transactional` 없음.
5. 웹 슬라이스 테스트 `api/XxxControllerTest extends RestDocsTest`. 도메인 서비스는 mock, 성공과 오류 응답을 문서화한다. 본보기 `MemberControllerTest`, `OrderControllerTest`.
6. 문서 `core/core-api/src/docs/asciidoc/index.adoc` 에 스니펫을 추가한다.
7. `./gradlew test restDocsTest`.

## 이 플레이북이 아닌 경우

- 다른 애그리거트의 확인이나 값이 필요하다 → new-use-case.md.
- 외부 시스템을 부른다 → new-use-case.md 와 split-transaction.md.
