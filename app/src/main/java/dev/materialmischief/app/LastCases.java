package dev.materialmischief.app;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

final class LastCases {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int PISTACHIO = 0xFFE1ECCF;
    private static final int WHITE = 0xFFFFFDF5;
    private static final int LILAC = 0xFFE0D6FA;
    private static final int SKY = 0xFFCCE4ED;
    private static final int BUTTER = 0xFFFFE49D;

    static void show(PlayRoom r, int level) {
        r.removeAllViews();
        switch (level) {
            case 3:
                r.addView(new Runaway(r), MATCH, r.d(240));
                r.space(10);
                r.addView(r.label("把底座拖到空站，或轻点一站布置。再戳戳那枚浮钮。", 14, r.ink(), false));
                break;
            case 4:
                new Notifications(r).render();
                break;
            case 5:
                if (r.getInt("knocks", 0) < 3) {
                    about(r);
                } else {
                    clock(r);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown case: " + level);
        }
    }

    private static TextView centered(PlayRoom r, String text, int sp, int color, boolean bold) {
        TextView view = r.label(text, sp, color, bold);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private static Drawable ripple(PlayRoom r) {
        android.util.TypedValue value = new android.util.TypedValue();
        r.getContext()
                .getTheme()
                .resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId == 0 ? null : r.getContext().getDrawable(value.resourceId);
    }

    private static void mono(TextView view) {
        view.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    }

    private static void pad(PlayRoom r, View v, int horizontal, int vertical) {
        v.setPadding(r.d(horizontal), r.d(vertical), r.d(horizontal), r.d(vertical));
    }

    private static void accessibleButton(View view) {
        view.setFocusable(true);
        view.setAccessibilityDelegate(
                new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            View host, android.view.accessibility.AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.setClassName(Button.class.getName());
                    }
                });
    }

    /** The track is drawn once. Each station above it is a native focusable button. */
    private static final class Runaway extends FrameLayout {
        private final PlayRoom r;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Button[] stations = new Button[4];
        private final Button handle;
        private int fab;
        private int base;
        private int flightFrom;
        private float flight = 1f;
        private ValueAnimator flightAnimator;
        private boolean dragging;
        private float startX, startY, fingerX, fingerY;
        private static final String[] NAMES = {"左上", "右上", "右下", "左下"};

        Runaway(PlayRoom r) {
            super(r.getContext());
            this.r = r;
            fab = r.getInt("fab", 0);
            base = r.getInt("base", -1);
            setWillNotDraw(false);
            setBackground(r.shape(PISTACHIO, 19, r.ink(), 1));
            for (int i = 0; i < stations.length; i++) {
                final int index = i;
                stations[i] = hotspot();
                stations[i].setOnClickListener(v -> stationTapped(index));
            }
            handle = hotspot();
            handle.setContentDescription("可移动底座。轻点任意空站布置，或拖动底座到该站。");
            handle.setOnClickListener(v -> r.say("底座认站牌。轻点空站也能把它放过去。"));
            handle.setOnTouchListener((v, e) -> dragBase(e));
            describe();
        }

        private Button hotspot() {
            Button b = new Button(getContext());
            b.setPadding(0, 0, 0, 0);
            b.setStateListAnimator(null);
            b.setBackground(ripple(r));
            b.setMinWidth(0);
            b.setMinimumWidth(0);
            b.setMinHeight(0);
            b.setMinimumHeight(0);
            addView(b);
            return b;
        }

        private void stationTapped(int station) {
            if (station != fab) {
                place(station);
                return;
            }
            int next = (fab + 1) % 4;
            r.move();
            if (base == next) {
                r.win();
                return;
            }
            flightFrom = fab;
            fab = next;
            r.putInt("fab", fab);
            if (flightAnimator != null) flightAnimator.cancel();
            if (r.reducedMotion()) {
                flight = 1f;
            } else {
                flight = 0f;
                flightAnimator = ValueAnimator.ofFloat(0f, 1f);
                flightAnimator.setDuration(240);
                flightAnimator.addUpdateListener(
                        a -> {
                            flight = (float) a.getAnimatedValue();
                            invalidate();
                        });
                flightAnimator.start();
            }
            if (base >= 0) {
                base = -1;
                r.putInt("base", -1);
            }
            r.say("嗖！它顺着箭头溜到了" + NAMES[fab] + "站。");
            describe();
            invalidate();
        }

