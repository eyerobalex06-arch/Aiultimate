package com.aiomniverse.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(33, 33, 33);
    private final int panel = Color.rgb(47, 47, 47);
    private final int white = Color.rgb(236, 236, 236);
    private final int muted = Color.rgb(180, 180, 180);
    private final int purple = Color.rgb(16, 163, 127);
    private LinearLayout list;
    private EditText search;
    private String activeFilter = "All";
    private final List<Model> models = Arrays.asList(
        new Model("Qwen", "Alibaba", "Strong coding, reasoning, multilingual tasks", "CODING", "REASONING", "MULTILINGUAL", "A flexible family with several model sizes. Pick a size that matches your device or hosted service.", "Text / code"),
        new Model("DeepSeek", "DeepSeek API", "Reasoning, mathematics, programming, and structured problem solving", "REASONING", "CODING", "MATH", "DeepSeek can be connected directly with its own API key. Tap ADD API to open the official DeepSeek key page and prepare the in-app provider settings.", "Text / code"),
        new Model("Web Search", "OpenRouter web-search tool", "Give supported chat models access to up-to-date web results and citations", "WEB", "SEARCH", "RESEARCH", "Web search is an API feature, not a standalone model. It uses OpenRouter's web-search tool. Search tooling may have separate usage charges, so check current provider pricing before enabling it.", "Web"),
        new Model("Llama", "Meta", "General chat, writing, tool use, customization", "GENERAL", "WRITING", "OPEN WEIGHTS", "A widely used model family with many sizes and community fine-tunes. Licensing differs by release.", "Text / code"),
        new Model("Gemma", "Google", "Efficient local use, summaries, everyday assistance", "FAST", "LOCAL", "GENERAL", "Smaller variants can be practical on consumer hardware. Hardware needs depend on parameter count and quantization.", "Text"),
        new Model("Mistral", "Mistral AI", "Fast assistants, coding, multilingual chat", "FAST", "CODING", "MULTILINGUAL", "A family of efficient and capable models, including compact options and larger hosted models.", "Text / code"),
        new Model("Phi", "Microsoft", "Compact reasoning, structured tasks, on-device experiments", "SMALL", "REASONING", "LOCAL", "Small models can be easier to run locally, though they generally trade some capability for size.", "Text"),
        new Model("Command R", "Cohere", "Retrieval-augmented generation and grounded answers", "RESEARCH", "RAG", "BUSINESS", "Designed for enterprise-style retrieval and working with supplied documents when used in a properly configured system.", "Text"),
        new Model("Whisper", "OpenAI", "Speech-to-text and transcription", "AUDIO", "TRANSCRIPTION", "MULTILINGUAL", "A speech recognition model family, not a general chat assistant. Language and audio quality affect results.", "Audio"),
        new Model("FLUX", "Black Forest Labs", "Text-to-image generation and visual concepts", "IMAGES", "ART", "DESIGN", "An image-generation model family. Usually needs a compatible hosted service or suitable graphics hardware.", "Images"),
        new Model("Stable Diffusion", "Stability AI / community", "Image generation, styles, image workflows", "IMAGES", "ART", "CUSTOMIZATION", "A large ecosystem of image models and tools. Check each checkpoint's license and hardware requirements.", "Images"),
        new Model("Nemotron 3 Ultra", "NVIDIA via OpenRouter", "High-end agentic reasoning, coding, planning, and long-context research", "REASONING", "CODING", "RESEARCH", "A current free variant appears in OpenRouter's free-model collection. Free model availability and quotas change; this is not unlimited cloud access.", "Text / code"),
        new Model("Nemotron 3.5 Super VL", "NVIDIA via OpenRouter", "Multimodal reasoning for visual analysis, documents, and agent workflows", "REASONING", "RESEARCH", "IMAGES", "Listed among OpenRouter's popular free models at the time this catalog was updated. Access can vary with provider capacity and model availability.", "Text / images"),
        new Model("Qwen 3.8 27B", "Qwen / OpenRouter", "Coding, general reasoning, multilingual tasks, and tool use", "CODING", "REASONING", "MULTILINGUAL", "Try a free hosted variant through OpenRouter's free collection. The same model family may be paid on other providers.", "Text / code"),
        new Model("GPT-OSS 120B", "OpenAI open-weight model", "Large open-weight reasoning model for coding and agent-style tasks", "REASONING", "CODING", "GENERAL", "Available through selected hosted providers. A model being open-weight does not make every hosted API endpoint free; check provider pricing and quotas.", "Text / code"),
        new Model("Gemini 3.6 Flash", "Google", "Fast general-purpose assistance, coding, and everyday agent tasks", "FAST", "GENERAL", "REASONING", "Google documents a free API tier for selected models, with usage limits and data-handling terms. Confirm access in AI Studio.", "Text / code"),
        new Model("Ministral 8B", "Mistral AI", "Compact chat and coding assistant for faster responses", "FAST", "CODING", "MULTILINGUAL", "Mistral documents a free mode with no card required, but model access and rate limits depend on your workspace and may change.", "Text / code")
    );

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(20), dp(14), dp(20), dp(12));
        TextView logo = text("✦", 26, purple, true);
        top.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(40)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("AI OMNIVERSE", 17, white, true));
        brand.addView(text("FREE AI HUB  •  v2.0", 10, muted, true));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("BETA", 10, purple, true);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(round(Color.rgb(37, 60, 52), 18));
        top.addView(badge);
        root.addView(top);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(10), dp(18), dp(28));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(22));
        hero.setBackground(gradient(new int[]{Color.rgb(34, 70, 61), Color.rgb(27, 38, 39)}, 24));
        TextView eyebrow = text("YOUR AI TOOLKIT", 11, Color.rgb(196, 181, 253), true);
        hero.addView(eyebrow);
        TextView headline = text("What would you like\nto explore?", 29, white, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        hero.addView(headline, hp);
        TextView subtitle = text("Explore model families, then chat with configured free cloud AI providers. Automatic failover can reduce interruptions, but provider quotas still apply.", 13, Color.rgb(207, 211, 231), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(9);
        hero.addView(subtitle, sp);
        page.addView(hero);

        Button liveChat = new Button(this);
        liveChat.setText("START A NEW CHAT  ↗");
        liveChat.setTextSize(13);
        liveChat.setTextColor(white);
        liveChat.setAllCaps(false);
        liveChat.setTypeface(Typeface.DEFAULT_BOLD);
        liveChat.setBackground(gradient(new int[]{Color.rgb(16, 163, 127), Color.rgb(12, 125, 98)}, 16));
        LinearLayout.LayoutParams liveChatParams = new LinearLayout.LayoutParams(-1, dp(50));
        liveChatParams.topMargin = dp(8);
        liveChat.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        page.addView(liveChat, liveChatParams);

        Button addApiKey = new Button(this);
        addApiKey.setText("ADD API KEY  +");
        addApiKey.setTextSize(13);
        addApiKey.setTextColor(white);
        addApiKey.setAllCaps(false);
        addApiKey.setTypeface(Typeface.DEFAULT_BOLD);
        addApiKey.setBackground(gradient(new int[]{Color.rgb(58, 70, 66), Color.rgb(43, 54, 51)}, 16));
        LinearLayout.LayoutParams addApiKeyParams = new LinearLayout.LayoutParams(-1, dp(48));
        addApiKeyParams.topMargin = dp(7);
        addApiKey.setOnClickListener(v -> {
            Intent setup = new Intent(this, AiChatActivity.class);
            setup.putExtra("open_key_setup", true);
            startActivity(setup);
        });
        page.addView(addApiKey, addApiKeyParams);

        Button getApiKey = new Button(this);
        getApiKey.setText("GET API KEY  ↗");
        getApiKey.setTextSize(13);
        getApiKey.setTextColor(white);
        getApiKey.setAllCaps(false);
        getApiKey.setTypeface(Typeface.DEFAULT_BOLD);
        getApiKey.setBackground(gradient(new int[]{Color.rgb(58, 58, 58), Color.rgb(48, 48, 48)}, 16));
        LinearLayout.LayoutParams getApiKeyParams = new LinearLayout.LayoutParams(-1, dp(48));
        getApiKeyParams.topMargin = dp(12);
        getApiKey.setOnClickListener(v -> showGetApiKeyDialog());
        page.addView(getApiKey, getApiKeyParams);

        Button agentHub = new Button(this);
        agentHub.setText("OPEN AI AGENT GROUP  ✦");
        agentHub.setTextSize(13);
        agentHub.setTextColor(white);
        agentHub.setAllCaps(false);
        agentHub.setTypeface(Typeface.DEFAULT_BOLD);
        agentHub.setBackground(gradient(new int[]{Color.rgb(58, 58, 58), Color.rgb(45, 45, 45)}, 16));
        LinearLayout.LayoutParams agentHubParams = new LinearLayout.LayoutParams(-1, dp(50));
        agentHubParams.topMargin = dp(8);
        agentHub.setOnClickListener(v -> startActivity(new Intent(this, AgentWorkspaceActivity.class)));
        page.addView(agentHub, agentHubParams);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(white);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search models, skills, or tasks");
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(round(Color.rgb(47, 47, 47), 16));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(56));
        searchParams.bottomMargin = dp(14);
        page.addView(search, searchParams);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(4), 0, dp(18));
        stats.addView(stat("17", "MODELS & TOOLS"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat("7+", "SKILL AREAS"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat("FREE", "TO EXPLORE"), new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(stats);

        TextView section = text("EXPLORE MODELS", 13, white, true);
        LinearLayout.LayoutParams secp = new LinearLayout.LayoutParams(-1, -2);
        secp.topMargin = dp(22);
        secp.bottomMargin = dp(11);
        page.addView(section, secp);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        String[] categories = {"All", "Coding", "Reasoning", "Fast", "Web", "Images", "Audio", "Local", "Research"};
        for (String category : categories) {
            TextView chip = text(category, 12, category.equals(activeFilter) ? bg : muted, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(15), dp(10), dp(15), dp(10));
            chip.setBackground(round(category.equals(activeFilter) ? purple : panel, 22));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(38));
            cp.rightMargin = dp(8);
            filters.addView(chip, cp);
            chip.setOnClickListener(v -> {
                activeFilter = category;
                for (int i = 0; i < filters.getChildCount(); i++) {
                    TextView c = (TextView) filters.getChildAt(i);
                    boolean selected = c.getText().toString().equals(activeFilter);
                    c.setTextColor(selected ? bg : muted);
                    c.setBackground(round(selected ? purple : panel, 22));
                }
                renderModels();
            });
        }
        filtersScroll.addView(filters);
        page.addView(filtersScroll);

        TextView note = text("STRENGTH LABELS ARE GUIDES, NOT A UNIVERSAL RANKING.", 10, muted, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(12);
        np.bottomMargin = dp(12);
        page.addView(note, np);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        page.addView(list);
        TextView footer = text("AI OMNIVERSE • Free cloud tiers only when available. Provider quotas, model access, and availability can change; no unlimited-use guarantee.", 11, muted, false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(18);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, fp);

        scroller.addView(page);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { renderModels(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        renderModels();
    }

    private void showGetApiKeyDialog() {
        String[] providerNames = {
            "OpenRouter (recommended to start)",
            "Google Gemini",
            "GroqCloud",
            "Cerebras",
            "Mistral AI",
            "NVIDIA NIM",
            "DeepSeek API"
        };
        String[] providerUrls = {
            "https://openrouter.ai/keys",
            "https://aistudio.google.com/apikey",
            "https://console.groq.com/keys",
            "https://cloud.cerebras.ai/",
            "https://console.mistral.ai/api-keys/",
            "https://build.nvidia.com/",
            "https://platform.deepseek.com/api_keys"
        };
        new AlertDialog.Builder(this)
            .setTitle("Get an API key")
            .setMessage("Choose a provider to create your own key on its official website. The provider creates the key after you sign in; AI OMNIVERSE cannot generate someone else's API key for you. After creating it, return here and tap ADD API KEY to save it.")
            .setItems(providerNames, (dialog, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(providerUrls[which])));
                } catch (Exception e) {
                    new AlertDialog.Builder(this)
                        .setTitle("Couldn't open website")
                        .setMessage("Open the provider website in your browser and try again.")
                        .setPositiveButton("OK", (d, w) -> d.dismiss())
                        .show();
                }
            })
            .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private LinearLayout stat(String number, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(12), dp(6), dp(12));
        box.addView(text(number, 18, white, true));
        TextView l = text(label, 9, muted, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(4);
        box.addView(l, p);
        return box;
    }

    private void renderModels() {
        if (list == null) return;
        list.removeAllViews();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Model m : models) {
            String all = (m.name + " " + m.maker + " " + m.description + " " + m.tags + " " + m.modality).toLowerCase(Locale.ROOT);
            if (!all.contains(q)) continue;
            if (!activeFilter.equals("All") && !Arrays.asList(m.tags.split(" ")).contains(activeFilter.toUpperCase(Locale.ROOT))) continue;
            list.addView(modelCard(m));
            count++;
        }
        if (count == 0) {
            TextView empty = text("No matching models. Try a different search or skill filter.", 14, muted, false);
            empty.setPadding(dp(14), dp(24), dp(14), dp(24));
            list.addView(empty);
        }
    }

    private View modelCard(Model m) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(15));
        card.setBackground(round(panel, 20));
        LinearLayout.LayoutParams cardp = new LinearLayout.LayoutParams(-1, -2);
        cardp.bottomMargin = dp(12);
        card.setLayoutParams(cardp);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text(m.name.substring(0, 1), 18, white, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(Color.rgb(49, 68, 61), 14));
        top.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        titleBox.addView(text(m.name, 17, white, true));
        titleBox.addView(text(m.maker, 11, muted, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView modality = text(m.modality.toUpperCase(Locale.ROOT), 9, purple, true);
        modality.setPadding(dp(8), dp(6), dp(8), dp(6));
        modality.setBackground(round(Color.rgb(37, 60, 52), 12));
        top.addView(modality);
        card.addView(top);

        TextView desc = text(m.description, 13, Color.rgb(218, 222, 239), false);
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-1, -2);
        dp1.topMargin = dp(13);
        card.addView(desc, dp1);

        LinearLayout tags = new LinearLayout(this);
        tags.setOrientation(LinearLayout.HORIZONTAL);
        tags.setPadding(0, dp(12), 0, dp(13));
        for (String tag : m.tags.split(" ")) {
            TextView t = text("✦ " + tag, 9, Color.rgb(190, 175, 255), true);
            t.setPadding(dp(8), dp(7), dp(8), dp(7));
            t.setBackground(round(Color.rgb(54, 54, 54), 10));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.rightMargin = dp(6);
            tags.addView(t, tp);
        }
        card.addView(tags);

        Button details = new Button(this);
        details.setText((m.name.equalsIgnoreCase("DeepSeek") || m.name.equalsIgnoreCase("Web Search"))
            ? "ADD API  ↗" : "VIEW STRENGTHS  ↗");
        details.setTextSize(11);
        details.setTextColor(white);
        details.setAllCaps(false);
        details.setTypeface(Typeface.DEFAULT_BOLD);
        details.setBackground(round(Color.rgb(60, 60, 60), 13));
        card.addView(details, new LinearLayout.LayoutParams(-1, dp(43)));
        View.OnClickListener open = v -> showDetails(m);
        card.setOnClickListener(open);
        details.setOnClickListener(open);
        return card;
    }

    private void showDetails(Model m) {
        if (m.name.equalsIgnoreCase("DeepSeek")) {
            new AlertDialog.Builder(this)
                .setTitle("Connect DeepSeek")
                .setMessage("DeepSeek needs its own API key. ADD API opens DeepSeek's official API-key page and prepares the DeepSeek provider form in AI OMNIVERSE. After you create a key, return to the app and save it. API usage is controlled by DeepSeek.")
                .setPositiveButton("ADD API", (d, w) -> launchApiSetup("deepseek", "deepseek-flash",
                    "https://platform.deepseek.com/api_keys", false))
                .setNeutralButton("VIEW STRENGTHS", (d, w) -> showModelNotes(m))
                .setNegativeButton("CANCEL", (d, w) -> d.dismiss())
                .show();
            return;
        }
        if (m.name.equalsIgnoreCase("Web Search")) {
            new AlertDialog.Builder(this)
                .setTitle("Connect Web Search")
                .setMessage("Web Search uses OpenRouter's web-search tool with compatible chat models. ADD API opens the OpenRouter key page and prepares the app with Web Search enabled. The search tool may have separate usage charges, even when the selected language model is free.")
                .setPositiveButton("ADD API", (d, w) -> launchApiSetup("openrouter", "openrouter/free",
                    "https://openrouter.ai/keys", true))
                .setNeutralButton("HOW IT WORKS", (d, w) -> showModelNotes(m))
                .setNegativeButton("CANCEL", (d, w) -> d.dismiss())
                .show();
            return;
        }
        showModelNotes(m);
    }

    private void showModelNotes(Model m) {
        new AlertDialog.Builder(this)
            .setTitle(m.name + "  •  " + m.maker)
            .setMessage("BEST AT\n" + m.description + "\n\nSKILL LABELS\n" + m.tags.replace(" ", "  •  ") + "\n\nMODALITY\n" + m.modality + "\n\nNOTES\n" + m.notes + "\n\nThis directory helps you choose a model. It does not claim every model family is connected or running inside the app.")
            .setNeutralButton("TRY AI ONLINE", (d, w) -> {
                startActivity(new Intent(this, AiChatActivity.class));
            })
            .setPositiveButton("GOT IT", (d, w) -> d.dismiss())
            .show();
    }

    private void launchApiSetup(String providerId, String modelId, String keyUrl, boolean enableWebSearch) {
        Intent setup = new Intent(this, AiChatActivity.class);
        setup.putExtra("open_key_setup", true);
        setup.putExtra("api_provider_id", providerId);
        setup.putExtra("requested_model", modelId);
        if (enableWebSearch) setup.putExtra("enable_web_search", true);
        startActivity(setup);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(keyUrl)));
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open the API-key website. The provider settings are ready in AI OMNIVERSE.", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(true);
        return t;
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

    private static class Model {
        String name, maker, description, tags, notes, modality;
        Model(String name, String maker, String description, String a, String b, String c, String notes, String modality) {
            this.name = name; this.maker = maker; this.description = description;
            this.tags = a + " " + b + " " + c; this.notes = notes; this.modality = modality;
        }
    }
}
