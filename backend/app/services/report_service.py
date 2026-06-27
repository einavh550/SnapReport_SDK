"""
Report service — create report tickets from SDK uploads and serve them to the
portal with filtering and status updates.
"""
import uuid
from datetime import datetime, timezone
from typing import Any

from fastapi import status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.errors import SnapReportError, INVALID_METADATA
from app.models.report_ticket import ALLOWED_STATUSES, build_report_ticket_document
from app.schemas.report_schema import ReportMetadataIn, TicketDetail, TicketListItem


def _device_summary(device_metadata: dict[str, Any]) -> str:
    """Build a short label like 'Pixel 7 / Android 15'."""
    model = device_metadata.get("model") or device_metadata.get("manufacturer") or "Unknown device"
    android_version = device_metadata.get("android_version")
    if android_version:
        return f"{model} / Android {android_version}"
    return str(model)


def new_ticket_id() -> str:
    return f"ticket_{uuid.uuid4().hex[:12]}"


async def create_ticket(
    db: AsyncIOMotorDatabase,
    project: dict[str, Any],
    metadata: ReportMetadataIn,
    image_url: str | None,
    ticket_id: str,
) -> str:
    device_metadata = metadata.device_metadata.model_dump(exclude_none=True)
    app_metadata = metadata.app_metadata.model_dump(exclude_none=True)

    doc = build_report_ticket_document(
        ticket_id=ticket_id,
        project_id=project["_id"],
        developer_id=project["developer_id"],
        description=metadata.description,
        user_id=metadata.user_id,
        image_url=image_url,
        screenshot_blocked_reason=metadata.screenshot_blocked_reason,
        device_metadata=device_metadata,
        app_metadata=app_metadata,
    )
    await db["report_tickets"].insert_one(doc)
    return ticket_id


async def list_tickets(
    db: AsyncIOMotorDatabase,
    developer_id: str,
    project_id: str | None = None,
    status_filter: str | None = None,
    os_version: str | None = None,
    app_version: str | None = None,
    device_model: str | None = None,
    date_from: datetime | None = None,
    date_to: datetime | None = None,
    skip: int = 0,
    limit: int = 50,
) -> list[TicketListItem]:
    query: dict[str, Any] = {"developer_id": developer_id}

    if project_id:
        query["project_id"] = project_id
    if status_filter:
        query["status"] = status_filter
    if os_version:
        query["device_metadata.android_version"] = os_version
    if app_version:
        query["app_metadata.app_version_name"] = app_version
    if device_model:
        query["device_metadata.model"] = device_model
    if date_from or date_to:
        ts: dict[str, Any] = {}
        if date_from:
            ts["$gte"] = date_from
        if date_to:
            ts["$lte"] = date_to
        query["timestamp"] = ts

    cursor = db["report_tickets"].find(query).sort("timestamp", -1).skip(skip).limit(limit)
    docs = await cursor.to_list(length=limit)

    return [
        TicketListItem(
            id=d["_id"],
            project_id=d["project_id"],
            timestamp=d["timestamp"],
            status=d["status"],
            image_url=d.get("image_url"),
            device_summary=_device_summary(d.get("device_metadata", {})),
            app_version=d.get("app_metadata", {}).get("app_version_name"),
            description=d.get("description"),
        )
        for d in docs
    ]


async def get_ticket_detail(
    db: AsyncIOMotorDatabase,
    ticket_id: str,
    developer_id: str,
) -> TicketDetail:
    doc = await db["report_tickets"].find_one({"_id": ticket_id})
    if not doc:
        raise SnapReportError(
            status_code=status.HTTP_404_NOT_FOUND,
            error="NOT_FOUND",
            message="Ticket not found.",
        )
    if doc["developer_id"] != developer_id:
        raise SnapReportError(
            status_code=status.HTTP_403_FORBIDDEN,
            error="FORBIDDEN",
            message="Access denied.",
        )

    return TicketDetail(
        id=doc["_id"],
        project_id=doc["project_id"],
        timestamp=doc["timestamp"],
        status=doc["status"],
        image_url=doc.get("image_url"),
        description=doc.get("description"),
        user_id=doc.get("user_id"),
        screenshot_blocked_reason=doc.get("screenshot_blocked_reason"),
        device_metadata=doc.get("device_metadata", {}),
        app_metadata=doc.get("app_metadata", {}),
    )


async def update_ticket_status(
    db: AsyncIOMotorDatabase,
    ticket_id: str,
    developer_id: str,
    new_status: str,
) -> TicketDetail:
    if new_status not in ALLOWED_STATUSES:
        raise SnapReportError(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            error=INVALID_METADATA,
            message=f"Invalid status. Allowed: {', '.join(sorted(ALLOWED_STATUSES))}.",
        )

    # Ensure ownership before updating.
    await get_ticket_detail(db, ticket_id, developer_id)

    await db["report_tickets"].update_one(
        {"_id": ticket_id},
        {"$set": {"status": new_status, "updated_at": datetime.now(timezone.utc)}},
    )
    return await get_ticket_detail(db, ticket_id, developer_id)
