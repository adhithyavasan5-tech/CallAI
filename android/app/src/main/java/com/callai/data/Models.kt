package com.callai.data

data class CallItem(
    val id: String,
    val caller_name: String,
    val caller_number: String,
    val purpose: String,
    val status: String,
    val forwarding_reason: String,
    val duration_seconds: Int,
    val callback_requested: Boolean,
    val callback_time: String?,
    val summary: String,
    val started_at: String
)
data class DashboardOverview(
    val total_calls: Int,
    val ai_handled: Int,
    val callback_requests: Int,
    val total_duration_seconds: Int,
    val recent_calls: List<CallItem>
)
data class TranscriptLine(val speaker: String, val message: String, val created_at: String)
data class CallDetail(
    val id: String, val caller_name: String, val caller_number: String,
    val purpose: String, val status: String, val forwarding_reason: String,
    val duration_seconds: Int, val callback_requested: Boolean,
    val callback_time: String?, val summary: String, val started_at: String,
    val transcript: List<TranscriptLine>
)
data class AiSettings(
    val id: Int = 1,
    val assistant_name: String = "CallAI",
    val language: String = "Tamil + English",
    val greeting: String = "Hello! I am Adhithyavasan’s AI assistant. How can I help you?",
    val enabled: Boolean = true,
    val handle_unanswered: Boolean = true,
    val handle_busy: Boolean = true
)
data class DemoResponse(val created: Boolean, val call_id: String, val message: String)
