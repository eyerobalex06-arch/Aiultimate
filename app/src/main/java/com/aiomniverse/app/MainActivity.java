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
    private final int bg = Color.rgb(9, 11, 20);
    private final int panel = Color.rgb(19, 22, 37);
    private final int white = Color.rgb(245, 247, 255);
    private final int muted = Color.rgb(160, 168, 190);
    private final int purple = Color.rgb(167, 139, 250);
    private LinearLayout list;
    private EditText search;
    private String activeFilter = "All";
    private final List<Model> models = Arrays.asList(
        new Model("Qwen", "Alibaba", "Strong coding, reasoning, multilingual tasks", "CODING", "REASONING", "MULTILINGUAL", "A flexible family with several model sizes. Pick a size that matches your device or hosted service.", "Text / code"),
        new Model("DeepSeek", "DeepSeek", "Step-by-step reasoning and code generation", "REASONING", "CODING", "MATH", "Often worth trying for hard logic, mathematics, and programming tasks. Exact strengths vary by model version.", "Text / code"),
        new Model("Llama", "Meta", "General chat, writing, tool use, customization", "GENERAL", "WRITING", "OPEN WEIGHTS", "A widely used model family with many sizes and community fine-tunes. Licensing differs by release.", "Text / code"),
        new Model("Gemma", "Google", "Efficient local use, summaries, everyday assistance", "FAST", "LOCAL", "GENERAL", "Smaller variants can be practical on consumer hardware. Hardware needs depend on parameter count and quantization.", "Text"),
        new Model("Mistral", "Mistral AI", "Fast assistants, coding, multilingual chat", "FAST", "CODING", "MULTILINGUAL", "A family of efficient and capable models, including compact options and larger hosted models.", "Text / code"),
        new Model("Phi", "Microsoft", "Compact reasoning, structured tasks, on-device experiments", "SMALL", "REASONING", "LOCAL", "Small models can be easier to run locally, though they generally trade some capability for size.", "Text"),
        new Model("Command R", "Cohere", "Retrieval-augmented generation and grounded answers", "RESEARCH", "RAG", "BUSINESS", "Designed for enterprise-style retrieval and working with supplied documents when used in a properly configured system.", "Text"),
        new Model("Whisper", "OpenAI", "Speech-to-text and transcription", "AUDIO", "TRANSCRIPTION", "MULTILINGUAL", "A speech recognition model family, not a general chat assistant. Language and audio quality affect results.", "Audio"),
        new Model("FLUX", "Black Forest Labs", "Text-to-image generation and visual concepts", "IMAGES", "ART", "DESIGN", "An image-generation model family. Usually needs a compatible hosted service or suitable graphics hardware.", "Images"),
        new Model("Stable Diffusion", "Stability AI / community", "Image generation, styles, image workflows", "IMAGES", "ART", "CUSTOMIZATION", "A large ecosystem of image models and tools. Check each checkpoint's license and hardware requirements.", "Images")
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
        brand.addView(text("MODEL EXPLORER  •  v1.0", 10, muted, true));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("BETA", 10, purple, true);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(round(Color.rgb(39, 30, 67), 18));
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
        hero.setBackground(gradient(new int[]{Color.rgb(45, 35, 83), Color.rgb(24, 30, 55)}, 24));
        TextView eyebrow = text("YOUR AI TOOLKIT", 11, Color.rgb(196, 181, 253), true);
        hero.addView(eyebrow);
        TextView headline = text("Find the right AI\nfor the job.", 29, white, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(8);
        hero.addView(headline, hp);
        TextView subtitle = text("Compare strengths, discover specialist models, and choose the right tool instead of throwing every task at one chatbot.", 13, Color.rgb(207, 211, 231), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(9);
        hero.addView(subtitle, sp);
        page.addView(hero);

        Button liveChat = new Button(this);
        liveChat.setText("OPEN LIVE AI CHAT  ↗");
        liveChat.setTextSize(13);
        liveChat.setTextColor(white);
        liveChat.setAllCaps(false);
        liveChat.setTypeface(Typeface.DEFAULT_BOLD);
        liveChat.setBackground(gradient(new int[]{Color.rgb(126, 87, 194), Color.rgb(91, 70, 164)}, 16));
        LinearLayout.LayoutParams liveChatParams = new LinearLayout.LayoutParams(-1, dp(50));
        liveChatParams.topMargin = dp(12);
        liveChat.setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        page.addView(liveChat, liveChatParams);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(14), 0, dp(18));
        stats.addView(stat("10", "MODEL FAMILIES"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat("6+", "SKILL AREAS"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat("FREE", "TO EXPLORE"), new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(stats);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(white);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search models, skills, or tasks");
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(round(panel, 16));
        page.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView section = text("EXPLORE MODELS", 13, white, true);
        LinearLayout.LayoutParams secp = new LinearLayout.LayoutParams(-1, -2);
        secp.topMargin = dp(22);
        secp.bottomMargin = dp(11);
        page.addView(section, secp);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        String[] categories = {"All", "Coding", "Reasoning", "Fast", "Images", "Audio", "Local", "Research"};
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
        TextView footer = text("AI OMNIVERSE  •  Model directory only. Live AI access depends on a connected provider or hosted service.", 11, muted, false);
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
        icon.setBackground(round(Color.rgb(56, 43, 94), 14));
        top.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        titleBox.addView(text(m.name, 17, white, true));
        titleBox.addView(text(m.maker, 11, muted, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView modality = text(m.modality.toUpperCase(Locale.ROOT), 9, purple, true);
        modality.setPadding(dp(8), dp(6), dp(8), dp(6));
        modality.setBackground(round(Color.rgb(39, 30, 67), 12));
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
            t.setBackground(round(Color.rgb(34, 29, 53), 10));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.rightMargin = dp(6);
            tags.addView(t, tp);
        }
        card.addView(tags);

        Button details = new Button(this);
        details.setText("VIEW STRENGTHS  ↗");
        details.setTextSize(11);
        details.setTextColor(white);
        details.setAllCaps(false);
        details.setTypeface(Typeface.DEFAULT_BOLD);
        details.setBackground(round(Color.rgb(44, 47, 70), 13));
        card.addView(details, new LinearLayout.LayoutParams(-1, dp(43)));
        View.OnClickListener open = v -> showDetails(m);
        card.setOnClickListener(open);
        details.setOnClickListener(open);
        return card;
    }

    private void showDetails(Model m) {
        new AlertDialog.Builder(this)
            .setTitle(m.name + "  •  " + m.maker)
            .setMessage("BEST AT\n" + m.description + "\n\nSKILL LABELS\n" + m.tags.replace(" ", "  •  ") + "\n\nMODALITY\n" + m.modality + "\n\nNOTES\n" + m.notes + "\n\nThis directory helps you choose a model. It does not claim this model is connected or running inside the app.")
            .setNeutralButton("TRY AI ONLINE", (d, w) -> {
                startActivity(new Intent(this, AiChatActivity.class));
            })
            .setPositiveButton("GOT IT", (d, w) -> d.dismiss())
            .show();
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
