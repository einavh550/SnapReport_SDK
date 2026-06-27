"""
Portal report APIs (JWT-authenticated).

All endpoints enforce that the authenticated developer owns the data.
"""
from datetime import datetime

from fastapi import APIRouter, Query

from app.api.deps import CurrentDeveloper
from app.db.database import get_database
from app.schemas.report_schema import StatusUpdateRequest, TicketDetail, TicketListItem
from app.services import report_service

router = APIRouter(prefix="/api/portal", tags=["Portal"])


@router.get("/reports", response_model=list[TicketListItem])
async def list_reports(
    developer: CurrentDeveloper,
    projectId: str | None = Query(default=None),
    status: str | None = Query(default=None),
    osVersion: str | None = Query(default=None),
    appVersion: str | None = Query(default=None),
    deviceModel: str | None = Query(default=None),
    dateFrom: datetime | None = Query(default=None),
    dateTo: datetime | None = Query(default=None),
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=50, ge=1, le=200),
) -> list[TicketListItem]:
    db = get_database()
    return await report_service.list_tickets(
        db=db,
        developer_id=developer["_id"],
        project_id=projectId,
        status_filter=status,
        os_version=osVersion,
        app_version=appVersion,
        device_model=deviceModel,
        date_from=dateFrom,
        date_to=dateTo,
        skip=skip,
        limit=limit,
    )


@router.get("/reports/{ticket_id}", response_model=TicketDetail)
async def get_report(ticket_id: str, developer: CurrentDeveloper) -> TicketDetail:
    db = get_database()
    return await report_service.get_ticket_detail(db, ticket_id, developer["_id"])


@router.patch("/reports/{ticket_id}/status", response_model=TicketDetail)
async def update_status(
    ticket_id: str,
    data: StatusUpdateRequest,
    developer: CurrentDeveloper,
) -> TicketDetail:
    db = get_database()
    return await report_service.update_ticket_status(
        db, ticket_id, developer["_id"], data.status
    )
