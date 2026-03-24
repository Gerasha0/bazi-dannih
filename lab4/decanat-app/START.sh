#!/bin/bash
# Скрипт для запуску додатку Деканат

# Кольори для виводу
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}  Запуск додатку ДЕКАНАТ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# Перевірка Java
echo -e "${YELLOW}Перевіряю Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java не знайдено!${NC}"
    echo "Встановіть Java 21: sudo apt install openjdk-21-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
echo -e "${GREEN}✅ Java версія: $JAVA_VERSION${NC}"
echo ""

# Перевірка MySQL
echo -e "${YELLOW}Перевіряю MySQL...${NC}"
if ! systemctl is-active --quiet mysql; then
    echo -e "${RED}❌ MySQL не запущено!${NC}"
    echo "Запустіть MySQL: sudo systemctl start mysql"
    exit 1
fi
echo -e "${GREEN}✅ MySQL запущено${NC}"
echo ""

# Перевірка БД
echo -e "${YELLOW}Перевіряю базу даних decanat_lab3...${NC}"
DB_EXISTS=$(mysql -u decanat_user -pdecanat123 -e "SHOW DATABASES LIKE 'decanat_lab3';" 2>/dev/null | grep decanat_lab3)
if [ -z "$DB_EXISTS" ]; then
    echo -e "${RED}❌ База даних decanat_lab3 не знайдена!${NC}"
    echo "Створіть БД: mysql -u root -p < ../lab3/00_setup_lab3_database.sql"
    exit 1
fi
echo -e "${GREEN}✅ База даних знайдена${NC}"
echo ""

# Перевірка JAR файлу
if [ ! -f "target/decanat-app.jar" ]; then
    echo -e "${RED}❌ JAR файл не знайдено!${NC}"
    echo "Виконайте збірку: mvn clean package"
    exit 1
fi
echo -e "${GREEN}✅ JAR файл знайдено ($(du -h target/decanat-app.jar | cut -f1))${NC}"
echo ""

# Запуск
echo -e "${GREEN}🚀 Запускаю додаток...${NC}"
echo ""
# Очищення змінних середовища GTK від Snap для уникнення помилок glibc
# -Dsun.java2d.uiScale=1.5 використовується для масштабування UI на Linux (можна змінити на 1.25, 2.0 тощо, якщо буде занадто крупно/дрібно)
env -u GTK_PATH -u GIO_MODULE_DIR -u GTK_IM_MODULE_FILE java -Dsun.java2d.uiScale=1.5 -jar target/decanat-app.jar
