package com.aiomniverse.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Shared group chat for role-based AI agents. Missions are queued with WorkManager so
 * they can continue after the activity is closed, subject to Android's background policy.
 */
public class AgentWorkspaceActivity extends Activity {
    private static final int BG = Color.rgb(9, 11, 20);
    private static final int PANEL = Color.rgb(19, 22, 37);
    private static final int WHITE = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(160, 168, 190);
    private static final int PURPLE = Color.rgb(167, 139, 250);
    private static final String PREFS = "ai_omniverse_agents";
    private static final String PROVIDER_PREFS = "ai_omniverse_private";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout agentsColumn;
    private LinearLayout feedColumn;
    private ScrollView feedScroll;
    private TextView missionStatus;
    private EditText taskInput;
    private String lastRenderedMission = "";
    private boolean refreshLoopStarted = false;

    private final Runnable refreshLoop = new Runnable() {
        @Override public void run() {
            refreshMissionFeed();
            if (refreshLoopStarted) handler.postDelayed(this, 1500);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ensureDefaultAgents();
        buildUi();
        refreshMissionFeed();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshLoopStarted = true;
        handler.removeCallbacks(refreshLoop);
        handler.post(refreshLoop);
    }

    @Override protected void onPause() {
        refreshLoopStarted = false;
        handler.removeCallbacks(refreshLoop);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(7), dp(16), dp(7));
        toolbar.setBackgroundColor(PANEL);
        Button back = button("‹ BACK", Color.rgb(43, 46, 66));
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(82), dp(42)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(10), 0, 0, 0);
        titleBox.addView(text("AI AGENT GROUP", 15, WHITE, true));
        titleBox.addView(text("SHARED CHAT · BACKGROUND MISSIONS", 9, PURPLE, true));
        toolbar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(toolbar);

