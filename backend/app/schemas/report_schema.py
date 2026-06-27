"""
Schemas for SDK report ingestion and portal report APIs.

The SDK sends camelCase JSON (e.g. deviceMetadata, screenshotBlockedReason),
so incoming/outgoing models use camelCase aliases while keeping snake_case
attribute names internally. Device/app metadata allow extra fields because the
shape varies across Android versions and manufacturers.
"""
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


# ---------------------------------------------------------------------------
# Incoming metadata (from the SDK multipart `metadata` part)
# ---------------------------------------------------------------------------

class DeviceMetadataIn(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="allow")

    manufacturer: str | None = None
    model: str | None = None
    device: str | None = None
    android_version: str | None = Field(default=None, alias="androidVersion")
    sdk_int: int | None = Field(default=None, alias="sdkInt")
    battery_level: int | None = Field(default=None, alias="batteryLevel")
    is_charging: bool | None = Field(default=None, alias="isCharging")
    total_ram_mb: int | None = Field(default=None, alias="totalRamMb")
    available_ram_mb: int | None = Field(default=None, alias="availableRamMb")
    total_storage_mb: int | None = Field(default=None, alias="totalStorageMb")
    available_storage_mb: int | None = Field(default=None, alias="availableStorageMb")
    network_type: str | None = Field(default=None, alias="networkType")
    locale: str | None = None
    timezone: str | None = None


class AppMetadataIn(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="allow")

    package_name: str | None = Field(default=None, alias="packageName")
    app_version_name: str | None = Field(default=None, alias="appVersionName")
    app_version_code: int | None = Field(default=None, alias="appVersionCode")
    timestamp_micros: int | None = Field(default=None, alias="timestampMicros")
    screen_width: int | None = Field(default=None, alias="screenWidth")
    screen_height: int | None = Field(default=None, alias="screenHeight")
    orientation: str | None = None


class ReportMetadataIn(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    user_id: str | None = Field(default=None, alias="userId")
    description: str | None = None
    device_metadata: DeviceMetadataIn = Field(alias="deviceMetadata")
    app_metadata: AppMetadataIn = Field(alias="appMetadata")
    screenshot_blocked_reason: str | None = Field(
        default=None, alias="screenshotBlockedReason"
    )


# ---------------------------------------------------------------------------
# SDK upload responses
# ---------------------------------------------------------------------------

class UploadSuccessResponse(BaseModel):
    success: bool = True
    ticket_id: str = Field(serialization_alias="ticketId")
    message: str = "Report uploaded successfully"


# ---------------------------------------------------------------------------
# Portal report responses
# ---------------------------------------------------------------------------

class TicketListItem(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str
    project_id: str = Field(serialization_alias="projectId")
    timestamp: datetime
    status: str
    image_url: str | None = Field(default=None, serialization_alias="imageUrl")
    device_summary: str = Field(serialization_alias="deviceSummary")
    app_version: str | None = Field(default=None, serialization_alias="appVersion")
    description: str | None = None


class TicketDetail(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str
    project_id: str = Field(serialization_alias="projectId")
    timestamp: datetime
    status: str
    image_url: str | None = Field(default=None, serialization_alias="imageUrl")
    description: str | None = None
    user_id: str | None = Field(default=None, serialization_alias="userId")
    screenshot_blocked_reason: str | None = Field(
        default=None, serialization_alias="screenshotBlockedReason"
    )
    device_metadata: dict[str, Any] = Field(serialization_alias="deviceMetadata")
    app_metadata: dict[str, Any] = Field(serialization_alias="appMetadata")


class StatusUpdateRequest(BaseModel):
    status: str
