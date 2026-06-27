"""
ReportTicket document shape.

Stored in the `report_tickets` collection.

Large binary screenshots are NOT stored in the document — only a relative
image URL pointing at the file in local/object storage. Searchable metadata
(device + app info) is embedded for fast filtering.
"""
from datetime import datetime, timezone
from typing import Any

# Allowed ticket lifecycle statuses.
STATUS_OPEN = "OPEN"
STATUS_IN_PROGRESS = "IN_PROGRESS"
STATUS_RESOLVED = "RESOLVED"
STATUS_ARCHIVED = "ARCHIVED"

ALLOWED_STATUSES = {STATUS_OPEN, STATUS_IN_PROGRESS, STATUS_RESOLVED, STATUS_ARCHIVED}


def build_report_ticket_document(
    ticket_id: str,
    project_id: str,
    developer_id: str,
    description: str | None,
    user_id: str | None,
    image_url: str | None,
    screenshot_blocked_reason: str | None,
    device_metadata: dict[str, Any],
    app_metadata: dict[str, Any],
) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    return {
        "_id": ticket_id,
        "project_id": project_id,
        "developer_id": developer_id,
        "status": STATUS_OPEN,
        "description": description,
        "user_id": user_id,
        "image_url": image_url,
        "screenshot_blocked_reason": screenshot_blocked_reason,
        "device_metadata": device_metadata,
        "app_metadata": app_metadata,
        "timestamp": now,
        "created_at": now,
        "updated_at": now,
    }
