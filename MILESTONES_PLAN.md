# SnapReport SDK — Milestones Plan

This document is the execution roadmap for building the **SnapReport SDK** system, a three-module Android bug-reporting platform consisting of:

1. **Android SDK Client** (Kotlin)
2. **Python FastAPI Backend Server** (MongoDB + local file storage)
3. **React + TypeScript Developer Portal** (Tailwind CSS)

The work is divided into **5 milestones**. Each milestone lists its goals, concrete technical tasks, the exact files to be created/modified, and the logical order of execution. Milestones are sequenced so that each one produces something testable that the next milestone depends on.

---

## Monorepo Target Structure

```
snapreport-system/
│
├── MILESTONES_PLAN.md          # This file
├── README.md                   # Final project documentation (Milestone 5)
│
├── backend/                    # Python FastAPI server (Milestones 1 & 2)
│   ├── app/
│   └── requirements.txt
│
├── portal/                     # React + TS + Tailwind portal (Milestone 3)
│   ├── src/
│   └── package.json
│
└── android/                    # Android Studio workspace (Milestones 4 & 5)
    ├── app/                    # Demo application
    └── snapreport-sdk/         # Android library module (the SDK)
```

---

## MILESTONE 1 — Backend Infrastructure & Database

**Goal:** Stand up the FastAPI project, connect to MongoDB, and implement developer authentication plus project/API-key generation. By the end, a developer can register, log in, create a project, and receive a one-time raw API key (stored hashed).

### Tasks

1. **Initialize FastAPI project skeleton** under `backend/`.
2. **Configure environment & settings** (Mongo URI, JWT secret, token expiry, storage path, max upload size).
3. **Set up MongoDB connection** using an async driver (Motor) with a `get_database()` dependency.
4. **Implement security utilities**: password hashing (passlib/bcrypt), JWT create/verify, API key generation (`sr_live_` + 32 random chars), API key hashing.
5. **Define data models** for `DeveloperAccount` and `Project` (BSON document shapes + helpers).
6. **Define Pydantic schemas** for auth (register/login requests, token responses) and projects (create request, project response with one-time `apiKey`).
7. **Implement auth service** (register, authenticate, fetch current developer from JWT).
8. **Implement project service** (create project, list projects, regenerate API key, store only `apiKeyHash` + `apiKeyPrefix`).
9. **Implement auth routes**: `POST /api/auth/register`, `POST /api/auth/login`.
10. **Implement project routes**: `POST /api/projects`, `GET /api/projects`, (regenerate endpoint stubbed for later).
11. **Wire JWT auth dependency** to protect project routes; enforce project ownership.
12. **Create database indexes** (unique `email`; `developerId`, `apiKeyHash` on projects).
13. **Verify** with FastAPI interactive docs (`/docs`): register → login → create project → list projects.

### Files to Create

```
backend/requirements.txt
backend/README.md
backend/.env.example
backend/app/__init__.py
backend/app/main.py
backend/app/core/__init__.py
backend/app/core/config.py
backend/app/core/security.py
backend/app/db/__init__.py
backend/app/db/database.py
backend/app/models/__init__.py
backend/app/models/developer_account.py
backend/app/models/project.py
backend/app/schemas/__init__.py
backend/app/schemas/auth_schema.py
backend/app/schemas/project_schema.py
backend/app/services/__init__.py
backend/app/services/auth_service.py
backend/app/services/project_service.py
backend/app/api/__init__.py
backend/app/api/deps.py
backend/app/api/routes_auth.py
backend/app/api/routes_projects.py
```

### Execution Order
config → database → security → models → schemas → services → deps (auth guard) → routes → main.py wiring → indexes → manual verification via `/docs`.

---

## MILESTONE 2 — Report Ingestion API

**Goal:** Accept multipart bug reports from the SDK, validate the API key, store the screenshot file locally, persist a `ReportTicket` in MongoDB, and expose portal APIs to list/inspect/update tickets and serve screenshots.

### Tasks

