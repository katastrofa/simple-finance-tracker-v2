# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Full-stack personal finance tracker. Scala 3.6.4, single sbt build with cross-compiled (JVM + Scala.js) modules. Backend is http4s + cats-effect + doobie/MySQL; frontend is a Tyrian (Elm-architecture) Scala.js app.

## Commands

```bash
# Start MySQL (first run pulls image + applies migrations via deploy/docker-init.sh)
docker-compose up -d        # or: podman-compose up -d   (MySQL 8 on host port 3307)

# Run backend (dev). Dev config enables auth bypass when no Google creds are set.
sbt -Dconfig.file=backend/src/main/resources/dev.conf "backend/run"

# Build frontend JS bundle -> copied into tyrian-front/src/main/resources/ignore/
sbt "tyrianFront/fastOptJS::webpack"      # dev build
sbt "tyrianFront/fullOptJS::webpack"      # optimized build

# Tests (scalatest; lives in the shared module)
sbt test
sbt "sharedJvm/testOnly org.big.pete.sft.domain.DomainTest"   # single test class

# Production deployment artifact (assembly jar + logback + html into target/deployment)
sbt sftFullBuild
```

In IntelliJ, run `backend/Main` with VM option `-Dconfig.file=backend/src/main/resources/dev.conf`.

## Module layout (`build.sbt`)

- `shared` — crossProject (JS/JVM). Domain model + Circe codecs in `org.big.pete.sft.domain`. Enum JSON codecs are derived via the `stringEnumDecoder`/`stringEnumEncoder` macros in `domain.scala`. The single shared source of truth between backend and frontend.
- `db` — doobie DAOs (`...db.dao.{Users,Accounts,Categories,Transactions,General}`), depends on `sharedJvm`.
- `cache` — `BpCache` cats-effect in-memory cache (full / full-refresh variants), used for wallets/users/currencies.
- `backend` — http4s ember server. Depends on `db` + `cache`. See architecture below.
- `tyrianFront` — the **active** frontend (Tyrian). Depends on `sharedJs` + `scalajsToolz`.
- `scalajsToolz` — Scala.js JS-interop helpers (cookies via js-cookie, mathjs, JSON).
- `chartsJs` — Scala.js Chart.js bindings.

Note: the `frontend/` and `react-toolz/` directories are a legacy scalajs-react UI being replaced by `tyrian-front` (branch `front/tyrian`). They are **not** in the sbt build aggregation — don't edit them for current work.

## Backend architecture

- `Main.scala` (cats-effect `IOApp`) wires everything: loads typesafe `Config`, builds a `HikariTransactor`, optional `TLSContext`, the `BpCache` instances, then constructs the API classes and `SftV2Server`, and runs the http4s stream.
- API logic is split into per-resource classes in `...server.api.{General,Accounts,Categories,Transactions}`, each parameterized over `F[_]`.
- `SftV2Server.scala` assembles http4s `Routes`, applies the `AuthMiddleware`, serves static assets, and (in dev) serves `tyrian-front/src/main/resources/index-main.html`; in prod serves `./static-assets/index-main.html`.
- Auth: `AuthHelper` does Google OAuth (cookie `SftV2Auth`). **Dev bypass**: when `environment=dev` AND no `google.client-id` is configured, all requests authenticate as user ID 1 — so dev seed data must contain that user. `AccessHelper` enforces per-wallet permissions.
- Config: `reference.conf` holds prod defaults; `dev.conf` overrides for local. Keys: `server.*`, `db.{url,user,pass,poolSize}`, `login.secret`, `google.*`, `ssl.*`.

## Frontend architecture (Tyrian / Elm)

- `Main.scala` is the `TyrianIOApp[Msg, Model]`: `init` / `update` / `view` / `router`. Single `AppModel` holds all page state.
- State updates use **Monocle lenses** (`focus(_.x).replace(...)`); nested page state is delegated to each part's own `update`.
- `domain.Msg` is the central message ADT; sub-pages define their own message types nested under it.
- `parts/` = pages (transactions, wallets, header, sidebar), `component/` = reusable widgets (DropDown, DatePicker, inputs), `toolz/` = routing/http/cookies.
- API calls go through `ApiMsgHelper`; results return as `Msg.HttpSuccess`/`Msg.HttpError`.

## Database

- Flyway-style migrations in `db/src/main/resources/db/V*.sql`. **Ordering matters and is NOT alphabetical** (e.g. `V1.0.100` must run after `V1.0.3`) — `deploy/docker-init.sh` lists files in explicit order; add new migrations to that list too.
