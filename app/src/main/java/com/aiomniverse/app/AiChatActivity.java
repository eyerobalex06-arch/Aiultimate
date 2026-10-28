package com.aiomniverse.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

public class AiChatActivity extends Activity {
    private static final String PREFS = "ai_omniverse_private";
    private static final String KEY_ALIAS = "ai_omniverse_provider_key_v1";
    private static final int BG = Color.rgb(9, 11, 20);
    private static final int PANEL = Color.rgb(19, 22, 37);
    private static final int WHITE = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(160, 168, 190);
    private static final int PURPLE = Color.rgb(167, 139, 250);

    private final List<Provider> providers = Arrays.asList(
        new Provider("openrouter", "OpenRouter Free Models", "https://openrouter.ai/api/v1/chat/completions",
            "https://openrouter.ai/settings/keys", "openrouter/free", false),
        new Provider("gemini", "Google Gemini API", "https://generativelanguage.googleapis.com/v1beta/models/",
            "https://aistudio.google.com/apikey", "gemini-3.6-flash", true),
        new Provider("groq", "GroqCloud API", "https://api.groq.com/openai/v1/chat/completions",
            "https://console.groq.com/keys", "openai/gpt-oss-120b", false),
        new Provider("cerebras", "Cerebras Inference API", "https://api.cerebras.ai/v1/chat/completions",
            "https://cloud.cerebras.ai/platform", "gpt-oss-120b", false),
        new Provider("mistral", "Mistral AI Free Mode", "https://api.mistral.ai/v1/chat/completions",
            "https://console.mistral.ai/api-keys/", "ministral-8b-latest", false),
        new Provider("nvidia", "NVIDIA NIM Free Endpoints", "https://integrate.api.nvidia.com/v1/chat/completions",
            "https://build.nvidia.com/", "nvidia/nemotron-3.5-super-vl-120b-a12b", false)
    );

    private Spinner providerSpinner;
    private EditText keyInput;
    private EditText keyNameInput;
    private EditText modelInput;
    private EditText messageInput;
    private TextView providerInfo;
    private TextView connectionStatus;
    private LinearLayout messagesColumn;
    private ScrollView messagesScroll;
    private Button saveButton;
    private Button forgetButton;
    private Button sendButton;
    private int currentProviderIndex = 0;
    private boolean busy = false;
    private final List<ChatMessage> history = new ArrayList<>();

    private static class Provider {
        final String id, label, endpoint, keyUrl, defaultModel;
        final boolean gemini;
        Provider(String id, String label, String endpoint, String keyUrl, String defaultModel, boolean gemini) {
            this.id = id; this.label = label; this.endpoint = endpoint; this.keyUrl = keyUrl;
            this.defaultModel = defaultModel; this.gemini = gemini;
        }
    }

    private static class ChatMessage {
        final String role, text, source;
        ChatMessage(String role, String text, String source) {
            this.role = role; this.text = text; this.source = source == null ? "" : source;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        if (getIntent().getBooleanExtra("open_key_setup", false)) {
            // Start directly on a real provider so the key field is enabled immediately.
            providerSpinner.setSelection(1);
            new AlertDialog.Builder(this)
                .setTitle("Add an API key")
                .setMessage("Choose a provider from PROVIDER ROUTER, paste your key into the API key field, then tap SAVE KEY. Use GET FREE KEY to open that provider's official site. Your saved key is encrypted on this device.")
                .setPositiveButton("GOT IT", (dialog, which) -> dialog.dismiss())
                .show();
        }
        addMessage("assistant", "Welcome to AI OMNIVERSE cloud chat. The app itself does not impose a daily message quota. Add multiple free-tier provider keys and Auto-switch will try other configured providers when one fails or is rate-limited. Real usage is still limited by each provider’s quotas, model access, account rules, and availability, which can change. This cannot guarantee uninterrupted or unlimited cloud usage. Your keys are encrypted on this device; messages are sent to the provider that answers.", "AI OMNIVERSE");
        updateConnectionStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(6), dp(16), dp(6));
        toolbar.setBackgroundColor(PANEL);
        Button back = button("‹  BACK", Color.TRANSPARENT);
        back.setTextColor(WHITE);
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(84), dp(44)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("AI OMNIVERSE", 15, WHITE, true);
        TextView sub = text("MULTI-PROVIDER CHAT", 10, PURPLE, true);
        titleBox.addView(title);
        titleBox.addView(sub);
        toolbar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView statusChip = text("FREE TIERS", 10, PURPLE, true);
        statusChip.setPadding(dp(10), dp(7), dp(10), dp(7));
        statusChip.setBackground(round(Color.rgb(39, 30, 67), 16));
        toolbar.addView(statusChip);
        Button keyMenu = button("⋮", Color.TRANSPARENT);
        keyMenu.setTextSize(20);
        keyMenu.setContentDescription("API key options");
        toolbar.addView(keyMenu, new LinearLayout.LayoutParams(dp(42), dp(44)));
        keyMenu.setOnClickListener(v -> showApiKeyMenu(v));
        root.addView(toolbar);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(8));

