package dev.materialmischief.app;

import static dev.materialmischief.app.UI.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int HOME = 0, PLAY = 1, RESULT = 2, ENDING = 3;
    private SharedPreferences prefs;
    private FrameLayout viewport;
    private LinearLayout content;
    private ScrollView scroll;
    private PlayRoom room;
    private TextView moveCount, feedback;
    private int screen, level, resultSteps;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("casebook", MODE_PRIVATE);
        viewport = new FrameLayout(this);
        viewport.setBackgroundColor(PAPER);
        int flags =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(Build.VERSION.SDK_INT >= 26 ? PAPER : INK);
        viewport.setOnApplyWindowInsetsListener(
                (v, insets) -> {
                    if (Build.VERSION.SDK_INT >= 30) {
                        android.graphics.Insets b =
                                insets.getInsets(
                                        WindowInsets.Type.systemBars()
                                                | WindowInsets.Type.displayCutout());
                        v.setPadding(b.left, b.top, b.right, b.bottom);
                    } else {
                        v.setPadding(
                                insets.getSystemWindowInsetLeft(),
                                insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(),
                                insets.getSystemWindowInsetBottom());
                    }
                    return insets;
                });
        setContentView(viewport);
        level = saved == null ? 0 : saved.getInt("level", 0);
        int restore = saved == null ? HOME : saved.getInt("screen", HOME);
        resultSteps = saved == null ? 0 : saved.getInt("resultSteps", 0);
        if (restore == PLAY) play(level);
        else if (restore == RESULT) result();
        else if (restore == ENDING) ending();
        else home();
        if (saved != null) {
            ScrollView restoredScroll = scroll;
            restoredScroll.post(() -> restoredScroll.scrollTo(0, saved.getInt("scroll", 0)));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("screen", screen);
        state.putInt("level", level);
        state.putInt("resultSteps", resultSteps);
        state.putInt("scroll", scroll.getScrollY());
    }

    private int d(float n) {
        return dp(this, n);
    }

    private void page(int next) {
        screen = next;
        room = null;
        viewport.removeAllViews();
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        viewport.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        FrameLayout center = new FrameLayout(this);
        scroll.addView(center);
        int width = getResources().getConfiguration().screenWidthDp;
        int max = next == HOME && width >= 720 ? 960 : 580;
        content = column(this);
        int edge = width >= 600 ? 26 : 18;
        inset(content, edge, 6, edge, 28);
        center.addView(
                content,
                new FrameLayout.LayoutParams(
                        width > max ? d(max) : MATCH,
                        WRAP,
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL));
    }

    private void bar(String title, boolean back) {
        LinearLayout bar = row(this);
        content.addView(bar, MATCH, WRAP);
        if (back) {
            bar.addView(iconAction("back", "返回档案", this::home), d(48), d(48));
            space(bar, 4);
        } else {
            bar.addView(new Icon(this, "mark", INK), d(32), d(32));
            space(bar, 9);
        }
        TextView name = text(this, title, 17, INK, true);
        bar.addView(name, grow());
        if (screen == PLAY) {
            bar.addView(iconAction("reset", "重新调查本案", this::confirmRestart), d(48), d(48));
        } else {
            bar.addView(iconAction("settings", "游戏设置", this::settings), d(48), d(48));
        }
    }

    private View iconAction(String glyph, String label, Runnable action) {
        Icon icon = new Icon(this, glyph, INK);
        int pad = glyph.equals("settings") ? 12 : 13;
        icon.setBackground(ripple(shape(this, PAPER, 24, INK, 0), false));
        inset(icon, pad, pad, pad, pad);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        click(icon, label, action);
        return icon;
    }

    private LinearLayout action(String label, String glyph, boolean dark, Runnable callback) {
        int fg = dark ? WHITE : INK;
        LinearLayout b = row(this);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setMinimumHeight(d(54));
        b.setBackground(ripple(shape(this, dark ? INK : PAPER, 16, INK, 1), dark));
        inset(b, 18, 10, 16, 10);
        b.addView(text(this, label, 17, fg, true), grow());
        space(b, 8);
        b.addView(new Icon(this, glyph, dark ? LIME : INK), d(22), d(22));
        click(b, label, callback);
        return b;
    }

    private TextView chip(String label, int bg) {
        TextView v = mono(this, label, 12, INK);
        v.setGravity(Gravity.CENTER);
        v.setBackground(shape(this, bg, 20, INK, 1));
        inset(v, 10, 5, 10, 5);
        return v;
    }

    private int done() {
        int count = 0;
        for (int i = 0; i < Casebook.ALL.length; i++) if (best(i) >= 0) count++;
        return count;
    }

    private int best(int i) {
        return prefs.getInt("best." + i, -1);
    }

    private String prefix(int i) {
        return "attempt." + i + ".";
    }

    int getCaseInt(int i, String key, int fallback) {
        return prefs.getInt(prefix(i) + key, fallback);
    }

    void setCaseInt(int i, String key, int val) {
        prefs.edit().putInt(prefix(i) + key, val).apply();
    }

    private void clearAttempt(SharedPreferences.Editor edit, int i) {
        String prefix = prefix(i);
        for (String key : prefs.getAll().keySet()) if (key.startsWith(prefix)) edit.remove(key);
    }

    private int nextOpen() {
        int last = prefs.getInt("last", 0);
        if (getCaseInt(last, "active", 0) == 1) return last;
        for (int i = 0; i < Casebook.ALL.length; i++) if (best(i) < 0) return i;
        return -1;
    }

    private void home() {
        page(HOME);
        bar("彩蛋稽查处", false);
        space(content, 12);
        boolean wide = getResources().getConfiguration().screenWidthDp >= 720;
        LinearLayout cover = column(this), files = column(this);
        if (wide) {
            LinearLayout pair = row(this);
            pair.setGravity(Gravity.TOP);
            content.addView(pair, MATCH, WRAP);
            pair.addView(cover, new LinearLayout.LayoutParams(0, WRAP, 1.05f));
            space(pair, 22);
            pair.addView(files, new LinearLayout.LayoutParams(0, WRAP, 1f));
        } else {
            content.addView(cover, MATCH, WRAP);
            space(content, 24);
            content.addView(files, MATCH, WRAP);
        }
        int count = done();
        LinearLayout poster = column(this);
        poster.setBackground(shape(this, LIME, 24, INK, 1));
        poster.setClipToOutline(true);
        cover.addView(poster, MATCH, WRAP);
        LinearLayout lead = column(this);
        inset(lead, 19, 17, 18, 0);
        poster.addView(lead, MATCH, WRAP);
        LinearLayout mast = row(this);
        mast.setGravity(Gravity.TOP);
        lead.addView(mast, MATCH, WRAP);
        TextView title = text(this, "Material\nMischief", 52, INK, true);
        title.setLetterSpacing(-.065f);
        title.setLineSpacing(-d(6), 1f);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        mast.addView(title, grow());
        TextView tally = chip(count + " / 6", PAPER);
        tally.setContentDescription("已归档 " + count + " 起，共六起");
        mast.addView(tally);
        space(lead, 10);
        TextView brief = text(this, "更新日志只写着「版本号 +1」。彩蛋去哪了？", 14, INK, false);
        lead.addView(brief);
        MischiefArt art = new MischiefArt(this, -1);
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        poster.addView(art, MATCH, d(wide ? 184 : 156));
        LinearLayout controls = column(this);
        inset(controls, 12, 0, 12, 12);
        poster.addView(controls, MATCH, WRAP);
        int next = nextOpen();
        String label =
                next < 0
                        ? "查看结案报告"
                        : (getCaseInt(next, "active", 0) == 1 ? "继续调查" : "开始调查")
                                + "  /  "
                                + Casebook.ALL[next].name;
        controls.addView(
                action(
                        label,
                        "arrow",
                        true,
                        () -> {
                            if (next < 0) ending();
                            else play(next);
                        }),
                MATCH,
                WRAP);
        LinearLayout head = row(this);
        files.addView(head, MATCH, WRAP);
        head.addView(text(this, "案件档案", 23, INK, true), grow());
        head.addView(mono(this, "CASE FILES", 11, MUTED));
        space(files, 10);
        for (int i = 0; i < Casebook.ALL.length; i++) fileRow(files, i);
        if (count == 6 && next >= 0) {
            space(files, 12);
            files.addView(action("查看结案报告", "archive", false, this::ending));
        }
    }

    private void fileRow(LinearLayout files, int index) {
        Casebook c = Casebook.ALL[index];
        LinearLayout item = row(this);
        item.setMinimumHeight(d(74));
        item.setBackground(ripple(shape(this, PAPER, 12, INK, 0), false));
        inset(item, 2, 7, 4, 7);
        files.addView(item, MATCH, WRAP);
        item.addView(new MischiefArt(this, index), d(53), d(53));
        space(item, 14);
        LinearLayout name = column(this);
        item.addView(name, grow());
        name.addView(mono(this, Casebook.number(index) + "  /  " + c.component, 11, MUTED));
        space(name, 4);
        name.addView(text(this, c.name, 18, INK, true));
        int best = best(index);
        if (best >= 0) {
            TextView record = chip(best + " 步", LIME);
            item.addView(record);
            space(item, 5);
        } else if (getCaseInt(index, "active", 0) == 1) {
            item.addView(text(this, "调查中", 12, MUTED, false));
            space(item, 5);
        }
        item.addView(new Icon(this, "chevron", INK), d(18), d(18));
        click(
                item,
                "案件 "
                        + Casebook.number(index)
                        + "，"
                        + c.name
                        + (best >= 0 ? "，最佳 " + best + " 步" : ""),
                () -> play(index));
        View line = new View(this);
        line.setBackgroundColor(LINE);
        files.addView(line, MATCH, d(1));
    }

    private void play(int i) {
        level = i;
        prefs.edit().putInt("last", i).putInt(prefix(i) + "active", 1).apply();
        page(PLAY);
        Casebook c = Casebook.ALL[i];
        bar("案件 " + Casebook.number(i) + " / 06", true);
        space(content, 16);
        LinearLayout meta = row(this);
        content.addView(meta, MATCH, WRAP);
        meta.addView(chip(c.component, LILAC));
        meta.addView(new View(this), grow());
        moveCount = mono(this, steps() + " 步", 14, MUTED);
        meta.addView(moveCount);
        space(content, 12);
        content.addView(text(this, c.name, 34, INK, true));
        space(content, 7);
        content.addView(text(this, c.brief, 16, MUTED, false));
        space(content, 20);
        room = new PlayRoom(this, i);
        content.addView(room, MATCH, WRAP);
        space(content, 12);
        feedback = text(this, prefs.getString(prefix(i) + "message", ""), 15, INK, false);
        feedback.setBackground(shape(this, PALE, 14, LINE, 0));
        inset(feedback, 14, 12, 14, 12);
        feedback.setVisibility(feedback.length() == 0 ? View.GONE : View.VISIBLE);
        feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(feedback, MATCH, WRAP);
        LinearLayout hint = row(this);
        hint.setGravity(Gravity.CENTER);
        hint.setMinimumHeight(d(48));
        hint.setBackground(ripple(shape(this, PAPER, 14, INK, 0), false));
        hint.addView(new Icon(this, "hint", MUTED), d(17), d(17));
        space(hint, 7);
        TextView hintLabel =
                text(this, getCaseInt(i, "hint", 0) > 0 ? "查看解法" : "查看线索", 14, MUTED, true);
        hint.addView(hintLabel);
        content.addView(hint, MATCH, WRAP);
        hint.setFocusable(true);
        hint.setOnClickListener(
                v -> {
                    boolean seen = getCaseInt(i, "hint", 0) > 0;
                    setCaseInt(i, "hint", 1);
                    caseMessage(room, seen ? c.solution : c.hint);
                    hintLabel.setText("查看解法");
                });
        if (i < 3) FirstCases.show(room, i);
        else LastCases.show(room, i);
    }

    private int steps() {
        return getCaseInt(level, "steps", 0);
    }

    void recordMove(PlayRoom origin) {
        if (screen != PLAY || origin != room) return;
        int moves = steps() + 1;
        setCaseInt(level, "steps", moves);
        moveCount.setText(moves + " 步");
        if (prefs.getBoolean("haptics", true))
            origin.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    void caseMessage(PlayRoom origin, String text) {
        if (screen != PLAY || origin != room) return;
        prefs.edit().putString(prefix(level) + "message", text).apply();
        feedback.setText(text);
        feedback.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    void solve(PlayRoom origin) {
        if (screen != PLAY || origin != room) return;
        resultSteps = steps();
        SharedPreferences.Editor edit = prefs.edit();
        int old = best(level);
        if (old < 0 || resultSteps < old) edit.putInt("best." + level, resultSteps);
        clearAttempt(edit, level);
        edit.apply();
        result();
    }

    private void confirmRestart() {
        if (steps() == 0) {
            restart();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("重新调查本案？")
                .setMessage("本次操作和步数将清零。已归档的最佳纪录会保留。")
                .setNegativeButton("继续调查", null)
                .setPositiveButton("重新开始", (d, w) -> restart())
                .show();
    }

    private void restart() {
        SharedPreferences.Editor edit = prefs.edit();
        clearAttempt(edit, level);
        edit.apply();
        play(level);
    }

    private LinearLayout dossierCard(int bg) {
        LinearLayout card = column(this);
        card.setBackground(shape(this, bg, 22, INK, 1));
        inset(card, 20, 20, 20, 20);
        content.addView(card, MATCH, WRAP);
        return card;
    }

    private void result() {
        page(RESULT);
        bar("物证入库", true);
        space(content, 18);
        Casebook c = Casebook.ALL[level];
        LinearLayout card = dossierCard(LILAC);
        LinearLayout heading = row(this);
        card.addView(heading, MATCH, WRAP);
        heading.addView(mono(this, "EVIDENCE  /  " + Casebook.number(level), 12, INK), grow());
        TextView stamp = chip("✓  已复现", PAPER);
        stamp.setRotation(-5);
        heading.addView(stamp);
        space(card, 16);
        MischiefArt art = new MischiefArt(this, level);
        LinearLayout.LayoutParams square = new LinearLayout.LayoutParams(d(120), d(120));
        square.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(art, square);
        space(card, 16);
        card.addView(text(this, c.collectible, 29, INK, true));
        space(card, 8);
        card.addView(text(this, c.report, 16, INK, false));
        space(card, 18);
        LinearLayout stats = row(this);
        card.addView(stats, MATCH, WRAP);
        stats.addView(chip("本次 " + resultSteps + " 步", PAPER));
        space(stats, 8);
        stats.addView(chip("最佳 " + best(level) + " 步", LIME));
        if (!reducedMotion()) {
            stamp.setScaleX(1.25f);
            stamp.setScaleY(1.25f);
            stamp.animate().scaleX(1).scaleY(1).setDuration(240).start();
        }
        space(content, 16);
        int next = nextOpen();
        content.addView(
                action(
                        done() == 6 ? "签收结案报告" : "下一起  /  " + Casebook.ALL[next].name,
                        "arrow",
                        true,
                        () -> {
                            if (done() == 6) ending();
                            else play(next);
                        }),
                MATCH,
                WRAP);
        space(content, 9);
        content.addView(action("重查本案", "reset", false, () -> play(level)), MATCH, WRAP);
    }

    private void ending() {
        page(ENDING);
        bar("结案报告", true);
        space(content, 18);
        LinearLayout card = dossierCard(LIME);
        card.addView(mono(this, "RELEASE NOTES  /  1.0.0", 12, INK));
        MischiefArt egg = new MischiefArt(this, 6);
        card.addView(egg, MATCH, d(154));
        card.addView(text(this, "彩蛋已找回。", 32, INK, true));
        space(card, 12);
        card.addView(text(this, "修复了彩蛋没有内容的问题。新增了六个拒绝修复的组件。", 17, INK, false));
        space(card, 18);
        int total = 0;
        for (int i = 0; i < Casebook.ALL.length; i++) total += Math.max(0, best(i));
        card.addView(chip("个人最佳合计  /  " + total + " 步", PAPER));
        space(content, 20);
        content.addView(text(this, "回收物证", 22, INK, true));
        space(content, 9);
        for (int i = 0; i < Casebook.ALL.length; i++) {
            final int index = i;
            LinearLayout item = row(this);
            item.setMinimumHeight(d(52));
            content.addView(item, MATCH, WRAP);
            item.setBackground(ripple(shape(this, PAPER, 12, INK, 0), false));
            item.addView(new MischiefArt(this, i), d(39), d(39));
            space(item, 12);
            item.addView(text(this, Casebook.ALL[i].collectible, 16, INK, true), grow());
            item.addView(mono(this, best(i) + " 步", 13, MUTED));
            space(item, 5);
            item.addView(new Icon(this, "chevron", INK), d(16), d(16));
            click(item, "重新调查" + Casebook.ALL[i].name, () -> play(index));
        }
        space(content, 12);
        content.addView(action("返回案件档案", "archive", true, this::home), MATCH, WRAP);
    }

    boolean reducedMotion() {
        return prefs.getBoolean("reducedMotion", false)
                || Settings.Global.getFloat(
                                getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                        == 0f;
    }

    private void settings() {
        LinearLayout panel = column(this);
        inset(panel, 24, 12, 24, 8);
        addSetting(panel, "触感反馈", "haptics", true);
        addSetting(panel, "减少动态效果", "reducedMotion", false);
        space(panel, 16);
        panel.addView(text(this, "Material Mischief  ·  1.0.0", 14, MUTED, true));
        space(panel, 4);
        panel.addView(text(this, "调查进度会自动保存在本机。", 14, MUTED, false));
        new AlertDialog.Builder(this)
                .setTitle("游戏设置")
                .setView(panel)
                .setPositiveButton("完成", null)
                .show();
    }

    private void addSetting(LinearLayout panel, String title, String key, boolean initial) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(17);
        sw.setTextColor(INK);
        sw.setMinimumHeight(d(54));
        sw.setChecked(prefs.getBoolean(key, initial));
        sw.setOnCheckedChangeListener((v, on) -> prefs.edit().putBoolean(key, on).apply());
        panel.addView(sw, MATCH, WRAP);
    }

    @Override
    public void onBackPressed() {
        if (screen == HOME) super.onBackPressed();
        else home();
    }
}
