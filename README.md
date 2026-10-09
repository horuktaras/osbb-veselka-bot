# OSBB Veselka Bot

Telegram-бот для подання заявок мешканцями ОСББ "Веселка" та їх обробки адміністрацією.

**Стек:** Java 21 · Spring Boot 3.5.5 · TelegramBots 9.5.0 · PostgreSQL · Flyway · Docker

---

## Можливості

### Для мешканців
- Подання заявки через діалог у приватному чаті (`/osbb` або `/start`)
- Типи заявок: Поломка, Скарга, Послуга
- Прикріплення фото або відео
- Сповіщення про зміну статусу з коментарем від ОСББ

### Для адміністрації (`/board`)
- Панель усіх заявок з фільтрацією за статусом та пагінацією
- Перегляд деталей заявки з медіа (фото/відео як підпис)
- Зміна статусу з можливістю додати коментар
- Сповіщення у виділений адмін-чат при нових заявках

### Захист від дублікатів
- AI-перевірка через Groq API — якщо знайдено схожі відкриті заявки, мешканець отримує попередження
- Дедуплікація Telegram `update_id` (Caffeine cache, TTL 60с)
- Захист від подвійного кліку підтвердження

---

## Статуси заявок

```
NEW → PROCESSED → IN_PROGRESS_OSBB ──→ VERIFICATION → CLOSED
                ↘ IN_PROGRESS_CONTRACTOR ↗
```

---

## Архітектура

```
OsbbBot (SpringLongPollingBot)
  └── UpdateDispatcher (Caffeine dedup)
        ├── MessageHandler   — діалог мешканця, команди
        └── CallbackHandler  — inline кнопки (заявки, board, статуси)

Services
  ├── ConversationService       — in-memory стан діалогу (ConcurrentHashMap)
  ├── RequestService            — CRUD заявок
  ├── AdminBoardService         — побудова board-повідомлень
  ├── AdminNotificationService  — нотифікації адмін-чату та мешканця
  ├── AiDuplicateService        — перевірка дублікатів через Groq API
  ├── AdminStatusCommentService — in-memory стан pending зміни статусу
  └── StatusChangeService       — застосування зміни статусу
```

---

## Команди бота

| Команда | Опис |
|---|---|
| `/start`, `/osbb` | Почати нову заявку |
| `/cancel` | Скасувати поточну заявку |
| `/board` | Панель заявок (тільки для адмінів) |

---

## База даних

Міграції Flyway (`src/main/resources/db/migration/`):

| Файл | Зміст |
|---|---|
| `V1__init_schema.sql` | Таблиця `requests` |
| `V2__add_source_chat_id.sql` | `source_chat_id` для нотифікацій мешканця |
| `V3__add_admin_users.sql` | Таблиця `admin_users` |
| `V4__add_status_comment.sql` | `status_comment` для коментарів до статусу |

Адміни додаються вручну в таблицю `admin_users` (поле `telegram_id`).

---

## Змінні середовища

| Змінна | Обов'язкова | Опис |
|---|---|---|
| `BOT_TOKEN` | Так | Токен бота від @BotFather |
| `BOT_USERNAME` | Так | Username бота (без @) |
| `ADMIN_CHAT_ID` | Так | ID чату/групи для адмін-нотифікацій |
| `DB_PASSWORD` | Так | Пароль PostgreSQL |
| `GROQ_API_KEY` | Ні | API-ключ Groq для AI-перевірки дублікатів |
| `GROQ_MODEL` | Ні | Модель Groq (за замовч. `llama-3.3-70b-versatile`) |
| `APP_PORT` | Ні | Порт застосунку (за замовч. `8080`) |

---

## Запуск

### Docker Compose

```bash
# 1. Скопіюй .env та заповни змінні
cp .env.example .env   # або редагуй .env напряму

# 2. Запуск
docker compose up --build -d
```

### Локально

```bash
# Потрібен запущений PostgreSQL
./gradlew bootRun
```

Spring Boot автоматично підхопить `application-local.yml` якщо є, або використає env-змінні.

---

## Збірка

```bash
./gradlew build          # збірка + тести
./gradlew bootJar        # тільки jar → build/libs/app.jar
```
