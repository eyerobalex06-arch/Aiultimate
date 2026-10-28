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
import android.view.WindowInsets;
import android.view.WindowManager;
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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

public class AiChatActivity extends Activity {
    private static final String PREFS = "ai_omniverse_private";
    private static final String KEY_ALIAS = "ai_omniverse_provider_key_v1";
    private static final String CHAT_HISTORY_PREF = "saved_conversations_json";
    private static final String ACTIVE_CHAT_PREF = "active_chat_id";
    private static final String ONEDRIVE_BACKUP_URI_PREF = "onedrive_backup_uri";
    private static final int REQUEST_CREATE_BACKUP = 7101;
    private static final int REQUEST_RESTORE_BACKUP = 7102;
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
    private Button modelPicker;
    private LinearLayout configPanel;
    private LinearLayout messagesColumn;
    private ScrollView messagesScroll;
    private Button saveButton;
    private Button forgetButton;
    private Button sendButton;
    private int currentProviderIndex = 0;
    private boolean busy = false;
    private final List<ChatMessage> history = new ArrayList<>();
    private final List<SavedChat> savedChats = new ArrayList<>();
    private String activeChatId = "";
    private volatile String lastBackupError = "";
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();

    private static class Provider {
        final String id, label, endpoint, keyUrl, defaultModel;
        final boolean gemini;
        Provider(String id, String label, String endpoint, String keyUrl, String defaultModel, boolean gemini) {
            this.id = id; this.label = label; this.endpoint = endpoint; this.keyUrl = keyUrl;
            this.defaultModel = defaultModel; this.gemini = gemini;
        }
    }

    private static class SavedChat {
        String id;
        String title;
        long updatedAt;
        final List<ChatMessage> messages = new ArrayList<>();
        SavedChat(String id, String title, long updatedAt) {
            this.id = id; this.title = title; this.updatedAt = updatedAt;
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUi();
        loadSavedChats();
        String lastActiveId = getSharedPreferences(PREFS, MODE_PRIVATE).getString(ACTIVE_CHAT_PREF, "");
        SavedChat lastActive = findSavedChat(lastActiveId);
        if (lastActive == null && !savedChats.isEmpty()) lastActive = savedChats.get(0);
        if (lastActive != null) restoreChat(lastActive, false);
        else startNewChat(false);

        if (getIntent().getBooleanExtra("open_key_setup", false)) {
            configPanel.setVisibility(View.VISIBLE);
            providerSpinner.setSelection(1);
            new AlertDialog.Builder(this)
                .setTitle("Connect an AI provider")
                .setMessage("Choose a provider, create a key on its official site with GET KEY, then return and save it here. You can add keys for multiple model providers. Keys stay encrypted on this device.")
                .setPositiveButton("GOT IT", (dialog, which) -> dialog.dismiss())
                .show();
        }
        updateConnectionStatus();
        updateModelPickerLabel();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(5), dp(10), dp(5));
        toolbar.setBackgroundColor(BG);

        Button newChatButton = button("＋", Color.TRANSPARENT);
        newChatButton.setTextSize(22);
        newChatButton.setContentDescription("Start a new chat");
        toolbar.addView(newChatButton, new LinearLayout.LayoutParams(dp(44), dp(46)));
        newChatButton.setOnClickListener(v -> startNewChat(true));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(4), 0, 0, 0);
        titleBox.addView(text("AI OMNIVERSE", 16, WHITE, true));
        titleBox.addView(text("Your AI workspace", 10, MUTED, false));
        toolbar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        Button keyMenu = button("⋮", Color.TRANSPARENT);
        keyMenu.setTextSize(22);
        keyMenu.setContentDescription("Chat options");
        toolbar.addView(keyMenu, new LinearLayout.LayoutParams(dp(42), dp(46)));
        keyMenu.setOnClickListener(v -> showApiKeyMenu(v));
        root.addView(toolbar);

