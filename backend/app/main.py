from fastapi import FastAPI, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime, timezone
from uuid import uuid4
import os
from .database import init_db, get_db
from .schemas import SettingsUpdate, WebhookCallEvent

app = FastAPI(title="CallAI API", version="0.1.0", description="Development API for the CallAI Android dashboard")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=False, allow_methods=["*"], allow_headers=["*"])

@app.on_event("startup")
def startup():
    init_db()

def now():
    return datetime.now(timezone.utc).isoformat()

def row_call(row):
    d = dict(row)
    d["callback_requested"] = bool(d["callback_requested"])
    return d

@app.get("/health")
def health():
    return {"status": "ok", "service": "CallAI API", "mode": "development"}

@app.get("/api/v1/dashboard/overview")
def overview():
    with get_db() as db:
        total = db.execute("SELECT COUNT(*) n FROM calls").fetchone()["n"]
        handled = db.execute("SELECT COUNT(*) n FROM calls WHERE status='completed'").fetchone()["n"]
        callbacks = db.execute("SELECT COUNT(*) n FROM calls WHERE callback_requested=1").fetchone()["n"]
        duration = db.execute("SELECT COALESCE(SUM(duration_seconds),0) n FROM calls").fetchone()["n"]
        recent = db.execute("SELECT * FROM calls ORDER BY started_at DESC LIMIT 5").fetchall()
    return {"total_calls": total, "ai_handled": handled, "callback_requests": callbacks,
            "total_duration_seconds": duration, "recent_calls": [row_call(x) for x in recent]}

@app.get("/api/v1/calls")
def list_calls():
    with get_db() as db:
        rows = db.execute("SELECT * FROM calls ORDER BY started_at DESC").fetchall()
    return [row_call(x) for x in rows]

@app.get("/api/v1/calls/{call_id}")
def call_detail(call_id: str):
    with get_db() as db:
        call = db.execute("SELECT * FROM calls WHERE id=?", (call_id,)).fetchone()
        if not call:
            raise HTTPException(404, "Call not found")
        transcript = db.execute("SELECT speaker, message, created_at FROM transcripts WHERE call_id=? ORDER BY id", (call_id,)).fetchall()
    result = row_call(call)
    result["transcript"] = [dict(x) for x in transcript]
    return result

@app.get("/api/v1/settings/ai")
def get_settings():
    with get_db() as db:
        row = db.execute("SELECT * FROM settings WHERE id=1").fetchone()
    return {**dict(row), "enabled": bool(row["enabled"]), "handle_unanswered": bool(row["handle_unanswered"]), "handle_busy": bool(row["handle_busy"])}

@app.put("/api/v1/settings/ai")
def update_settings(payload: SettingsUpdate):
    with get_db() as db:
        db.execute("""UPDATE settings SET assistant_name=?, language=?, greeting=?, enabled=?, handle_unanswered=?, handle_busy=? WHERE id=1""",
                   (payload.assistant_name, payload.language, payload.greeting, int(payload.enabled), int(payload.handle_unanswered), int(payload.handle_busy)))
    return get_settings()

@app.post("/api/v1/demo/calls")
def create_demo_call():
    call_id = str(uuid4())
    timestamp = now()
    with get_db() as db:
        db.execute("""INSERT INTO calls VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                   (call_id, "Rahul (Demo)", "+91 90000 12345", "E-commerce website enquiry", "completed", "unanswered (simulated)", 164, 1, "After 5 PM", "Caller is interested in an e-commerce website and requested a callback after 5 PM.", timestamp))
        messages = [("assistant", "Hello! I am CallAI, an AI assistant. How can I help you?"),
                    ("caller", "I need an e-commerce website for my business."),
                    ("assistant", "Certainly. May I know your name and preferred callback time?"),
                    ("caller", "My name is Rahul. Please call me after 5 PM.")]
        for speaker, message in messages:
            db.execute("INSERT INTO transcripts(call_id,speaker,message,created_at) VALUES(?,?,?,?)", (call_id, speaker, message, timestamp))
    return {"created": True, "call_id": call_id, "message": "Simulated call created. No telephone call was placed."}

@app.post("/api/v1/webhooks/call-event")
def receive_call_event(payload: WebhookCallEvent, x_callai_webhook_secret: str | None = Header(default=None)):
    # Local demo protection. Production must implement provider-specific
    # signature verification and replay/idempotency protection.
    expected = os.getenv("WEBHOOK_SECRET", "change-this-before-deploying")
    if expected != "change-this-before-deploying" and x_callai_webhook_secret != expected:
        raise HTTPException(401, "Invalid webhook secret")
    with get_db() as db:
        exists = db.execute("SELECT id FROM calls WHERE id=?", (payload.call_id,)).fetchone()
        if exists:
            return {"accepted": True, "duplicate": True}
        db.execute("INSERT INTO calls VALUES (?,?,?,?,?,?,?,?,?,?,?)", (payload.call_id, payload.caller_name, payload.caller_number, payload.purpose, payload.status, payload.forwarding_reason, payload.duration_seconds, int(payload.callback_requested), payload.callback_time, payload.summary, now()))
    return {"accepted": True, "call_id": payload.call_id}
