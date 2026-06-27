"""
Standardized error codes and a custom exception that produces the
top-level JSON error shape required by the spec:

    { "success": false, "error": "INVALID_API_KEY", "message": "..." }
"""

# Error codes
MISSING_API_KEY = "MISSING_API_KEY"
INVALID_API_KEY = "INVALID_API_KEY"
PROJECT_DISABLED = "PROJECT_DISABLED"
INVALID_METADATA = "INVALID_METADATA"
FILE_TOO_LARGE = "FILE_TOO_LARGE"
UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE"
SERVER_ERROR = "SERVER_ERROR"


class SnapReportError(Exception):
    """Raised to return a structured SnapReport error response."""

    def __init__(self, status_code: int, error: str, message: str) -> None:
        self.status_code = status_code
        self.error = error
        self.message = message
        super().__init__(message)
