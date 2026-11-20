package dev.materialmischief.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Resolution-independent artwork for the case files and their runaway specimen. */
public final class MischiefArt extends View {
    private static final int COVER_GREEN = 0xFFC2F675;

    private final Drawable artwork;

    /** -1 is the cover, 0 through 5 are case emblems, and 6 is the recovered egg. */
    public MischiefArt(Context context, int kind) {
        super(context);
        artwork = context.getDrawable(resourceForKind(kind)).mutate();
        if (kind == -1) {
            setBackgroundColor(COVER_GREEN);
        }
        // The adjacent case title or result already names each illustration.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private static int resourceForKind(int kind) {
        switch (kind) {
            case -1:
                return R.drawable.mm_art_cover;
            case 0:
                return R.drawable.mm_art_switch;
            case 1:
                return R.drawable.mm_art_slider;
            case 2:
                return R.drawable.mm_art_snackbar;
            case 3:
                return R.drawable.mm_art_fab;
            case 4:
                return R.drawable.mm_art_notifications;
            case 5:
                return R.drawable.mm_art_about;
            case 6:
                return R.drawable.mm_art_egg;
            default:
                throw new IllegalArgumentException("Unknown artwork " + kind);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int width = resolveSize(artwork.getIntrinsicWidth() + horizontalPadding, widthMeasureSpec);
        int height = resolveSize(artwork.getIntrinsicHeight() + verticalPadding, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }
        int artWidth = artwork.getIntrinsicWidth();
        int artHeight = artwork.getIntrinsicHeight();
        float scale =
                Math.min((float) availableWidth / artWidth, (float) availableHeight / artHeight);
        int width = Math.round(artWidth * scale);
        int height = Math.round(artHeight * scale);
        int x = getPaddingLeft() + (availableWidth - width) / 2;
        int y = getPaddingTop() + (availableHeight - height) / 2;
        artwork.setBounds(x, y, x + width, y + height);
        artwork.draw(canvas);
    }
}
