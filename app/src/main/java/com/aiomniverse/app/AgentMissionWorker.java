package com.aiomniverse.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

/**
 * Runs a saved AI agent mission after the app is backgrounded or closed.
 * WorkManager persists the request; Android may defer execution for battery/network policy.
 */
public class AgentMissionWorker extends Worker {
    private static final String PROVIDER_PREFS = "ai_omniverse_private";
    private static final String MISSION_PREFS = "ai_omniverse_agents";
    private static final String KEY_ALIAS = "ai_omniverse_provider_key_v1";

    private static class Provider {
        final String id, label, endpoint, defaultModel;
        final boolean gemini;
        Provider(String id, String label, String endpoint, String defaultModel, boolean gemini) {
            this.id = id; this.label = label; this.endpoint = endpoint;
            this.defaultModel = defaultModel; this.gemini = gemini;
        }
    }

    private static final List<Provider> PROVIDERS = Arrays.asList(
        new Provider("openrouter", "OpenRouter Free Models", "https://openrouter.ai/api/v1/chat/completions", "openrouter/free", false),
        new Provider("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/models/", "gemini-3.6-flash", true),
        new Provider("groq", "GroqCloud", "https://api.groq.com/openai/v1/chat/completions", "openai/gpt-oss-120b", false),
        new Provider("cerebras", "Cerebras", "https://api.cerebras.ai/v1/chat/completions", "gpt-oss-120b", false),
        new Provider("mistral", "Mistral Free Mode", "https://api.mistral.ai/v1/chat/completions", "ministral-8b-latest", false),
        new Provider("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1/chat/completions", "nvidia/nemotron-3.5-super-vl-120b-a12b", false)
    );

    public AgentMissionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        String missionId = getInputData().getString("mission_id");
        String prompt = getInputData().getString("prompt");
        String agentsText = getInputData().getString("agents_json");
        int agentIndex = getInputData().getInt("agent_index", -1);
        if (missionId == null || prompt == null || agentsText == null || agentIndex < 0) return Result.failure();

        SharedPreferences missions = getApplicationContext().getSharedPreferences(MISSION_PREFS, Context.MODE_PRIVATE);
        JSONObject mission;
        JSONArray logs;
        JSONArray agents;
        try {
            String stored = missions.getString("mission_" + missionId, "");
            mission = stored.isEmpty() ? new JSONObject() : new JSONObject(stored);
            String status = mission.optString("status", "");
            if ("NEEDS_SETUP".equals(status)) return Result.success();
            agents = new JSONArray(agentsText);
            logs = mission.optJSONArray("logs");
            if (logs == null) logs = new JSONArray();
            mission.put("id", missionId);
            mission.put("prompt", prompt);
            mission.put("agents", agents);
            mission.put("logs", logs);
            if (agentIndex < logs.length()) {
                if (logs.length() >= agents.length()) {
                    mission.put("status", "COMPLETED");
                    mission.put("current", "");
                    saveMission(missions, mission);
                }
                return Result.success();
            }
            if (agentIndex >= agents.length()) return Result.success();
            mission.put("status", "RUNNING");
            saveMission(missions, mission);
        } catch (Exception e) {
            return Result.failure();
        }

        List<Provider> configured = configuredProviders();
        if (configured.isEmpty()) {
            try {
                mission.put("status", "NEEDS_SETUP");
                mission.put("current", "");
                mission.put("error", "No provider keys are saved. Open Multi-AI Chat and connect at least one eligible free provider.");
                saveMission(missions, mission);
            } catch (Exception ignored) {}
            return Result.success();
        }

