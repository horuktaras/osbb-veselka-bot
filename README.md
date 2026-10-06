# OSBB Veselka Bot

A production-ready Telegram community moderation bot for the OSBB (Ukrainian homeowners association) community "Veselka".

Built with Spring Boot 3.4, Java 21 (virtual threads), TelegramBots 9.0.0, PostgreSQL, and Flyway.

---

## Features

- **New member verification** — restricts new members until they click an inline "I agree to the rules" button
- **Anti-spam (flood control)** — sliding-window rate limiter; auto-mutes offenders
- **Anti-link** — detects and removes HTTP/HTTPS links, Telegram invite links; configurable allowlist per chat
- **Blacklisted keywords** — deletes messages containing configured keywords
- **Warning system** — `/warn` command increments a counter; at the limit the user is auto-muted or auto-banned
- **Manual moderation** — `/mute`, `/unmute`, `/ban`, `/unban` commands
- **Verification expiration** — scheduler kicks users who fail to verify in time
- **Idempotent update processing** — each Telegram `update_id` is stored so duplicate updates are skipped
- **Admin menu** — inline buttons for viewing logs, pending verifications, and settings
- **Per-chat configuration** — all thresholds stored in the database with sensible defaults

---

## Architecture

```
OsbbBot (SpringLongPollingBot)
  └─ UpdateDispatcher
       ├─ NewMemberHandler        — verification flow for new joins
       ├─ MessageHandler          — spam/link detection + command routing
       └─ CallbackQueryHandler
            ├─ VerificationCallbackHandler
            └─ AdminMenuCallbackHandler

CommandProcessor
  ├─ /help, /warn, /warns, /mute, /unmute, /ban, /unban, /user, /settings

Services
  ├─ ChatConfigService            — per-chat settings (in-memory cache + DB)
  ├─ TelegramUserService          — upsert user records
  ├─ ChatMemberService            — membership, warnings, mute/ban state
  ├─ VerificationService          — create, complete, expire verifications
  ├─ ModerationService            — orchestrates warn/mute/ban flows
  ├─ TelegramApiService           — wraps TelegramClient with retry + error handling
  ├─ AdminPermissionService       — checks admin status (30-second cache)
  ├─ BroadcastService             — parallel broadcast using virtual threads
  └─ UpdateIdempotencyService     — deduplication via DB table

Schedulers
  ├─ VerificationExpirationScheduler  — runs every 60 s, kicks unverified users
  ├─ RateLimitCleanupScheduler        — runs every 5 min, cleans up FloodControl memory
  └─ ProcessedUpdateCleanupScheduler  — runs every hour, purges old processed_updates rows
```

---

## Prerequisites

- Java 21+
- Docker + Docker Compose
- A Telegram bot token (from @BotFather)

---

## Quick Start

### 1. Create a `.env` file

```env
BOT_TOKEN=your_telegram_bot_token
BOT_USERNAME=your_bot_username
```

### 2. Run with Docker Compose

```bash
docker-compose up --build
```

This starts PostgreSQL and the bot application. Flyway migrations run automatically on startup.

### 3. Run locally (without Docker)

Start a local PostgreSQL instance, then:

