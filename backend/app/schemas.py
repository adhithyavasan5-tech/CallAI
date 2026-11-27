from pydantic import BaseModel, Field
from typing import Optional

class SettingsUpdate(BaseModel):
    assistant_name: str = Field(min_length=1, max_length=60)
    language: str = Field(default="Tamil + English")
    greeting: str = Field(min_length=5, max_length=500)
    enabled: bool = True
    handle_unanswered: bool = True
    handle_busy: bool = True

class WebhookCallEvent(BaseModel):
    # Generic development contract. Map actual provider fields only after
    # checking the provider's current webhook payload documentation.
    call_id: str
    caller_number: str = "Unknown"
    caller_name: str = "Unknown caller"
    purpose: str = "Unclassified call"
    status: str = "completed"
    forwarding_reason: str = "provider_event"
    duration_seconds: int = 0
    summary: str = "Call received. Summary not yet available."
    callback_requested: bool = False
    callback_time: Optional[str] = None