1. **Define `ReportTicket` model** (document shape with embedded `deviceMetadata` / `appMetadata`, status, image URL, timestamps).
2. **Define report schemas**: incoming metadata JSON schema, upload success/failure responses, ticket list item, ticket detail, status-update request.
3. **Implement storage service**: ensure screenshots directory exists, sanitize filenames, prevent path traversal, write/serve files, enforce file type + size limits.
4. **Implement API-key auth dependency for SDK**: read `X-SnapReport-Api-Key`, hash, look up active project, reject with structured 401 on missing/invalid/disabled.
5. **Implement report service**: parse + validate metadata, create ticket record, link stored image URL, generate `ticketId`.
6. **Implement SDK report route**: `POST /api/sdk/reports` (multipart: `metadata` JSON string + optional `screenshot` file), async handler, validate API key **before** storing the file.
7. **Implement portal report routes**:
   - `GET /api/portal/reports` (filter by projectId/status/osVersion/appVersion/device; paginated).
   - `GET /api/portal/reports/{ticketId}` (full detail; enforce ownership).
   - `PATCH /api/portal/reports/{ticketId}/status` (OPEN / IN_PROGRESS / RESOLVED / ARCHIVED).
8. **Implement static screenshot serving**: `GET /storage/screenshots/{filename}` with path-traversal protection.
9. **Standardize error responses** with codes (`MISSING_API_KEY`, `INVALID_API_KEY`, `PROJECT_DISABLED`, `INVALID_METADATA`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `SERVER_ERROR`).
10. **Create report indexes** (`projectId`, `developerId`, `status`, `timestamp`, `deviceMetadata.androidVersion`, `appMetadata.appVersionName`).
11. **Verify end-to-end** with a curl/Postman multipart upload → ticket appears via portal list API → screenshot served.

### Files to Create

```
backend/app/models/report_ticket.py
backend/app/schemas/report_schema.py
backend/app/services/report_service.py
backend/app/services/storage_service.py
backend/app/api/routes_reports.py       # POST /api/sdk/reports
backend/app/api/routes_portal.py        # portal report list/detail/status
backend/app/storage/screenshots/.gitkeep
```

### Files to Modify
```
backend/app/main.py                      # register report + portal routers, static screenshot route
backend/app/core/security.py             # API key hashing/verify (if extended)
backend/app/api/deps.py                  # add SDK API-key dependency
```

### Execution Order
report model → report/storage schemas → storage service → SDK API-key dep → report service → SDK upload route → portal routes → static serving → error codes → indexes → manual multipart verification.

---

## MILESTONE 3 — Developer Portal (React)

**Goal:** A working React + TypeScript + Tailwind portal that lets a developer register/login, create projects (and view the one-time API key), browse the ticket feed with filters, inspect ticket details, and update ticket status — all consuming the FastAPI backend.

### Tasks

1. **Initialize React + TypeScript project** in `portal/` (Vite) and install **Tailwind CSS**.
2. **Configure API client** (Axios/fetch wrapper) with base URL, JWT injection, and error handling.
3. **Implement auth state** (context/store) with JWT storage (localStorage for demo; note production should use secure cookies).
4. **Build routing** for: `/login`, `/register`, `/dashboard`, `/projects`, `/projects/:projectId`, `/projects/:projectId/tickets`, `/projects/:projectId/tickets/:ticketId`, `/settings`.
5. **Build Register & Login pages** wired to `/api/auth/*`.
6. **Build Projects page**: list projects, create project modal, **show raw API key once** with copy button, show API key prefix afterward.
7. **Build Dashboard**: totals (total / open / in-progress / resolved), reports by Android version, by app version, recent reports.
8. **Build Ticket Feed**: card grid with screenshot thumbnail, status, device model, Android version, app version, timestamp, short description.
9. **Build Filters**: date range, status, app version, Android version, device model (call portal list API with query params).
10. **Build Ticket Inspector**: large screenshot (left); status dropdown, description, userId, device metadata, app metadata, timestamp (right). Wire status update to `PATCH .../status`.
11. **Add protected-route guard** + logout.
12. **Verify** full portal flow against the running backend.

### Files to Create (representative)

```
portal/package.json
portal/index.html
portal/vite.config.ts
portal/tailwind.config.js
portal/postcss.config.js
portal/tsconfig.json
portal/.env.example
portal/src/main.tsx
portal/src/App.tsx
portal/src/index.css
portal/src/api/client.ts
portal/src/api/auth.ts
portal/src/api/projects.ts
portal/src/api/reports.ts
portal/src/types/index.ts
portal/src/context/AuthContext.tsx
portal/src/components/ProtectedRoute.tsx
portal/src/components/Layout.tsx
portal/src/components/TicketCard.tsx
portal/src/components/TicketFilters.tsx
portal/src/components/StatusBadge.tsx
portal/src/pages/Login.tsx
portal/src/pages/Register.tsx
portal/src/pages/Dashboard.tsx
portal/src/pages/Projects.tsx
portal/src/pages/ProjectDetail.tsx
portal/src/pages/TicketFeed.tsx
portal/src/pages/TicketDetail.tsx
portal/src/pages/Settings.tsx
```

