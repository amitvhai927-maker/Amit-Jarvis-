# AMIT JARVIS backend

This small Node.js server keeps your AI provider key on the server, not inside the Android APK.

## Run locally
1. Install Node.js 20 or newer.
2. In this folder, copy `.env.example` to `.env`.
3. Add your provider API key and verify the provider/model are available for your account.
4. Run `npm install` then `npm start`.
5. Deploy to a host that supports Node.js and environment variables.
6. In the app, set the endpoint to `https://YOUR-HOST/api/chat`.

The example defaults to a Groq-compatible OpenAI API format, but you can configure another compatible provider through `AI_BASE_URL`, `AI_API_KEY`, and `AI_MODEL`. Free tiers, available models, quotas, and terms can change; check your provider dashboard.

## Production hardening
Add rate limiting, authentication, abuse controls, request logging without sensitive message contents, and a specific CORS origin before public release. Do not expose `.env` or commit real API keys.