        @Override
        protected void onDetachedFromWindow() {
            if (flightAnimator != null) flightAnimator.cancel();
            super.onDetachedFromWindow();
        }

        private void place(int station) {
            if (station == fab) {
                r.say("这里已经挤着一枚浮钮。底座只收空站。");
                return;
            }
            if (station != base) {
                base = station;
                r.putInt("base", base);
                r.move();
            }
            r.say("底座已卡进" + NAMES[station] + "站，等一枚自己跳进来的浮钮。");
            describe();
            invalidate();
        }

        private void describe() {
            for (int i = 0; i < stations.length; i++) {
                String state = i == fab ? "浮动按钮，点击会逃跑" : i == base ? "已布置底座" : "空站，点击布置底座";
                stations[i].setContentDescription(NAMES[i] + "站，" + state);
            }
        }

        private boolean dragBase(MotionEvent e) {
            float x = handle.getLeft() + e.getX();
            float y = handle.getTop() + e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    startX = fingerX = x;
                    startY = fingerY = y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    fingerX = x;
                    fingerY = y;
                    if (Math.hypot(x - startX, y - startY) > r.d(6)) dragging = true;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        int near = -1;
                        for (int i = 0; i < 4; i++) {
                            if (Math.hypot(x - cx(i), y - cy(i)) < r.d(39)) near = i;
                        }
                        if (near >= 0) place(near);
                        else r.say("底座滑回来了。把它对准四个圆站中的一个。");
                    } else {
                        handle.performClick();
                    }
                    // A cancelled drag also releases the enclosing ScrollView.
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    return true;
                default:
                    return false;
            }
        }

        private float cx(int i) {
            return getWidth() * (i == 0 || i == 3 ? .25f : .75f);
        }

        private float cy(int i) {
            return getHeight() * (i < 2 ? .22f : .57f);
        }

        private float trayY() {
            return getHeight() * .855f;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int half = r.d(28);
            for (int i = 0; i < 4; i++) {
                int x = Math.round(cx(i)), y = Math.round(cy(i));
                stations[i].layout(x - half, y - half, x + half, y + half);
            }
            int inset = r.d(8);
            int y = Math.round(trayY());
            handle.layout(inset, y - r.d(27), getWidth() - inset, y + r.d(27));
        }

        private void pen(int color, float width) {
            p.setColor(color);
            p.setStyle(width > 0 ? Paint.Style.STROKE : Paint.Style.FILL);
            p.setStrokeWidth(r.d(width));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }

        private void arrow(Canvas c, float x, float y, int degrees) {
            c.save();
            c.rotate(degrees, x, y);
            float u = r.d(5);
            c.drawLine(x - u, y - u, x, y, p);
            c.drawLine(x - u, y + u, x, y, p);
            c.restore();
        }

        private void dock(Canvas c, float x, float y) {
            float h = r.d(21), w = r.d(24);
            pen(r.ink(), 2);
            c.drawRoundRect(x - w, y - h, x + w, y + h, r.d(11), r.d(11), p);
            pen(r.ink(), 1);
            c.drawRoundRect(
                    x - w + r.d(4),
                    y - h + r.d(4),
                    x + w - r.d(4),
                    y + h - r.d(4),
                    r.d(8),
                    r.d(8),
                    p);
            c.drawLine(x - r.d(5), y, x + r.d(5), y, p);
            c.drawLine(x, y - r.d(5), x, y + r.d(5), p);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float x0 = cx(0), x1 = cx(1), y0 = cy(0), y1 = cy(2);
            pen(r.ink(), 2);
            c.drawRoundRect(x0, y0, x1, y1, r.d(12), r.d(12), p);
            arrow(c, (x0 + x1) / 2, y0, 0);
            arrow(c, x1, (y0 + y1) / 2, 90);
            arrow(c, (x0 + x1) / 2, y1, 180);
            arrow(c, x0, (y0 + y1) / 2, 270);
            for (int i = 0; i < 4; i++) {
                float x = cx(i), y = cy(i), rad = r.d(19);
                pen(WHITE, 0);
                c.drawCircle(x, y, rad, p);
                pen(r.ink(), 1);
                c.drawCircle(x, y, rad, p);
                pen(r.ink(), 0);
                p.setTypeface(Typeface.MONOSPACE);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(r.d(12));
                c.drawText("0" + (i + 1), x, y + r.d(4), p);
                if (i == base) {
                    pen(BUTTER, 0);
                    c.drawRoundRect(
                            x - r.d(24),
                            y - r.d(21),
                            x + r.d(24),
                            y + r.d(21),
                            r.d(11),
                            r.d(11),
                            p);
                    dock(c, x, y);
                }
            }
            float x = cx(flightFrom) + (cx(fab) - cx(flightFrom)) * flight;
            float y =
                    cy(flightFrom)
                            + (cy(fab) - cy(flightFrom)) * flight
                            - r.d(11) * 4 * flight * (1 - flight);
            float half = r.d(24);
            pen(r.ink(), 0);
            c.drawRoundRect(
                    x - half + r.d(2),
                    y - half + r.d(4),
                    x + half + r.d(2),
                    y + half + r.d(4),
                    r.d(13),
                    r.d(13),
                    p);
            pen(r.green(), 0);
            c.drawRoundRect(x - half, y - half, x + half, y + half, r.d(13), r.d(13), p);
            pen(r.ink(), 2);
            c.drawRoundRect(x - half, y - half, x + half, y + half, r.d(13), r.d(13), p);
            pen(r.ink(), 0);
            c.drawOval(x - r.d(12), y - r.d(8), x - r.d(5), y + r.d(3), p);
            c.drawOval(x + r.d(3), y - r.d(8), x + r.d(10), y + r.d(3), p);
            pen(r.ink(), 2);
            c.drawLine(x + r.d(2), y + r.d(9), x + r.d(9), y + r.d(7), p);
            pen(r.ink(), 1);
            c.drawLine(r.d(12), getHeight() * .735f, getWidth() - r.d(12), getHeight() * .735f, p);
            x = getWidth() / 2f;
            y = trayY();
            dock(c, x - r.d(38), y);
            pen(r.ink(), 0);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(r.d(14));
            c.drawText("拖动底座", x - r.d(4), y + r.d(5), p);
            if (dragging) dock(c, fingerX, fingerY);
        }
    }

    private static final class Notifications {
        private static final String[] NAMES = {"闹钟", "截图", "勿扰模式", "系统更新"};
        private static final String[] CLUES = {
            "别催。整个通知栏都是我叫醒的。", "照片里，闹钟就在我正上方。", "等截图存好，我才开始静音。", "请在勿扰之后重启。仍需 1 分钟。"
        };
        private static final int[] COLORS = {BUTTER, SKY, LILAC, PISTACHIO};
        private static final int[] INITIAL = {2, 3, 1, 0};
        private final PlayRoom r;
        private final int[] order = new int[4];
        private int selected;

        Notifications(PlayRoom r) {
            this.r = r;
            for (int i = 0; i < 4; i++) order[i] = r.getInt("notice" + i, INITIAL[i]);
            selected = r.getInt("chosen", -1);
        }

        void render() {
            r.removeAllViews();
            r.addView(r.label("从上往下排出事发顺序。长按拖动卡片，或点两张卡片交换。", 14, r.ink(), false));
            r.space(10);
            for (int i = 0; i < 4; i++) {
                int id = order[i];
                boolean active = id == selected;
                LinearLayout card = r.row();
                card.setGravity(Gravity.CENTER_VERTICAL);
                pad(r, card, 10, 8);
                card.setMinimumHeight(r.d(58));
                card.setBackground(
                        r.shape(active ? r.green() : WHITE, 15, r.ink(), active ? 2 : 1));
                card.setForeground(ripple(r));
                NoticeIcon icon = new NoticeIcon(r, id);
                card.addView(icon, r.d(36), r.d(36));
                LinearLayout copy = new LinearLayout(r.getContext());
                copy.setOrientation(LinearLayout.VERTICAL);
                pad(r, copy, 9, 0);
                copy.addView(r.label(NAMES[id], 15, r.ink(), true));
                copy.addView(r.label(CLUES[id], 12, r.ink(), false));
                card.addView(copy, new LinearLayout.LayoutParams(0, WRAP, 1));
                TextView grip = centered(r, active ? "✓" : "≡", 22, r.ink(), true);
                card.addView(grip, r.d(20), WRAP);
                card.setContentDescription(
                        "第"
                                + (i + 1)
                                + "条，"
                                + NAMES[id]
                                + "。"
                                + CLUES[id]
                                + (active ? " 已选中。" : " 点击选择，或长按拖动。"));
                accessibleButton(card);
                card.setOnClickListener(v -> choose(id));
                card.setOnLongClickListener(
                        v -> {
                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                return v.startDragAndDrop(
                                        null,
                                        new View.DragShadowBuilder(v),
                                        Integer.valueOf(id),
                                        0);
                            }
                            return v.startDrag(
                                    null, new View.DragShadowBuilder(v), Integer.valueOf(id), 0);
                        });
                card.setOnDragListener((v, event) -> drag(card, id, event));
                r.addView(card);
                r.space(6);
            }
            r.addView(
                    r.button(
                            "归档这组通知",
                            () -> {
                                r.move();
                                for (int i = 0; i < 4; i++) {
                                    if (order[i] != i) {
                                        r.say("归档退回。再听听「" + NAMES[i] + "」说了什么。");
                                        return;
                                    }
                                }
                                r.win();
                            }));
        }

        private int index(int id) {
            for (int i = 0; i < 4; i++) if (order[i] == id) return i;
            throw new IllegalArgumentException("Unknown notice: " + id);
        }

        private void choose(int id) {
            if (selected == -1) {
                selected = id;
                r.putInt("chosen", id);
                r.say("已夹起「" + NAMES[id] + "」。再点一张和它换位。");
            } else if (selected == id) {
                selected = -1;
                r.putInt("chosen", -1);
                r.say("已放回原位。");
            } else {
                int from = index(selected), to = index(id);
                order[from] = id;
                order[to] = selected;
                changed("两张通知换了位置。");
            }
            render();
        }

        private boolean drag(View card, int dest, DragEvent event) {
            if (!(event.getLocalState() instanceof Integer)) return false;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    card.setAlpha(.65f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    card.setAlpha(1f);
                    return true;
                case DragEvent.ACTION_DROP:
                    int id = (Integer) event.getLocalState();
                    int from = index(id), to = index(dest);
                    if (from != to) {
                        if (from < to) System.arraycopy(order, from + 1, order, from, to - from);
                        else System.arraycopy(order, to, order, to + 1, from - to);
                        order[to] = id;
                        changed("「" + NAMES[id] + "」移动到第 " + (to + 1) + " 行。");
                        card.post(this::render);
                    }
                    return true;
                default:
                    return true;
            }
        }

        private void changed(String feedback) {
            for (int i = 0; i < 4; i++) r.putInt("notice" + i, order[i]);
            selected = -1;
            r.putInt("chosen", -1);
            r.move();
            r.say(feedback);
        }

        private static final class NoticeIcon extends View {
            private final int id;
            private final PlayRoom r;
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            NoticeIcon(PlayRoom r, int id) {
                super(r.getContext());
                this.r = r;
                this.id = id;
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            }

            @Override
            protected void onDraw(Canvas c) {
                c.save();
                c.scale(getWidth() / 36f, getHeight() / 36f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(COLORS[id]);
                c.drawRoundRect(1, 1, 35, 35, 10, 10, p);
                p.setColor(r.ink());
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.8f);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);
                if (id == 0) {
                    c.drawCircle(18, 19, 7, p);
                    c.drawLine(18, 15, 18, 19, p);
                    c.drawLine(18, 19, 21, 20, p);
                    c.drawLine(13, 10, 10, 13, p);
                    c.drawLine(23, 10, 26, 13, p);
                } else if (id == 1) {
                    c.drawRoundRect(9, 11, 27, 25, 2, 2, p);
                    c.drawLine(10, 23, 16, 17, p);
                    c.drawLine(16, 17, 20, 21, p);
                    c.drawCircle(22, 15, 1, p);
                } else if (id == 2) {
                    c.drawCircle(18, 18, 8, p);
                    c.drawLine(14, 18, 22, 18, p);
                } else {
                    c.drawLine(18, 9, 18, 21, p);
                    c.drawLine(14, 17, 18, 21, p);
                    c.drawLine(22, 17, 18, 21, p);
                    c.drawLine(11, 25, 25, 25, p);
                }
                c.restore();
            }
        }
    }

    private static void about(PlayRoom r) {
        int knocks = r.getInt("knocks", 0);
        LinearLayout panel = new LinearLayout(r.getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackground(r.shape(LILAC, 20, r.ink(), 1));
        pad(r, panel, 12, 14);
        panel.addView(new Egg(r, knocks), r.d(112), r.d(78));
        TextView title = centered(r, "关于手机", 21, r.ink(), true);
        panel.addView(title);
        r.addView(panel);
        r.space(12);
        TextView version = r.label("Android 版本     " + (16 + knocks), 18, r.ink(), true);
        pad(r, version, 14, 10);
        version.setMinHeight(r.d(54));
        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setBackground(r.shape(WHITE, 12, r.ink(), 1));
        version.setForeground(ripple(r));
        version.setContentDescription("Android 版本 " + (16 + knocks) + "，可点击");
        accessibleButton(version);
        version.setOnClickListener(
                v -> {
                    r.move();
                    r.putInt("knocks", knocks + 1);
                    show(r, 5);
                    String[] log = {
                        "更新日志：版本号 +1。蛋壳里好像响了一声。",
                        "更新日志：版本号又 +1。蛋壳已经裂开。",
                        "第三次更新把时钟震了出来。它还停在发布会开始那刻。"
                    };
                    r.say(log[knocks]);
                });
        r.addView(version);
        r.space(12);
        r.addView(r.label("更新日志", 13, r.ink(), true));
        r.addView(r.label("本次更新：版本号 +1。彩蛋工程师已下班。", 15, r.ink(), false));
    }

    private static final class Egg extends View {
        private final PlayRoom r;
        private final int knocks;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Egg(PlayRoom r, int knocks) {
            super(r.getContext());
            this.r = r;
            this.knocks = knocks;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas c) {
            c.save();
            c.scale(getWidth() / 112f, getHeight() / 78f);
            Path egg = new Path();
            egg.moveTo(17, 53);
            egg.cubicTo(14, 33, 34, 7, 56, 7);
            egg.cubicTo(76, 7, 99, 36, 95, 55);
            egg.cubicTo(91, 76, 22, 77, 17, 53);
            egg.close();
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(r.ink());
            p.setStrokeWidth(3);
            c.drawLine(34, 18, 28, 7, p);
            c.drawLine(78, 18, 84, 7, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(r.green());
            c.drawPath(egg, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(r.ink());
            c.drawPath(egg, p);
            p.setStyle(Paint.Style.FILL);
            c.drawOval(35, 36, 42, 46, p);
            c.drawOval(69, 36, 76, 46, p);
            if (knocks > 0) {
                p.setStyle(Paint.Style.STROKE);
                Path crack = new Path();
                crack.moveTo(54, 7);
                crack.lineTo(51, 17);
                crack.lineTo(58, 17);
                crack.lineTo(53, knocks == 1 ? 23 : 30);
                if (knocks > 1) {
                    crack.lineTo(60, 29);
                    crack.lineTo(56, 37);
                }
                c.drawPath(crack, p);
            }
            c.restore();
        }
    }

    private static void clock(PlayRoom r) {
        boolean minutes = r.getInt("part", 0) == 1;
        int hour = r.getInt("hour", 12), minute = r.getInt("minute", 0);
        r.addView(r.label("打卡簿：彩蛋九点上班，每天准时迟到一刻钟。", 15, r.ink(), true));
        r.space(8);
        LinearLayout time = r.row();
        time.setGravity(Gravity.CENTER);
        Button hours =
                timeChip(
                        r,
                        hour,
                        "时",
                        !minutes,
                        () -> {
                            r.putInt("part", 0);
                            show(r, 5);
                        });
        Button mins =
                timeChip(
                        r,
                        minute,
                        "分",
                        minutes,
                        () -> {
                            r.putInt("part", 1);
                            show(r, 5);
                        });
        time.addView(hours, r.d(64), r.d(48));
        time.addView(centered(r, ":", 24, r.ink(), true), r.d(20), WRAP);
        time.addView(mins, r.d(64), r.d(48));
        r.addView(time);
        ClockFace face = new ClockFace(r, minutes, minutes ? minute / 5 : hour % 12);
        r.addView(face, MATCH, r.d(248));
        r.addView(
                r.button(
                        "敲壳验收",
                        () -> {
                            r.move();
                            if (hour == 9 && minute == 15) r.win();
                            else r.say("壳里传出呼噜声。再查一眼打卡簿，把钟拨到它到岗的时间。");
                        }));
    }

    private static Button timeChip(
            PlayRoom r, int value, String unit, boolean active, Runnable action) {
        Button b = r.button(two(value), action);
        mono(b);
        b.setTextSize(23);
        b.setTextColor(r.ink());
        b.setBackground(r.shape(active ? r.green() : WHITE, 10, r.ink(), active ? 2 : 1));
        b.setPadding(0, 0, 0, 0);
        b.setSelected(active);
        b.setContentDescription(value + " " + unit + "，切换编辑" + (unit.equals("时") ? "小时" : "分钟"));
        return b;
    }

    private static String two(int value) {
        return String.format(Locale.ROOT, "%02d", value);
    }

    private static final class ClockFace extends FrameLayout {
        private final PlayRoom r;
        private final boolean minutes;
        private final int selected;
        private int preview;
        private float downX, downY;
        private boolean canDrag, dragging;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Button[] ticks = new Button[12];

        ClockFace(PlayRoom r, boolean minutes, int selected) {
            super(r.getContext());
            this.r = r;
            this.minutes = minutes;
            this.selected = selected;
            preview = selected;
            setWillNotDraw(false);
            for (int i = 0; i < ticks.length; i++) {
                int value = minutes ? i * 5 : i == 0 ? 12 : i;
                Button tick = new Button(getContext());
                tick.setText(minutes ? two(value) : String.valueOf(value));
                tick.setTextSize(15);
                tick.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                tick.setTextColor(r.ink());
                tick.setPadding(0, 0, 0, 0);
                tick.setMinWidth(0);
                tick.setMinimumWidth(0);
                tick.setMinHeight(0);
                tick.setMinimumHeight(0);
                tick.setStateListAnimator(null);
                tick.setBackground(ripple(r));
                tick.setContentDescription(value + (minutes ? " 分" : " 时"));
                tick.setSelected(i == selected);
                final int index = i;
                tick.setOnClickListener(v -> chooseTick(index));
                addView(tick);
                ticks[i] = tick;
            }
        }

        private void chooseTick(int index) {
            int value = minutes ? index * 5 : index == 0 ? 12 : index;
            r.move();
            r.putInt(minutes ? "minute" : "hour", value);
            if (!minutes) r.putInt("part", 1);
            show(r, 5);
            r.say(minutes ? "分针已归位。可以敲壳了。" : "时针已归位。再拨分针。");
        }

        private float radius() {
            return Math.max(0, Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f - r.d(25));
        }

        private int tickAt(float px, float py) {
            double angle = Math.atan2(px - getWidth() / 2f, getHeight() / 2f - py);
            return ((int) Math.round(angle * 6 / Math.PI) + 12) % 12;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                canDrag =
                        Math.hypot(downX - getWidth() / 2f, downY - getHeight() / 2f)
                                <= radius() + r.d(20);
                dragging = false;
                if (canDrag) getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_MOVE && canDrag) {
                dragging = Math.hypot(event.getX() - downX, event.getY() - downY) > r.d(6);
                return dragging;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!canDrag) return super.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    if (Math.hypot(event.getX() - downX, event.getY() - downY) > r.d(6)) {
                        dragging = true;
                        preview = tickAt(event.getX(), event.getY());
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (dragging) chooseTick(tickAt(event.getX(), event.getY()));
                    break;
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    preview = selected;
                    invalidate();
                    break;
            }
            return true;
        }

        private int tickHalfSize() {
            return Math.min(r.d(24), Math.round(radius() * .255f));
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec, heightSpec);
            int spec = MeasureSpec.makeMeasureSpec(2 * tickHalfSize(), MeasureSpec.EXACTLY);
            for (Button tick : ticks) tick.measure(spec, spec);
        }

        private float x(int i) {
            return (float) (getWidth() / 2f + radius() * Math.sin(i * Math.PI / 6));
        }

        private float y(int i) {
            return (float) (getHeight() / 2f - radius() * Math.cos(i * Math.PI / 6));
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int right, int bottom) {
            int half = tickHalfSize();
            for (int i = 0; i < ticks.length; i++) {
                int cx = Math.round(x(i)), cy = Math.round(y(i));
                ticks[i].layout(cx - half, cy - half, cx + half, cy + half);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, rim = radius() + r.d(17);
            p.setStyle(Paint.Style.FILL);
            p.setColor(PISTACHIO);
            canvas.drawCircle(cx, cy, rim, p);
            p.setColor(r.ink());
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(r.d(1));
            canvas.drawCircle(cx, cy, rim, p);
            p.setStrokeWidth(r.d(2));
            canvas.drawLine(cx, cy, x(preview), y(preview), p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, r.d(4), p);
            p.setColor(r.green());
            canvas.drawCircle(x(preview), y(preview), r.d(19), p);
            p.setColor(r.ink());
            p.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(x(preview), y(preview), r.d(19), p);
        }
    }
}
