---
paths:
  - "core/core-api/src/main/java/com/meteor/api/**"
---

# api 경로 규칙 (통합형)

- 엔드포인트 메서드 하나는 `*UseCase`/`*Reader`/`*Writer` 메서드 하나만 부른다. 트랜잭션 없음.
- 애그리거트는 접근자만 읽고, 값 객체는 만들고 구성 요소만 읽는다.
- 요청·응답 record 의 필드에 도메인 타입을 두지 않는다. enum 은 허용.

고치기 전에 읽는다:

- [R-03 클래스 이름은 닫힌 어휘에서 고른다](../../harness/rules/R-03-closed-naming.md)
- [R-10 api 는 usecase 와 도메인 서비스만 부르고 애그리거트는 읽기만 한다](../../harness/rules/R-10-what-api-may-use.md)
- [R-11 엔드포인트 하나는 usecase 나 도메인 서비스 메서드 하나만 부른다](../../harness/rules/R-11-one-call-per-endpoint.md)
- [R-12 API 요청·응답 객체는 도메인 타입을 필드로 갖지 않는다](../../harness/rules/R-12-api-dtos-carry-no-domain-types.md)
- [R-19 api 는 값 객체를 만들고 읽기만 한다](../../harness/rules/R-19-api-does-not-compute-with-value-objects.md)
