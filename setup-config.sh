#!/bin/bash
echo "⚙️  Настройка конфигурации Emergency Management System"
echo "====================================================="
echo ""

# Создаем необходимые директории
echo "1. Создание директорий..."
mkdir -p uploads logs postgres-backups demo/reports

# Создаем конфигурационные файлы из примеров
echo "2. Создание конфигурационных файлов..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "   ✅ .env создан из примера"
    echo "   ⚠️  Отредактируйте: nano .env"
else
    echo "   ⚠️  .env уже существует"
fi

if [ ! -f "docker-compose.yml" ]; then
    cp docker-compose.yml.example docker-compose.yml
    echo "   ✅ docker-compose.yml создан из примера"
else
    echo "   ⚠️  docker-compose.yml уже существует"
fi

# Настройка Spring конфигураций
cd demo
if [ ! -f "src/main/resources/application.yml" ]; then
    cp src/main/resources/application.yml.example src/main/resources/application.yml
    echo "   ✅ application.yml создан из примера"
else
    echo "   ⚠️  application.yml уже существует"
fi

if [ ! -f "src/main/resources/application-docker.yml" ]; then
    cp src/main/resources/application-docker.yml.example src/main/resources/application-docker.yml
    echo "   ✅ application-docker.yml создан из примера"
else
    echo "   ⚠️  application-docker.yml уже существует"
fi
cd ..

# Генерация секретов
echo ""
echo "3. Генерация секретов..."
if [ -f "generate-secrets.sh" ]; then
    echo "   Запустите скрипт для генерации JWT секрета:"
    echo "   ./generate-secrets.sh"
    echo "   Затем добавьте сгенерированные значения в .env файл"
else
    echo "   ⚠️  Скрипт generate-secrets.sh не найден"
fi

echo ""
echo "4. Инструкция по настройке:"
echo "   ========================="
echo "   1. Отредактируйте .env файл:"
echo "      nano .env"
echo ""
echo "   2. Установите следующие значения:"
echo "      - POSTGRES_PASSWORD: надежный пароль для PostgreSQL"
echo "      - JWT_SECRET: сгенерируйте командой: openssl rand -base64 32"
echo "      - TELEGRAM_BOT_TOKEN: токен вашего бота (опционально)"
echo "      - TELEGRAM_CHAT_ID: ваш chat ID (опционально)"
echo ""
echo "   3. Запустите проект:"
echo "      docker-compose up --build -d"
echo ""
echo "   4. Приложение будет доступно: http://localhost:8080"
echo ""
echo "   5. Тестовые учетные данные:"
echo "      - admin / admin"
echo "      - manager / manager"
echo "      - user / user"
echo ""
echo "✅ Настройка завершена!"