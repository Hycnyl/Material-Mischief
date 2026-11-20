package dev.materialmischief.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class UI {
    static final int INK = 0xff192019, PAPER = 0xfff8f7ef, LIME = 0xffc2f675;
    static final int CORAL = 0xffff856d, LILAC = 0xffcec6ff, WHITE = 0xfffffff8;
    static final int MUTED = 0xff536052, LINE = 0xffc6cbbd, PALE = 0xffeaece0;
    static final int WRAP = -2, MATCH = -1;

    static int dp(Context c, float n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable shape(Context c, int fill, float radius, int stroke, float width) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radius));
        if (width > 0) g.setStroke(Math.max(1, dp(c, width)), stroke);
        return g;
    }

    static Drawable ripple(Drawable bg, boolean dark) {
        return new RippleDrawable(ColorStateList.valueOf(dark ? 0x44ffffff : 0x24192019), bg, null);
    }

    static TextView text(Context c, String s, float size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setTypeface(
                Typeface.create(
                        bold ? "sans-serif-medium" : "sans-serif",
                        bold ? Typeface.BOLD : Typeface.NORMAL));
        return v;
    }

    static TextView mono(Context c, String s, int size, int color) {
        TextView v = text(c, s, size, color, true);
        v.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        v.setLetterSpacing(.055f);
        return v;
    }

    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    static void inset(View v, int l, int t, int r, int b) {
        Context c = v.getContext();
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    static void space(LinearLayout v, int n) {
        boolean vert = v.getOrientation() == LinearLayout.VERTICAL;
        int px = dp(v.getContext(), n);
        v.addView(new android.widget.Space(v.getContext()), vert ? 1 : px, vert ? px : 1);
    }

    static LinearLayout.LayoutParams grow() {
        return new LinearLayout.LayoutParams(0, WRAP, 1f);
    }

    static void click(View v, String description, Runnable action) {
        v.setContentDescription(description);
        v.setFocusable(true);
        v.setOnClickListener(view -> action.run());
    }

    static class Icon extends View {
        final String glyph;
        final int color;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Icon(Context c, String glyph, int color) {
            super(c);
            this.glyph = glyph;
            this.color = color;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w = getWidth() - getPaddingLeft() - getPaddingRight();
            int h = getHeight() - getPaddingTop() - getPaddingBottom();
            float size = Math.min(w, h);
            c.save();
            c.translate(getPaddingLeft() + (w - size) / 2f, getPaddingTop() + (h - size) / 2f);
            c.scale(size / 24f, size / 24f);
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.85f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            switch (glyph) {
                case "back":
                    line(c, 17, 5, 9, 12, 17, 19);
                    c.drawLine(9, 12, 21, 12, p);
                    break;
                case "arrow":
                    line(c, 13, 5, 20, 12, 13, 19);
                    c.drawLine(3, 12, 20, 12, p);
                    break;
                case "chevron":
                    line(c, 9, 6, 15, 12, 9, 18);
                    break;
                case "settings":
                    c.drawLine(3, 7, 12, 7, p);
                    c.drawLine(18, 7, 21, 7, p);
                    c.drawLine(3, 17, 6, 17, p);
                    c.drawLine(12, 17, 21, 17, p);
                    c.drawCircle(15, 7, 3, p);
                    c.drawCircle(9, 17, 3, p);
                    break;
                case "hint":
                    c.drawCircle(12, 10, 6, p);
                    c.drawLine(9, 16, 15, 16, p);
                    c.drawLine(10, 19, 14, 19, p);
                    c.drawLine(11, 22, 13, 22, p);
                    break;
                case "check":
                    line(c, 4, 12, 10, 18, 21, 6);
                    break;
                case "reset":
                    c.drawArc(5, 6, 20, 21, 205, 303, false, p);
                    line(c, 3, 4, 4, 11, 10, 9);
                    break;
                case "archive":
                    c.drawRoundRect(3, 5, 21, 10, 1, 1, p);
                    c.drawRect(5, 10, 19, 20, p);
                    c.drawLine(10, 14, 14, 14, p);
                    break;
                case "mark":
                    p.setStyle(Paint.Style.FILL);
                    c.drawRoundRect(0, 0, 24, 24, 7, 7, p);
                    p.setColor(LIME);
                    c.drawRoundRect(3, 7, 21, 17, 5, 5, p);
                    p.setColor(INK);
                    c.drawCircle(14, 11, 1.15f, p);
                    c.drawCircle(17.5f, 10.5f, 1.15f, p);
                    break;
            }
            c.restore();
        }

        private void line(Canvas c, float ax, float ay, float bx, float by, float dx, float dy) {
            Path path = new Path();
            path.moveTo(ax, ay);
            path.lineTo(bx, by);
            path.lineTo(dx, dy);
            c.drawPath(path, p);
        }
    }
}
