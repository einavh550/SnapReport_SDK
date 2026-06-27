# SnapReport Portal

The developer web portal for the SnapReport SDK, built with React + TypeScript +
Tailwind CSS (Vite). It consumes the FastAPI backend REST API.

## Requirements

- Node.js 18+ and npm

## Setup

### 1. Install dependencies

```bash
cd portal
npm install
```

### 2. Configure the backend URL (optional)

By default the portal talks to `http://localhost:8000`. To override, copy the
example env file and edit it:

```bash
cp .env.example .env
```

```
VITE_API_BASE_URL=http://localhost:8000
```

### 3. Run the dev server

```bash
npm run dev
```

Open http://localhost:5173.

> Make sure the backend (`backend/`) and MongoDB are running first.

### Production build

```bash
npm run build
npm run preview
```

## Features

- **Auth** — register, login, logout (JWT stored in `localStorage` for the demo).
- **Projects** — create projects, view API key prefix, copy the raw API key
  shown **once** on creation, and regenerate keys.
- **Dashboard** — totals by status, breakdowns by Android version and app
  version, and recent reports.
- **Ticket feed** — screenshot cards with status, device, app version, date, and
  description; filter by project, status, Android version, app version, device
  model, and date range.
- **Ticket inspector** — large screenshot on the left; status dropdown, device
  metadata, and app metadata on the right; update status inline.

## Project structure

```
src/
├── api/            # axios client + auth/projects/reports modules
├── components/     # Layout, ProtectedRoute, TicketCard, TicketFilters, StatusBadge
├── context/        # AuthContext (token + login/register/logout)
├── pages/          # Login, Register, Dashboard, Projects, TicketFeed, TicketDetail, Settings
├── types/          # TypeScript types mirroring the backend API
├── App.tsx         # routes
└── main.tsx        # entry
```

## Security note

JWT is stored in `localStorage` for simplicity. Production deployments should
use secure, http-only cookies to reduce the risk of token theft via XSS.
