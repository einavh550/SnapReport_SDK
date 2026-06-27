# SnapReport Backend

FastAPI backend server for the SnapReport SDK bug-reporting system.

## Requirements

- Python 3.11+
- MongoDB 6+ (local or Atlas)

## Setup

### 1. Create and activate a virtual environment

```bash
cd backend
python -m venv venv

# Windows
venv\Scripts\activate

# macOS / Linux
source venv/bin/activate
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Configure environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|---|---|---|
| `MONGODB_URL` | `mongodb://localhost:27017` | MongoDB connection string |
| `MONGODB_DB_NAME` | `snapreport` | Database name |
| `JWT_SECRET_KEY` | *(change me)* | Secret used to sign JWT tokens |
| `JWT_ALGORITHM` | `HS256` | JWT signing algorithm |
| `JWT_ACCESS_TOKEN_EXPIRE_MINUTES` | `60` | Token lifetime in minutes |
| `SCREENSHOT_STORAGE_PATH` | `app/storage/screenshots` | Local path for screenshot files |
| `MAX_SCREENSHOT_SIZE_MB` | `10` | Max allowed upload size in MB |

### 4. Start the server

```bash
# Run from the backend/ directory
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The interactive API docs will be available at:
- Swagger UI: http://localhost:8000/docs
- ReDoc:       http://localhost:8000/redoc

## API Overview

### Auth

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new developer account |
| POST | `/api/auth/login` | Log in and receive a JWT |

### Projects

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/projects` | Bearer JWT | Create a project and receive the raw API key (shown once) |
| GET | `/api/projects` | Bearer JWT | List all projects for the authenticated developer |
| POST | `/api/projects/{id}/regenerate-key` | Bearer JWT | Rotate the API key for a project |

### SDK Report Ingestion

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/sdk/reports` | `X-SnapReport-Api-Key` header | Upload a bug report (multipart: `metadata` JSON string + optional `screenshot` file) |

The API key is validated **before** any file is read or stored. Screenshots
must be `image/jpeg`, `image/webp`, or `image/png`, and are limited by
`MAX_SCREENSHOT_SIZE_MB`. The screenshot filename is derived from the
server-generated ticket id (clients cannot influence the path).

### Portal Reports

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/portal/reports` | Bearer JWT | List tickets. Filters: `projectId`, `status`, `osVersion`, `appVersion`, `deviceModel`, `dateFrom`, `dateTo`; paginated via `skip`/`limit` |
| GET | `/api/portal/reports/{ticketId}` | Bearer JWT | Full ticket detail (ownership enforced) |
| PATCH | `/api/portal/reports/{ticketId}/status` | Bearer JWT | Update status (`OPEN` / `IN_PROGRESS` / `RESOLVED` / `ARCHIVED`) |

### Storage

| Method | Path | Description |
|---|---|---|
| GET | `/storage/screenshots/{filename}` | Serve a stored screenshot (path-traversal protected) |

### Error Responses

SDK/report errors return a structured JSON body:

```json
{ "success": false, "error": "INVALID_API_KEY", "message": "API key is invalid." }
```

Error codes: `MISSING_API_KEY`, `INVALID_API_KEY`, `PROJECT_DISABLED`,
`INVALID_METADATA`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `SERVER_ERROR`.

### Health

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Service health check |

## Security Notes

- Developer passwords are hashed with bcrypt.
- API keys are hashed with SHA-256. The raw key is **never** stored.
- The raw API key is returned **only once** when a project is created.
- All project routes enforce ownership — developers can only access their own projects.