        JSONObject agent = agents.optJSONObject(agentIndex);
        if (agent == null) return Result.success();
        String name = agent.optString("name", "Agent " + (agentIndex + 1));
        String role = agent.optString("role", "Helpful assistant");
        try {
            mission.put("current", name);
            mission.put("status", "RUNNING");
            saveMission(missions, mission);

            String context = buildContext(prompt, logs);
            String instruction = "You are " + name + ", an AI agent in a shared multi-agent group chat. "
                + "Your role: " + role + ". Work on the user's task from your assigned perspective. "
                + "Read prior agent messages in the shared group transcript, improve on them, and point out useful disagreements or gaps. "
                + "Be practical and concrete. Do not claim you performed real-world actions or web research unless tools actually provided them. "
                + "Return a useful response that other agents can build on.\\n\\nUSER TASK:\\n" + prompt
                + "\\n\\nSHARED GROUP TRANSCRIPT SO FAR:\\n" + context;

            String answer = null;
            String usedProvider = "";
            String errors = "";
            for (int attempt = 0; attempt < configured.size(); attempt++) {
                Provider p = configured.get((agentIndex + attempt) % configured.size());
                try {
                    SharedPreferences secretPrefs = getApplicationContext().getSharedPreferences(PROVIDER_PREFS, Context.MODE_PRIVATE);
                    String key = decryptKey(secretPrefs.getString("encrypted_key_" + p.id, ""));
                    if (key.trim().isEmpty()) continue;
                    String model = secretPrefs.getString("model_" + p.id, p.defaultModel);
                    answer = callProvider(p, key, model, instruction);
                    usedProvider = p.label + " · " + model;
                    break;
                } catch (Exception ex) {
                    String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    if (message.length() > 220) message = message.substring(0, 220);
                    errors += (errors.isEmpty() ? "" : "\\n") + p.label + ": " + message;
                }
            }

            JSONObject log = new JSONObject();
            log.put("agent", name);
            log.put("role", role);
            log.put("provider", usedProvider);
            if (answer == null || answer.trim().isEmpty()) {
                log.put("text", "This agent could not get a reply.\\n" + (errors.isEmpty()
                    ? "No saved provider key was available." : errors)
                    + "\\nCheck provider quotas, model availability, and saved API-key settings.");
                log.put("error", true);
            } else {
                log.put("text", answer.trim());
                log.put("error", false);
            }
            logs.put(log);
            mission.put("logs", logs);
            mission.put("current", "");
            mission.put("status", logs.length() >= agents.length() ? "COMPLETED" : "QUEUED");
            saveMission(missions, mission);
        } catch (Exception e) {
            try {
                JSONObject log = new JSONObject();
                log.put("agent", name);
                log.put("role", role);
                log.put("provider", "");
                log.put("text", "Agent error: " + safe(e));
                log.put("error", true);
                logs.put(log);
                mission.put("logs", logs);
                mission.put("current", "");
                mission.put("status", logs.length() >= agents.length() ? "COMPLETED" : "QUEUED");
                saveMission(missions, mission);
            } catch (Exception ignored) {}
        }
        return Result.success();
    }

    private List<Provider> configuredProviders() {
        List<Provider> result = new ArrayList<>();
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PROVIDER_PREFS, Context.MODE_PRIVATE);
        for (Provider p : PROVIDERS) {
            try {
                String encrypted = prefs.getString("encrypted_key_" + p.id, "");
                if (!decryptKey(encrypted).trim().isEmpty()) result.add(p);
            } catch (Exception ignored) {}
        }
        return result;
    }

    private String buildContext(String prompt, JSONArray logs) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < logs.length(); i++) {
            JSONObject item = logs.optJSONObject(i);
            if (item == null) continue;
            String text = item.optString("text", "");
            if (text.length() > 1800) text = text.substring(0, 1800) + "…";
            out.append("\n[").append(item.optString("agent", "Agent")).append("] ")
                .append(text).append("\n");
        }
        if (out.length() > 7500) return out.substring(out.length() - 7500);
        return out.toString().isEmpty() ? "(No agent replies yet.)" : out.toString();
    }

    private String callProvider(Provider p, String key, String model, String prompt) throws Exception {
        JSONObject request = new JSONObject();
        String requestUrl;
        if (p.gemini) {
            JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
            JSONArray contents = new JSONArray().put(new JSONObject().put("role", "user").put("parts", parts));
            request.put("contents", contents);
            request.put("generationConfig", new JSONObject().put("temperature", 0.65).put("maxOutputTokens", 1800));
            requestUrl = p.endpoint + URLEncoder.encode(model, "UTF-8") + ":generateContent";
        } else {
            JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content",
                    "You are one member of a collaborative AI agent group. Stay within your assigned role and be truthful about what you did."))
                .put(new JSONObject().put("role", "user").put("content", prompt));
            request.put("messages", messages);
            request.put("model", model);
            request.put("temperature", 0.65);
            request.put("max_tokens", 1800);
            requestUrl = p.endpoint;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            if (p.gemini) connection.setRequestProperty("x-goog-api-key", key);
            else connection.setRequestProperty("Authorization", "Bearer " + key);
            if (p.id.equals("openrouter")) {
                connection.setRequestProperty("HTTP-Referer", "https://github.com/eyerobalex06-arch/Aiultimate");
                connection.setRequestProperty("X-Title", "AI OMNIVERSE Agent Group");
            }
            connection.setDoOutput(true);
            byte[] bytes = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            if (status < 200 || status >= 300) {
                String detail = body;
                try {
                    JSONObject error = new JSONObject(body);
                    if (error.opt("error") instanceof JSONObject) detail = error.getJSONObject("error").optString("message", body);
                    else detail = error.optString("message", body);
                } catch (Exception ignored) {}
                if (detail.length() > 320) detail = detail.substring(0, 320) + "...";
                throw new Exception("HTTP " + status + ": " + detail);
            }
            JSONObject response = new JSONObject(body);
            if (p.gemini) {
                JSONArray candidates = response.optJSONArray("candidates");
                if (candidates == null || candidates.length() == 0) throw new Exception("No Gemini response candidates.");
                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                JSONArray responseParts = content == null ? null : content.optJSONArray("parts");
                StringBuilder text = new StringBuilder();
                if (responseParts != null) for (int i = 0; i < responseParts.length(); i++)
                    text.append(responseParts.getJSONObject(i).optString("text", ""));
                if (text.toString().trim().isEmpty()) throw new Exception("Gemini returned no text.");
                return text.toString();
            }
            JSONArray choices = response.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new Exception("Provider returned no choices.");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            String answer = message == null ? "" : message.optString("content", "");
            if (answer.trim().isEmpty()) throw new Exception("Provider returned empty text.");
            return answer;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String decryptKey(String stored) throws Exception {
        if (stored == null || stored.isEmpty()) return "";
        byte[] combined = Base64.decode(stored, Base64.NO_WRAP);
        if (combined.length < 13) throw new Exception("Encrypted API key data is invalid.");
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getSecretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        return (SecretKey) store.getKey(KEY_ALIAS, null);
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
        }
        return out.toString();
    }

    private String safe(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) message = e.getClass().getSimpleName();
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    private void saveMission(SharedPreferences prefs, JSONObject mission) {
        prefs.edit().putString("mission_" + mission.optString("id"), mission.toString()).commit();
    }
}
