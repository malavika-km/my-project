/**
 * RESQUE-X Secure Cloud AI Gateway
 * Serves as the intermediary between the mobile application and cloud AI providers.
 * Keeps provider credentials secure and never exposes them to mobile clients.
 */

const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8000;

app.use(cors());
app.use(express.json({ limit: '15mb' }));

const AI_PROVIDER = process.env.AI_PROVIDER || 'mock'; // 'gemini', 'openai', 'groq', or 'mock'
const AI_API_KEY = process.env.AI_API_KEY || '';

// System safety prompt used for cloud AI generation
const SYSTEM_PROMPT = `
You are RESQUE-X, an emergency multimodal first-aid assistant.
CRITICAL RULES:
1. Provide concise, clear, step-by-step first-aid guidance.
2. In life-threatening emergencies, explicitly advise the user to call 112/911 first.
3. If an image of a wound, burn, bite, or injury is supplied, analyze visible signs (bleeding, swelling, burn depth, puncture marks) and provide immediate safety steps.
4. State critical contraindications and warnings (e.g., do not apply ice/butter to burns, do not induce vomiting for poisons, do not cut snakebites).
5. Do NOT prescribe medications, compute drug dosages, or diagnose complex illnesses.
6. Emphasize that your advice is emergency first-aid support, not a replacement for professional emergency medicine.
`;

// Health check endpoint
app.get('/api/v1/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'RESQUE-X Multimodal AI Gateway',
    provider: AI_PROVIDER,
    hasApiKey: Boolean(AI_API_KEY),
    multimodalSupported: true,
    timestamp: new Date().toISOString()
  });
});

// Chat endpoint
app.post('/api/v1/chat', async (req, res) => {
  const { query, imageBase64 } = req.body;

  if (!query || typeof query !== 'string' || query.trim().length === 0) {
    return res.status(400).json({
      status: 'error',
      message: 'Query field is required and must not be empty.'
    });
  }

  const sanitizedQuery = query.trim().slice(0, 500); // Limit length to 500 characters
  const hasImage = Boolean(imageBase64 && typeof imageBase64 === 'string' && imageBase64.length > 50);
  console.log(`[${new Date().toISOString()}] Received first-aid query: "${sanitizedQuery}" (Multimodal Image: ${hasImage})`);

  try {
    let answerText = '';
    let providerName = AI_PROVIDER;
    let modelName = 'default';

    if (AI_PROVIDER === 'openai' && AI_API_KEY) {
      // Query OpenAI Multimodal API (gpt-4o-mini supports text + vision)
      const userContent = [{ type: 'text', text: sanitizedQuery }];
      if (hasImage) {
        userContent.push({
          type: 'image_url',
          image_url: { url: `data:image/jpeg;base64,${imageBase64}` }
        });
      }

      const response = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${AI_API_KEY}`
        },
        body: JSON.stringify({
          model: process.env.AI_MODEL || 'gpt-4o-mini',
          messages: [
            { role: 'system', content: SYSTEM_PROMPT },
            { role: 'user', content: userContent }
          ],
          max_tokens: 380,
          temperature: 0.2
        })
      });

      if (!response.ok) {
        throw new Error(`OpenAI API responded with status ${response.status}`);
      }
      const data = await response.json();
      answerText = data.choices[0]?.message?.content || 'No response generated.';
      modelName = process.env.AI_MODEL || 'gpt-4o-mini';

    } else if (AI_PROVIDER === 'gemini' && AI_API_KEY) {
      // Query Google Gemini Multimodal API (gemini-1.5-flash / gemini-2.0-flash)
      const model = process.env.AI_MODEL || 'gemini-1.5-flash';
      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${AI_API_KEY}`;
      
      const parts = [{ text: sanitizedQuery }];
      if (hasImage) {
        parts.push({
          inline_data: {
            mime_type: 'image/jpeg',
            data: imageBase64
          }
        });
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          system_instruction: { parts: [{ text: SYSTEM_PROMPT }] },
          contents: [{ parts }],
          generationConfig: { maxOutputTokens: 380, temperature: 0.2 }
        })
      });

      if (!response.ok) {
        throw new Error(`Gemini API responded with status ${response.status}`);
      }
      const data = await response.json();
      answerText = data.candidates?.[0]?.content?.parts?.[0]?.text || 'No response generated.';
      modelName = model;

    } else {
      // Default / Mock mode (allows seamless testing without third-party API keys)
      providerName = 'RESQUE-X Multimodal Gateway (Simulated Mode)';
      modelName = 'emergency-multimodal-v1';
      
      if (hasImage) {
        answerText = `[Multimodal Vision Assessment]\nI have analyzed the provided injury photograph and your query: "${sanitizedQuery}"\n\n1. Immediate First-Aid Steps:\n   - Check bleeding severity: If blood is actively flowing or spurting, apply direct, firm pressure with a clean cloth immediately.\n   - Do not touch open or broken tissue with bare hands.\n   - Rinse shallow cuts or abrasions with clean running water; do NOT apply harsh chemicals (e.g. hydrogen peroxide/alcohol) directly into deep tissue.\n2. When to Call 112/911 Immediately:\n   - Deep lacerations, gaping skin, numbness below injury, or uncontrolled bleeding.\n   - Blistering, charred, or large-area burns.\n   - Suspected venomous snake/insect bites with rapid swelling or systemic symptoms.\n3. Keep the patient seated or lying down, calm, and warm to prevent shock.\n\nDisclaimer: Automated visual assessment is an emergency aid and does not replace examination by a licensed medical practitioner.`;
      } else {
        answerText = `[Cloud AI Response]\nFor "${sanitizedQuery}":\n\n1. Ensure immediate scene safety before taking any action.\n2. Call local emergency services (112 or 911) if the person is unresponsive, bleeding severely, or in severe distress.\n3. Apply direct pressure to any bleeding wounds using a sterile or clean cloth.\n4. Keep the person calm and warm until emergency personnel arrive.\n\nDisclaimer: This is emergency first-aid guidance. Always verify with emergency medical responders.`;
      }
    }

    return res.json({
      status: 'success',
      data: {
        answer: answerText,
        provider: providerName,
        model: modelName,
        isVerified: false,
        disclaimer: 'Cloud AI generated guidance. Always verify with certified medical personnel.'
      }
    });

  } catch (error) {
    console.error('Error handling chat request:', error);
    return res.status(502).json({
      status: 'error',
      message: error.message || 'Failed to communicate with the cloud AI provider.'
    });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`RESQUE-X Cloud Gateway running on port ${PORT}`);
  console.log(`Active Provider: ${AI_PROVIDER}`);
  console.log(`To test in Android Emulator, connect to: http://10.0.2.2:${PORT}`);
});
