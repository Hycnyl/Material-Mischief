package dev.materialmischief.app;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** The switchboard, the backwards slider, and the missing Snackbar receipt. */
final class FirstCases {
    private static final int MATCH = LinearLayout.LayoutParams.MATCH_PARENT;
    private static final int WRAP = LinearLayout.LayoutParams.WRAP_CONTENT;

    static void show(PlayRoom room, int level) {
        room.removeAllViews();
        switch (level) {
            case 0:
                switchboard(room);
                break;
            case 1:
                ruler(room);
                break;
            case 2:
                new Receipts(room).show();
                break;
            default:
                throw new IllegalArgumentException("Unknown case: " + level);
        }
    }

    private static void switchboard(PlayRoom r) {
        final String key = "circuit";
        final int[] mask = {r.getInt(key, 0)};
        final boolean[] syncing = {false};
        final Switch[] toggles = new Switch[3];
        final String[] names = {"A   值班灯", "B   陪班灯", "C   加班灯"};
        RelayPanel diagram = new RelayPanel(r);
        diagram.mask = mask[0];
        r.addView(diagram, MATCH, r.d(80));
        r.space(8);

        ColorStateList thumbs =
                new ColorStateList(
                        new int[][] {{android.R.attr.state_checked}, {}},
                        new int[] {r.green(), r.paper()});
        ColorStateList tracks =
                new ColorStateList(
                        new int[][] {{android.R.attr.state_checked}, {}},
                        new int[] {r.ink(), 0xffa6aa9f});
        for (int i = 0; i < toggles.length; i++) {
            final int index = i;
            Switch toggle = new Switch(r.getContext());
            toggles[i] = toggle;
            toggle.setGravity(Gravity.CENTER_VERTICAL);
            toggle.setText(names[i]);
            toggle.setTextSize(16);
            toggle.setTextColor(r.ink());
            toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            toggle.setShowText(false);
            toggle.setSwitchMinWidth(r.d(58));
            toggle.setThumbTintList(thumbs);
            toggle.setTrackTintList(tracks);
            toggle.setPadding(r.d(10), r.d(4), r.d(10), r.d(4));
            toggle.setMinHeight(r.d(48));
            toggle.setChecked((mask[0] & (1 << i)) != 0);
            r.addView(toggle, MATCH, WRAP);
            toggle.setOnCheckedChangeListener(
                    (button, checked) -> {
                        if (syncing[0]) return;
                        int flips = (1 << index) | ((1 << (index + 1)) & 7);
                        mask[0] ^= flips;
                        r.putInt(key, mask[0]);
                        syncing[0] = true;
                        for (int j = 0; j < toggles.length; j++) {
                            toggles[j].setChecked((mask[0] & (1 << j)) != 0);
                        }
                        syncing[0] = false;
                        diagram.mask = mask[0];
                        diagram.invalidate();
                        r.move();
                        if (mask[0] == 7) {
                            r.win();
                        } else {
                            String clue =
                                    index == 2
                                            ? "C 的右边空着，这次只有它翻身。"
                                            : (index == 0 ? "A 一动，B 也跟着翻了个身。" : "B 一动，C 也跟着翻了个身。");
                            r.say(clue + "（" + Integer.bitCount(mask[0]) + "/3 盏亮）");
                        }
                    });
        }
    }

    private static void ruler(PlayRoom r) {
        TextView readout = r.label("", 19, r.ink(), true);
        readout.setGravity(Gravity.CENTER);
        r.addView(readout, MATCH, WRAP);
        r.space(8);
        RulerTrack track = new RulerTrack(r, readout);
        r.addView(track, MATCH, r.d(124));
        r.space(12);
        LinearLayout row = r.row();
        Button minus = r.button("− 5%", () -> track.nudge(-5));
        Button plus = r.button("+ 5%", () -> track.nudge(5));
        minus.setContentDescription("刻度减 5%");
        plus.setContentDescription("刻度加 5%");
        row.addView(minus, new LinearLayout.LayoutParams(0, WRAP, 1));
        View gap = new View(r.getContext());
        row.addView(gap, r.d(8), 1);
        row.addView(plus, new LinearLayout.LayoutParams(0, WRAP, 1));
        r.addView(row, MATCH, WRAP);
    }

    private static final class Receipts {
        final PlayRoom r;
        int index;
        boolean opened;

        Receipts(PlayRoom room) {
            r = room;
            index = r.getInt("receipt", 0);
            opened = r.getInt("opened", 0) == 1;
        }

