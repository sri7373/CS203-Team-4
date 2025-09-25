# TARIFF Frontend (React + Vite)

A minimal, working UI for the TARIFF backend. Includes:
- Login & Register
- Tariff Calculator (breakdown)
- Rates Search (filter)

## Quick start

1) Install deps
```bash
npm install
```

2) Configure API base (defaults to localhost:8080)
Create `.env` and set:
```
VITE_API_BASE_URL=http://localhost:8080
```

3) Run dev server
```bash
npm run dev
```

Open http://localhost:5173

## Notes
- Requires the backend running with CORS enabled (the provided backend has permissive CORS for dev).
- Seed users: `admin/admin123`, `analyst/analyst123`.
