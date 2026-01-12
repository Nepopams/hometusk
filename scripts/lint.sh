#!/bin/bash
# Run linting/formatting checks for HomeTusk backend
# Usage: ./scripts/lint.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/services/backend"

echo "🔍 Running lint checks for HomeTusk backend..."

cd "$BACKEND_DIR"

# Run Spotless check
./gradlew spotlessCheck

echo "✅ Lint checks passed!"