        void show() {
            r.removeAllViews();
            ReceiptTray tray = new ReceiptTray(r, opened && index < 2 ? index : -1);
            r.addView(tray, MATCH, r.d(78));
            r.space(10);
            TextView count =
                    r.label(
                            "回执  0"
                                    + (index + 1)
                                    + " / 03"
                                    + (opened && index == 2 ? "   ·   内层" : ""),
                            13,
                            r.ink(),
                            true);
            count.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            r.addView(count, MATCH, WRAP);
            r.space(6);
            LinearLayout bar = new LinearLayout(r.getContext());
            bar.setOrientation(LinearLayout.VERTICAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setBackground(r.shape(r.ink(), 10, r.ink(), 1));
            bar.setPadding(r.d(14), r.d(12), r.d(14), r.d(6));
            String[] outer = {"已归档：无聊的更新日志。", "已归档：转个不停的省略号。", "已收起：一条可疑的回执。"};
            String[] inner = {
                "更新日志已恢复。\n「优化了『优化了』的措辞。」", "省略号已恢复。\n「正在准备开始准备……」", "彩蛋已归档。\n「等等，我还能再抢救一下！」"
            };
            TextView message = r.label(opened ? inner[index] : outer[index], 16, r.paper(), false);
            bar.addView(message, MATCH, WRAP);
            Button undo = r.button(opened && index < 2 ? "塞回去" : "撤销", this::undo);
            undo.setTextColor(r.green());
            undo.setBackgroundColor(0x00000000);
            undo.setStateListAnimator(null);
            undo.setMinWidth(r.d(60));
            undo.setMinimumWidth(r.d(60));
            undo.setPadding(r.d(12), 0, r.d(4), 0);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(WRAP, WRAP);
            actionParams.gravity = Gravity.END;
            bar.addView(undo, actionParams);
            r.addView(bar, MATCH, WRAP);
            if (!opened) {
                r.space(6);
                Button next =
                        r.button(
                                "翻到下一条  →",
                                () -> {
                                    index = (index + 1) % 3;
                                    r.putInt("receipt", index);
                                    r.move();
                                    show();
                                    r.say(index == 2 ? "它把回执本身也藏起来了。" : "这是另一条操作留下的回执。");
                                });
                r.addView(next, MATCH, WRAP);
            }
        }

        void undo() {
            r.move();
            if (opened && index == 2) {
                r.win();
                return;
            }
            opened = !opened;
            r.putInt("opened", opened ? 1 : 0);
            show();
            if (!opened) r.say("收好了。翻翻别的回执吧。");
            else if (index == 2) r.say("被藏起来的原始回执，里面还有一次撤销机会。");
            else r.say("恢复了一个老熟人。彩蛋还没回来。");
        }
    }

    private static class Illustration extends View {
        final PlayRoom r;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Illustration(PlayRoom room) {
            super(room.getContext());
            r = room;
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void ink(int color) {
            p.setColor(color);
            p.setStyle(Paint.Style.FILL);
        }

        void stroke(int color, float width) {
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
        }

        void center(Canvas canvas, float designWidth, float designHeight) {
            float scale = Math.min(getWidth() / designWidth, getHeight() / designHeight);
            canvas.translate(
                    (getWidth() - designWidth * scale) / 2,
                    (getHeight() - designHeight * scale) / 2);
            canvas.scale(scale, scale);
        }

        void text(Canvas c, String value, float x, float baseline, float size, int color) {
            ink(color);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(size);
            p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            c.drawText(value, x, baseline, p);
        }
    }

    private static final class RelayPanel extends Illustration {
        int mask;

        RelayPanel(PlayRoom room) {
            super(room);
        }

        @Override
        protected void onDraw(Canvas c) {
            c.save();
            center(c, 288, 80);
            ink(r.green());
            c.drawRoundRect(1, 1, 287, 79, 13, 13, p);
            stroke(r.ink(), 2);
            c.drawRoundRect(1, 1, 287, 79, 13, 13, p);
            c.drawLine(15, 33, 273, 33, p);
            for (int i = 0; i < 3; i++) {
                float x = 51 + i * 93;
                boolean lit = (mask & (1 << i)) != 0;
                ink(lit ? r.ink() : r.paper());
                c.drawRoundRect(x - 22, 16, x + 22, 50, 12, 12, p);
                stroke(r.ink(), 2);
                c.drawRoundRect(x - 22, 16, x + 22, 50, 12, 12, p);
                int face = lit ? r.green() : r.ink();
                if (lit) {
                    ink(face);
                    c.drawOval(x - 11, 25, x - 4, 38, p);
                    c.drawOval(x + 3, 25, x + 10, 38, p);
                } else {
                    stroke(face, 2.3f);
                    c.drawLine(x - 11, 33, x - 4, 33, p);
                    c.drawLine(x + 4, 33, x + 11, 33, p);
                }
                text(c, ((char) ('A' + i)) + (lit ? " / ON" : " / OFF"), x, 67, 11, r.ink());
                if (i < 2) {
                    float arrow = x + 49;
                    stroke(r.ink(), 2);
                    c.drawLine(arrow - 4, 29, arrow, 33, p);
                    c.drawLine(arrow, 33, arrow - 4, 37, p);
                }
            }
            c.restore();
        }
    }

