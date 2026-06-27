from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

from app.api.routes_auth import router as auth_router
from app.api.routes_portal import router as portal_router
from app.api.routes_projects import router as projects_router
from app.api.routes_reports import router as reports_router
from app.core.errors import SnapReportError
from app.db.database import close_db, connect_db, create_indexes
from app.services import storage_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_db()
    await create_indexes()
    yield
    await close_db()


app = FastAPI(
    title="SnapReport API",
    description="Backend for the SnapReport SDK bug-reporting system.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(SnapReportError)
async def snapreport_error_handler(request: Request, exc: SnapReportError) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content={"success": False, "error": exc.error, "message": exc.message},
    )


app.include_router(auth_router)
app.include_router(projects_router)
app.include_router(reports_router)
app.include_router(portal_router)


@app.get("/storage/screenshots/{filename}", tags=["Storage"])
async def get_screenshot(filename: str):
    path = storage_service.resolve_screenshot_path(filename)
    if path is None:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"success": False, "error": "NOT_FOUND", "message": "File not found."},
        )
    return FileResponse(path)


@app.get("/health", tags=["Health"])
async def health() -> dict:
    return {"status": "ok", "service": "SnapReport API"}