### Execution Order
project init + Tailwind → API client + types → auth context + routing/guards → Register/Login → Projects (API key reveal) → Dashboard → Ticket Feed + Filters → Ticket Inspector + status update → end-to-end verification.

---

## MILESTONE 4 — Android SDK Core (Kotlin)

**Goal:** Initialize the Android workspace with a demo `app` module and a `snapreport-sdk` library module. Implement SDK initialization/config, metadata collection, and screenshot capture (PixelCopy + Canvas fallback + FLAG_SECURE handling). Apply MVVM where appropriate.

### Tasks

1. **Initialize Android project** in `android/` with Gradle, a demo `app` module, and a `snapreport-sdk` library module (min SDK 24+).
2. **Configure SDK module dependencies** (Retrofit/OkHttp, Coroutines, WorkManager, lifecycle).
3. **Implement config objects**: `SnapReportConfig` (all spec fields/defaults) and `SnapReportEnvironment`.
4. **Implement the public `SnapReportSdk` object**: `init`, `setUserId`, `triggerReport`, `enableDebugLogging`, `shutdown` plus internal private functions (activity resolution, capture, metadata, compress, enqueue/store, sanitize).
5. **Implement lifecycle observer** to track the current foreground Activity via `ActivityLifecycleCallbacks` (weak references to avoid leaks).
6. **Implement `MetadataCollector`** producing `DeviceMetadata` (manufacturer, model, OS, sdkInt, battery, charging, RAM, storage, network type, locale, timezone) and `AppMetadata` (package, version name/code, timestampMicros, screen size, orientation).
7. **Implement data models**: `DeviceMetadata`, `AppMetadata`, `ReportPayload`, `ReportDraft`, `UploadResult`.
8. **Implement `ScreenshotCapture` interface** + `PixelCopyScreenshotCapture` (primary) and `ViewCanvasScreenshotCapture` (fallback).
9. **Implement FLAG_SECURE handling**: if window is secure, skip screenshot and set `screenshotBlockedReason = "FLAG_SECURE"`.
10. **Implement `ImageCompressor`** (resize to `maxScreenshotWidth`, preserve aspect, JPEG/WebP at quality, target < 500 KB).
11. **Implement networking layer**: `SnapReportApi` (Retrofit multipart `POST /api/sdk/reports` with `X-SnapReport-Api-Key`), `NetworkClientFactory`, `ReportUploader`.
12. **Implement utilities**: `Logger` (no secrets), `TimeProvider`, `SensitiveViewMasker` + `PrivacyConfig` (interfaces/skeletons for privacy rules).
13. **Wire demo app** to call `SnapReportSdk.init(...)` and provide a manual "Report a bug" button (MVVM screen).
14. **Verify** init + metadata + screenshot capture run without crashing the host app.

### Files to Create (representative)

```
android/settings.gradle(.kts)
android/build.gradle(.kts)
android/gradle.properties
android/snapreport-sdk/build.gradle(.kts)
android/snapreport-sdk/src/main/AndroidManifest.xml
android/snapreport-sdk/src/main/java/com/snapreport/sdk/SnapReportSdk.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/config/SnapReportConfig.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/config/SnapReportEnvironment.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/capture/ScreenshotCapture.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/capture/PixelCopyScreenshotCapture.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/capture/ViewCanvasScreenshotCapture.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/metadata/MetadataCollector.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/metadata/DeviceMetadata.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/metadata/AppMetadata.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/model/ReportPayload.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/model/ReportDraft.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/model/UploadResult.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/network/SnapReportApi.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/network/ReportUploader.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/network/NetworkClientFactory.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/privacy/SensitiveViewMasker.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/privacy/PrivacyConfig.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/util/ImageCompressor.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/util/Logger.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/util/TimeProvider.kt
android/app/build.gradle(.kts)
android/app/src/main/AndroidManifest.xml
android/app/src/main/java/com/example/snapreportdemo/MyApplication.kt
android/app/src/main/java/com/example/snapreportdemo/MainActivity.kt
```

### Execution Order
Gradle/workspace + modules → config → models → lifecycle/Activity tracking → MetadataCollector → ScreenshotCapture (PixelCopy → Canvas → FLAG_SECURE) → ImageCompressor → networking → utils/privacy skeletons → public `SnapReportSdk` wiring → demo app integration → verify.

