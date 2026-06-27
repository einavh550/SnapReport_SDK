from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from app.core.config import settings

_client: AsyncIOMotorClient | None = None


async def connect_db() -> None:
    global _client
    _client = AsyncIOMotorClient(settings.mongodb_url)


async def close_db() -> None:
    global _client
    if _client is not None:
        _client.close()
        _client = None


def get_database() -> AsyncIOMotorDatabase:
    if _client is None:
        raise RuntimeError("Database client is not initialized. Call connect_db() first.")
    return _client[settings.mongodb_db_name]


async def create_indexes() -> None:
    import logging
    logger = logging.getLogger(__name__)
    try:
        db = get_database()
        await db["developer_accounts"].create_index("email", unique=True)
        await db["projects"].create_index("developer_id")
        await db["projects"].create_index("api_key_hash")
        logger.info("Database indexes ensured.")
    except Exception as exc:
        logger.warning("Could not create indexes (is MongoDB running?): %s", exc)
