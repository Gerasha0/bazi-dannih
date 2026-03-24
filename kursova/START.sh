#!/bin/bash
# ============================================================
#  Курсова робота — Інформаційна система мережі ресторанів
#  Швидкий старт
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$SCRIPT_DIR/sql"
APP_DIR="$SCRIPT_DIR/restaurant-app"

# ── Облікові дані БД ───────────────────────────────────────
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="restaurant_db"
DB_PASS="restaurant123"
DB_NAME="restaurant_network"

# ── Кольори ────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
die()     { error "$*"; exit 1; }

# ── Перевірки наявності утиліт ─────────────────────────────
command -v mysql >/dev/null 2>&1 || die "mysql client not found!"
command -v mvn   >/dev/null 2>&1 || die "mvn not found! Install Apache Maven."
command -v java  >/dev/null 2>&1 || die "java not found! Install JDK 21+."

MYSQL_CMD="mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} --protocol=TCP"

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════╗"
echo -e " Ресторанна мережа — Система управління           "
echo -e "╚══════════════════════════════════════════════════╝${NC}"
echo ""

# ── Перевірка підключення ──────────────────────────────────
info "Перевірка підключення до MySQL (${DB_USER}@${DB_HOST}:${DB_PORT})..."
if ! ${MYSQL_CMD} "${DB_NAME}" -e "SELECT 1;" >/dev/null 2>&1; then
    error "Не вдалося підключитися до MySQL або база '${DB_NAME}' не існує."
    echo ""
    echo "  Якщо БД ще не створена, виконай один раз:"
    echo "    sudo mysql < sql/01_create_database.sql"
    echo "    sudo mysql < sql/02_create_tables.sql"
    echo "    sudo mysql < sql/03_create_indexes.sql"
    echo "    mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < sql/04_insert_data.sql"
    exit 1
fi
success "MySQL підключення OK"

# ── Перевірка наявності таблиць ────────────────────────────
TABLE_COUNT=$(${MYSQL_CMD} -sN "${DB_NAME}" -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';" \
    2>/dev/null || echo 0)

if [ "${TABLE_COUNT:-0}" -lt 5 ] 2>/dev/null; then
    warn "Таблиці в БД '${DB_NAME}' відсутні. Спочатку виконай DDL-скрипти:"
    echo "    sudo mysql < sql/01_create_database.sql"
    echo "    sudo mysql < sql/02_create_tables.sql"
    echo "    sudo mysql < sql/03_create_indexes.sql"
    echo "    mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < sql/04_insert_data.sql"
    exit 1
fi
success "Знайдено ${TABLE_COUNT} таблиць у '${DB_NAME}'"

# ── Пропозиція перезалити тестові дані ────────────────────
echo ""
read -rp "Перезалити тестові дані? (поточні буде видалено) [y/N] " CONFIRM
if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
    SCRIPT="${SQL_DIR}/04_insert_data.sql"
    [[ -f "$SCRIPT" ]] || die "Файл не знайдено: $SCRIPT"
    info "Очищення таблиць..."
    ${MYSQL_CMD} "${DB_NAME}" -e "
        SET FOREIGN_KEY_CHECKS=0;
        TRUNCATE reservations; TRUNCATE order_items; TRUNCATE orders;
        TRUNCATE employees; TRUNCATE customers; TRUNCATE tables;
        TRUNCATE menu_item_ingredients; TRUNCATE menu_items;
        TRUNCATE ingredients; TRUNCATE categories;
        TRUNCATE positions; TRUNCATE restaurants;
        SET FOREIGN_KEY_CHECKS=1;" 2>/dev/null
    info "Завантаження тестових даних..."
    ${MYSQL_CMD} "${DB_NAME}" < "$SCRIPT" 2>&1 | grep -v "Warning" || true
    success "Тестові дані завантажено!"
fi

# ── Збірка ─────────────────────────────────────────────────
echo ""
info "Збірка Java-додатку..."
cd "$APP_DIR"
mvn clean package -q -DskipTests 2>&1 || die "Збірка завершилась з помилкою!"
success "Збірка успішна!"

# ── Запуск ─────────────────────────────────────────────────
echo ""
info "Запуск додатку..."
echo -e "${CYAN}Для виходу закрийте вікно JavaFX${NC}"
echo ""
mvn javafx:run -q
