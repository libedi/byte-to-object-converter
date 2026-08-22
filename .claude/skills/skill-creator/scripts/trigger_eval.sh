#!/usr/bin/env bash
# Measure description trigger rate for a skill against a set of eval queries.
# Usage: scripts/trigger_eval.sh <skill-name> <queries.json> [runs]
#
# queries.json format: [{"query": "...", "should_trigger": true}, ...]
# Requires: jq, the `claude` CLI (Claude Code)
set -euo pipefail

SKILL_NAME="${1:?Usage: scripts/trigger_eval.sh <skill-name> <queries.json> [runs]}"
QUERIES_FILE="${2:?Usage: scripts/trigger_eval.sh <skill-name> <queries.json> [runs]}"
RUNS="${3:-3}"

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq is required but not found on PATH." >&2
  exit 1
fi
if ! command -v claude >/dev/null 2>&1; then
  echo "Error: the claude CLI is required but not found on PATH." >&2
  exit 1
fi
if [[ ! -f "$QUERIES_FILE" ]]; then
  echo "Error: queries file not found: $QUERIES_FILE" >&2
  exit 1
fi

check_triggered() {
  local query="$1"
  claude -p "$query" --output-format json 2>/dev/null \
    | jq -e --arg skill "$SKILL_NAME" \
      'any(.messages[].content[]; .type == "tool_use" and .name == "Skill" and .input.skill == $skill)' \
      > /dev/null 2>&1
}

count=$(jq length "$QUERIES_FILE")
for i in $(seq 0 $((count - 1))); do
  query=$(jq -r ".[$i].query" "$QUERIES_FILE")
  should_trigger=$(jq -r ".[$i].should_trigger" "$QUERIES_FILE")
  triggers=0

  for run in $(seq 1 "$RUNS"); do
    check_triggered "$query" && triggers=$((triggers + 1))
  done

  jq -n \
    --arg query "$query" \
    --argjson should_trigger "$should_trigger" \
    --argjson triggers "$triggers" \
    --argjson runs "$RUNS" \
    '{query: $query, should_trigger: $should_trigger, triggers: $triggers, runs: $runs, trigger_rate: ($triggers / $runs)}'
done | jq -s '.'
