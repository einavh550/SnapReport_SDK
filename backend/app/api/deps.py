"""
FastAPI dependency injection helpers.
"""
from typing import Annotated, Any

from fastapi import Depends, Header, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core import errors
from app.core.errors import SnapReportError
from app.core.security import decode_access_token, hash_api_key
from app.db.database import get_database
from app.services.auth_service import get_developer_by_id

_bearer = HTTPBearer()


async def get_current_developer(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(_bearer)],
) -> dict[str, Any]:
    """
    Validate the Bearer JWT and return the developer document.
    Raises 401 if the token is missing, expired, or the developer no longer exists.
    """
    token = credentials.credentials
    developer_id = decode_access_token(token)

    if developer_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    db = get_database()
    developer = await get_developer_by_id(db, developer_id)

    if developer is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Developer account not found.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return developer


CurrentDeveloper = Annotated[dict[str, Any], Depends(get_current_developer)]


async def get_project_from_api_key(
    x_snapreport_api_key: Annotated[str | None, Header()] = None,
) -> dict[str, Any]:
    """
    Authenticate an SDK request using the `X-SnapReport-Api-Key` header.
    Hashes the incoming key and looks up the matching active project.
    """
    if not x_snapreport_api_key:
        raise SnapReportError(
            status_code=status.HTTP_401_UNAUTHORIZED,
            error=errors.MISSING_API_KEY,
            message="API key is missing.",
        )

    db = get_database()
    project = await db["projects"].find_one(
        {"api_key_hash": hash_api_key(x_snapreport_api_key)}
    )

    if project is None:
        raise SnapReportError(
            status_code=status.HTTP_401_UNAUTHORIZED,
            error=errors.INVALID_API_KEY,
            message="API key is invalid.",
        )

    if not project.get("is_active", True):
        raise SnapReportError(
            status_code=status.HTTP_403_FORBIDDEN,
            error=errors.PROJECT_DISABLED,
            message="This project is disabled.",
        )

    return project


CurrentProject = Annotated[dict[str, Any], Depends(get_project_from_api_key)]
