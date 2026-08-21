# Dhan-OM · Free & Powerful AI Backends — Guide & API Keys

This explains the best **free** ways to give Dhan-OM ChatGPT/Claude-class
accuracy, and how to get an API key in ~2 minutes each.

## TL;DR — what I recommend for you

| Provider | Model | Cost | Best for |
|---|---|---|---|
| **Google Gemini** | gemini-2.5-flash | Free tier (no card) | General + finance answers |
| **Groq** | llama-3.3-70b-versatile | Free tier (fast) | Speed |
| **OpenRouter** | llama-3.3-70b-instruct:free | Free | Many models, one key |
| **Mistral** | mistral-small-latest | Free tier | Solid reasoning |
| **DeepSeek** | deepseek-chat | Very cheap | Long/structured answers |

All of them are already **presets inside the app** (Profile → Cloud Brain →
tap a preset → paste the key). Add a key and Dhan-OM uses the cloud brain
first (fast, accurate) and falls back to the on-device Gemma offline.

## How to get each API key (free)

### 1. Google Gemini (easiest, no card)
1. Open **https://aistudio.google.com/apikey**
2. Sign in with Google → **Create API key**.
3. Paste it in Profile → Cloud Brain → API key, and pick the **Gemini 2.5 Flash** preset.

### 2. Groq (fast, no card)
1. Open **https://console.groq.com/keys**
2. Sign up (email/GitHub) → **Create API Key**.
3. Paste it and pick the **Groq Llama 3.3 70B** preset.

### 3. OpenRouter (many free models, one key)
1. Open **https://openrouter.ai/keys**
2. Sign in → **Create Key**.
3. Paste it and pick any `:free` preset (Llama 3.3 70B, DeepSeek R1).

### 4. Mistral (free tier)
1. Open **https://console.mistral.ai/api-keys**
2. Sign up → create key (free tier is enough).
3. Paste it and pick the **Mistral** preset.

### 5. DeepSeek (cheapest paid, ~₹10 for weeks of use)
1. Open **https://platform.deepseek.com**
2. Sign up → **API Keys** → create.
3. Top up a small amount; paste it and pick **DeepSeek V3**.

## About the GitHub repos you shared

I checked them against GitHub:

- **mnfst/awesome-free-llm-apis** ✅ — curated free API keys. Use it to
  discover more providers (each links to its key page).
- **12britz/awesome-free-models** ✅ — free models/APIs/tools list.
- **nejib1/Free-LLM** ✅ — catalogue of free/open models. Same constraint:
  a 70B model does **not** fit in phone RAM. Dhan-OM already ships Gemma 4
  E4B on-device; use a free cloud key (Groq/Gemini/OpenRouter) for 70B-class
  accuracy.
- **xtekky/gpt4free** ⚠️ — a **Python server** that reverse-proxies many
  providers *without* keys. It CANNOT run inside an Android APK. You'd run it
  on a PC (`pip install g4f`) and point Dhan-OM's Cloud Brain at its local
  endpoint. Unofficial, can be unstable, may violate provider ToS.
- **gacabartosz/gaca-core** — TypeScript "AI bus" with 50+ free models +
  failover. Also server-side; expose it as an OpenAI-compatible URL for Dhan-OM.
- **skydoves/chatgpt-android** — an Android *sample app* for OpenAI
  streaming (reference code, not a provider).

## Downloading free LLMs to your device (no key, fully offline)

Your 300 GB storage is plenty — but **RAM (16 GB) is the real limit**. The
sweet spot for a phone is a **3–8B model in 4-bit (Q4)**:

| Model | Size | Notes |
|---|---|---|
| **Gemma 4 E4B** | ~3.7 GB | Already built in (auto-download) |
| Gemma 4 E2B | ~2.6 GB | Lighter, faster |
| Gemma 3n E4B / E2B | 2.5–4 GB | Multimodal (text+image+audio) |
| Llama 3.2 3B / Qwen 2.5 3B | ~2 GB | Fast, general |

To use any of them, put a `.litertlm` file URL into **Profile → AI Brain →
Model URL** and tap Download (Dhan-OM's downloader resumes dropped connections).
LiteRT-LM models live at **huggingface.co/litert-community**.

> A 4B model on a phone CPU runs ~15–20 tokens/sec; a 70B model does NOT fit in
> 16 GB RAM and would be unusably slow. That's why the cloud presets above are
> the path to "ChatGPT-class" quality — the phone runs a small fast model
> offline, and a free cloud key gives you the big accurate model when online.
