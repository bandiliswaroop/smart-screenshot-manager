package com.example.screenshoot.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.screenshoot.R;
import com.example.screenshoot.overlay.MiniDisplayOverlay;
import com.example.screenshoot.util.SoundPrefs;

/**
 * Full-screen activity that shows the screenshot over the lockscreen
 * for a limited time so it can be scanned without unlocking.
 */
public class DisplayActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_END_TIME = "extra_end_time";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timerText;
    private long endTimeMillis;
    private Uri imageUri;
    private boolean allowMiniOnStop = true;
    private ToneGenerator toneGenerator;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (updateTimer()) {
                handler.postDelayed(this, 1000L);
            } else {
                // Time finished; do not create mini overlay after this.
                allowMiniOnStop = false;
                finish();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow this activity to appear over the lockscreen and wake it up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        setContentView(R.layout.view_screenshot_display);

        ImageView imageView = findViewById(R.id.display_image);
        timerText = findViewById(R.id.display_timer);
        View closeBtn = findViewById(R.id.display_close);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) {
            finish();
            return;
        }
        imageUri = Uri.parse(uriStr);
        imageView.setImageURI(imageUri);

        long defaultEnd = System.currentTimeMillis() + 15 * 60L * 1000L;
        endTimeMillis = getIntent().getLongExtra(EXTRA_END_TIME, defaultEnd);

        try {
            // Use ALARM stream so beeps are more audible than notifications.
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (Exception ignored) {
            toneGenerator = null;
        }

        // Only the explicit CLOSE button will close the screen; tapping the image/background
        // will no longer dismiss it accidentally. Closing via this button should not show mini.
        closeBtn.setOnClickListener(v -> {
            allowMiniOnStop = false;
            finish();
        });

        updateTimer();
        handler.postDelayed(tickRunnable, 1000L);
    }

    /**
     * @return true if there is still time left and we should continue ticking
     */
    private boolean updateTimer() {
        if (timerText == null) return false;
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            timerText.setText("Time left: 0:00 (for scanning) — closing…");
            return false;
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

        // When only 10 seconds or less remain, play a short tick every second, if enabled.
        if (totalSeconds <= 10 && totalSeconds > 0 && toneGenerator != null) {
            String mode = SoundPrefs.getCountdownSound(getApplicationContext());
            if (!"off".equals(mode)) {
                int vol = "loud".equals(mode) ? 100 : 60;
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, vol);
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tickRunnable);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // If user navigates away (Home/Recents/Back) while time is still running,
        // show a small floating mini window over other apps (unlocked only).
        // We only skip this when timer finished or CLOSE was pressed
        // (those paths set allowMiniOnStop to false).
        if (allowMiniOnStop && imageUri != null) {
            MiniDisplayOverlay.show(getApplicationContext(), imageUri, endTimeMillis);
        }
    }
}

