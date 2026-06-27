"""
DeveloperAccount document shape.

Stored in the `developer_accounts` collection.
"""
from datetime import datetime, timezone
from typing import Any


def build_developer_document(
    developer_id: str,
    email: str,
    password_hash: str,
) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    return {
        "_id": developer_id,
        "email": email,
        "password_hash": password_hash,
        "created_at": now,
    }
