#!/bin/bash
#
# Валидация примеров AI Platform Integration против JSON Schema
#
# Использование:
#   ./scripts/validate-aiplatform-contracts.sh
#
# Требования:
#   - Python 3 с модулем jsonschema (устанавливается автоматически)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

INTEGRATION_DIR="$ROOT_DIR/docs/integration/ai-platform/v1"
SCHEMAS_DIR="$INTEGRATION_DIR/contracts/schemas"
EXAMPLES_DIR="$INTEGRATION_DIR/examples"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "AI Platform Contract Validation"
echo "========================================"
echo ""

# Проверяем наличие Python3
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is not installed${NC}"
    exit 1
fi

# Проверяем/устанавливаем jsonschema
if ! python3 -c "import jsonschema" 2>/dev/null; then
    echo -e "${YELLOW}Installing jsonschema module...${NC}"
    pip3 install --user jsonschema > /dev/null 2>&1
    echo -e "${GREEN}jsonschema installed${NC}"
    echo ""
fi

# Python скрипт для валидации
VALIDATOR=$(cat << 'PYTHON'
import json
import sys
from jsonschema import validate, ValidationError, Draft202012Validator

def validate_file(schema_path, example_path):
    with open(schema_path) as f:
        schema = json.load(f)
    with open(example_path) as f:
        example = json.load(f)

    # Удаляем $comment из примера (не часть данных)
    if "$comment" in example:
        del example["$comment"]

    try:
        validate(instance=example, schema=schema, cls=Draft202012Validator)
        return True, None
    except ValidationError as e:
        return False, str(e.message)

if __name__ == "__main__":
    schema_path = sys.argv[1]
    example_path = sys.argv[2]
    success, error = validate_file(schema_path, example_path)
    if success:
        print("OK")
        sys.exit(0)
    else:
        print(f"FAILED: {error}")
        sys.exit(1)
PYTHON
)

# Функция валидации
validate_example() {
    local schema_file="$1"
    local example_file="$2"
    local description="$3"

    echo -n "Validating: $description... "

    result=$(echo "$VALIDATOR" | python3 - "$schema_file" "$example_file" 2>&1)
    exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}OK${NC}"
        return 0
    else
        echo -e "${RED}FAILED${NC}"
        echo "  $result"
        return 1
    fi
}

# Проверяем существование файлов
check_file() {
    local file="$1"
    if [ ! -f "$file" ]; then
        echo -e "${RED}Error: File not found: $file${NC}"
        exit 1
    fi
}

echo "Checking required files..."
check_file "$SCHEMAS_DIR/decision.schema.json"
check_file "$EXAMPLES_DIR/start-job-response.json"
check_file "$EXAMPLES_DIR/clarify-response.json"
check_file "$EXAMPLES_DIR/reject-response.json"
echo ""

FAILED=0

echo "Validating Decision Response examples..."
echo "----------------------------------------"

# Валидация примеров decision response
validate_example \
    "$SCHEMAS_DIR/decision.schema.json" \
    "$EXAMPLES_DIR/start-job-response.json" \
    "start-job-response.json" || FAILED=1

validate_example \
    "$SCHEMAS_DIR/decision.schema.json" \
    "$EXAMPLES_DIR/clarify-response.json" \
    "clarify-response.json" || FAILED=1

validate_example \
    "$SCHEMAS_DIR/decision.schema.json" \
    "$EXAMPLES_DIR/reject-response.json" \
    "reject-response.json" || FAILED=1

echo ""

# Проверяем синтаксис command schema
echo "Validating JSON syntax..."
echo "----------------------------------------"

echo -n "Validating: command.schema.json syntax... "
if jq empty "$SCHEMAS_DIR/command.schema.json" 2>/dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    FAILED=1
fi

echo ""

# Выводим версию контрактов
if [ -f "$INTEGRATION_DIR/contracts/VERSION" ]; then
    VERSION=$(head -1 "$INTEGRATION_DIR/contracts/VERSION")
    echo "Contract version: $VERSION"
fi

echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}========================================"
    echo "All validations passed!"
    echo -e "========================================${NC}"
    exit 0
else
    echo -e "${RED}========================================"
    echo "Some validations failed!"
    echo -e "========================================${NC}"
    exit 1
fi
