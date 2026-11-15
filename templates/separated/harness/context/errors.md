# 에러 처리 요약

사람용 전체 설명은 docs/error-handling.md. 코드를 쓸 때 지킬 것만 모았다.

- 도메인 규칙 위반은 애그리거트나 `*Policy` 가 `CoreException(ErrorCode.XXX)` 으로 던진다. `.property("orderId", id)` 로 구조화된 값을 붙인다. ProblemDetail 표준 이름(`type`, `title`, `status`, `detail`, `instance`)은 property 이름으로 쓰지 않는다(`status` 대신 `orderStatus`).
- 없는 리소스(NOT_FOUND)는 `*Reader`/`*Writer` 가 던진다. 애그리거트가 아니다.
- 값 객체(record) 검증 실패는 `IllegalArgumentException`. `ApiControllerAdvice` 가 `C001` 400 으로 바꾼다.
- 외부 연동 실패: client 는 자기 예외(`PgApprovalException`)를 던지고, usecase 가 받아 `ErrorCode` 로 바꾼다. 원인은 `initCause` 로 남긴다.
- 컨트롤러는 try/catch 를 쓰지 않는다.
- HTTP 상태는 에러 코드가 정한다. 새 코드는 `support/error/ErrorCode.java` 에 도메인 접두어(C 공통, M 회원, O 주문, P 결제) + 순번으로 추가한다. 한 번 배포한 코드 문자열은 바꾸지 않는다. 추가하면 `ErrorCodeTest` 를 돌리고 `core/core-api/src/docs/asciidoc/index.adoc` 의 표에 한 줄 넣는다.