    private static final class ReceiptTray extends Illustration {
        final int recovered;
        final DashPathEffect dashes = new DashPathEffect(new float[] {4, 4}, 0);

        ReceiptTray(PlayRoom room, int recovered) {
            super(room);
            this.recovered = recovered;
        }

        @Override
        protected void onDraw(Canvas c) {
            c.save();
            center(c, 288, 78);
            ink(r.green());
            c.drawRoundRect(1, 1, 287, 77, 14, 14, p);
            stroke(r.ink(), 2);
            c.drawRoundRect(1, 1, 287, 77, 14, 14, p);
            // A little thermal receipt peeks from the printer on the left.
            ink(r.ink());
            c.drawRoundRect(20, 53, 83, 62, 4, 4, p);
            Path receipt = new Path();
            receipt.moveTo(27, 54);
            receipt.lineTo(27, 18);
            for (int i = 0; i < 5; i++) {
                receipt.lineTo(32 + i * 10, 22);
                receipt.lineTo(37 + i * 10, 18);
            }
            receipt.lineTo(77, 54);
            receipt.close();
            ink(r.paper());
            c.drawPath(receipt, p);
            stroke(r.ink(), 2);
            c.drawPath(receipt, p);
            c.drawLine(35, 31, 67, 31, p);
            c.drawLine(35, 38, 59, 38, p);
            c.drawLine(35, 45, 64, 45, p);
            p.setPathEffect(dashes);
            c.drawLine(92, 40, 213, 40, p);
            p.setPathEffect(null);
            c.drawLine(209, 36, 214, 40, p);
            c.drawLine(214, 40, 209, 44, p);
            Path egg = new Path();
            egg.moveTo(246, 13);
            egg.cubicTo(236, 13, 225, 38, 225, 46);
            egg.cubicTo(225, 70, 267, 70, 267, 46);
            egg.cubicTo(267, 36, 256, 13, 246, 13);
            if (recovered < 0) {
                p.setPathEffect(dashes);
                c.drawPath(egg, p);
                p.setPathEffect(null);
                text(c, "?", 246, 49, 22, r.ink());
            } else if (recovered == 0) {
                ink(r.paper());
                c.drawRoundRect(228, 20, 264, 59, 3, 3, p);
                stroke(r.ink(), 2);
                c.drawRoundRect(228, 20, 264, 59, 3, 3, p);
                for (int i = 0; i < 3; i++) c.drawLine(234, 31 + i * 8, 256, 31 + i * 8, p);
            } else {
                for (int i = 0; i < 3; i++) {
                    ink(r.ink());
                    c.drawCircle(232 + i * 13, 40, 4, p);
                }
            }
            c.restore();
        }
    }

    private static final class RulerTrack extends Illustration {
        final TextView readout;
        int value;
        float downX;
        float downY;
        int downValue;
        boolean dragging;
        final int slop;

        RulerTrack(PlayRoom r, TextView readout) {
            super(r);
            this.readout = readout;
            value = r.getInt("mark", 40);
            slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            setFocusable(true);
            setClickable(true);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            update();
        }

        float spacing() {
            return Math.max(1, getWidth() * 0.077f);
        }

        void update() {
            readout.setText("当前 " + value + "%    /    目标 70%");
            setContentDescription("活动刻度轨，固定指针读数 " + value + "%，目标 70%。");
            invalidate();
        }

        void nudge(int delta) {
            value = Math.max(0, Math.min(100, value + delta));
            update();
            commit();
        }

        private void commit() {
            if (value == r.getInt("mark", 40)) return;
            r.putInt("mark", value);
            r.move();
            if (value == 70) r.win();
            else r.say(value < 70 ? "刻度还差一点，往左拉轨道。" : "越过目标了，把轨道往右送。");
        }

