package com.example.screenshoot.overlay;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.screenshoot.R;

/**
 * Full-screen overlay that shows a screenshot for a set duration (e.g. for scanning a ticket).
 * User can close early by tapping the button.
 */
public class ScreenshotDisplayOverlay extends FrameLayout {

    private final int durationMinutes;
    private final Runnable onRemove;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable removeRunnable;
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            // Schedule next tick if still active
            if (removeRunnable != null) {
                handler.postDelayed(this, 1000L);
            }
        }
    };
    private TextView timerText;
    private long endTimeMillis;

    public ScreenshotDisplayOverlay(Context context, Uri imageUri, int durationMinutes, Runnable onRemove) {
        super(context);
        this.durationMinutes = durationMinutes;
        this.onRemove = onRemove;
        init(context, imageUri);
    }

    private void init(Context context, Uri imageUri) {
        LayoutInflater.from(context).inflate(R.layout.view_screenshot_display, this, true);
        ImageView imageView = findViewById(R.id.display_image);
        timerText = findViewById(R.id.display_timer);
        View closeBtn = findViewById(R.id.display_close);

        if (imageUri != null) {
            imageView.setImageURI(imageUri);
        }

        // Compute end time and start countdown
        endTimeMillis = System.currentTimeMillis() + durationMinutes * 60L * 1000L;
        updateTimer();
        handler.post(tickRunnable);

        View root = findViewById(R.id.display_root);
        root.setOnClickListener(v -> remove());
        closeBtn.setOnClickListener(v -> remove());

        removeRunnable = () -> remove();
        handler.postDelayed(removeRunnable, durationMinutes * 60L * 1000L);
    }

    private void updateTimer() {
        if (timerText == null) return;
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            timerText.setText("Time left: 0:00 (scanning) — closing…");
            // Let the delayed remove() handle actual removal shortly
            return;
        }
        long totalSeconds = remaining / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        String text = String.format(
                java.util.Locale.getDefault(),
                "Time left: %d:%02d (for scanning) — tap CLOSE to exit",
                minutes,
                seconds
        );
        timerText.setText(text);
    }

    private void remove() {
        handler.removeCallbacks(removeRunnable);
        handler.removeCallbacks(tickRunnable);
        removeRunnable = null;
        handler.post(() -> {
            if (onRemove != null) onRemove.run();
        });
    }
}