        View hairline = new View(this);
        hairline.setBackgroundColor(Color.rgb(37, 40, 55));
        root.addView(hairline, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout modelBar = new LinearLayout(this);
        modelBar.setGravity(Gravity.CENTER_VERTICAL);
        modelBar.setPadding(dp(14), dp(7), dp(14), dp(7));
        modelPicker = button("Auto-switch · all connected models  ▾", Color.rgb(25, 28, 43));
        modelPicker.setTextSize(12);
        modelPicker.setTextColor(WHITE);
        modelPicker.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        modelPicker.setPadding(dp(14), 0, dp(14), 0);
        modelBar.addView(modelPicker, new LinearLayout.LayoutParams(-1, dp(44)));
        modelPicker.setOnClickListener(v -> showModelPicker());
        root.addView(modelBar);

        configPanel = new LinearLayout(this);
        configPanel.setOrientation(LinearLayout.VERTICAL);
        configPanel.setPadding(dp(13), dp(10), dp(13), dp(10));
        configPanel.setBackground(round(PANEL, 16));
        configPanel.setVisibility(View.GONE);

        TextView configHeading = text("PROVIDER & API KEY SETTINGS", 11, PURPLE, true);
        configPanel.addView(configHeading);

        providerSpinner = new Spinner(this);
        List<String> providerNames = new ArrayList<>();
        providerNames.add("Auto-switch (all saved providers)");
        for (Provider p : providers) providerNames.add(p.label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        providerSpinner.setAdapter(adapter);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(-1, dp(44));
        spinnerParams.topMargin = dp(5);
        configPanel.addView(providerSpinner, spinnerParams);

        providerInfo = text("", 11, MUTED, false);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(5);
        configPanel.addView(providerInfo, infoParams);
        providerInfo.setOnClickListener(v -> showProviderChooserForKey());

        keyNameInput = input("Name this key", false);
        LinearLayout.LayoutParams keyNameParams = new LinearLayout.LayoutParams(-1, dp(42));
        keyNameParams.topMargin = dp(7);
        configPanel.addView(keyNameInput, keyNameParams);

        modelInput = input("Model ID", false);
        LinearLayout.LayoutParams modelParams = new LinearLayout.LayoutParams(-1, dp(42));
        modelParams.topMargin = dp(6);
        configPanel.addView(modelInput, modelParams);

        keyInput = input("Paste this provider's API key", true);
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(-1, dp(42));
        keyParams.topMargin = dp(6);
        configPanel.addView(keyInput, keyParams);

        LinearLayout configButtons = new LinearLayout(this);
        configButtons.setOrientation(LinearLayout.HORIZONTAL);
        configButtons.setGravity(Gravity.CENTER_VERTICAL);
        saveButton = button("SAVE KEY", Color.rgb(91, 70, 164));
        forgetButton = button("DELETE KEY", Color.rgb(44, 47, 70));
        Button getKeyButton = button("GET KEY ↗", Color.rgb(44, 47, 70));
        configButtons.addView(saveButton, new LinearLayout.LayoutParams(0, dp(40), 1));
        LinearLayout.LayoutParams forgetParams = new LinearLayout.LayoutParams(0, dp(40), 1);
        forgetParams.leftMargin = dp(6);
        configButtons.addView(forgetButton, forgetParams);
        LinearLayout.LayoutParams getParams = new LinearLayout.LayoutParams(0, dp(40), 1);
        getParams.leftMargin = dp(6);
        configButtons.addView(getKeyButton, getParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(6);
        configPanel.addView(configButtons, buttonsParams);

        connectionStatus = text("", 10, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(5);
        configPanel.addView(connectionStatus, statusParams);

        providerSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentProviderIndex = position;
                refreshProviderForm();
                updateModelPickerLabel();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        saveButton.setOnClickListener(v -> saveCurrentKey());
        forgetButton.setOnClickListener(v -> forgetCurrentKey());
        getKeyButton.setOnClickListener(v -> showProviderChooserForKey());

        root.addView(configPanel, new LinearLayout.LayoutParams(-1, -2));

        messagesColumn = new LinearLayout(this);
        messagesColumn.setOrientation(LinearLayout.VERTICAL);
        messagesColumn.setPadding(dp(13), dp(10), dp(13), dp(18));
        messagesScroll = new ScrollView(this);
        messagesScroll.setFillViewport(true);
        messagesScroll.setClipToPadding(false);
        messagesScroll.setVerticalScrollBarEnabled(false);
        messagesScroll.setBackgroundColor(BG);
        messagesScroll.addView(messagesColumn, new ScrollView.LayoutParams(-1, -2));
        root.addView(messagesScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(8));
        composer.setBackgroundColor(BG);

        Button plusButton = button("＋", Color.rgb(31, 34, 50));
        plusButton.setTextSize(20);
        plusButton.setContentDescription("Chat tools");
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dp(42), dp(48));
        plusParams.rightMargin = dp(7);
        composer.addView(plusButton, plusParams);
        plusButton.setOnClickListener(v -> {
            PopupMenu tools = new PopupMenu(this, v);
            tools.getMenu().add("New chat").setOnMenuItemClickListener(item -> { startNewChat(true); return true; });
            tools.getMenu().add("Manage API keys").setOnMenuItemClickListener(item -> { showKeySettings(); return true; });
            tools.getMenu().add("Back up chats to OneDrive").setOnMenuItemClickListener(item -> { chooseBackupFile(); return true; });
            tools.show();
        });

        messageInput = input("Message AI OMNIVERSE", false);
        messageInput.setSingleLine(false);
        messageInput.setMinHeight(dp(48));
        messageInput.setMaxLines(5);
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setBackground(round(Color.rgb(27, 30, 44), 23));
        messageInput.setPadding(dp(15), dp(9), dp(15), dp(9));
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, -2, 1));

        sendButton = button("↑", Color.rgb(245, 247, 255));
        sendButton.setTextColor(Color.rgb(20, 22, 32));
        sendButton.setTextSize(20);
        sendButton.setContentDescription("Send message");
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        sendParams.leftMargin = dp(7);
        composer.addView(sendButton, sendParams);
        sendButton.setOnClickListener(v -> sendMessage());
        root.addView(composer);

        TextView footer = text("AI can make mistakes. Check important information.", 9, Color.rgb(115, 122, 145), false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(6), 0, dp(6), dp(5));
        root.addView(footer);

        // Android 15+ edge-to-edge support: the composer stays clear of the gesture area.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int navigationBottom = android.os.Build.VERSION.SDK_INT >= 30
                ? insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                : insets.getSystemWindowInsetBottom();
            composer.setPadding(dp(10), dp(8), dp(10), dp(8) + navigationBottom);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
        renderWelcome();
        updateModelPickerLabel();
    }