        private void dragTo(float x) {
            int steps = Math.round((x - downX) / spacing());
            int next = Math.max(0, Math.min(100, downValue - steps * 5));
            if (next != value) {
                value = next;
                update();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    downValue = value;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - downX;
                    if (!dragging
                            && Math.abs(dx) > slop
                            && Math.abs(dx) > Math.abs(event.getY() - downY)) {
                        dragging = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (dragging) dragTo(event.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (dragging) {
                        dragTo(event.getX());
                        commit();
                    } else {
                        performClick();
                    }
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    value = downValue;
                    dragging = false;
                    update();
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            r.say("指针被钉住了。从刻度上左右拖动，或用下方按钮微调。");
            return true;
        }

        @Override
        public boolean onKeyDown(int code, KeyEvent event) {
            if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                nudge(code == KeyEvent.KEYCODE_DPAD_RIGHT ? 5 : -5);
                return true;
            }
            return super.onKeyDown(code, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.SeekBar");
            info.setRangeInfo(
                    AccessibilityNodeInfo.RangeInfo.obtain(
                            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0, 100, value));
            info.setScrollable(true);
            if (value < 100)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            if (value > 0)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle args) {
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD && value < 100) {
                nudge(5);
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD && value > 0) {
                nudge(-5);
                return true;
            }
            return super.performAccessibilityAction(action, args);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float dp = getResources().getDisplayMetrics().density;
            float cx = w / 2;
            float railTop = h * .45f;
            float railBottom = h * .86f;
            float radius = 12 * dp;
            ink(r.green());
            c.drawRoundRect(1, 1, w - 1, h - 1, radius, radius, p);
            stroke(r.ink(), 2 * dp);
            c.drawRoundRect(dp, dp, w - dp, h - dp, radius, radius, p);
            c.save();
            c.clipRect(3 * dp, 0, w - 3 * dp, h - 2 * dp);
            float start = cx - (value / 5f) * spacing();
            float end = start + spacing() * 20;
            RectF rail = new RectF(start - 9 * dp, railTop, end + 9 * dp, railBottom);
            ink(r.paper());
            c.drawRoundRect(rail, 3 * dp, 3 * dp, p);
            stroke(r.ink(), 1.5f * dp);
            c.drawRoundRect(rail, 3 * dp, 3 * dp, p);
            for (int n = 0; n <= 100; n += 5) {
                float x = start + (n / 5) * spacing();
                if (x < -12 * dp || x > w + 12 * dp) continue;
                boolean major = n % 10 == 0;
                stroke(r.ink(), (n == 70 ? 3 : 1.5f) * dp);
                c.drawLine(x, railTop + dp, x, railTop + (major ? 14 : 7) * dp, p);
                if (major) {
                    if (n == 70) {
                        ink(r.coral());
                        c.drawRoundRect(
                                x - 13 * dp,
                                railBottom - 23 * dp,
                                x + 13 * dp,
                                railBottom - 3 * dp,
                                4 * dp,
                                4 * dp,
                                p);
                    }
                    text(c, Integer.toString(n), x, railBottom - 8 * dp, 12 * dp, r.ink());
                }
            }
            c.restore();

            // The thumb is riveted to the panel; only the printed track moves.
            float faceTop = railTop - 38 * dp;
            ink(r.ink());
            c.drawRoundRect(
                    cx - 19 * dp, faceTop, cx + 19 * dp, railTop - 7 * dp, 8 * dp, 8 * dp, p);
            Path point = new Path();
            point.moveTo(cx - 8 * dp, railTop - 8 * dp);
            point.lineTo(cx + 8 * dp, railTop - 8 * dp);
            point.lineTo(cx, railTop + 3 * dp);
            point.close();
            c.drawPath(point, p);
            ink(r.paper());
            c.drawOval(cx - 10 * dp, faceTop + 8 * dp, cx - 3 * dp, faceTop + 19 * dp, p);
            c.drawOval(cx + 3 * dp, faceTop + 8 * dp, cx + 10 * dp, faceTop + 19 * dp, p);
            ink(r.ink());
            float look = (value < 70 ? 1 : value == 70 ? 0 : -1) * 1.5f * dp;
            c.drawCircle(cx - 6.5f * dp + look, faceTop + 15 * dp, 1.9f * dp, p);
            c.drawCircle(cx + 6.5f * dp + look, faceTop + 15 * dp, 1.9f * dp, p);
        }
    }
}
