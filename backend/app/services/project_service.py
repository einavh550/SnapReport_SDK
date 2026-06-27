"""
Project service — create projects, generate and hash API keys, list projects.
"""
import uuid
from datetime import datetime, timezone
from typing import Any

from fastapi import HTTPException, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.security import generate_api_key, hash_api_key, api_key_prefix
from app.models.project import build_project_document


async def create_project(
    db: AsyncIOMotorDatabase,
    developer_id: str,
    app_name: str,
) -> tuple[dict[str, Any], str]:
    """
    Create a new project and return (document, raw_api_key).
    The raw API key is returned once to the caller and never stored.
    """
    project_id = f"proj_{uuid.uuid4().hex[:12]}"
    raw_key = generate_api_key()

    doc = build_project_document(
        project_id=project_id,
        developer_id=developer_id,
        app_name=app_name,
        api_key_hash=hash_api_key(raw_key),
        api_key_prefix=api_key_prefix(raw_key),
    )
    await db["projects"].insert_one(doc)
    return doc, raw_key


async def list_projects(
    db: AsyncIOMotorDatabase,
    developer_id: str,
) -> list[dict[str, Any]]:
    cursor = db["projects"].find({"developer_id": developer_id}).sort("created_at", -1)
    return await cursor.to_list(length=100)


async def get_project_by_id(
    db: AsyncIOMotorDatabase,
    project_id: str,
    developer_id: str,
) -> dict[str, Any]:
    """Return a project, enforcing that it belongs to the requesting developer."""
    doc = await db["projects"].find_one({"_id": project_id})
    if not doc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Project not found.")
    if doc["developer_id"] != developer_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied.")
    return doc


async def regenerate_api_key(
    db: AsyncIOMotorDatabase,
    project_id: str,
    developer_id: str,
) -> tuple[dict[str, Any], str]:
    """Rotate the API key for a project. Returns (updated_doc, new_raw_api_key)."""
    doc = await get_project_by_id(db, project_id, developer_id)

    raw_key = generate_api_key()
    now = datetime.now(timezone.utc)

    await db["projects"].update_one(
        {"_id": project_id},
        {
            "$set": {
                "api_key_hash": hash_api_key(raw_key),
                "api_key_prefix": api_key_prefix(raw_key),
                "updated_at": now,
            }
        },
    )
    doc["api_key_prefix"] = api_key_prefix(raw_key)
    return doc, raw_key