```bash
export BOT_TOKEN=your_token
export BOT_USERNAME=your_username
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/osbb_bot
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres

./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## Configuration

All per-chat settings have defaults defined in `chat_configs`. You can also edit `application.yml` for global settings.

| Property | Default | Description |
|---|---|---|
| `verification_enabled` | `true` | Require new members to click a verification button |
| `verification_timeout_seconds` | `600` | Time before unverified user is kicked (10 min) |
| `anti_link_enabled` | `true` | Delete messages containing links |
| `anti_spam_enabled` | `true` | Enable flood control |
| `max_messages_per_window` | `10` | Max messages in the rate-limit window |
| `rate_limit_window_seconds` | `60` | Rate limit window size in seconds |
| `warning_limit` | `3` | Warnings before automatic punishment |
| `warning_punishment` | `MUTE` | Punishment on warning limit: `MUTE` or `BAN` |
| `warning_punishment_duration_seconds` | `86400` | Duration of auto-mute/ban (24 h) |
| `allowed_domains` | `null` | Comma-separated list of allowed domains (null = block all links) |
| `blacklisted_keywords` | `null` | Comma-separated list of banned keywords |

---

## Bot Commands

All moderation commands require the calling user to be a chat administrator.

| Command | Description |
|---|---|
| `/help` | List all available commands |
| `/warn <userId> [reason]` | Issue a warning to a user |
| `/warns <userId>` | Show warning count and history for a user |
| `/mute <userId> <duration> [reason]` | Mute a user (durations: 30m, 2h, 1d, 7d) |
| `/unmute <userId> [reason]` | Remove mute from a user |
| `/ban <userId> [reason]` | Ban a user from the chat |
| `/unban <userId> [reason]` | Unban a previously banned user |
| `/user <userId>` | Show membership info, verification status, warnings |
| `/settings` | Show current chat configuration with admin action buttons |

---

## Database Schema

Managed by Flyway. Migration file: `src/main/resources/db/migration/V1__init_schema.sql`

Tables:
- `telegram_users` — basic user metadata
- `chat_configs` — per-chat configuration
- `chat_members` — membership state (verification, warnings, mute/ban) with optimistic locking
- `verifications` — verification session records
- `moderation_logs` — audit log of all moderation actions
- `processed_updates` — deduplication table for Telegram update IDs

---

## Running Tests

Unit tests (no Docker required):
```bash
./gradlew test --tests "ua.horuktaras.osbb.bot.util.*"
./gradlew test --tests "ua.horuktaras.osbb.bot.moderation.*"
./gradlew test --tests "ua.horuktaras.osbb.bot.service.VerificationServiceTest"
./gradlew test --tests "ua.horuktaras.osbb.bot.service.ModerationServiceTest"
./gradlew test --tests "ua.horuktaras.osbb.bot.service.AdminPermissionServiceTest"
```

Integration tests (requires Docker for Testcontainers):
```bash
./gradlew test --tests "ua.horuktaras.osbb.bot.integration.*"
```

All tests:
```bash
./gradlew test
```

---

## Health & Metrics

The app exposes Spring Boot Actuator endpoints:

- `GET /actuator/health` — application health
- `GET /actuator/info` — app info
- `GET /actuator/metrics` — metrics
- `GET /actuator/prometheus` — Prometheus-compatible metrics

---

## Project Structure

```
src/main/java/ua/horuktaras/osbb/bot/
├── OsbbVeselkaBotApplication.java
├── bot/
│   ├── OsbbBot.java
│   ├── callback/
│   │   ├── AdminMenuCallbackHandler.java
│   │   ├── CallbackQueryHandler.java
│   │   └── VerificationCallbackHandler.java
│   ├── command/
│   │   ├── BanCommand.java
│   │   ├── BotCommand.java         (interface)
│   │   ├── CommandProcessor.java
│   │   ├── HelpCommand.java
│   │   ├── MuteCommand.java
│   │   ├── SettingsCommand.java
│   │   ├── UnbanCommand.java
│   │   ├── UnmuteCommand.java
│   │   ├── UserCommand.java
│   │   ├── WarnCommand.java
│   │   └── WarnsCommand.java
│   └── handler/
│       ├── MessageHandler.java
│       ├── NewMemberHandler.java
│       └── UpdateDispatcher.java
├── config/
│   ├── AppConfig.java
│   ├── BotProperties.java
│   └── TelegramClientConfig.java
├── exception/
│   ├── BotException.java
│   ├── PermissionDeniedException.java
│   ├── UserNotFoundException.java
│   └── VerificationException.java
├── model/
│   ├── dto/
│   │   ├── ModerationResult.java
│   │   └── UserInfo.java
│   ├── entity/
│   │   ├── ChatConfig.java
│   │   ├── ChatMember.java
│   │   ├── ModerationLog.java
│   │   ├── ProcessedUpdate.java
│   │   ├── TelegramUser.java
│   │   └── Verification.java
│   └── enums/
│       ├── ModerationAction.java
│       ├── VerificationStatus.java
│       └── WarningPunishment.java
├── moderation/
│   ├── FloodControl.java
│   └── LinkDetector.java
├── repository/
│   ├── ChatConfigRepository.java
│   ├── ChatMemberRepository.java
│   ├── ModerationLogRepository.java
│   ├── ProcessedUpdateRepository.java
│   ├── TelegramUserRepository.java
│   └── VerificationRepository.java
├── scheduler/
│   ├── ProcessedUpdateCleanupScheduler.java
│   ├── RateLimitCleanupScheduler.java
│   └── VerificationExpirationScheduler.java
├── service/
│   ├── AdminPermissionService.java
│   ├── BroadcastService.java
│   ├── ChatConfigService.java
│   ├── ChatMemberService.java
│   ├── ModerationLogService.java
│   ├── ModerationService.java
│   ├── TelegramApiService.java
│   ├── TelegramUserService.java
│   ├── UpdateIdempotencyService.java
│   └── VerificationService.java
├── util/
│   ├── DurationParser.java
│   ├── MessageFormatter.java
│   └── UserUtils.java
└── verification/
    └── VerificationKeyboardFactory.java
```

---

## Notes

- The bot token is never logged. It is bound via `BotProperties` and only passed to `OkHttpTelegramClient`.
- All messages use `parseMode("HTML")` for consistent formatting.
- Warning increments use `SERIALIZABLE` isolation to prevent race conditions.
- `TelegramApiService` retries transient errors (429 rate limit) up to 3 times with exponential backoff. 403/400 errors are not retried.
- Ban/unban operations are idempotent and are never retried to avoid double-actions.
