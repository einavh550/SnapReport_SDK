"""
Storage service — local filesystem storage for screenshot files.

Designed so it can later be swapped for S3/Azure Blob/GCP without changing
callers. Files are named by ticket id; the public URL is a relative path
served by the backend.
"""
import os
from pathlib import Path

from app.core.config import settings

# Supported screenshot content types and their file extensions.
_ALLOWED_CONTENT_TYPES = {
    "image/jpeg": "jpg",
    "image/jpg": "jpg",
    "image/webp": "webp",
    "image/png": "png",
}

PUBLIC_URL_PREFIX = "/storage/screenshots"


def _storage_dir() -> Path:
    path = Path(settings.screenshot_storage_path)
    path.mkdir(parents=True, exist_ok=True)
    return path


def is_supported_content_type(content_type: str | None) -> bool:
    return content_type in _ALLOWED_CONTENT_TYPES


def extension_for(content_type: str) -> str:
    return _ALLOWED_CONTENT_TYPES.get(content_type, "jpg")


def max_size_bytes() -> int:
    return settings.max_screenshot_size_mb * 1024 * 1024


def save_screenshot(ticket_id: str, content: bytes, content_type: str) -> str:
    """
    Persist screenshot bytes and return the public relative URL.
    Filename is derived solely from the server-generated ticket id, so the
    client cannot influence the path (prevents path traversal).
    """
    extension = extension_for(content_type)
    filename = f"{ticket_id}.{extension}"
    file_path = _storage_dir() / filename
    with open(file_path, "wb") as f:
        f.write(content)
    return f"{PUBLIC_URL_PREFIX}/{filename}"


def resolve_screenshot_path(filename: str) -> Path | None:
    """
    Resolve a filename to an absolute path inside the storage directory.
    Returns None if the resulting path escapes the storage directory
    (path-traversal protection) or the file does not exist.
    """
    # Reject anything that isn't a bare filename.
    if filename != os.path.basename(filename):
        return None

    storage_dir = _storage_dir().resolve()
    candidate = (storage_dir / filename).resolve()

    if storage_dir not in candidate.parents:
        return None
    if not candidate.is_file():
        return None
    return candidate
