from fastapi import APIRouter

from app.core.security import create_access_token
from app.db.database import get_database
from app.schemas.auth_schema import (
    DeveloperResponse,
    LoginRequest,
    RegisterRequest,
    TokenResponse,
)
from app.services.auth_service import authenticate_developer, register_developer

router = APIRouter(prefix="/api/auth", tags=["Auth"])


@router.post("/register", response_model=DeveloperResponse, status_code=201)
async def register(data: RegisterRequest) -> DeveloperResponse:
    db = get_database()
    doc = await register_developer(db, data)
    return DeveloperResponse(id=doc["_id"], email=doc["email"])


@router.post("/login", response_model=TokenResponse)
async def login(data: LoginRequest) -> TokenResponse:
    db = get_database()
    doc = await authenticate_developer(db, data.email, data.password)
    token = create_access_token(subject=doc["_id"])
    return TokenResponse(access_token=token)
