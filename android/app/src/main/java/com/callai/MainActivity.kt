package com.callai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callai.data.*
import kotlinx.coroutines.launch

private val Ink = Color(0xFF111827)
private val Muted = Color(0xFF64748B)
private val Accent = Color(0xFF5B6CF9)
private val Bg = Color(0xFFF5F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Bg)) { CallAiApp() } }
    }
}

class CallAiViewModel : ViewModel() {
    var overview by mutableStateOf(DashboardOverview(3, 3, 1, 326, sampleCalls()))
        private set
    var calls by mutableStateOf(sampleCalls())
        private set
    var settings by mutableStateOf(AiSettings())
        private set
    var online by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf("Showing sample data")
        private set

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        loading = true
        try {
            val api = ApiClient.api
            overview = api.overview(); calls = api.calls(); settings = api.settings()
            online = true; message = "Connected to CallAI backend"
        } catch (_: Exception) { online = false; message = "Backend offline · Sample data shown" }
        loading = false
    }
    fun createDemo() = viewModelScope.launch {
        try { ApiClient.api.createDemoCall(); refresh(); message = "Demo call added" }
        catch (_: Exception) { message = "Start the backend to create a saved demo call" }
    }
    fun toggleAssistant(value: Boolean) = viewModelScope.launch {
        val updated = settings.copy(enabled = value)
        settings = updated
        try { settings = ApiClient.api.updateSettings(updated); message = "Assistant setting saved" }
        catch (_: Exception) { message = "Preview only · backend unavailable" }
    }
}

@Composable fun CallAiApp(vm: CallAiViewModel = viewModel()) {
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<CallItem?>(null) }
    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = Color.White) {
            val entries = listOf("Home", "Calls", "AI", "Settings")
            val icons = listOf(Icons.Default.Home, Icons.Default.Call, Icons.Default.SmartToy, Icons.Default.Settings)
            entries.forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, onClick = { tab = index; selected = null }, icon = { Icon(icons[index], null) }, label = { Text(label) }) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selected != null) CallDetailScreen(selected!!, onBack = { selected = null })
            else when (tab) {
                0 -> DashboardScreen(vm, onCall = { selected = it })
                1 -> CallsScreen(vm.calls, onCall = { selected = it }, onRefresh = vm::refresh)
                2 -> AiScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun DashboardScreen(vm: CallAiViewModel, onCall: (CallItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Good day 👋", color = Muted); Text("Your call overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink) }
                Box(Modifier.size(44.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsNone, null, tint = Ink) }
            }
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (vm.online) Color(0xFF16A34A) else Color(0xFFF59E0B), CircleShape))
                Spacer(Modifier.width(8.dp)); Text(if (vm.online) "Cloud connected" else "Demo mode · Backend offline", color = Muted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f)); TextButton(onClick = vm::refresh) { Text("Refresh") }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total calls", vm.overview.total_calls.toString(), Icons.Default.Call, Modifier.weight(1f))
                    StatCard("AI handled", vm.overview.ai_handled.toString(), Icons.Default.SmartToy, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Callbacks", vm.overview.callback_requests.toString(), Icons.Default.Schedule, Modifier.weight(1f))
                    StatCard("Talk time", formatDuration(vm.overview.total_duration_seconds), Icons.Default.Timer, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Ink), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your AI receptionist", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(if (vm.settings.enabled) "Ready for configured calls" else "Assistant paused", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = vm.settings.enabled, onCheckedChange = vm::toggleAssistant)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Recent calls", fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.weight(1f)); Text("View all", color = Accent, modifier = Modifier.clickable { }) }
            }
        }
        items(vm.overview.recent_calls) { call -> CallRow(call, onClick = { onCall(call) }) }
        item { Button(onClick = vm::createDemo, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create demo call") } }
        item { Text(vm.message, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 20.dp)) }
    }
}

