import hashlib
import secrets
import string
from datetime import datetime, timedelta, timezone

import bcrypt
from jose import JWTError, jwt

from app.core.config import settings

# ---------------------------------------------------------------------------
# Password helpers
# ---------------------------------------------------------------------------
# bcrypt only considers the first 72 bytes of a password and (in bcrypt 5+)
# raises if a longer value is passed, so we truncate explicitly.

def _to_72_bytes(plain: str) -> bytes:
    return plain.encode("utf-8")[:72]


def hash_password(plain: str) -> str:
    return bcrypt.hashpw(_to_72_bytes(plain), bcrypt.gensalt()).decode("utf-8")


def verify_password(plain: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(_to_72_bytes(plain), hashed.encode("utf-8"))
    except (ValueError, TypeError):
        return False


# ---------------------------------------------------------------------------
# JWT helpers
# ---------------------------------------------------------------------------

def create_access_token(subject: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=settings.jwt_access_token_expire_minutes
    )
    payload = {"sub": subject, "exp": expire}
    return jwt.encode(payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm)


def decode_access_token(token: str) -> str | None:
    """Return the subject (developer_id) or None if the token is invalid."""
    try:
        payload = jwt.decode(
            token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm]
        )
        return payload.get("sub")
    except JWTError:
        return None


# ---------------------------------------------------------------------------
# API key helpers
# ---------------------------------------------------------------------------

_API_KEY_ALPHABET = string.ascii_letters + string.digits
_API_KEY_RANDOM_LENGTH = 32


def generate_api_key() -> str:
    """Return a new raw API key, e.g. sr_live_N9sA7kLmP0qR4xT2zW8yB6cD1eF5gH3j."""
    random_part = "".join(secrets.choice(_API_KEY_ALPHABET) for _ in range(_API_KEY_RANDOM_LENGTH))
    return f"sr_live_{random_part}"


def hash_api_key(raw_key: str) -> str:
    """SHA-256 hash of the raw key for safe database storage."""
    return hashlib.sha256(raw_key.encode()).hexdigest()


def api_key_prefix(raw_key: str) -> str:
    """Return first 16 characters for display (e.g. sr_live_N9sA7k)."""
    return raw_key[:16]