    private SavedChat findSavedChat(String id) {
        if (id == null || id.isEmpty()) return null;
        for (SavedChat chat : savedChats) if (id.equals(chat.id)) return chat;
        return null;
    }

    private boolean isProviderConfigured(Provider provider) {
        try {
            String model = getPreferences(MODEL_KEY_PREFIX + provider.id, provider.defaultModel);
            String encrypted = getSavedKeyForModel(provider, model);
            return encrypted != null && !encrypted.isEmpty() && !decryptKey(encrypted).trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private int getConfiguredProviderCount() {
        int count = 0;
        for (Provider provider : providers) {
            if (isProviderConfigured(provider)) count++;
        }
        return count;
    }

    private void updateModelPickerLabel() {
        if (modelPicker == null) return;
        if (currentProviderIndex <= 0) {
            int ready = getConfiguredProviderCount();
            modelPicker.setText("✦  Auto-switch · " + ready + " provider" + (ready == 1 ? "" : "s") + " ready  ▾");
            return;
        }
        Provider p = providers.get(currentProviderIndex - 1);
        String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
        String shortName = p.label.replace(" API", "").replace("Cloud", "").replace(" Inference", "");
        String status = isProviderConfigured(p) ? "KEY SAVED" : "ADD KEY";
        modelPicker.setText("✦  " + shortName + " · " + model + " · " + status + "  ▾");
    }

    private void showModelPicker() {
        String[] choices = new String[providers.size() + 1];
        choices[0] = "✦  Auto-switch · try connected providers";
        for (int i = 0; i < providers.size(); i++) {
            Provider p = providers.get(i);
            String model = getPreferences(MODEL_KEY_PREFIX + p.id, p.defaultModel);
            boolean configured = false;
            try { configured = !decryptKey(getSavedKeyForModel(p, model)).trim().isEmpty(); } catch (Exception ignored) {}
            choices[i + 1] = (configured ? "✓  " : "＋  ") + p.label + "\n" + model + (configured ? " · API KEY SAVED" : " · TAP TO SET UP");
        }
        new AlertDialog.Builder(this)
            .setTitle("Choose a model")
            .setMessage("Choose a preferred model. Auto-switch can try other saved providers if the first one fails. Use provider settings to enter a different model ID.")
            .setItems(choices, (dialog, which) -> {
                providerSpinner.setSelection(which);
                currentProviderIndex = which;
                updateModelPickerLabel();
            })
            .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showKeySettings() {
        configPanel.setVisibility(View.VISIBLE);
        if (currentProviderIndex == 0) providerSpinner.setSelection(1);
        Toast.makeText(this, "Saved keys are encrypted on this device. Choose a provider below.", Toast.LENGTH_LONG).show();
    }

    private void renderWelcome() {
        messagesColumn.removeAllViews();
        LinearLayout welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setGravity(Gravity.CENTER_HORIZONTAL);
        welcome.setPadding(dp(8), dp(24), dp(8), dp(12));
        TextView mark = text("✳", 38, PURPLE, true);
        mark.setGravity(Gravity.CENTER);
        welcome.addView(mark, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView title = text("What can I help with?", 25, WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        welcome.addView(title, titleParams);
        TextView subtitle = text("Ask a question, build something, or explore an idea.", 13, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(7);
        welcome.addView(subtitle, subtitleParams);
        String[][] prompts = {
            {"⌘  Help me write code", "Help me write and explain a small Python program."},
            {"✦  Explain something", "Explain a difficult topic in simple steps, with an example."},
            {"◈  Brainstorm ideas", "Help me brainstorm creative ideas and compare the best ones."},
            {"↗  Plan a project", "Help me plan a project from idea to finished result."}
        };
        for (String[] prompt : prompts) {
            Button chip = button(prompt[0], Color.rgb(24, 28, 43));
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            chip.setPadding(dp(14), 0, dp(12), 0);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(48));
            cp.topMargin = dp(9);
            welcome.addView(chip, cp);
            chip.setOnClickListener(v -> {
                messageInput.setText(prompt[1]);
                messageInput.setSelection(messageInput.length());
                messageInput.requestFocus();
                ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        }
        TextView footer = text("Choose a model above to change your AI.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(15);
        welcome.addView(footer, fp);
        messagesColumn.addView(welcome, new LinearLayout.LayoutParams(-1, -2));
    }

    private void startNewChat(boolean showToast) {
        if (!history.isEmpty()) saveLocalChats();
        activeChatId = UUID.randomUUID().toString();
        history.clear();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(ACTIVE_CHAT_PREF, activeChatId).apply();
        renderWelcome();
        saveLocalChats();
        if (showToast) Toast.makeText(this, "New conversation ready.", Toast.LENGTH_SHORT).show();
    }

    private void openSavedChats() {
        if (savedChats.isEmpty()) {
            Toast.makeText(this, "Conversations are saved on this phone as you chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<SavedChat> sorted = new ArrayList<>(savedChats);
        Collections.sort(sorted, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        String[] labels = new String[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            SavedChat chat = sorted.get(i);
            labels[i] = (chat.title == null || chat.title.isEmpty() ? "New chat" : chat.title)
                + " · " + chat.messages.size() + " messages";
        }
        new AlertDialog.Builder(this).setTitle("Your conversations")
            .setItems(labels, (dialog, which) -> restoreChat(sorted.get(which), true))
            .setNeutralButton("NEW CHAT", (dialog, which) -> startNewChat(true))
            .setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss()).show();
    }

    private void restoreChat(SavedChat chat, boolean showToast) {
        if (chat == null) return;
        if (!history.isEmpty()) saveLocalChats();
        activeChatId = chat.id;
        history.clear();
        history.addAll(chat.messages);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(ACTIVE_CHAT_PREF, activeChatId).apply();
        messagesColumn.removeAllViews();
        if (history.isEmpty()) renderWelcome();
        else {
            for (ChatMessage message : history) renderMessage(message);
            messagesScroll.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
        }
        if (showToast) Toast.makeText(this, "Conversation reopened.", Toast.LENGTH_SHORT).show();
    }

    private String titleForMessages(List<ChatMessage> messages) {
        for (ChatMessage message : messages) if ("user".equals(message.role)) {
            String title = message.text.trim().replace('\n', ' ');
            if (title.length() > 38) title = title.substring(0, 35) + "...";
            return title.isEmpty() ? "New chat" : title;
        }
        return "New chat";
    }

    private JSONObject chatToJson(SavedChat chat) throws Exception {
        JSONObject object = new JSONObject();
        object.put("id", chat.id);
        object.put("title", chat.title);
        object.put("updatedAt", chat.updatedAt);
        JSONArray array = new JSONArray();
        int start = Math.max(0, chat.messages.size() - 40);
        for (int i = start; i < chat.messages.size(); i++) {
            ChatMessage m = chat.messages.get(i);
            array.put(new JSONObject().put("role", m.role).put("text", m.text).put("source", m.source));
        }
        object.put("messages", array);
        return object;
    }

    private String serializeChats() throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "AI OMNIVERSE conversation backup");
        root.put("version", 1);
        root.put("savedAt", System.currentTimeMillis());
        JSONArray array = new JSONArray();
        for (SavedChat chat : savedChats) array.put(chatToJson(chat));
        root.put("chats", array);
        return root.toString(2);
    }

    private List<SavedChat> parseChats(String json) throws Exception {
        List<SavedChat> result = new ArrayList<>();
        JSONArray array = new JSONObject(json).optJSONArray("chats");
        if (array == null) throw new Exception("This file is not an AI OMNIVERSE conversation backup.");
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            SavedChat chat = new SavedChat(o.optString("id", UUID.randomUUID().toString()),
                o.optString("title", "New chat"), o.optLong("updatedAt", System.currentTimeMillis()));
            JSONArray msgs = o.optJSONArray("messages");
            if (msgs != null) {
                int start = Math.max(0, msgs.length() - 40);
                for (int j = start; j < msgs.length(); j++) {
                    JSONObject m = msgs.optJSONObject(j);
                    if (m == null) continue;
                    String role = m.optString("role", "");
                    if (role.equals("user") || role.equals("assistant"))
                        chat.messages.add(new ChatMessage(role, m.optString("text", ""), m.optString("source", "")));
                }
            }
            result.add(chat);
        }
        return result;
    }

    private void loadSavedChats() {
        savedChats.clear();
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(CHAT_HISTORY_PREF, "");
        if (json.isEmpty()) return;
        try {
            savedChats.addAll(parseChats(json));
            Collections.sort(savedChats, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't read saved conversations. API keys are unchanged.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveLocalChats() {
        if (activeChatId.isEmpty()) activeChatId = UUID.randomUUID().toString();
        SavedChat active = findSavedChat(activeChatId);
        if (active == null) {
            active = new SavedChat(activeChatId, "New chat", System.currentTimeMillis());
            savedChats.add(active);
        }
        active.messages.clear();
        active.messages.addAll(history);
        active.title = titleForMessages(history);
        active.updatedAt = System.currentTimeMillis();
        Collections.sort(savedChats, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        while (savedChats.size() > 30 && !savedChats.get(savedChats.size() - 1).id.equals(activeChatId))
            savedChats.remove(savedChats.size() - 1);
        try {
            String json = serializeChats();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(CHAT_HISTORY_PREF, json).putString(ACTIVE_CHAT_PREF, activeChatId).apply();
            queueOneDriveBackup(json);
        } catch (Exception e) {
            android.util.Log.e("AIOMNIVERSE", "Unable to save conversation history", e);
        }
    }

    private void chooseBackupFile() {
        new AlertDialog.Builder(this)
            .setTitle("Connect OneDrive backup")
            .setMessage("Choose a OneDrive location in the Android file picker and save AI-OMNIVERSE-chats.json. Once connected, AI OMNIVERSE will update that file after messages and when you leave the chat. Conversations are also saved on this phone immediately.")
            .setPositiveButton("CHOOSE FILE", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "AI-OMNIVERSE-chats.json");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(intent, REQUEST_CREATE_BACKUP); }
                catch (Exception e) { Toast.makeText(this, "The file picker couldn't open.", Toast.LENGTH_LONG).show(); }
            })
            .setNegativeButton("NOT NOW", (dialog, which) -> dialog.dismiss()).show();
    }

    private void restoreBackupFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, REQUEST_RESTORE_BACKUP); }
        catch (Exception e) { Toast.makeText(this, "The file picker couldn't open.", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_CREATE_BACKUP) {
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (Exception ignored) {}
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(ONEDRIVE_BACKUP_URI_PREF, uri.toString()).apply();
            saveLocalChats();
            Toast.makeText(this, "Backup file connected. New messages will automatically update it.", Toast.LENGTH_LONG).show();
        } else if (requestCode == REQUEST_RESTORE_BACKUP) {
            try {
                String json = readAll(getContentResolver().openInputStream(uri));
                List<SavedChat> imported = parseChats(json);
                if (imported.isEmpty()) throw new Exception("No conversations were found in that backup file.");
                for (SavedChat chat : imported) {
                    SavedChat old = findSavedChat(chat.id);
                    if (old != null) savedChats.remove(old);
                    savedChats.add(chat);
                }
                Collections.sort(savedChats, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
                restoreChat(savedChats.get(0), false);
                saveLocalChats();
                Toast.makeText(this, "Restored " + imported.size() + " conversation(s).", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't restore backup: " + safeError(e), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void queueOneDriveBackup(String json) {
        String target = getSharedPreferences(PREFS, MODE_PRIVATE).getString(ONEDRIVE_BACKUP_URI_PREF, "");
        if (target.isEmpty()) return;
        final String snapshot = json;
        final Uri uri = Uri.parse(target);
        try {
            backupExecutor.execute(() -> {
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new Exception("No output stream for the backup file.");
                    output.write(snapshot.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    lastBackupError = "";
                } catch (Exception e) {
                    String error = safeError(e);
                    if (!error.equals(lastBackupError)) {
                        lastBackupError = error;
                        runOnUiThread(() -> Toast.makeText(this,
                            "Cloud backup didn't finish. Your chat is still saved on this phone. Check your backup location.",
                            Toast.LENGTH_LONG).show());
                    }
                    android.util.Log.w("AIOMNIVERSE", "Cloud backup failed", e);
                }
            });
        } catch (Exception e) {
            android.util.Log.w("AIOMNIVERSE", "Cloud backup task couldn't be queued", e);
        }
    }

    @Override protected void onStop() {
        saveLocalChats();
        super.onStop();
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
            keyInput.setHint("API key saved securely");
            providerInfo.setText("✓  " + p.label + "  •  " + model + "  •  Saved as: " + keyName + "  •  Encrypted on this device.");
            updateConnectionStatus();
            updateModelPickerLabel();

            // Saving a key should finish setup and return the user to the clean chat screen.
            configPanel.animate().alpha(0f).setDuration(140).withEndAction(() -> {
                configPanel.setVisibility(View.GONE);
                configPanel.setAlpha(1f);
            }).start();
            View focused = getCurrentFocus();
            if (focused != null) {
                android.view.inputmethod.InputMethodManager keyboard =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                focused.clearFocus();
            }
            Toast.makeText(this, "API key saved securely. Provider settings closed.", Toast.LENGTH_SHORT).show();
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
        updateModelPickerLabel();
    }


    private void showApiKeyMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("New chat").setOnMenuItemClickListener(item -> { startNewChat(true); return true; });
        menu.getMenu().add("Your conversations").setOnMenuItemClickListener(item -> { openSavedChats(); return true; });
        menu.getMenu().add("Manage API keys / providers").setOnMenuItemClickListener(item -> { showKeySettings(); return true; });
        menu.getMenu().add(configPanel.getVisibility() == View.VISIBLE ? "Hide provider settings" : "Show provider settings")
            .setOnMenuItemClickListener(item -> {
                configPanel.setVisibility(configPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                return true;
            });
        menu.getMenu().add("Set up / change OneDrive backup").setOnMenuItemClickListener(item -> { chooseBackupFile(); return true; });
        menu.getMenu().add("Restore conversations from backup").setOnMenuItemClickListener(item -> { restoreBackupFromFile(); return true; });

        android.view.SubMenu keyLinks = menu.getMenu().addSubMenu("Get API key from provider");
        for (Provider provider : providers) {
            keyLinks.add(provider.label).setOnMenuItemClickListener(item -> {
                configPanel.setVisibility(View.VISIBLE);
                providerSpinner.setSelection(providers.indexOf(provider) + 1);
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(provider.keyUrl))); }
                catch (Exception e) { Toast.makeText(this, "Couldn't open the provider website.", Toast.LENGTH_SHORT).show(); }
                return true;
            });
        }
        menu.getMenu().add("Copy latest AI reply").setOnMenuItemClickListener(item -> {
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessage message = history.get(i);
                if (message.role.equals("assistant") && !message.source.equals("AI OMNIVERSE") && !message.source.equals("ROUTER")) {
                    android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI reply", message.text));
                    Toast.makeText(this, "Latest AI reply copied.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            Toast.makeText(this, "There isn't an AI reply to copy yet.", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.getMenu().add("Delete this model's API key").setOnMenuItemClickListener(item -> {
            if (currentProviderIndex == 0) Toast.makeText(this, "Choose a specific model first.", Toast.LENGTH_SHORT).show();
            else forgetCurrentKey();
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

    private void showProviderChooserForKey() {
        String[] names = new String[providers.size()];
        for (int i = 0; i < providers.size(); i++) names[i] = providers.get(i).label;
        new AlertDialog.Builder(this)
            .setTitle("Choose an API key provider")
            .setMessage("All six providers are supported, including NVIDIA NIM. Choose where you want to create a key.")
            .setItems(names, (dialog, which) -> {
                Provider selected = providers.get(which);
                configPanel.setVisibility(View.VISIBLE);
                providerSpinner.setSelection(which + 1);
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(selected.keyUrl))); }
                catch (Exception e) { Toast.makeText(this, "Couldn't open " + selected.label + " website.", Toast.LENGTH_SHORT).show(); }
            })
            .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss()).show();
    }

    private void openProviderSetup() {
        showProviderChooserForKey();
    }

    private void updateConnectionStatus() {
        int connected = getConfiguredProviderCount();
        if (connectionStatus != null) {
            connectionStatus.setText("✓  " + connected + " of " + providers.size() +
                " providers have a saved key. Free-tier quotas and provider availability still apply.");
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
                errors = "No API keys are configured yet. Choose OpenRouter, Gemini, GroqCloud, Cerebras, Mistral, or NVIDIA NIM in PROVIDER ROUTER, then use GET FREE KEY to open its official key page.";
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
        sendButton.setText(value ? "…" : "↑");
        messageInput.setEnabled(!value);
        if (value) connectionStatus.setText("Trying configured cloud providers. Free quotas and availability apply...");
    }

    private void addMessage(String role, String body, String source) {
        if (history.isEmpty()) messagesColumn.removeAllViews();
        ChatMessage message = new ChatMessage(role, body, source);
        history.add(message);
        if (history.size() > 40) history.remove(0);
        renderMessage(message);
        saveLocalChats();
    }

    private void renderMessage(ChatMessage message) {
        String role = message.role;
        String body = message.text;
        String source = message.source;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(role.equals("user") ? Gravity.END : Gravity.START);
        row.setPadding(0, dp(5), 0, dp(8));

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(role.equals("user") ? dp(13) : dp(2), dp(9), role.equals("user") ? dp(13) : dp(2), dp(9));
        if (role.equals("user")) bubble.setBackground(round(Color.rgb(49, 44, 76), 20));
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int bubbleMaxWidth = Math.max(dp(220), screenWidth - dp(role.equals("user") ? 100 : 52));

        TextView who = text(role.equals("user") ? "YOU" : "✦  " + (source.isEmpty() ? "AI" : source), 9,
            role.equals("user") ? Color.rgb(221, 214, 254) : PURPLE, true);
        bubble.addView(who);
        TextView content = text(body, 14, WHITE, false);
        content.setTextIsSelectable(true);
        content.setMaxWidth(bubbleMaxWidth);
        content.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-2, -2);
        contentParams.topMargin = dp(5);
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
