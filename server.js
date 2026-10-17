import express from "express";
import cors from "cors";
import dotenv from "dotenv";

dotenv.config();
const app = express();
app.use(cors({ origin: process.env.ALLOWED_ORIGIN || "*" }));
app.use(express.json({ limit: "32kb" }));

app.get("/", (_req, res) => {
  res.json({ name: "AMIT JARVIS API", status: "ok", endpoint: "/api/chat" });
});

app.post("/api/chat", async (req, res) => {
  const message = typeof req.body?.message === "string" ? req.body.message.trim() : "";
  if (!message) return res.status(400).json({ error: "Please send a message." });
  if (message.length > 6000) return res.status(413).json({ error: "Message is too long." });
  if (!process.env.AI_API_KEY) {
    return res.status(503).json({ error: "AI backend is not configured. Add AI_API_KEY in the server environment." });
  }

  try {
    const base = (process.env.AI_BASE_URL || "https://api.groq.com/openai/v1").replace(/\/$/, "");
    const response = await fetch(`${base}/chat/completions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.AI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: process.env.AI_MODEL || "llama-3.3-70b-versatile",
        messages: [
          { role: "system", content: "You are AMIT, a helpful personal AI assistant. Reply clearly and safely. If the user writes Nepali or Hindi, answer in that language. Never claim to have performed device actions unless the app explicitly confirms them." },
          { role: "user", content: message }
        ],
        temperature: 0.6,
        max_tokens: 800
      })
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      console.error("AI provider error:", response.status, data?.error?.message || "provider error");
      return res.status(502).json({ error: "The AI provider request failed. Check your provider key, model, and quota." });
    }
    const reply = data?.choices?.[0]?.message?.content;
    if (!reply) return res.status(502).json({ error: "The AI provider returned no answer." });
    res.json({ reply });
  } catch (err) {
    console.error("Backend request failed:", err?.message || err);
    res.status(500).json({ error: "Server error while contacting the AI provider." });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, "0.0.0.0", () => console.log(`AMIT JARVIS API listening on ${port}`));
