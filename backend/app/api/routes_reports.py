"""
SDK report ingestion endpoint.

POST /api/sdk/reports  (multipart/form-data)
  - metadata:   JSON string (required)
  - screenshot: image file  (optional)

Authenticated via the `X-SnapReport-Api-Key` header. The API key is validated
(by the dependency) BEFORE any file is read or stored.
"""
import json

from fastapi import APIRouter, File, Form, UploadFile, status
from pydantic import ValidationError

from app.api.deps import CurrentProject
from app.core import errors
from app.core.errors import SnapReportError
from app.db.database import get_database
from app.schemas.report_schema import ReportMetadataIn, UploadSuccessResponse
from app.services import report_service, storage_service

router = APIRouter(prefix="/api/sdk", tags=["SDK"])


@router.post("/reports", response_model=UploadSuccessResponse, status_code=201)
async def upload_report(
    project: CurrentProject,
    metadata: str = Form(...),
    screenshot: UploadFile | None = File(default=None),
) -> UploadSuccessResponse:
    # Parse + validate metadata JSON.
    try:
        raw = json.loads(metadata)
    except json.JSONDecodeError:
        raise SnapReportError(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            error=errors.INVALID_METADATA,
            message="metadata is not valid JSON.",
        )

    try:
        parsed = ReportMetadataIn.model_validate(raw)
    except ValidationError as exc:
        raise SnapReportError(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            error=errors.INVALID_METADATA,
            message=f"metadata failed validation: {exc.errors()}",
        )

    ticket_id = report_service.new_ticket_id()
    image_url: str | None = None

    # Handle the optional screenshot file.
    if screenshot is not None and screenshot.filename:
        if not storage_service.is_supported_content_type(screenshot.content_type):
            raise SnapReportError(
                status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
                error=errors.UNSUPPORTED_FILE_TYPE,
                message=f"Unsupported screenshot type: {screenshot.content_type}.",
            )

        content = await screenshot.read()
        if len(content) > storage_service.max_size_bytes():
            raise SnapReportError(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                error=errors.FILE_TOO_LARGE,
                message=f"Screenshot exceeds {storage_service.max_size_bytes()} bytes.",
            )

        image_url = storage_service.save_screenshot(
            ticket_id, content, screenshot.content_type
        )

    db = get_database()
    await report_service.create_ticket(
        db=db,
        project=project,
        metadata=parsed,
        image_url=image_url,
        ticket_id=ticket_id,
    )

    return UploadSuccessResponse(ticket_id=ticket_id)
