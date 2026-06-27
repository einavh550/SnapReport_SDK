from fastapi import APIRouter

from app.api.deps import CurrentDeveloper
from app.db.database import get_database
from app.schemas.project_schema import (
    CreateProjectRequest,
    ProjectCreatedResponse,
    ProjectListItem,
)
from app.services.project_service import (
    create_project,
    list_projects,
    regenerate_api_key,
)

router = APIRouter(prefix="/api/projects", tags=["Projects"])


@router.post("", response_model=ProjectCreatedResponse, status_code=201)
async def create(
    data: CreateProjectRequest,
    developer: CurrentDeveloper,
) -> ProjectCreatedResponse:
    db = get_database()
    doc, raw_key = await create_project(db, developer["_id"], data.app_name)
    return ProjectCreatedResponse(
        id=doc["_id"],
        app_name=doc["app_name"],
        api_key=raw_key,
        api_key_prefix=doc["api_key_prefix"],
        created_at=doc["created_at"],
    )


@router.get("", response_model=list[ProjectListItem])
async def list_all(developer: CurrentDeveloper) -> list[ProjectListItem]:
    db = get_database()
    docs = await list_projects(db, developer["_id"])
    return [
        ProjectListItem(
            id=d["_id"],
            app_name=d["app_name"],
            api_key_prefix=d["api_key_prefix"],
            is_active=d["is_active"],
            created_at=d["created_at"],
        )
        for d in docs
    ]


@router.post("/{project_id}/regenerate-key", response_model=ProjectCreatedResponse)
async def regenerate_key(
    project_id: str,
    developer: CurrentDeveloper,
) -> ProjectCreatedResponse:
    db = get_database()
    doc, raw_key = await regenerate_api_key(db, project_id, developer["_id"])
    return ProjectCreatedResponse(
        id=doc["_id"],
        app_name=doc["app_name"],
        api_key=raw_key,
        api_key_prefix=doc["api_key_prefix"],
        created_at=doc["created_at"],
    )
