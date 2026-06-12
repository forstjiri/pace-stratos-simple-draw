package cz.kresliciapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;

public class DrawActivity extends Activity {
    private static final float EDGE_BLOCK_DP = 16f;
    private static final float EDGE_SWIPE_DISTANCE_DP = 40f;
    private static final float EDGE_VERTICAL_SLOP_DP = 30f;

    private DrawView drawView;
    private float edgeStartX;
    private float edgeStartY;
    private boolean edgeCandidate;
    private boolean edgeBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        attrs.flags &= ~0xF0000000;
        window.setAttributes(attrs);

        drawView = new DrawView(this);
        drawView.setSaveFile(new File(getFilesDir(), "drawing.png"));
        FrameLayout container = new FrameLayout(this);
        container.addView(drawView);
        setContentView(container);
        applyImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @Override
    protected void onPause() {
        drawView.commitCurrentStrokeAndSave();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float edgeSize = dp(EDGE_BLOCK_DP);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                edgeStartX = ev.getX();
                edgeStartY = ev.getY();
                edgeBlocked = false;
                edgeCandidate = edgeStartX <= edgeSize || edgeStartX >= getWindow().getDecorView().getWidth() - edgeSize;
                break;
            case MotionEvent.ACTION_MOVE:
                if (edgeCandidate && !edgeBlocked) {
                    float dx = Math.abs(ev.getX() - edgeStartX);
                    float dy = Math.abs(ev.getY() - edgeStartY);
                    if (dx >= dp(EDGE_SWIPE_DISTANCE_DP) && dy <= dp(EDGE_VERTICAL_SLOP_DP)) {
                        edgeBlocked = true;
                        return true;
                    }
                }
                if (edgeBlocked) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (edgeBlocked) {
                    edgeCandidate = false;
                    edgeBlocked = false;
                    return true;
                }
                edgeCandidate = false;
                edgeBlocked = false;
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void applyImmersiveMode() {
        drawView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
