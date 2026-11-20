package dev.materialmischief.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native component playground. Each case owns its controls and its saved attempt keys. */
public final class PlayRoom extends LinearLayout {
    private final MainActivity host;
    final int level;

    PlayRoom(MainActivity host, int level) {
        super(host);
        this.host = host;
        this.level = level;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setBackground(shape(UI.WHITE, 20, UI.INK, 1));
        UI.inset(this, 18, 20, 18, 20);
    }

    public int d(float dp) {
        return UI.dp(getContext(), dp);
    }

    public int ink() {
        return UI.INK;
    }

    public int green() {
        return UI.LIME;
    }

    public int paper() {
        return UI.PAPER;
    }

    public int coral() {
        return UI.CORAL;
    }

    public GradientDrawable shape(int fill, int radius, int strokeColor, int strokeDp) {
        return UI.shape(getContext(), fill, radius, strokeColor, strokeDp);
    }

    public LinearLayout row() {
        return UI.row(getContext());
    }

    public TextView label(String text, int sp, int color, boolean bold) {
        return UI.text(getContext(), text, sp, color, bold);
    }

    public Button button(String text, Runnable action) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(ink());
        b.setTextSize(15);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setBackground(UI.ripple(shape(paper(), 14, ink(), 1), false));
        b.setStateListAnimator(null);
        b.setMinHeight(d(48));
        b.setMinimumHeight(d(48));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        UI.inset(b, 10, 8, 10, 8);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    public void space(int dp) {
        UI.space(this, dp);
    }

    public int getInt(String key, int fallback) {
        return host.getCaseInt(level, key, fallback);
    }

    public void putInt(String key, int value) {
        host.setCaseInt(level, key, value);
    }

    public void move() {
        host.recordMove(this);
    }

    public void say(String text) {
        host.caseMessage(this, text);
    }

    public void win() {
        host.solve(this);
    }

    public boolean reducedMotion() {
        return host.reducedMotion();
    }
}
