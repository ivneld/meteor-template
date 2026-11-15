---
paths:
  - "harness/**"
  - "docs/architecture/**"
  - "AGENTS.md"
  - "core/core-api/src/test/java/com/meteor/architecture/**"
---

# 규칙과 하네스를 고칠 때

- 규칙은 ArchUnit 필드(`Rnn_`), `harness/rules/<ID>-*.md`, `AGENTS.md` 규칙 인덱스, `docs/architecture/rules.md` 네 곳에 같은 ID 로 있다. 한 변경에서 함께 고친다.
- `RuleCatalogTest` 가 네 곳의 ID, 규칙 파일의 `archunit_fields`, `human_doc` 앵커를 검사한다.
- 없앤 번호는 다시 쓰지 않는다. 의미 규칙을 구조 규칙으로 옮기면 새 R 번호를 주고 인덱스의 "없는 번호" 에 적는다.
- 사람용 문서(docs/)는 장 단위 설명, 에이전트용(harness/)은 규칙 단위 파일이다. 한쪽 형식을 다른 쪽에 복사하지 않는다.
- 사용자가 요청하지 않았으면 규칙 자체를 바꾸지 않는다(harness/playbooks/rule-exception.md).
