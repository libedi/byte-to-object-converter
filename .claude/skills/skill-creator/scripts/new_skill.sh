#!/usr/bin/env bash
# Scaffold a new skill directory with a name-matched SKILL.md template.
# Usage: scripts/new_skill.sh <skill-name>
set -euo pipefail

name="${1:-}"
if [[ -z "$name" ]]; then
  echo "Error: skill name is required." >&2
  echo "Usage: scripts/new_skill.sh <skill-name>" >&2
  exit 1
fi

if ! [[ "$name" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
  echo "Error: invalid skill name \"$name\"." >&2
  echo "Must contain only lowercase letters, numbers, and single hyphens (no leading/trailing/consecutive hyphens)." >&2
  exit 1
fi

if [[ ${#name} -gt 64 ]]; then
  echo "Error: skill name \"$name\" is ${#name} characters; max is 64." >&2
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
skills_root="$(cd "$script_dir/../.." && pwd)"
dir="$skills_root/$name"

if [[ -e "$dir" ]]; then
  echo "Error: $dir already exists." >&2
  exit 1
fi

mkdir -p "$dir"
cat > "$dir/SKILL.md" <<EOF
---
name: $name
description: TODO — 무엇을 하는지와 언제 쓰는지를 모두 담아 1024자 이내로 작성 (references/optimizing-descriptions.md 참고)
---

# TODO: 제목

TODO: 본문 작성 (references/best-practices.md 참고)
EOF

echo "Created $dir/SKILL.md"
echo "name (\"$name\") already matches the directory name — no mismatch risk."
