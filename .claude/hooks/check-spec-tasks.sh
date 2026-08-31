#!/usr/bin/env bash
# Stop hook for the `spec` skill (.claude/skills/spec/).
#
# tasks.md 태스크는 [ ](작성됨) -> [-](대기) -> [~](진행 중) -> [x](완료) 순으로 전환된다.
# [~]는 "지금 이 순간 실제로 작업 중"이라는 뜻이라 턴이 끝나는 시점에 남아있으면 안 된다
# ([-]는 여러 턴에 걸쳐 정상적으로 오래 남을 수 있으므로 검사하지 않는다).
#
# 이미 이 훅이 한 번 알렸는데도 Claude가 응답을 마치려 하면(stop_hook_active=true)
# 무한 루프를 막기 위해 다시 막지 않는다.
#
# jq에 의존하지 않는다 (이 환경에 설치돼 있지 않음) - stdin JSON은 grep으로,
# 출력 JSON은 수동 이스케이프로 만든다.

set -u
shopt -s nullglob

input="$(cat)"

if printf '%s' "$input" | grep -q '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  exit 0
fi

files=(specs/*/tasks.md)
if [ ${#files[@]} -eq 0 ]; then
  exit 0
fi

matches="$(grep -nHE '^[[:space:]]*- \[~\]' "${files[@]}" 2>/dev/null)"
if [ -z "$matches" ]; then
  exit 0
fi

body="다음 태스크가 [~](진행 중) 상태로 남아 있습니다:
$matches

실제로 끝났다면 [x]로 갱신하세요. 아직 진행 중이라 의도적으로 남겨둔 것이라면, 그 상태를 인지했다고 밝히고 응답을 마쳐도 됩니다."

# JSON 문자열 이스케이프 (순서 중요: 백슬래시 -> 따옴표 -> 탭 -> 개행)
escaped="${body//\\/\\\\}"
escaped="${escaped//\"/\\\"}"
escaped="${escaped//$'\t'/\\t}"
escaped="${escaped//$'\n'/\\n}"

printf '{"decision":"block","reason":"%s"}\n' "$escaped"