---

## MILESTONE 5 — Trigger, Offline Queue & Integration

**Goal:** Complete the SDK with shake detection, the user-facing report dialog, offline caching with WorkManager retry, and a verified end-to-end demo. Finish with project-wide documentation.

### Tasks

1. **Implement `ShakeDetector`** using `SensorManager` accelerometer (magnitude vs threshold, cooldown, register only in foreground, `SENSOR_DELAY_UI`).
2. **Wire shake → `triggerReportInternal()`** through the same pipeline as the manual trigger; respect `enableShakeTrigger`.
3. **Build `ReportDialogActivity` / `ReportDialogFragment`**: show screenshot preview, "What happened?" input, Cancel / Send; handle dialog states (Capturing / Reviewing / Uploading / Success / Failed-but-saved). Optional `AnnotationView` skeleton.
4. **Implement offline storage**: `OfflineReportStore` + `LocalReportEntity` (Room or encrypted local file) to persist payload + screenshot bytes.
5. **Implement `ReportUploadWorker`** (CoroutineWorker): load pending reports, attempt upload, delete on success, stop on invalid API key, retry on transient/network errors; constrain to network-connected (and Wi-Fi-only if configured).
6. **Schedule the retry worker** from `SnapReportSdk.init` and after offline saves.
7. **Implement online/offline decision** in `enqueueUpload`: upload immediately when online, otherwise store offline and schedule retry.
8. **Harden error handling**: ensure all SDK failures are swallowed/logged and never crash the host app (covers all spec error cases).
9. **Integrate into demo app**: shake on emulator + manual button → capture → dialog → upload → ticket visible in portal; toggle airplane mode to validate offline queue + retry.
10. **Write root `README.md`** (overview, architecture diagram, tech stack, backend/portal/Android setup, API key generation, SDK init, public functions, API endpoints, DB structure, privacy, known limitations, future improvements).
11. **Write API documentation** and demo instructions; confirm all Acceptance Criteria (Section 26) pass.

### Files to Create (representative)

```
android/snapreport-sdk/src/main/java/com/snapreport/sdk/sensor/ShakeDetector.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/ui/ReportDialogActivity.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/ui/ReportDialogFragment.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/ui/AnnotationView.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/storage/OfflineReportStore.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/storage/LocalReportEntity.kt
android/snapreport-sdk/src/main/java/com/snapreport/sdk/worker/ReportUploadWorker.kt
android/snapreport-sdk/src/main/res/layout/   # dialog layouts
README.md
docs/API.md
docs/DEMO.md
```

### Files to Modify
```
android/snapreport-sdk/src/main/java/com/snapreport/sdk/SnapReportSdk.kt   # wire shake, dialog, offline, worker scheduling
android/snapreport-sdk/build.gradle(.kts)                                  # Room/WorkManager deps
android/app/src/main/java/com/example/snapreportdemo/MainActivity.kt       # demo trigger button
```

### Execution Order
ShakeDetector → trigger wiring → ReportDialog (+ states) → OfflineReportStore/entity → ReportUploadWorker → worker scheduling + online/offline routing → error hardening → demo integration & offline test → README + API docs + acceptance verification.

---

## Cross-Cutting Principles (apply across all milestones)

- **Security:** hash passwords and API keys; JWT for portal; API key header for SDK; enforce project ownership on every portal request.
- **Privacy:** anonymous optional user ID; respect FLAG_SECURE; collect no sensitive personal data.
- **Storage separation:** large screenshots → file storage; searchable metadata → MongoDB.
- **Resilience:** SDK must never crash the host app; backend returns structured error codes.
- **Performance:** async upload, off-main-thread work, image compression, paginated feeds, foreground-only sensors.

---

## Dependency Flow Between Milestones

```
M1 (auth + projects + API keys)
        │
        ▼
M2 (report ingestion + portal report APIs)   ──►   M3 (portal UI consumes APIs)
        │
        ▼
M4 (SDK core: init, metadata, capture)
        │
        ▼
M5 (trigger + offline + end-to-end integration + docs)
```

Milestone 1 unblocks everything (API keys are required by the SDK and portal). Milestone 2 unblocks both the portal feed (M3) and SDK uploads (M4/M5). Milestone 3 can proceed in parallel once M2's APIs exist. Milestones 4–5 require a working backend to upload against.