@Composable fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = Accent); Spacer(Modifier.height(12.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink); Text(label, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable fun CallRow(call: CallItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Accent) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(call.caller_name, fontWeight = FontWeight.SemiBold, color = Ink); Text(call.purpose, color = Muted, style = MaterialTheme.typography.bodySmall); Text(call.caller_number, color = Muted, style = MaterialTheme.typography.labelSmall) }
        Column(horizontalAlignment = Alignment.End) { Text(if (call.callback_requested) "Callback" else "Handled", color = if (call.callback_requested) Color(0xFFB45309) else Color(0xFF15803D), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Text(formatDuration(call.duration_seconds), color = Muted, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable fun CallsScreen(calls: List<CallItem>, onCall: (CallItem) -> Unit, onRefresh: () -> Unit) {
    Column {
        Header("Call history", "Every conversation handled by CallAI")
        TextButton(onClick = onRefresh, modifier = Modifier.padding(horizontal = 16.dp)) { Icon(Icons.Default.Refresh, null); Text("Refresh calls") }
        LazyColumn { items(calls) { CallRow(it) { onCall(it) } } }
    }
}

@Composable fun CallDetailScreen(call: CallItem, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Icon(Icons.Default.ArrowBack, null); Text("Back") }
        Header(call.caller_name, call.caller_number)
        Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) { Text("Call summary", fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(8.dp)); Text(call.summary, color = Muted); Spacer(Modifier.height(12.dp)); Text("Reason: ${call.forwarding_reason}", color = Muted); Text("Duration: ${formatDuration(call.duration_seconds)}", color = Muted); if (call.callback_requested) Text("Callback: ${call.callback_time ?: "Requested"}", color = Accent) }
        }
        Header("Conversation", "Transcript preview")
        Column(Modifier.padding(horizontal = 20.dp)) {
            listOf("AI assistant" to "Hello! I am CallAI, an AI assistant. How can I help you?", "Caller" to call.purpose, "AI assistant" to "May I take a message and callback preference?").forEach { (speaker, message) ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(speaker, color = Accent, fontWeight = FontWeight.Bold); Text(message, color = Ink) } }
            }
        }
        Text("Full transcript is available when the backend returns transcript records.", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(20.dp))
    }
}

@Composable fun AiScreen(vm: CallAiViewModel) {
    Column(Modifier.fillMaxSize()) {
        Header("AI receptionist", "Configure how your assistant speaks")
        Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Text("Assistant status", fontWeight = FontWeight.Bold, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) { Text(if (vm.settings.enabled) "Enabled" else "Paused", color = Muted, modifier = Modifier.weight(1f)); Switch(vm.settings.enabled, vm::toggleAssistant) }
                Divider()
                Text("Language", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)); Text(vm.settings.language, color = Muted)
                Text("Greeting", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)); Text(vm.settings.greeting, color = Muted)
                Text("Call handling", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)); Text("Unanswered calls: ${if (vm.settings.handle_unanswered) "On" else "Off"}"); Text("Busy calls: ${if (vm.settings.handle_busy) "On" else "Off"}")
            }
        }
        Text("Voice and prompt editing will be connected to the live provider configuration in the next integration stage.", color = Muted, modifier = Modifier.padding(20.dp))
    }
}

@Composable fun SettingsScreen(vm: CallAiViewModel) {
    Column(Modifier.fillMaxSize()) {
        Header("Settings", "Your CallAI account and connections")
        listOf("Mobile network" to "Airtel India", "Telephony" to "Exotel · Not connected", "Voice AI" to "Sarvam · Not connected", "Backend" to if (vm.online) "Connected" else "Local demo", "Database" to "SQLite local · Supabase schema included", "Privacy" to "Recording consent and retention must be configured").forEach { (a,b) ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(a, fontWeight = FontWeight.SemiBold, color = Ink); Text(b, color = Muted, style = MaterialTheme.typography.bodySmall) } }
        }
        Text("CallAI prototype v0.1.0", color = Muted, modifier = Modifier.padding(20.dp))
    }
}

fun formatDuration(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
fun sampleCalls() = listOf(
    CallItem("demo-001", "Rahul (Demo)", "+91 90000 12345", "E-commerce website enquiry", "completed", "unanswered (simulated)", 164, true, "After 5 PM", "Caller is interested in an e-commerce website and requested a callback after 5 PM.", "Today · 10:30 AM"),
    CallItem("demo-002", "Priya (Demo)", "+91 90000 45678", "Asked for a project update", "completed", "busy (simulated)", 92, false, null, "Caller asked for a project update.", "Today · 09:45 AM"),
    CallItem("demo-003", "Unknown caller", "+91 90000 78901", "General enquiry", "completed", "unanswered (simulated)", 70, false, null, "Caller left a general enquiry.", "Yesterday · 06:20 PM")
)
