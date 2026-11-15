#!/usr/bin/env bash
# 두 템플릿에서 같아야 하는 경로가 실제로 같은지 검사한다. CI 에서 돌고, 로컬에서도 그대로 실행한다.
#
#   scripts/check-common.sh
#
# 공통 경로를 고칠 때는 두 템플릿을 함께 고친다. 템플릿마다 달라야 하는 파일이 생기면 아래 목록에서 빼고 그 이유를 주석으로 남긴다.
set -euo pipefail
cd "$(dirname "$0")/../templates"

COMMON=(
    # 빌드와 도구
    .env.example .gitattributes .gitignore .springjavaformatconfig .githooks .github .pi
    build.gradle lint.gradle gradle.properties gradle gradlew gradlew.bat
    # 모듈: 도메인·저장소와 무관한 것
    clients support tests core/core-enum
    core/core-api/src/main/resources core/core-api/src/docs
    core/core-api/src/main/java/com/meteor/MeteorApplication.java
    core/core-api/src/main/java/com/meteor/api
    core/core-api/src/main/java/com/meteor/usecase
    core/core-api/src/main/java/com/meteor/support
    storage/db-core/src/main/resources storage/db-core/src/test
    storage/db-core/src/main/java/com/meteor/support/storage/CoreDataSourceConfig.java
    # 도메인 중 영속화 방식과 무관한 것: 규칙(*Policy)과 도메인 서비스(*Reader, *Writer)
    core/core-api/src/main/java/com/meteor/domain/OrderLimitPolicy.java
    core/core-api/src/main/java/com/meteor/domain/MemberReader.java
    core/core-api/src/main/java/com/meteor/domain/MemberWriter.java
    core/core-api/src/main/java/com/meteor/domain/OrderReader.java
    core/core-api/src/main/java/com/meteor/domain/OrderWriter.java
    core/core-api/src/main/java/com/meteor/domain/PaymentReader.java
    core/core-api/src/main/java/com/meteor/domain/PaymentWriter.java
    # 테스트
    core/core-api/src/test/java/com/meteor/ContextTest.java
    core/core-api/src/test/java/com/meteor/DevelopTest.java
    core/core-api/src/test/java/com/meteor/MeteorApplicationTest.java
    core/core-api/src/test/java/com/meteor/usecase
    core/core-api/src/test/java/com/meteor/support/docs
    core/core-api/src/test/java/com/meteor/support/error
    core/core-api/src/test/java/com/meteor/support/web
    core/core-api/src/test/java/com/meteor/domain/OrderTest.java
    core/core-api/src/test/java/com/meteor/domain/OrderLimitPolicyTest.java
    core/core-api/src/test/java/com/meteor/domain/MoneyTest.java
    core/core-api/src/test/java/com/meteor/domain/EmailTest.java
    core/core-api/src/test/java/com/meteor/architecture/RuleCatalogTest.java
    # 문서와 하네스 중 영속화 방식과 무관한 것
    CLAUDE.md docs/README.md docs/error-handling.md docs/review-agent.md
    .claude/skills .claude/rules/harness-sync.md
    harness/review harness/context/errors.md
    harness/playbooks/new-use-case.md harness/playbooks/split-transaction.md harness/playbooks/rule-exception.md
)

failed=0
for path in "${COMMON[@]}"; do
    if [ ! -e "separated/$path" ] || [ ! -e "unified/$path" ]; then
        echo "MISSING  $path" >&2
        failed=1
        continue
    fi
    if ! diff -r -q -x build -x .gradle "separated/$path" "unified/$path" >/dev/null; then
        echo "DIFFERS  $path" >&2
        diff -r -u -x build -x .gradle "separated/$path" "unified/$path" | head -40 >&2 || true
        failed=1
    fi
done

if [ "$failed" = 1 ]; then
    echo >&2
    echo "공통 경로가 두 템플릿에서 다릅니다. 두 템플릿을 함께 고치거나, 달라야 한다면 scripts/check-common.sh 의 목록에서 빼세요." >&2
    exit 1
fi
echo "common paths are identical (${#COMMON[@]} paths)"