        ScrollView pageScroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(14), dp(14), dp(24));

        LinearLayout hero = card();
        hero.setBackground(gradient(new int[]{Color.rgb(43, 38, 82), Color.rgb(22, 42, 61)}, 20));
        hero.addView(text("GIVE THE TEAM A TASK.", 10, Color.rgb(196, 181, 253), true));
        TextView heroTitle = text("Multiple AIs. One shared mission.", 22, WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(7);
        hero.addView(heroTitle, titleParams);
        TextView heroCopy = text("Agents reply in sequence and can build on each other's messages. Queue a mission, leave the app, and check the group transcript later. Android may defer background work, and each AI provider's free quota still applies.", 12, Color.rgb(215, 222, 240), false);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-1, -2);
        copyParams.topMargin = dp(8);
        hero.addView(heroCopy, copyParams);
        page.addView(hero);

        LinearLayout agentsHeader = new LinearLayout(this);
        agentsHeader.setGravity(Gravity.CENTER_VERTICAL);
        agentsHeader.setPadding(0, dp(18), 0, dp(9));
        agentsHeader.addView(text("YOUR AGENTS", 12, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button addAgent = button("+ ADD BOT", Color.rgb(58, 46, 99));
        addAgent.setTextColor(Color.rgb(221, 214, 254));
        agentsHeader.addView(addAgent, new LinearLayout.LayoutParams(dp(104), dp(38)));
        addAgent.setOnClickListener(v -> showAddAgentDialog());
        page.addView(agentsHeader);

        agentsColumn = new LinearLayout(this);
        agentsColumn.setOrientation(LinearLayout.VERTICAL);
        page.addView(agentsColumn);
        renderAgents();

        page.addView(text("MISSION PROMPT", 12, WHITE, true), topMarginParams(18, 8));
        taskInput = new EditText(this);
        taskInput.setTextColor(WHITE);
        taskInput.setHintTextColor(MUTED);
        taskInput.setHint("Describe the task for the whole team...");
        taskInput.setTextSize(14);
        taskInput.setGravity(Gravity.TOP | Gravity.START);
        taskInput.setMinLines(4);
        taskInput.setMaxLines(8);
        taskInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        taskInput.setPadding(dp(13), dp(12), dp(13), dp(12));
        taskInput.setBackground(round(Color.rgb(25, 29, 46), 14));
        page.addView(taskInput, new LinearLayout.LayoutParams(-1, dp(120)));

        Button run = button("QUEUE MISSION · RUN IN BACKGROUND  ↗", Color.rgb(108, 78, 181));
        LinearLayout.LayoutParams runParams = new LinearLayout.LayoutParams(-1, dp(49));
        runParams.topMargin = dp(10);
        page.addView(run, runParams);
        run.setOnClickListener(v -> queueMission());

        missionStatus = text("No mission queued yet.", 11, MUTED, false);
        page.addView(missionStatus, topMarginParams(9, 10));

        TextView groupLabel = text("SHARED GROUP CHAT", 12, WHITE, true);
        page.addView(groupLabel, topMarginParams(18, 8));
        feedScroll = new ScrollView(this);
        feedScroll.setBackground(round(Color.rgb(12, 15, 26), 16));
        feedColumn = new LinearLayout(this);
        feedColumn.setOrientation(LinearLayout.VERTICAL);
        feedColumn.setPadding(dp(9), dp(9), dp(9), dp(12));
        feedScroll.addView(feedColumn, new ScrollView.LayoutParams(-1, -2));
        page.addView(feedScroll, new LinearLayout.LayoutParams(-1, dp(400)));

        TextView footnote = text("Privacy note: prompts are sent to whichever configured provider answers each agent. Do not submit passwords or sensitive personal information. Agent missions do not have unrestricted web access or permission to control your phone; they generate responses only.", 10, MUTED, false);
        page.addView(footnote, topMarginParams(12, 4));

        pageScroll.addView(page);
        root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void queueMission() {
        String prompt = taskInput.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Enter a task for the agents first.", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray agents = getAgents();
        if (agents.length() == 0) {
            Toast.makeText(this, "Add at least one AI agent.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasAnySavedKey()) {
            new AlertDialog.Builder(this)
                .setTitle("Connect a free AI provider")
                .setMessage("The group needs at least one provider key saved in Multi-AI Chat. OpenRouter has a free-model router, and Gemini, Mistral Free Mode, and NVIDIA provide free access under their current conditions. Their quotas and model availability still apply.")
                .setNegativeButton("CANCEL", (d, w) -> d.dismiss())
                .setPositiveButton("OPEN MULTI-AI CHAT", (d, w) -> startActivity(new Intent(this, AiChatActivity.class)))
                .show();
            return;
        }

        String id = UUID.randomUUID().toString();
        try {
            JSONObject mission = new JSONObject();
            mission.put("id", id);
            mission.put("prompt", prompt);
            mission.put("status", "QUEUED");
            mission.put("current", "");
            mission.put("agents", agents);
            mission.put("logs", new JSONArray());
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("mission_" + id, mission.toString())
                .putString("last_mission_id", id)
                .commit();

            Data input = new Data.Builder()
                .putString("mission_id", id)
                .putString("prompt", prompt)
                .putString("agents_json", agents.toString())
                .build();
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(AgentMissionWorker.class)
                .setInputData(input)
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
            WorkManager.getInstance(getApplicationContext())
                .beginUniqueWork("ai_omniverse_agent_queue", ExistingWorkPolicy.APPEND, work)
                .enqueue();

            taskInput.setText("");
            lastRenderedMission = "";
            refreshMissionFeed();
            Toast.makeText(this, "Mission queued. It can continue while this screen is closed.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not queue mission: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasAnySavedKey() {
        android.content.SharedPreferences prefs = getSharedPreferences(PROVIDER_PREFS, MODE_PRIVATE);
        String[] ids = {"openrouter", "gemini", "groq", "cerebras", "mistral", "nvidia"};
        for (String id : ids) if (!prefs.getString("encrypted_key_" + id, "").isEmpty()) return true;
        return false;
    }

    private void ensureDefaultAgents() {
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getString("agent_configs", "").isEmpty()) return;
        try {
            JSONArray defaults = new JSONArray();
            defaults.put(new JSONObject().put("name", "Scout").put("role", "Research planner: break down the task, identify assumptions, and list what must be verified."));
            defaults.put(new JSONObject().put("name", "Builder").put("role", "Implementation specialist: propose concrete steps, code or structured deliverables when appropriate."));
            defaults.put(new JSONObject().put("name", "Skeptic").put("role", "Critical reviewer: find mistakes, missing details, risks, and test the other agents' claims."));
            defaults.put(new JSONObject().put("name", "Coordinator").put("role", "Team lead: combine earlier messages into the most useful next step, resolve conflicts, and provide a final synthesis."));
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("agent_configs", defaults.toString()).commit();
        } catch (Exception ignored) {}
    }

    private JSONArray getAgents() {
        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString("agent_configs", "");
            return json.isEmpty() ? new JSONArray() : new JSONArray(json);
        } catch (Exception e) { return new JSONArray(); }
    }

    private void saveAgents(JSONArray agents) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("agent_configs", agents.toString()).commit();
        renderAgents();
    }

    private void showAddAgentDialog() {
        JSONArray existing = getAgents();
        if (existing.length() >= 8) {
            Toast.makeText(this, "Keep the team to 8 bots or fewer to reduce free-tier usage.", Toast.LENGTH_LONG).show();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(10), dp(18), 0);
        EditText name = makeInput("Bot name (e.g. Code Reviewer)", false);
        EditText role = makeInput("Role and instructions (e.g. test the plan and find bugs)", false);
        role.setSingleLine(false);
        role.setMinLines(3);
        role.setGravity(Gravity.TOP | Gravity.START);
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(role, new LinearLayout.LayoutParams(-1, dp(100)));
        new AlertDialog.Builder(this)
            .setTitle("Add an AI bot")
            .setView(form)
            .setNegativeButton("CANCEL", (d, w) -> d.dismiss())
            .setPositiveButton("ADD BOT", (d, w) -> {
                String botName = name.getText().toString().trim();
                String botRole = role.getText().toString().trim();
                if (botName.isEmpty() || botRole.isEmpty()) {
                    Toast.makeText(this, "Both name and role are required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    JSONArray updated = getAgents();
                    updated.put(new JSONObject().put("name", botName).put("role", botRole));
                    saveAgents(updated);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not add bot.", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    private void removeAgent(int index) {
        JSONArray current = getAgents();
        if (current.length() <= 1) {
            Toast.makeText(this, "Keep at least one agent in the group.", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray updated = new JSONArray();
        for (int i = 0; i < current.length(); i++) {
            if (i != index) updated.put(current.optJSONObject(i));
        }
        saveAgents(updated);
    }

    private void renderAgents() {
        if (agentsColumn == null) return;
        agentsColumn.removeAllViews();
        JSONArray agents = getAgents();
        for (int i = 0; i < agents.length(); i++) {
            JSONObject a = agents.optJSONObject(i);
            if (a == null) continue;
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(a.optString("name", "Agent"), 13, WHITE, true));
            TextView role = text(a.optString("role", ""), 10, MUTED, false);
            role.setMaxLines(3);
            info.addView(role);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = button("REMOVE", Color.rgb(47, 38, 59));
            remove.setTextColor(Color.rgb(220, 190, 220));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(76), dp(36));
            rp.leftMargin = dp(8);
            row.addView(remove, rp);
            final int index = i;
            remove.setOnClickListener(v -> removeAgent(index));
            LinearLayout.LayoutParams rowp = new LinearLayout.LayoutParams(-1, -2);
            rowp.bottomMargin = dp(7);
            agentsColumn.addView(row, rowp);
        }
    }

    private void refreshMissionFeed() {
        if (feedColumn == null || missionStatus == null) return;
        String id = getSharedPreferences(PREFS, MODE_PRIVATE).getString("last_mission_id", "");
        if (id.isEmpty()) {
            missionStatus.setText("No mission queued yet.");
            if (lastRenderedMission.isEmpty()) {
                feedColumn.removeAllViews();
                feedColumn.addView(text("Your agents' replies will appear here. Queue a task to start the shared conversation.", 12, MUTED, false));
            }
            return;
        }
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("mission_" + id, "");
        if (raw.isEmpty() || raw.equals(lastRenderedMission)) return;
        lastRenderedMission = raw;
        try {
            JSONObject mission = new JSONObject(raw);
            String status = mission.optString("status", "QUEUED");
            String current = mission.optString("current", "");
            String statusLine = status.replace('_', ' ');
            if (!current.isEmpty()) statusLine += " · Agent: " + current;
            if (status.equals("COMPLETED")) statusLine += " · transcript saved on this device";
            if (status.equals("NEEDS_SETUP")) statusLine += " · connect a provider key in Multi-AI Chat";
            missionStatus.setText(statusLine);

            feedColumn.removeAllViews();
            LinearLayout taskCard = card();
            taskCard.setBackground(round(Color.rgb(42, 35, 73), 14));
            taskCard.addView(text("YOU · MISSION", 9, Color.rgb(221, 214, 254), true));
            TextView task = text(mission.optString("prompt", ""), 13, WHITE, false);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
            tp.topMargin = dp(5);
            taskCard.addView(task, tp);
            feedColumn.addView(taskCard);

            JSONArray logs = mission.optJSONArray("logs");
            if (logs != null) for (int i = 0; i < logs.length(); i++) {
                JSONObject log = logs.optJSONObject(i);
                if (log == null) continue;
                LinearLayout entry = card();
                entry.setBackground(round(log.optBoolean("error", false) ? Color.rgb(48, 34, 43) : Color.rgb(25, 31, 48), 14));
                entry.addView(text(log.optString("agent", "AI AGENT"), 11, PURPLE, true));
                TextView role = text(log.optString("role", ""), 9, MUTED, false);
                LinearLayout.LayoutParams rolep = new LinearLayout.LayoutParams(-1, -2);
                rolep.topMargin = dp(3);
                entry.addView(role, rolep);
                TextView reply = text(log.optString("text", ""), 12, WHITE, false);
                reply.setTextIsSelectable(true);
                LinearLayout.LayoutParams replyp = new LinearLayout.LayoutParams(-1, -2);
                replyp.topMargin = dp(7);
                entry.addView(reply, replyp);
                String provider = log.optString("provider", "");
                if (!provider.isEmpty()) {
                    TextView source = text(provider, 9, MUTED, false);
                    LinearLayout.LayoutParams sourcep = new LinearLayout.LayoutParams(-1, -2);
                    sourcep.topMargin = dp(6);
                    entry.addView(source, sourcep);
                }
                LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
                ep.topMargin = dp(7);
                feedColumn.addView(entry, ep);
            }
            if (status.equals("QUEUED") || status.equals("RUNNING")) {
                feedColumn.addView(text("Mission is queued/running. Replies will be saved as agents finish, even when this screen is closed. Android may delay background work.", 10, MUTED, false), topMarginParams(8, 4));
            } else if (status.equals("NEEDS_SETUP")) {
                feedColumn.addView(text(mission.optString("error", "Connect a provider first."), 11, MUTED, false), topMarginParams(8, 4));
            }
            feedScroll.post(() -> feedScroll.fullScroll(View.FOCUS_DOWN));
        } catch (Exception e) {
            missionStatus.setText("Could not read the saved mission transcript.");
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(11), dp(12), dp(11));
        box.setBackground(round(PANEL, 15));
        return box;
    }

    private EditText makeInput(String hint, boolean password) {
        EditText field = new EditText(this);
        field.setTextSize(13);
        field.setTextColor(WHITE);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setPadding(dp(11), dp(6), dp(11), dp(6));
        field.setBackground(round(Color.rgb(29, 33, 51), 12));
        field.setInputType(password ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
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

    private LinearLayout.LayoutParams topMarginParams(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        return p;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable gradient(int[] colors, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
