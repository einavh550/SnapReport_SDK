"""
FastAPI dependency injection helpers.
"""
from typing import Annotated, Any

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.security import decode_access_token
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
