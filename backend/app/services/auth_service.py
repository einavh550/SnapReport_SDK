"""
Auth service — register, authenticate, and fetch developer accounts.
"""
import uuid
from typing import Any

from fastapi import HTTPException, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.security import hash_password, verify_password
from app.models.developer_account import build_developer_document
from app.schemas.auth_schema import RegisterRequest


async def register_developer(
    db: AsyncIOMotorDatabase,
    data: RegisterRequest,
) -> dict[str, Any]:
    existing = await db["developer_accounts"].find_one({"email": data.email})
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists.",
        )

    developer_id = f"dev_{uuid.uuid4().hex[:12]}"
    doc = build_developer_document(
        developer_id=developer_id,
        email=data.email,
        password_hash=hash_password(data.password),
    )
    await db["developer_accounts"].insert_one(doc)
    return doc


async def authenticate_developer(
    db: AsyncIOMotorDatabase,
    email: str,
    password: str,
) -> dict[str, Any]:
    doc = await db["developer_accounts"].find_one({"email": email})
    if not doc or not verify_password(password, doc["password_hash"]):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
        )
    return doc


async def get_developer_by_id(
    db: AsyncIOMotorDatabase,
    developer_id: str,
) -> dict[str, Any] | None:
    return await db["developer_accounts"].find_one({"_id": developer_id})
