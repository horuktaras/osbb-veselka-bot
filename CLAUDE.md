# OSBB Veselka Bot — Project Context

## Overview

Production Telegram bot for **ОСББ "Веселка"** (Ukrainian homeowners association). Residents submit maintenance requests via private chat conversation; admins manage requests through an inline keyboard board.

## Tech Stack

| Layer | Tech |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.5 |
| Build | Gradle Kotlin DSL (`build.gradle.kts`) |
| DB | PostgreSQL + Flyway + Spring Data JPA |
| Telegram API | TelegramBots 9.5.0 (long-polling, `SpringLongPollingBot`) |
| Deployment | Docker + Docker Compose |

## Package Structure

Root package: `ua.horuktaras.osbb.bot`

```
bot/
  OsbbBot.java          — SpringLongPollingBot, registers /osbb and /board commands on startup
  UpdateDispatcher.java — routes Update to MessageHandler or CallbackHandler
  MessageHandler.java   — handles text messages and commands
  CallbackHandler.java  — handles inline keyboard callbacks
config/
  BotProperties.java    — @ConfigurationProperties record: token, username, adminChatId
  TelegramClientConfig.java
  AppConfig.java
model/
  entity/
    Request.java        — main entity, all request data
    AdminUser.java      — admin access control table
  dto/
    RequestDraft.java   — in-memory draft during conversation
  enums/
    ConversationStep    — AWAITING_NAME/CONTACT/URGENCY/TYPE/DESCRIPTION/MEDIA/CONFIRMATION
    RequestStatus       — NEW→PROCESSED→IN_PROGRESS_OSBB/IN_PROGRESS_CONTRACTOR→VERIFICATION→CLOSED
    RequestType         — BREAKAGE / COMPLAINT / SERVICE
    MediaType           — PHOTO / VIDEO
repository/
  RequestRepository.java
  AdminUserRepository.java
service/
  ConversationService.java      — in-memory ConcurrentHashMap<userId, RequestDraft> drafts
  RequestService.java           — DB CRUD for requests
  AdminBoardService.java        — builds paginated board messages (5/page), isAdmin() check
  AdminNotificationService.java — sends new request to admin chat, updates on status change, notifies resident
```

## Key Flows

### Resident submits a request (private chat)
1. `/start` or `/osbb` → starts draft
2. If user has `@username` → contact auto-filled, phone step skipped
3. Steps: Name → (Contact?) → Urgency → Type → Description → Media (optional) → Confirm
4. On confirm: saves to DB, sends notification to admin chat, stores `adminChatMessageId`

### Admin manages requests
- `/board` command → paginated list with filter buttons (by status)
- Click "📋 Заявка #N" → detail view with status-change buttons
- Status transitions are defined in `RequestStatus.nextStatuses()`:
  - NEW → PROCESSED
  - PROCESSED → IN_PROGRESS_OSBB | IN_PROGRESS_CONTRACTOR
  - IN_PROGRESS_* → VERIFICATION | CLOSED
  - VERIFICATION → IN_PROGRESS_OSBB | IN_PROGRESS_CONTRACTOR | CLOSED
  - CLOSED → (none)
- Admin access controlled via `admin_users` DB table (`AdminBoardService.isAdmin()`)

### Callback data formats
- `urgency:yes|no`
- `type:BREAKAGE|COMPLAINT|SERVICE`
- `media:skip`
- `confirm:yes|no`
- `status:<requestId>:<STATUS>`
- `board:<page>:<filter>` — list navigation
- `board:v:<requestId>:<returnPage>:<returnFilter>` — detail view
- `board:noop` — disabled nav button

## Database Schema (Flyway)

- `V1__init_schema.sql` — `requests` table
- `V2__add_source_chat_id.sql` — adds `source_chat_id` to requests
- `V3__add_admin_users.sql` — `admin_users` table

Key `requests` columns: `id, telegram_user_id, telegram_username, name, contact, urgent, type, description, media_file_id, media_type, status, source_chat_id, admin_chat_message_id, created_at, updated_at`

## Configuration

Environment variables required:
```
BOT_TOKEN
BOT_USERNAME
ADMIN_CHAT_ID
SPRING_DATASOURCE_URL   (default: jdbc:postgresql://localhost:5432/osbb_bot)
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Profiles: `application.yml` (prod), `application-local.yml` (local dev), `application-test.yml`

## Important Design Decisions

- **Conversation state is in-memory** (`ConcurrentHashMap`). No persistence of drafts — lost on restart.
- **Admin auth is DB-based** (`admin_users` table), not config-based. Add admins by inserting rows.
- **Status change from board vs. admin notification**: `CallbackHandler.handleStatusChange()` checks `adminChatMessageId` to decide whether to use `AdminNotificationService.updateRequestMessage()` (edit original notification) or re-render the board detail in-place.
- **Media messages**: when request has media, `adminChatMessageId` points to a photo/video message — only the reply markup can be edited (not the caption), so `EditMessageReplyMarkup` is used instead of `EditMessageText`.
- **Resident notification**: on every status update, `AdminNotificationService.notifyResident()` sends status update to `source_chat_id`.
- `ddl-auto: validate` — schema managed exclusively by Flyway.

## Build & Run

```bash
# Build
./gradlew build

# Run locally (needs .env or env vars)
./gradlew bootRun

# Docker
docker-compose up --build
```

Output jar: `build/libs/app.jar` (configured in `bootJar` task)