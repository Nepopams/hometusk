#!/bin/bash
# Run all tests for HomeTusk backend
# Usage: ./scripts/test.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/services/backend"

echo "🧪 Running HomeTusk backend tests..."

cd "$BACKEND_DIR"

# Run tests with Gradle
./gradlew test --info

echo "✅ All tests passed!"
