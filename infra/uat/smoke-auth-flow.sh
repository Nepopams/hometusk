#!/bin/sh
set -eu

DOMAIN="${DOMAIN:-homiya.site}"
BASE_URL="${BASE_URL:-https://$DOMAIN}"
PASSWORD="${PASSWORD:-password123}"
EMAIL="${EMAIL:-smoke-$(date +%s)@test.local}"
COOKIE_JAR="${TMPDIR:-/tmp}/hometusk-smoke-cookies.$$"
BODY_FILE="${TMPDIR:-/tmp}/hometusk-smoke-body.$$"

cleanup() {
  rm -f "$COOKIE_JAR" "$BODY_FILE"
}
trap cleanup EXIT

request() {
  method="$1"
  path="$2"
  data="${3:-}"

  if [ -n "$data" ]; then
    curl -sS -o "$BODY_FILE" -w "%{http_code}" \
      -X "$method" \
      -H "Content-Type: application/json" \
      -c "$COOKIE_JAR" \
      -b "$COOKIE_JAR" \
      -d "$data" \
      "$BASE_URL$path"
  else
    curl -sS -o "$BODY_FILE" -w "%{http_code}" \
      -X "$method" \
      -c "$COOKIE_JAR" \
      -b "$COOKIE_JAR" \
      "$BASE_URL$path"
  fi
}

expect_status() {
  actual="$1"
  expected="$2"
  label="$3"

  if [ "$actual" != "$expected" ]; then
    echo "FAIL $label: expected $expected, got $actual" >&2
    cat "$BODY_FILE" >&2 || true
    echo >&2
    exit 1
  fi

  echo "OK $label: $actual"
}

extract_json_field() {
  field="$1"
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2], ""))' "$BODY_FILE" "$field"
}

echo "Smoke auth flow against $BASE_URL"
echo "Email: $EMAIL"

status=$(request POST /api/v1/auth/register "{\"name\":\"Smoke User\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
expect_status "$status" 204 "register"

status=$(request GET /api/v1/users/me)
expect_status "$status" 200 "users/me after register"

status=$(request POST /api/v1/households "{\"name\":\"Smoke Household $(date +%s)\"}")
expect_status "$status" 201 "create household"
HOUSEHOLD_ID=$(extract_json_field id)

if [ -z "$HOUSEHOLD_ID" ]; then
  echo "FAIL create household: response did not include id" >&2
  cat "$BODY_FILE" >&2 || true
  echo >&2
  exit 1
fi

status=$(request GET /api/v1/users/me)
expect_status "$status" 200 "users/me after F5-equivalent"

status=$(request GET "/api/v1/households/$HOUSEHOLD_ID/members")
expect_status "$status" 200 "household members"

status=$(request POST /api/v1/auth/logout)
expect_status "$status" 204 "logout"

status=$(request POST /api/v1/auth/login "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
expect_status "$status" 204 "login same user"

status=$(request GET /api/v1/users/me)
expect_status "$status" 200 "users/me after login"

echo "Smoke auth flow passed"
