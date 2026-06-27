"""
Project document shape.

Stored in the `projects` collection.

Important: raw API keys are NEVER stored. Only the SHA-256 hash and a short
display prefix are persisted.
"""
from datetime import datetime, timezone
from typing import Any


def build_project_document(
    project_id: str,
    developer_id: str,
    app_name: str,
    api_key_hash: str,
    api_key_prefix: str,
) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    return {
        "_id": project_id,
        "developer_id": developer_id,
        "app_name": app_name,
        "api_key_hash": api_key_hash,
        "api_key_prefix": api_key_prefix,
        "is_active": True,
        "created_at": now,
        "updated_at": now,
    }
