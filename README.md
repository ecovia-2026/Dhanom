# Dhan-OM ॐ — Offline-First Personal Finance AI Companion 🇮🇳

> **Install:** see **[INSTALL.md](INSTALL.md)**. GitHub Actions gives a **ZIP**.
> Unzip, then tap **`app-debug.apk`**. Installing the zip = “App not installed”.

**Dhan-OM** is a privacy-first Android app and your personal finance brain:
expense tracker, budget planner, investment & portfolio manager, financial-goal
strategist, and a conversational AI that you can **talk to** (type or voice).
Everything runs **offline-first** on your device; the cloud is an optional
upgrade, never a requirement.

## Your money, one place

| Area | What you get |
|---|---|
| **Expense & income tracking** | Ledger with search/filter/sort, recurring flags, merchant, account, notes, Need/Want/Savings tags |
| **Chat-driven tracking** | *"Spent ₹450 on Swiggy"*, *"Add income 50000 salary"*, *"Delete my last transaction"*, *"Delete transaction 3"*, *"Set budget 8000 groceries"* — commands run instantly and update the tracker live |
| **Budgets** | Monthly category budgets with progress + near-limit / over-budget alerts |
| **Goals & strategies** | Savings goals with deposits, pacing projections and step-by-step strategies to hit each goal on time |
| **Investments & share market** | Indian + international holdings (stocks, ETFs, mutual funds/SIP, gold & SGB, PPF/EPF/NPS, crypto, REITs, bonds) with P&L and asset allocation |
| **Indian + international finance** | INR default, multi-currency, Section 80C tax nudges, SIP guidance, PPF/EPF, Sovereign Gold Bonds, US/global equities |
| **Revenue & planning** | Salary, freelance, dividends/returns; cash-flow forecasts, 50/30/20 allocation, financial health score |
| **Daily suggestions** | Budget alerts, burn-rate warnings, subscription audits, weekend-spend reminders, goal acceleration, tax planning |
| **Export & device transfer** | CSV (Excel), PDF report, full JSON backup; share via Quick Share/Nearby Share/Bluetooth/email and import on the new phone |
| **Navigation** | Clean **side drawer** menu (hamburger), no bottom bar clutter |
| **On-device LLM** | Run **Gemma 4 E4B** fully offline (LiteRT-LM); auto-download prompt on first launch; share it with other apps/phones via a local OpenAI-compatible server |
| **Self-healing** | Any crash is logged and the app restarts itself; crash log viewable in Profile |
| **Files in chat** | Attach **any file type up to 500 MB** (images, PDF, Excel, ZIP, CSV, JSON…). Images no longer crash the app; previews show in the bubble. Unsent chat text is remembered after you close the app. |

## The "brain" — 4 modes

Dhan-OM's brain is layered so it always works (Profile → AI Brain):

1. **Offline Brain (default, zero internet)** — an on-device NLU command engine
   (English + Hinglish: *kharcha, khana, rashan, aaya, tankhwah…*) plus an
   on-device ML engine that learns merchants, categories, recurring
   subscriptions, spending surges and anomalies, and stores them as
   persistent "brain memories".
2. **Gemma 4 E4B (on-device LLM)** — runs Google's ~4.5B-parameter model
   **fully on your phone** via **LiteRT-LM**. No internet, no API key,
   ~3.66 GB model file (needs 8 GB+ RAM; your 16 GB device is ideal).
   *First launch asks to download it automatically* (built-in official URL);
   or do it manually in Profile → AI Brain → **Download model**.
   See **[GEMMA-GUIDE.md](GEMMA-GUIDE.md)** for setup, troubleshooting and the
   monthly update checklist.
3. **Gemini Cloud Brain (optional)** — paste a Google AI Studio key for cloud
   LLM answers (falls back offline when there's no internet).
4. **Custom Agent Brain (optional)** — point the app at your own
   OpenAI-compatible `/v1/chat/completions` endpoint to connect the research
   agents you mentioned — **[FinSight](https://github.com/RUC-NLPIR/FinSight)**,
   **[FinRobot](https://github.com/ai4finance-foundation/finrobot)**,
   **[finance-agent](https://github.com/kamathhrishi/finance-agent)**,
   **[FTShare-MCP](https://github.com/FTShare-Lab/FTShare-MCP)** — which are
   Python/LLM frameworks that run **server-side** (they need an LLM API key and
   a machine, so they can't be embedded inside an APK).

### Brain plugin server (share the brain with other apps/phones)
Dhan-OM can expose its on-device brain as an **OpenAI-compatible HTTP API** so
other Android apps on the same phone — or other phones on the same Wi-Fi — can
use it: Profile → AI Brain → **Brain Plugin Server** → set port (+ optional
Bearer token) → **Start server**. Endpoint:

```
POST http://<this-phone-ip>:8080/v1/chat/completions
GET  http://<this-phone-ip>:8080/v1/models
```

Keep Dhan-OM open while serving (the server runs with the app process).

## No fake data

The app **starts completely empty** (only a welcome message). There is **no
auto-seeded demo data**. Every entry you add — by chat, voice, or the manual
forms — is stored in the on-device Room database and reflected immediately
across the Dashboard, Ledger, Flow and Portfolio screens.

- Want to explore the UI? *Profile → Data Control → Load demo data* (explicit).
- Want a clean slate? *Profile → Data Control → Clear ALL data*.

## Install the APK (sideload)

v1.7 is a **new app id** (`com.dhanom.finance`) signed the same way as the
original 64 MB APK that installed (Android debug cert + v1+v2+v3, all native
ABIs). It will **not** fight an old Dhan-OM left on the phone.

1. Open the latest green run: [Android CI - Build APK](https://github.com/ecovia-2026/Dhanom/actions/workflows/android-build.yml)
2. Download **only** **`DhanomAI-Finance-debug-apk`** (tens of MB).  
   Skip `merged-apks` (a few hundred bytes — not the app).
3. **Unzip**. Install **`app-debug.apk`**. Never install the `.zip`.
4. Allow **Install unknown apps** for Files/Chrome.
5. If Play Protect / MIUI Security warns, tap **Install anyway**.
6. You can delete any leftover older Dhan-OM afterwards.

**Build locally:** `gradle assembleDebug` (JDK 17 + Android SDK API 36).

## Optional: Gemini key

Put your key in a `.env` file (gitignored) or set the `GEMINI_API_KEY` env var
at build time — or simply paste it at runtime in *Profile → AI Brain*.

## Notes

- **Install-safe on Android 15+ 16 KB phones:** no native `.so` in the APK
  (LiteRT 4 KB libs were the install blocker). Cloud Brain for LLM answers.
- **Welcome voice:** the app speaks *"Welcome to Dhan Om, your personal AI"* on
  launch (toggle in Profile).
- All financial data stays on-device; no account or cloud sync unless you
  explicitly share/import a backup file.
- App icon: **ॐ**. App name: **Dhan-OM**.
