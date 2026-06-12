package cz.kresliciapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DrawView extends View {
    private static final int[] COLORS = {
            Color.WHITE,
            Color.parseColor("#FF5A5F"),
            Color.parseColor("#FF8C42"),
            Color.parseColor("#FFD166"),
            Color.parseColor("#B8F74A"),
            Color.parseColor("#50FA7B"),
            Color.parseColor("#00F5D4"),
            Color.parseColor("#4CC9F0"),
            Color.parseColor("#4895EF"),
            Color.parseColor("#4361EE"),
            Color.parseColor("#9B5DE5"),
            Color.parseColor("#F15BB5"),
            Color.parseColor("#FF66C4")
    };
    private static final int ERASER_INDEX = COLORS.length;
    private static final int[] STROKE_WIDTHS = {3, 7, 13};
    private static final int VISIBLE_TOOL_COUNT = 5;
    private static final int BG_COLOR = Color.BLACK;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path currentPath = new Path();
    private File saveFile;

    private float centerX;
    private float centerY;
    private float drawRadius;
    private float toolbarTop;
    private float toolbarBottom;
    private float toolbarLeft;
    private float toolbarRight;
    private float widthButtonWidth;

    private float lastX;
    private float lastY;
    private boolean drawing;
    private boolean toolbarTracking;
    private boolean toolbarDragged;
    private float toolbarDownX;
    private float toolbarDownY;

    private int selectedColorIndex = 0;
    private int selectedWidthIndex = 1;
    private int toolbarPage = 0;

    public DrawView(Context context) {
        super(context);
        backgroundPaint.setColor(BG_COLOR);
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        drawRadius = Math.min(w, h) / 2f - dp(6);
        widthButtonWidth = dp(34);
        toolbarLeft = dp(10);
        toolbarRight = w - dp(10);
        toolbarBottom = h - dp(10);
        toolbarTop = toolbarBottom - dp(44);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawRect(0, 0, w, h, backgroundPaint);
        loadFromFile();
    }

    public void setSaveFile(File file) {
        saveFile = file;
        if (bitmap != null) {
            loadFromFile();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        canvas.drawBitmap(bitmap, 0, 0, null);
        if (drawing) {
            drawStroke(canvas, currentPath, selectedColorIndex == ERASER_INDEX, true);
        }
        drawToolbar(canvas);
    }

    private void drawToolbar(Canvas canvas) {
        Paint panel = new Paint(Paint.ANTI_ALIAS_FLAG);
        panel.setColor(Color.argb(220, 20, 20, 24));
        RectF panelRect = new RectF(toolbarLeft, toolbarTop, toolbarRight, toolbarBottom);
        canvas.drawRoundRect(panelRect, dp(20), dp(20), panel);

        float buttonsLeft = toolbarRight - widthButtonWidth * STROKE_WIDTHS.length;
        float colorLeft = toolbarLeft + dp(8);
        float colorRight = buttonsLeft - dp(6);
        float slotWidth = (colorRight - colorLeft) / VISIBLE_TOOL_COUNT;
        float chipRadius = Math.min(dp(11), slotWidth * 0.32f);
        float centerY = (toolbarTop + toolbarBottom) / 2f;

        for (int i = 0; i < VISIBLE_TOOL_COUNT; i++) {
            int toolIndex = toolbarPage * VISIBLE_TOOL_COUNT + i;
            if (toolIndex > ERASER_INDEX) {
                break;
            }
            float chipCenterX = colorLeft + slotWidth * i + slotWidth / 2f;
            drawToolChip(canvas, chipCenterX, centerY, chipRadius, toolIndex);
        }

        if (toolbarPage > 0) {
            Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrow.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawCircle(toolbarLeft + dp(8), centerY, dp(2), arrow);
        }
        if (toolbarPage < getMaxToolbarPage()) {
            Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrow.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawCircle(buttonsLeft - dp(8), centerY, dp(2), arrow);
        }

        for (int i = 0; i < STROKE_WIDTHS.length; i++) {
            drawWidthButton(canvas, buttonsLeft + i * widthButtonWidth, toolbarTop, widthButtonWidth,
                    toolbarBottom - toolbarTop, i);
        }
    }

    private void drawToolChip(Canvas canvas, float x, float y, float radius, int toolIndex) {
        Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);
        chip.setStyle(Paint.Style.FILL);
        if (toolIndex == ERASER_INDEX) {
            chip.setColor(Color.LTGRAY);
            canvas.drawCircle(x, y, radius, chip);
            chip.setStyle(Paint.Style.STROKE);
            chip.setColor(Color.BLACK);
            chip.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(x, y, radius * 0.65f, chip);
        } else {
            chip.setColor(COLORS[toolIndex]);
            canvas.drawCircle(x, y, radius, chip);
        }

        if (selectedColorIndex == toolIndex) {
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(2));
            ring.setColor(Color.WHITE);
            canvas.drawCircle(x, y, radius + dp(4), ring);
        }
    }

    private void drawWidthButton(Canvas canvas, float left, float top, float width, float height, int index) {
        Paint button = new Paint(Paint.ANTI_ALIAS_FLAG);
        button.setColor(index == selectedWidthIndex ? Color.argb(255, 60, 62, 74) : Color.argb(180, 36, 38, 46));
        RectF rect = new RectF(left + dp(2), top + dp(4), left + width - dp(2), top + height - dp(4));
        canvas.drawRoundRect(rect, dp(10), dp(10), button);

        Paint sample = new Paint(Paint.ANTI_ALIAS_FLAG);
        sample.setColor(Color.WHITE);
        sample.setStyle(Paint.Style.STROKE);
        sample.setStrokeCap(Paint.Cap.ROUND);
        sample.setStrokeWidth(STROKE_WIDTHS[index]);
        float cy = rect.centerY();
        canvas.drawLine(rect.left + dp(8), cy, rect.right - dp(8), cy, sample);
    }

    private boolean isInToolbar(float x, float y) {
        return x >= toolbarLeft && x <= toolbarRight && y >= toolbarTop && y <= toolbarBottom;
    }

    private boolean isInsideDrawCircle(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return dx * dx + dy * dy <= drawRadius * drawRadius && y < toolbarTop - dp(4);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (isInToolbar(x, y)) {
            return handleToolbarTouch(event, x, y);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInsideDrawCircle(x, y)) {
                    drawing = true;
                    lastX = x;
                    lastY = y;
                    currentPath.reset();
                    currentPath.moveTo(x, y);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (drawing && isInsideDrawCircle(x, y)) {
                    currentPath.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drawing) {
                    currentPath.lineTo(x, y);
                    drawStroke(bitmapCanvas, currentPath, selectedColorIndex == ERASER_INDEX, false);
                    currentPath.reset();
                    drawing = false;
                    saveToFile();
                    invalidate();
                }
                return true;
            default:
                return true;
        }
    }

    public void commitCurrentStrokeAndSave() {
        if (drawing) {
            drawStroke(bitmapCanvas, currentPath, selectedColorIndex == ERASER_INDEX, false);
            currentPath.reset();
            drawing = false;
        }
        saveToFile();
    }

    private boolean handleToolbarTouch(MotionEvent event, float x, float y) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                toolbarTracking = true;
                toolbarDragged = false;
                toolbarDownX = x;
                toolbarDownY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (toolbarTracking) {
                    float dx = x - toolbarDownX;
                    float dy = y - toolbarDownY;
                    if (Math.abs(dx) > dp(20) && Math.abs(dx) > Math.abs(dy)) {
                        if (dx < 0 && toolbarPage < getMaxToolbarPage()) {
                            toolbarPage++;
                        } else if (dx > 0 && toolbarPage > 0) {
                            toolbarPage--;
                        }
                        toolbarDragged = true;
                        toolbarDownX = x;
                        toolbarDownY = y;
                        invalidate();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (toolbarTracking && !toolbarDragged) {
                    handleToolbarTap(x, y);
                }
                toolbarTracking = false;
                toolbarDragged = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                toolbarTracking = false;
                toolbarDragged = false;
                return true;
            default:
                return true;
        }
    }

    private void handleToolbarTap(float x, float y) {
        float buttonsLeft = toolbarRight - widthButtonWidth * STROKE_WIDTHS.length;
        if (x >= buttonsLeft) {
            int widthIndex = (int) ((x - buttonsLeft) / widthButtonWidth);
            if (widthIndex >= 0 && widthIndex < STROKE_WIDTHS.length) {
                selectedWidthIndex = widthIndex;
                invalidate();
            }
            return;
        }

        float colorLeft = toolbarLeft + dp(8);
        float colorRight = buttonsLeft - dp(6);
        float slotWidth = (colorRight - colorLeft) / VISIBLE_TOOL_COUNT;
        int slot = (int) ((x - colorLeft) / slotWidth);
        if (slot < 0 || slot >= VISIBLE_TOOL_COUNT) {
            return;
        }
        int toolIndex = toolbarPage * VISIBLE_TOOL_COUNT + slot;
        if (toolIndex <= ERASER_INDEX) {
            selectedColorIndex = toolIndex;
            invalidate();
        }
    }

    private int getMaxToolbarPage() {
        return ERASER_INDEX / VISIBLE_TOOL_COUNT;
    }

    private void drawStroke(Canvas canvas, Path path, boolean erasing, boolean preview) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        int strokeWidth = STROKE_WIDTHS[selectedWidthIndex];
        if (erasing) {
            paint.setColor(BG_COLOR);
            paint.setStrokeWidth(Math.max(strokeWidth + dp(8), dp(12)));
            canvas.drawPath(path, paint);
            return;
        }

        int color = COLORS[selectedColorIndex];
        drawGlowLayer(canvas, path, color, strokeWidth * 4f, 0x40, paint);
        drawGlowLayer(canvas, path, color, strokeWidth * 2f, 0x80, paint);
        drawGlowLayer(canvas, path, color, strokeWidth, preview ? 0xE0 : 0xFF, paint);
    }

    private void drawGlowLayer(Canvas canvas, Path path, int color, float strokeWidth, int alpha, Paint paint) {
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setStrokeWidth(strokeWidth);
        canvas.drawPath(path, paint);
    }

    private void loadFromFile() {
        if (bitmap == null || bitmapCanvas == null) {
            return;
        }
        bitmapCanvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), backgroundPaint);
        if (saveFile == null || !saveFile.exists()) {
            invalidate();
            return;
        }
        Bitmap loaded = BitmapFactory.decodeFile(saveFile.getAbsolutePath());
        if (loaded != null) {
            bitmapCanvas.drawBitmap(loaded, null,
                    new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), null);
            loaded.recycle();
        }
        invalidate();
    }

    private void saveToFile() {
        if (bitmap == null || saveFile == null) {
            return;
        }
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tmpFile = new File(saveFile.getAbsolutePath() + ".tmp");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(tmpFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.flush();
            if (saveFile.exists() && !saveFile.delete()) {
                return;
            }
            tmpFile.renameTo(saveFile);
        } catch (IOException ignored) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
