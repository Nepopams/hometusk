#!/usr/bin/env bash
set -euo pipefail

fail=0

files="$(git ls-files | grep -E '^docs/planning/(sprints/[^/]+\.md|gates/[^/]+\.md|reviews/[^/]+\.md|workpacks/[^/]+/workpack\.md)$' || true)"

if [ -z "${files}" ]; then
  echo "OK: no planning docs to lint"
  exit 0
fi

require_heading() {
  local f="$1"
  local heading="$2"
  if ! grep -q "^## ${heading}\b" "$f"; then
    echo "FAIL: Missing '## ${heading}' in $f" >&2
    fail=1
  fi
}

require_any_match() {
  local f="$1"
  local pattern="$2"
  local msg="$3"
  if ! grep -Eq "$pattern" "$f"; then
    echo "FAIL: ${msg} ($f)" >&2
    fail=1
  fi
}

while IFS= read -r f; do
  # Common requirement
  require_heading "$f" "Sources of Truth"

  # Ensure MVP is referenced somewhere in Sources of Truth block (soft but useful)
  require_any_match "$f" 'docs/planning/mvp\.md' "Missing MVP link in Sources of Truth"

  case "$f" in
    docs/planning/sprints/*)
      require_heading "$f" "Goal"
      require_any_match "$f" '^### Committed' "Missing '### Committed' scope"
      require_any_match "$f" '^### Stretch' "Missing '### Stretch' scope"
      require_any_match "$f" '^### Out of scope' "Missing '### Out of scope' scope"
      ;;
    docs/planning/workpacks/*/workpack.md)
      require_heading "$f" "Files to change"
      require_heading "$f" "Verification commands"
      require_heading "$f" "DoD checklist"
      require_heading "$f" "Rollback"
      # At least one backticked file path in Files to change
      require_any_match "$f" '^-\s+`[^`]+`' "No backticked file paths (expected under Files to change)"
      ;;
    docs/planning/gates/*|docs/planning/reviews/*)
      # Minimal sanity: decision/evidence-like headers exist (soft)
      # Do not fail hard here to keep lint lightweight.
      :
      ;;
  esac
done <<< "$files"

if [ "$fail" -ne 0 ]; then
  echo "FAIL: planning lint failed" >&2
  exit 1
fi

echo "OK: planning docs look sane"
