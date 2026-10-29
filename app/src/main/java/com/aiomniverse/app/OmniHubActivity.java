package com.aiomniverse.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class OmniHubActivity extends Activity {
    private final int bg = Color.rgb(18, 19, 22);
    private final int panel = Color.rgb(29, 31, 36);
    private final int white = Color.rgb(245, 246, 248);
    private final int muted = Color.rgb(163, 169, 179);
    private final int green = Color.rgb(16, 163, 127);
    private String attachedText = "";
    private String attachedName = "";
    private static final int PICK_FILE = 9107;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(18), dp(18), dp(10));
        TextView logo = text("✦", 28, green, true);
        toolbar.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(46)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("AI OMNIVERSE", 18, white, true));
        brand.addView(text("ONE HUB. MANY INTELLIGENCES.", 9, muted, true));
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(10), dp(16), dp(28));

        LinearLayout hero = card();
        hero.setBackground(gradient(new int[]{Color.rgb(24, 74, 63), Color.rgb(31, 38, 53)}, 22));
        hero.addView(text("YOUR AI CONTROL CENTER", 10, Color.rgb(164, 238, 214), true));
        TextView heading = text("One place for every idea.", 27, white, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(9);
        hero.addView(heading, hp);
        TextView copy = text("Chat with connected models, set up an AI team, explore dedicated AI services, or open image and video studios. Live response status is being improved inside chat; actual speed still depends on the model and its provider.", 13, Color.rgb(221, 229, 231), false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(8);
        hero.addView(copy, cp);
        page.addView(hero);

        section(page, "WORKSPACE");
        addAction(page, "✦  Multi-AI Chat", "Choose a connected model, save conversations, and manage API keys.", green, () -> startActivity(new Intent(this, AiChatActivity.class)));
        addAction(page, "◎  OmniBot · AI team room", "Put multiple configured AI agents on one mission and review their shared run log.", Color.rgb(77, 70, 130), () -> startActivity(new Intent(this, AgentWorkspaceActivity.class)));
        addAction(page, "▦  Model Directory", "Explore model families, capabilities, and setup links.", Color.rgb(57, 65, 76), () -> startActivity(new Intent(this, MainActivity.class)));

        section(page, "DEDICATED AI EXPERIENCES");
        addAction(page, "✧  Gemini", "Open Google's official Gemini experience. Sign-in and limits are controlled by Google.", Color.rgb(59, 88, 148), () -> open("https://gemini.google.com/"));
        addAction(page, "◈  Microsoft Copilot", "Open Copilot's own interface, without needing to paste an API key into this app.", Color.rgb(55, 86, 122), () -> open("https://copilot.microsoft.com/"));
        addAction(page, "⌘  ChatGPT", "Open ChatGPT's official experience.", Color.rgb(39, 83, 72), () -> open("https://chatgpt.com/"));
        addAction(page, "✦  Claude", "Open Anthropic's official assistant.", Color.rgb(108, 76, 59), () -> open("https://claude.ai/"));
        addAction(page, "✺  Grok", "Open xAI's official assistant.", Color.rgb(55, 55, 60), () -> open("https://grok.com/"));
        addAction(page, "⌕  Perplexity", "Open the research-focused assistant and web-search interface.", Color.rgb(34, 91, 105), () -> open("https://www.perplexity.ai/"));

        section(page, "IMAGE GENERATION");
        addAction(page, "▧  Hugging Face Spaces", "Browse image-generation demos such as FLUX and Stable Diffusion. Availability varies by demo.", Color.rgb(95, 76, 38), () -> open("https://huggingface.co/spaces"));
        addAction(page, "▧  Adobe Firefly", "Image generation and editing tools. Some features require an Adobe account or credits.", Color.rgb(105, 57, 57), () -> open("https://firefly.adobe.com/"));
        addAction(page, "▧  Ideogram", "Image generation with strong text-in-image workflows. Account and usage limits may apply.", Color.rgb(75, 62, 119), () -> open("https://ideogram.ai/"));

        section(page, "VIDEO GENERATION");
        addAction(page, "▶  Google Flow", "Explore Google's AI filmmaking tools where available.", Color.rgb(45, 77, 119), () -> open("https://labs.google/fx/tools/flow"));
        addAction(page, "▶  Runway", "Open Runway's video-generation workspace. Account, credits, and access rules apply.", Color.rgb(70, 65, 78), () -> open("https://runwayml.com/"));
        addAction(page, "▶  Pika", "Open Pika's AI video creation workspace.", Color.rgb(89, 55, 98), () -> open("https://pika.art/"));
        addAction(page, "▶  Luma Dream Machine", "Open Luma's video-generation service. Availability and credits depend on its current plan.", Color.rgb(43, 83, 84), () -> open("https://lumalabs.ai/dream-machine"));

        section(page, "FILES & PROJECTS");
        addAction(page, "＋  Attach a project file", "Choose a text, code, JSON, Markdown, or log file. Its text can be copied into a chat prompt. Binary APKs and other app packages are not executed or uploaded automatically.", Color.rgb(53, 70, 67), this::pickFile);
        if (!attachedName.isEmpty()) {
            LinearLayout fileCard = card();
            fileCard.addView(text("ATTACHED FILE", 10, green, true));
            fileCard.addView(text(attachedName, 14, white, true), top(7));
            fileCard.addView(text(attachedText.length() + " characters loaded locally. Nothing has been uploaded yet.", 11, muted, false), top(4));
            Button copyFileButton = button("COPY FILE TEXT", green);
            fileCard.addView(copyFileButton, top(10));
            copyFileButton.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText(attachedName, attachedText));
                Toast.makeText(this, "File text copied. Open Multi-AI Chat and paste it into your message.", Toast.LENGTH_LONG).show();
            });
            page.addView(fileCard);
        }

        TextView note = text("HONESTY CHECK  •  A website shortcut is not the same as a native API integration. Hosted AI services may require accounts, keys, credits, or have free-tier limits. This hub never stores another service's password.", 10, muted, false);
        note.setPadding(dp(8), dp(20), dp(8), dp(6));
        page.addView(note);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/markdown", "text/x-python", "text/x-java", "application/json", "text/csv", "application/xml", "text/xml", "application/octet-stream"});
        try { startActivityForResult(intent, PICK_FILE); }
        catch (Exception e) { Toast.makeText(this, "Could not open the file picker.", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            String mime = getContentResolver().getType(uri);
            String name = "Selected file";
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) name = cursor.getString(index);
                }
            }
            if (mime != null && !(mime.startsWith("text/") || mime.equals("application/json") || mime.equals("application/xml") || mime.equals("application/octet-stream"))) {
                Toast.makeText(this, "This file type isn't read as text. Choose a code or text file.", Toast.LENGTH_LONG).show();
                return;
            }
            try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (input == null) throw new Exception("File couldn't be opened.");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1 && out.size() < 200000) out.write(buffer, 0, Math.min(count, 200000 - out.size()));
                attachedText = new String(out.toByteArray(), StandardCharsets.UTF_8);
                attachedName = name;
            }
            if (attachedText.indexOf('\u0000') >= 0) throw new Exception("This appears to be a binary file, not readable source text.");
            Toast.makeText(this, "Loaded " + attachedName + " locally. Use COPY FILE TEXT to place it in a prompt.", Toast.LENGTH_LONG).show();
            buildUi();
        } catch (Exception e) {
            attachedText = "";
            attachedName = "";
            Toast.makeText(this, "Couldn't read this file as text: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, "Couldn't open the service website.", Toast.LENGTH_LONG).show(); }
    }

    private void section(LinearLayout page, String name) {
        TextView t = text(name, 12, white, true);
        t.setPadding(dp(3), dp(20), 0, dp(9));
        page.addView(t);
    }

    private void addAction(LinearLayout page, String title, String description, int accent, Runnable action) {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout icon = new LinearLayout(this);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(accent, 15));
        icon.addView(text(title.substring(0, 1), 20, white, true));
        c.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), 0, 0, 0);
        body.addView(text(title, 14, white, true));
        TextView desc = text(description, 11, muted, false);
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(-1, -2);
        d.topMargin = dp(5);
        body.addView(desc, d);
        c.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        c.setOnClickListener(v -> action.run());
        page.addView(c);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        c.setBackground(round(panel, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10);
        c.setLayoutParams(p);
        return c;
    }
    private LinearLayout.LayoutParams top(int value) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(value);
        return p;
    }
    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(white);
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
    private GradientDrawable gradient(int[] colors, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
