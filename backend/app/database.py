import os
import sqlite3
from pathlib import Path
from contextlib import contextmanager

DB_URL = os.getenv("DATABASE_URL", "sqlite:///./callai.db")
DB_PATH = DB_URL.replace("sqlite:///", "")
if not os.path.isabs(DB_PATH):
    DB_PATH = str((Path(__file__).resolve().parents[1] / DB_PATH).resolve())
Path(DB_PATH).parent.mkdir(parents=True, exist_ok=True)

@contextmanager
def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()

def init_db():
    with get_db() as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS calls (
            id TEXT PRIMARY KEY,
            caller_name TEXT NOT NULL,
            caller_number TEXT NOT NULL,
            purpose TEXT NOT NULL,
            status TEXT NOT NULL,
            forwarding_reason TEXT NOT NULL,
            duration_seconds INTEGER NOT NULL DEFAULT 0,
            callback_requested INTEGER NOT NULL DEFAULT 0,
            callback_time TEXT,
            summary TEXT NOT NULL,
            started_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS transcripts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            call_id TEXT NOT NULL,
            speaker TEXT NOT NULL,
            message TEXT NOT NULL,
            created_at TEXT NOT NULL,
            FOREIGN KEY(call_id) REFERENCES calls(id)
        );
        CREATE TABLE IF NOT EXISTS settings (
            id INTEGER PRIMARY KEY CHECK(id=1),
            assistant_name TEXT NOT NULL,
            language TEXT NOT NULL,
            greeting TEXT NOT NULL,
            enabled INTEGER NOT NULL DEFAULT 1,
            handle_unanswered INTEGER NOT NULL DEFAULT 1,
            handle_busy INTEGER NOT NULL DEFAULT 1
        );
        INSERT OR IGNORE INTO settings
        (id, assistant_name, language, greeting, enabled, handle_unanswered, handle_busy)
        VALUES (1, 'CallAI', 'Tamil + English', 'Hello! I am Adhithyavasan’s AI assistant. How can I help you?', 1, 1, 1);
        """)
