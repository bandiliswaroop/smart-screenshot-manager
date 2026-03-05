package com.example.screenshoot.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.screenshoot.R;
import com.example.screenshoot.ui.DisplayActivity;

/**
 * Small floating thumbnail that stays on top of other apps while unlocked.
 * Tapping it re-opens the full DisplayActivity, until the timer ends.
 */
public class MiniDisplayOverlay extends FrameLayout {

    private static MiniDisplayOverlay instance;

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long endTimeMillis;
    private Uri imageUri;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (updateTimer()) {
                handler.postDelayed(this, 1000L);
            } else {
                removeSelf();
            }
        }
    };

    public static void show(Context context, Uri imageUri, long endTimeMillis) {
        Context appCtx = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(appCtx)) {
                return;
            }
        }
        if (instance != null) {
            instance.update(imageUri, endTimeMillis);
            return;
        }
        instance = new MiniDisplayOverlay(appCtx, imageUri, endTimeMillis);
        instance.addToWindow();
    }

    public static void hide(Context context) {
        if (instance != null) {
            instance.removeSelf();
        }
    }

    private MiniDisplayOverlay(Context context, Uri imageUri, long endTimeMillis) {
        super(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        this.params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        // Use TOP|START so x/y behave like normal screen coordinates.
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 200;

        initView(context, imageUri, endTimeMillis);
    }

    private void initView(Context context, Uri uri, long endTime) {
        LayoutInflater.from(context).inflate(R.layout.view_mini_display_overlay, this, true);
        ImageView imageView = findViewById(R.id.mini_image);
        TextView timerView = findViewById(R.id.mini_timer);

        this.imageUri = uri;
        this.endTimeMillis = endTime;

        if (uri != null) {
            imageView.setImageURI(uri);
        }

        setOnClickListener(v -> {
            // Re-open the full DisplayActivity
            Context appCtx = getContext().getApplicationContext();
            Intent intent = new Intent(appCtx, DisplayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(DisplayActivity.EXTRA_URI, imageUri.toString());
            intent.putExtra(DisplayActivity.EXTRA_END_TIME, endTimeMillis);
            appCtx.startActivity(intent);
            removeSelf();
        });

        setOnTouchListener(new OnTouchListener() {
            private int startX;
            private int startY;
            private float downRawX;
            private float downRawY;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        dragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float moveRawX = event.getRawX();
                        float moveRawY = event.getRawY();
                        int dx = (int) (moveRawX - downRawX);
                        int dy = (int) (moveRawY - downRawY);

                        // If the finger moved more than a small threshold, treat as drag.
                        if (!dragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                            dragging = true;
                        }

                        if (dragging) {
                            params.x = startX + dx;
                            params.y = startY + dy;
                            windowManager.updateViewLayout(MiniDisplayOverlay.this, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            // Consider this a click (no significant movement)
                            performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        updateTimerText(timerView);
        handler.postDelayed(tickRunnable, 1000L);
    }

    private void addToWindow() {
        try {
            windowManager.addView(this, params);
        } catch (Exception ignored) {
        }
    }

    private void update(Uri uri, long endTime) {
        this.imageUri = uri;
        this.endTimeMillis = endTime;
    }

    private boolean updateTimer() {
        TextView timerView = findViewById(R.id.mini_timer);
        if (timerView == null) return false;
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            timerView.setText("0:00");
            return false;
        }
        long totalSeconds = remaining / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        timerView.setText(String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds));
        return true;
    }

    private void updateTimerText(TextView timerView) {
        long remaining = endTimeMillis - System.currentTimeMillis();
        long totalSeconds = Math.max(0, remaining / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        timerView.setText(String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds));
    }

    private void removeSelf() {
        handler.removeCallbacks(tickRunnable);
        try {
            windowManager.removeView(this);
        } catch (Exception ignored) {
        }
        if (instance == this) {
            instance = null;
        }
    }
}