        LinearLayout config = new LinearLayout(this);
        config.setOrientation(LinearLayout.VERTICAL);
        config.setPadding(dp(14), dp(13), dp(14), dp(12));
        config.setBackground(round(PANEL, 18));
        body.addView(config, new LinearLayout.LayoutParams(-1, -2));

        config.addView(text("PROVIDER ROUTER", 11, PURPLE, true));
        providerSpinner = new Spinner(this);
        List<String> providerNames = new ArrayList<>();
        providerNames.add("Auto-switch (all saved free providers)");
        for (Provider p : providers) providerNames.add(p.label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(-1, dp(48));
        spinnerParams.topMargin = dp(6);
        config.addView(providerSpinner, spinnerParams);

        providerInfo = text("", 11, MUTED, false);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(5);
        config.addView(providerInfo, infoParams);
        providerInfo.setOnClickListener(v -> openProviderSetup());

        keyNameInput = input("Name this key (e.g. My OpenRouter Key)", false);
        LinearLayout.LayoutParams keyNameParams = new LinearLayout.LayoutParams(-1, dp(46));
        keyNameParams.topMargin = dp(9);
        config.addView(keyNameInput, keyNameParams);

        modelInput = input("Model ID (provider default)", false);
        LinearLayout.LayoutParams modelParams = new LinearLayout.LayoutParams(-1, dp(46));
        modelParams.topMargin = dp(9);
        config.addView(modelInput, modelParams);

        keyInput = input("Paste this provider's API key", true);
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(-1, dp(46));
        keyParams.topMargin = dp(8);
        config.addView(keyInput, keyParams);

        LinearLayout configButtons = new LinearLayout(this);
        configButtons.setOrientation(LinearLayout.HORIZONTAL);
        configButtons.setGravity(Gravity.CENTER_VERTICAL);
        saveButton = button("SAVE KEY", Color.rgb(91, 70, 164));
        forgetButton = button("FORGET KEY", Color.rgb(44, 47, 70));
        Button getKeyButton = button("GET FREE KEY ↗", Color.rgb(44, 47, 70));
        configButtons.addView(saveButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams forgetParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        forgetParams.leftMargin = dp(6);
        configButtons.addView(forgetButton, forgetParams);
        LinearLayout.LayoutParams getParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        getParams.leftMargin = dp(6);
        configButtons.addView(getKeyButton, getParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(8);
        config.addView(configButtons, buttonsParams);

        connectionStatus = text("", 10, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        config.addView(connectionStatus, statusParams);

        providerSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentProviderIndex = position;
                refreshProviderForm();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        saveButton.setOnClickListener(v -> saveCurrentKey());
        forgetButton.setOnClickListener(v -> forgetCurrentKey());
        getKeyButton.setOnClickListener(v -> openProviderSetup());

        TextView chatLabel = text("CONVERSATION", 11, WHITE, true);
        LinearLayout.LayoutParams chatLabelParams = new LinearLayout.LayoutParams(-1, -2);
        chatLabelParams.topMargin = dp(14);
        chatLabelParams.bottomMargin = dp(7);
        body.addView(chatLabel, chatLabelParams);

        messagesColumn = new LinearLayout(this);
        messagesColumn.setOrientation(LinearLayout.VERTICAL);
        messagesScroll = new ScrollView(this);
        messagesScroll.setFillViewport(false);
        messagesScroll.setBackground(round(Color.rgb(12, 15, 26), 16));
        messagesScroll.addView(messagesColumn, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams messagesParams = new LinearLayout.LayoutParams(-1, 0, 1);
        body.addView(messagesScroll, messagesParams);

        LinearLayout conversationControls = new LinearLayout(this);
        conversationControls.setOrientation(LinearLayout.HORIZONTAL);
        conversationControls.setGravity(Gravity.CENTER_VERTICAL);
        Button toTop = button("↑ TOP", Color.rgb(35, 38, 57));
        Button toBottom = button("↓ BOTTOM", Color.rgb(35, 38, 57));
        conversationControls.addView(toTop, new LinearLayout.LayoutParams(0, dp(38), 1));
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(0, dp(38), 1);
        bottomParams.leftMargin = dp(8);
        conversationControls.addView(toBottom, bottomParams);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(6);
        body.addView(conversationControls, controlsParams);
        toTop.setOnClickListener(v -> messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_UP)));
        toBottom.setOnClickListener(v -> messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN)));

        Button clearChat = button("CLEAR CONVERSATION", Color.rgb(35, 38, 57));
        clearChat.setTextColor(MUTED);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, dp(36));
        clearParams.topMargin = dp(7);
        body.addView(clearChat, clearParams);
        clearChat.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Clear this conversation?")
            .setMessage("This clears the messages from this chat screen.")
            .setNegativeButton("KEEP", (d, w) -> d.dismiss())
            .setPositiveButton("CLEAR", (d, w) -> {
                history.clear();
                messagesColumn.removeAllViews();
                addMessage("assistant", "Conversation cleared. Your provider keys are unchanged.", "AI OMNIVERSE");
            }).show());

        page.addView(body);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(8));
        composer.setBackgroundColor(PANEL);
        messageInput = input("Ask anything...", false);
        messageInput.setMinHeight(dp(48));
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(4);
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1));
        sendButton = button("SEND  ➤", Color.rgb(126, 87, 194));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(92), dp(48));
        sendParams.leftMargin = dp(7);
        composer.addView(sendButton, sendParams);
        sendButton.setOnClickListener(v -> sendMessage());
        root.addView(composer);
        setContentView(root);
    }

    private void refreshProviderForm() {
        boolean auto = currentProviderIndex == 0;
        if (auto) {
            providerInfo.setText("No app-set daily message quota. Tries every saved provider in order and falls back after errors or rate limits. Free quotas, account rules, and availability still apply; switching providers does not bypass limits. Add as many eligible free-provider accounts as their terms permit.");
            modelInput.setText("");
            modelInput.setHint("Auto mode chooses each provider's saved model");
            keyNameInput.setText("");
            keyNameInput.setHint("Select a provider to name its key");
            keyNameInput.setEnabled(false);
            keyInput.setText("");
            keyInput.setHint("Select a specific provider to add its key");
            keyInput.setEnabled(false);
            modelInput.setEnabled(false);
            saveButton.setEnabled(false);
            forgetButton.setEnabled(false);
        } else {
            Provider p = providers.get(currentProviderIndex - 1);
            String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
            String savedKeyName = getSavedNameForModel(p, model);
            providerInfo.setText(p.label + (savedKeyName.isEmpty() ? "" : "  •  Saved as: " + savedKeyName)
                + "  •  Keys are stored separately for each model ID.");
            keyNameInput.setEnabled(true);
            keyNameInput.setHint("Name this model key");
            keyNameInput.setText(savedKeyName.isEmpty() ? "My " + p.label + " Key" : savedKeyName);
            modelInput.setHint("Model ID (each model has its own key)");
            modelInput.setEnabled(true);
            keyInput.setEnabled(true);
            saveButton.setEnabled(true);
            forgetButton.setEnabled(true);
            modelInput.setText(model);
            try { keyInput.setText(decryptKey(getSavedKeyForModel(p, model))); }
            catch (Exception e) { keyInput.setText(""); }
            keyInput.setHint("Paste API key for this model");
            modelInput.setOnFocusChangeListener((view, hasFocus) -> {
                if (!hasFocus && currentProviderIndex > 0) {
                    Provider selected = providers.get(currentProviderIndex - 1);
                    String selectedModel = modelInput.getText().toString().trim();
                    if (!selectedModel.isEmpty()) {
                        String savedName = getSavedNameForModel(selected, selectedModel);
                        keyNameInput.setText(savedName.isEmpty() ? "My " + selected.label + " Key" : savedName);
                        try { keyInput.setText(decryptKey(getSavedKeyForModel(selected, selectedModel))); }
                        catch (Exception ignored) { keyInput.setText(""); }
                        keyInput.setHint("Paste API key for this model");
                    }
                }
            });
        }
        updateConnectionStatus();
    }

    private static final String KEY_PREFIX = "encrypted_key_";
    private static final String MODEL_KEY_PREFIX = "model_";
    private static final String KEY_NAME_PREFIX = "key_name_";

    private String getPreferences(String key, String fallback) {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, fallback);
    }

    private String modelKeySuffix(Provider provider, String model) {
        String value = model == null ? "" : model.trim();
        String encoded = Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),
            Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return provider.id + "_" + (encoded.isEmpty() ? "default" : encoded);
    }
    private String getSavedKeyForModel(Provider provider, String model) {
        String value = getPreferences(KEY_PREFIX + modelKeySuffix(provider, model), "");
        if (value.isEmpty() && model.equals(getPreferences(MODEL_KEY_PREFIX + provider.id, provider.defaultModel))) {
            value = getPreferences(KEY_PREFIX + provider.id, "");
        }
        return value;
    }
    private String getSavedNameForModel(Provider provider, String model) {
        String value = getPreferences(KEY_NAME_PREFIX + modelKeySuffix(provider, model), "");
        if (value.isEmpty() && model.equals(getPreferences(MODEL_KEY_PREFIX + provider.id, provider.defaultModel))) {
            value = getPreferences(KEY_NAME_PREFIX + provider.id, "");
        }
        return value;
    }

    private void saveCurrentKey() {
        if (currentProviderIndex == 0) return;
        Provider p = providers.get(currentProviderIndex - 1);
        String key = keyInput.getText().toString().trim();
        String keyName = keyNameInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        if (keyName.isEmpty()) keyName = "My " + p.label + " Key";
        if (keyName.length() > 60) keyName = keyName.substring(0, 60);
        try {
            if (model.isEmpty()) model = p.defaultModel;
            String keyPreference = KEY_PREFIX + modelKeySuffix(p, model);
            String namePreference = KEY_NAME_PREFIX + modelKeySuffix(p, model);
            String existingKey = getSavedKeyForModel(p, model);
            android.content.SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            if (!key.isEmpty()) editor.putString(keyPreference, encryptKey(key));
            else if (existingKey.isEmpty()) {
                Toast.makeText(this, "Paste an API key for this model first. It is saved only on this device.", Toast.LENGTH_LONG).show();
                return;
            }
            editor.putString(namePreference, keyName);
            editor.putString(MODEL_KEY_PREFIX + p.id, model);
            editor.remove(KEY_PREFIX + p.id).remove(KEY_NAME_PREFIX + p.id).apply();
            keyInput.setText("");
            keyInput.setHint("Saved securely for model: " + model);
            providerInfo.setText(p.label + "  •  " + model + "  •  Saved as: " + keyName + "  •  Encrypted on this device.");
            Toast.makeText(this, "Saved '" + keyName + "' for model " + model + ". Other model keys stay separate.", Toast.LENGTH_LONG).show();
            updateConnectionStatus();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save settings: " + safeError(e), Toast.LENGTH_LONG).show();
        }
    }

    private void forgetCurrentKey() {
        if (currentProviderIndex == 0) return;
        Provider p = providers.get(currentProviderIndex - 1);
        String model = modelInput.getText().toString().trim();
        if (model.isEmpty()) model = p.defaultModel;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .remove(KEY_PREFIX + modelKeySuffix(p, model))
            .remove(KEY_NAME_PREFIX + modelKeySuffix(p, model))
            .remove(KEY_PREFIX + p.id)
            .remove(KEY_NAME_PREFIX + p.id)
            .apply();
        keyInput.setText("");
        keyNameInput.setText("My " + p.label + " Key");
        providerInfo.setText(p.label + "  •  Key for model " + model + " removed from this device.");
        Toast.makeText(this, p.label + " key forgotten.", Toast.LENGTH_SHORT).show();
        updateConnectionStatus();
    }


    private void showApiKeyMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Add / change API key").setOnMenuItemClickListener(item -> {
            providerSpinner.setSelection(1);
            Toast.makeText(this, "Choose a provider and model, paste the key, then tap SAVE KEY.", Toast.LENGTH_LONG).show();
            return true;
        });
        android.view.SubMenu keyLinks = menu.getMenu().addSubMenu("Get API key from provider");
        for (Provider provider : providers) {
            keyLinks.add(provider.label).setOnMenuItemClickListener(item -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(provider.keyUrl))); }
                catch (Exception e) { Toast.makeText(this, "Couldn't open the provider website.", Toast.LENGTH_SHORT).show(); }
                return true;
            });
        }
        menu.getMenu().add("Delete this model's API key").setOnMenuItemClickListener(item -> {
            forgetCurrentKey();
            return true;
        });
        menu.getMenu().add("Scroll conversation to top").setOnMenuItemClickListener(item -> {
            messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_UP));
            return true;
        });
        menu.getMenu().add("Scroll conversation to bottom").setOnMenuItemClickListener(item -> {
            messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
            return true;
        });
        menu.show();
    }

    private void openProviderSetup() {
        if (currentProviderIndex == 0) {
            new AlertDialog.Builder(this)
                .setTitle("Connect free AI providers")
                .setMessage("Choose OpenRouter, Gemini, Groq, Cerebras, Mistral Free Mode, or NVIDIA NIM, then tap GET FREE KEY. The app has no daily message quota of its own. Add multiple eligible free-provider keys and choose Auto-switch to try alternatives after errors or rate limits. Provider quotas, model restrictions, account rules, and availability still apply; auto-switching cannot bypass them or guarantee uninterrupted use. Do not enable paid billing unless you intentionally want to pay.")
                .setPositiveButton("GOT IT", (d, w) -> d.dismiss()).show();
            return;
        }
        Provider p = providers.get(currentProviderIndex - 1);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.keyUrl)));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open the provider website.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConnectionStatus() {
        int connected = 0;
        for (Provider p : providers) {
            try {
                String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
                String encrypted = getSavedKeyForModel(p, model);
                if (!encrypted.isEmpty() && !decryptKey(encrypted).trim().isEmpty()) connected++;
            } catch (Exception ignored) {}
        }
        if (connectionStatus != null) {
            connectionStatus.setText(connected + " of " + providers.size() +
                " providers configured. No app-set daily message cap; provider quotas still apply.");
        }
    }

    private void sendMessage() {
        if (busy) return;
        String prompt = messageInput.getText().toString().trim();
        if (prompt.isEmpty()) return;
        messageInput.setText("");
        addMessage("user", prompt, "");
        setBusy(true);
        new Thread(() -> {
            String answer = null;
            String source = "";
            String errors = "";
            List<Provider> order = getProviderOrder();
            if (order.isEmpty()) {
                errors = "No API keys are configured yet. Select OpenRouter Free, Gemini, Groq, or Cerebras above and tap GET FREE KEY.";
            } else {
                for (Provider p : order) {
                    try {
                        String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
                        String key = decryptKey(getSavedKeyForModel(p, model));
                        if (key.trim().isEmpty()) continue;
                        answer = requestProvider(p, key, model, history);
                        source = p.label + " · " + model;
                        break;
                    } catch (Exception e) {
                        String message = safeError(e);
                        errors = (errors.isEmpty() ? "" : errors + "\n") + p.label + ": " + message;
                    }
                }
            }
            final String finalAnswer = answer;
            final String finalSource = source;
            final String finalErrors = errors;
            runOnUiThread(() -> {
                if (finalAnswer != null && !finalAnswer.trim().isEmpty()) {
                    addMessage("assistant", finalAnswer.trim(), finalSource);
                } else {
                    String errorText = "None of the configured providers returned a reply.\n\n" +
                        (finalErrors.isEmpty() ? "Check that your saved API keys and model IDs are correct." : finalErrors) +
                        "\n\nTry another free provider or check its dashboard for quota/service status.";
                    addMessage("assistant", errorText, "ROUTER");
                }
                setBusy(false);
                updateConnectionStatus();
            });
        }).start();
    }

    private List<Provider> getProviderOrder() {
        List<Provider> configured = new ArrayList<>();
        for (Provider p : providers) {
            try {
                String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
                String key = decryptKey(getSavedKeyForModel(p, model));
                if (!key.trim().isEmpty()) configured.add(p);
            } catch (Exception ignored) {}
        }
        if (currentProviderIndex > 0) {
            Provider selected = providers.get(currentProviderIndex - 1);
            if (configured.remove(selected)) configured.add(0, selected);
        }
        return configured;
    }

    private String requestProvider(Provider p, String key, String model, List<ChatMessage> messages) throws Exception {
        JSONObject request = new JSONObject();
        if (p.gemini) {
            JSONArray contents = new JSONArray();
            int start = Math.max(0, messages.size() - 20);
            for (int i = start; i < messages.size(); i++) {
                ChatMessage m = messages.get(i);
                if (!m.role.equals("user") && !m.role.equals("assistant")) continue;
                if (m.source.equals("AI OMNIVERSE") || m.source.equals("ROUTER")) continue;
                JSONObject item = new JSONObject();
                item.put("role", m.role.equals("assistant") ? "model" : "user");
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", m.text));
                item.put("parts", parts);
                contents.put(item);
            }
            request.put("contents", contents);
        } else {
            JSONArray apiMessages = new JSONArray();
            int start = Math.max(0, messages.size() - 20);
            for (int i = start; i < messages.size(); i++) {
                ChatMessage m = messages.get(i);
                if (!m.role.equals("user") && !m.role.equals("assistant")) continue;
                if (m.source.equals("AI OMNIVERSE") || m.source.equals("ROUTER")) continue;
                apiMessages.put(new JSONObject().put("role", m.role).put("content", m.text));
            }
            request.put("messages", apiMessages);
            request.put("model", model);
            request.put("temperature", 0.7);
        }

        String requestUrl;
        if (p.gemini) {
            requestUrl = p.endpoint + URLEncoder.encode(model, "UTF-8") + ":generateContent";
        } else {
            requestUrl = p.endpoint;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(70000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            if (p.gemini) connection.setRequestProperty("x-goog-api-key", key);
            else connection.setRequestProperty("Authorization", "Bearer " + key);
            if (p.id.equals("openrouter")) {
                connection.setRequestProperty("HTTP-Referer", "https://github.com/eyerobalex06-arch/Aiultimate");
                connection.setRequestProperty("X-Title", "AI OMNIVERSE");
            }
            connection.setDoOutput(true);
            byte[] data = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(data.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(data); }
            int code = connection.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            String responseText = readAll(stream);
            if (code < 200 || code >= 300) {
                String detail = responseText;
                try {
                    JSONObject errorJson = new JSONObject(responseText);
                    if (errorJson.has("error")) {
                        Object err = errorJson.get("error");
                        if (err instanceof JSONObject) detail = ((JSONObject) err).optString("message", responseText);
                        else detail = String.valueOf(err);
                    }
                } catch (Exception ignored) {}
                if (detail.length() > 350) detail = detail.substring(0, 350) + "...";
                throw new Exception("HTTP " + code + ": " + detail);
            }
            JSONObject result = new JSONObject(responseText);
            if (p.gemini) {
                JSONArray candidates = result.optJSONArray("candidates");
                if (candidates == null || candidates.length() == 0) throw new Exception("No response candidates. The model may be unavailable for this key.");
                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                JSONArray parts = content == null ? null : content.optJSONArray("parts");
                StringBuilder text = new StringBuilder();
                if (parts != null) for (int i = 0; i < parts.length(); i++) text.append(parts.getJSONObject(i).optString("text", ""));
                if (text.toString().trim().isEmpty()) throw new Exception("The provider returned no text. Check the model or provider safety response.");
                return text.toString();
            }
            JSONArray choices = result.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new Exception("Provider returned no choices. Try a different model ID.");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            String answer = message == null ? "" : message.optString("content", "");
            if (answer.trim().isEmpty()) throw new Exception("Provider returned an empty response.");
            return answer;
        } finally {
            if (connection != null) connection.disconnect();
        }
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

    private String encryptKey(String plaintext) throws Exception {
        SecretKey secretKey = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decryptKey(String stored) throws Exception {
        if (stored == null || stored.isEmpty()) return "";
        byte[] combined = Base64.decode(stored, Base64.NO_WRAP);
        if (combined.length < 13) throw new Exception("Encrypted key data is invalid");
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build();
        generator.init(spec);
        return generator.generateKey();
    }

    private String safeError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return e.getClass().getSimpleName();
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    private void setBusy(boolean value) {
        busy = value;
        sendButton.setEnabled(!value);
        sendButton.setText(value ? "WAIT..." : "SEND  ➤");
        messageInput.setEnabled(!value);
        if (value) connectionStatus.setText("Trying configured cloud providers. Free quotas and availability apply...");
    }

    private void addMessage(String role, String body, String source) {
        ChatMessage message = new ChatMessage(role, body, source);
        history.add(message);
        if (history.size() > 40) history.remove(0);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(role.equals("user") ? Gravity.END : Gravity.START);
        row.setPadding(dp(8), dp(5), dp(8), dp(5));
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        int color = role.equals("user") ? Color.rgb(64, 50, 110) : Color.rgb(28, 32, 48);
        bubble.setBackground(round(color, 15));
        TextView who = text(role.equals("user") ? "YOU" : (source.isEmpty() ? "AI" : source), 9,
            role.equals("user") ? Color.rgb(221, 214, 254) : PURPLE, true);
        bubble.addView(who);
        TextView content = text(body, 13, WHITE, false);
        content.setTextIsSelectable(true);
        content.setMaxWidth(dp(310));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-2, -2);
        contentParams.topMargin = dp(4);
        bubble.addView(content, contentParams);
        row.addView(bubble, new LinearLayout.LayoutParams(-2, -2));
        messagesColumn.addView(row, new LinearLayout.LayoutParams(-1, -2));
        messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
    }

    private EditText input(String hint, boolean password) {
        EditText field = new EditText(this);
        field.setTextSize(13);
        field.setTextColor(WHITE);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setPadding(dp(12), dp(6), dp(12), dp(6));
        field.setBackground(round(Color.rgb(29, 33, 51), 12));
        field.setSingleLine(true);
        if (password) field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return field;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(10);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(WHITE);
        b.setBackground(round(color, 12));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
