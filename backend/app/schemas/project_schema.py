from datetime import datetime

from pydantic import BaseModel


class CreateProjectRequest(BaseModel):
    app_name: str


class ProjectCreatedResponse(BaseModel):
    """Returned once when a project is first created. Contains the raw API key."""
    id: str
    app_name: str
    api_key: str          # raw key shown ONCE — never stored raw in DB
    api_key_prefix: str
    created_at: datetime


class ProjectListItem(BaseModel):
    """Safe project summary — never exposes the raw API key."""
    id: str
    app_name: str
    api_key_prefix: str
    is_active: bool
    created_at: datetime
