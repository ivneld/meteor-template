---
paths:
  - "clients/*/src/main/java/**"
---

# clients 경로 규칙 (통합형)

- client 는 도메인을 모른다. 원시 타입과 자기 record 로 계약을 정하고, 실패는 자기 예외로 던진다.
- 규칙(금액 비교 등)을 넣지 않는다. 변환은 usecase 가 한다.

고치기 전에 읽는다:

- [R-02 의존은 바깥에서 안쪽으로만 흐른다](../../harness/rules/R-02-dependency-direction.md)
- [S-08 저장소 쿼리와 client 에 규칙을 넣지 않는다](../../harness/rules/S-08-adapters-only-translate.md)
