# CallAI — Android + Cloud AI Receptionist (starter project)

This repository contains a runnable **development prototype** of CallAI:
- Native Android dashboard (Kotlin + Jetpack Compose)
- FastAPI backend with SQLite for local development
- Demo call creation, call history, summaries, settings and webhook receiver
- Supabase SQL schema for a later hosted deployment
- Exotel + Sarvam setup notes for real voice calls

> Important: This is not yet connected to your Airtel number or a live telephone provider. The app and backend can be tested locally using simulated calls. Real call forwarding requires a provider-issued destination number, provider accounts, approved configuration, and end-to-end testing. Do not activate forwarding on your personal number until the test number works. Provider accounts, billing and telecom changes should be handled by a parent/guardian where required.

## Requirements
- Android Studio (recent stable version)
- Android SDK 35
- JDK 17
- Python 3.11+

## 1. Run the backend

```bash
cd backend
python -m venv .venv
# Windows PowerShell: .venv\Scripts\Activate.ps1
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # Windows: copy .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Open `http://127.0.0.1:8000/docs` to inspect the API.

Create a demo call:
```bash
curl -X POST http://127.0.0.1:8000/api/v1/demo/calls
```

The demo creates a sample caller, transcript and summary. This does not place a real phone call.

## 2. Run the Android app

1. Open `android/` in Android Studio.
2. Allow Gradle sync and install Android SDK 35 if prompted.
3. Run the `app` configuration on an emulator.
4. The emulator connects to the local backend at `http://10.0.2.2:8000/`.

For a physical Android phone, change `BASE_URL` in `android/app/src/main/java/com/callai/data/ApiClient.kt` to your computer's LAN IP, for example `http://192.168.1.20:8000/`. Keep the phone and computer on the same Wi-Fi. For production, use HTTPS and a deployed backend.

If the backend is offline, the app displays sample data and an offline indicator.

## 3. API overview

- `GET /health`
- `GET /api/v1/dashboard/overview`
- `GET /api/v1/calls`
- `GET /api/v1/calls/{call_id}`
- `GET /api/v1/settings/ai`
- `PUT /api/v1/settings/ai`
- `POST /api/v1/demo/calls` (simulated call only)
- `POST /api/v1/webhooks/call-event` (development webhook receiver)

The webhook endpoint is intentionally a development receiver. Before production, verify provider signatures, enforce idempotency, validate event schemas, and restrict network access.

## 4. Real voice integration plan

The current starter uses a **managed Sarvam Voice Agents + Exotel** route rather than implementing low-level telephony audio streaming ourselves.

1. Create and test a Sarvam voice agent in the Sarvam workspace.
2. Configure its prompt to disclose that it is an AI assistant, ask for the caller's purpose and callback preference, and avoid collecting sensitive information.
3. Set up an Exotel test number and inbound app/Voicebot flow following the official integration guide: https://docs.exotel.com/exotel-agentstream/connect-exotel-to-sarvam-voice-agents
4. In Exotel's Voicebot applet, use the Sarvam endpoint documented by Exotel: `https://apps.sarvam.ai/api/app-runtime/channels/exotel`.
5. Link the Exophone to the inbound app and deploy the agent in Sarvam.
6. Test calls to the Exotel test number first. Verify caller ID, call status, transcripts, recording settings and post-call events.
7. Implement the exact provider webhook payload mapping in `backend/app/integrations/` after obtaining the current payload examples from your provider dashboard/docs. Do not assume webhook fields without checking them.
8. Only after the test number works, ask Airtel to confirm the correct conditional forwarding options and test with a separate controlled call. Busy/rejected-call behaviour can vary by carrier/device configuration.

Official docs:
- Sarvam Voice Agents: https://docs.sarvam.ai/voice-agents/introduction
- Exotel ↔ Sarvam setup: https://docs.exotel.com/exotel-agentstream/connect-exotel-to-sarvam-voice-agents
- Airtel call forwarding overview: https://www.airtel.in/blog/airtel-app/call-forwarding-in-airtel/

## 5. Supabase migration

`supabase/schema.sql` is a starting schema with user ownership and RLS policies. The local FastAPI prototype currently uses SQLite so it works without cloud credentials. Before deploying, replace the local repository implementation with Supabase queries, enable RLS, use private storage buckets for recordings, and never expose service-role keys in the Android app.

## 6. Production checklist

- [ ] Provider account and test number approved and configured
- [ ] Live inbound call works on test number
- [ ] AI clearly identifies itself as AI
- [ ] Caller is informed about recording and consent is handled appropriately
- [ ] Webhook signatures verified and duplicate events handled
- [ ] HTTPS backend and secret manager configured
- [ ] Supabase RLS tested with multiple users
- [ ] Recordings private, access-controlled and retention configured
- [ ] Rate limits, audit events and error monitoring enabled
- [ ] Airtel conditional forwarding tested before enabling on personal number
- [ ] Billing and per-minute costs reviewed before live use

## Scope note
This is a complete **development starter**, not a production-certified telephony service. Live phone answering cannot be enabled just by installing the APK; it depends on provider-side telephony configuration and credentials.
