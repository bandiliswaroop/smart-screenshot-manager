package com.example.screenshoot.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.screenshoot.ui.OcrResultActivity;
import com.example.screenshoot.ui.DisplayActivity;
import com.example.screenshoot.ui.ShareActivity;
import com.example.screenshoot.work.DeleteScreenshotWorker;

import java.util.concurrent.TimeUnit;

public class OverlayManager {

    private static final String TAG = "OverlayManager";
    private final Context context;
    private final WindowManager windowManager;
    private OverlayBubbleView bubbleView;
    private ScreenshotDisplayOverlay displayOverlay;

    public OverlayManager(Context context) {
        this.context = context.getApplicationContext();
        windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    /** @param contentUri optional; use for share/delete on Android 10+ when path may be invalid */
    public void showBubble(String screenshotPath, Uri contentUri) {
        // Check overlay permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.e(TAG, "Overlay permission not granted!");
                Toast.makeText(context, "Overlay permission required! Enable 'Display over other apps' in settings", Toast.LENGTH_LONG).show();
                return;
            }
        }

        removeBubble();

        bubbleView = new OverlayBubbleView(context, screenshotPath, contentUri, new OverlayBubbleView.ActionListener() {
            @Override
            public void onKeepForHour(String path, Uri uri) {
                scheduleDeleteAfterDelay(path, uri, 1, TimeUnit.HOURS);
                Toast.makeText(context, "🕒 Will auto-delete in 1 hour", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteAfterShare(String path, Uri uri) {
                Intent intent = new Intent(context, ShareActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ShareActivity.EXTRA_PATH, path);
                if (uri != null) intent.putExtra(ShareActivity.EXTRA_URI, uri.toString());
                context.startActivity(intent);
            }

            @Override
            public void onDisplayOnScreen(String path, Uri uri, int durationMinutes) {
                // When user chooses to display for scanning, hide the bubble/smart actions
                // so only the full-screen ticket is visible.
                removeBubble();
                showScreenshotOnScreen(
                        uri != null ? uri : (path != null ? Uri.fromFile(new java.io.File(path)) : null),
                        durationMinutes
                );
            }

            @Override
            public void onExtractText(String path) {
                Intent intent = new Intent(context, OcrResultActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(OcrResultActivity.EXTRA_PATH, path);
                context.startActivity(intent);
            }

            @Override
            public void onDismiss() {
                removeBubble();
            }
        });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 50;
        params.y = 200;

        try {
            windowManager.addView(bubbleView, params);
            Log.d(TAG, "Bubble added successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add bubble view", e);
            Toast.makeText(context, "Failed to show bubble: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleDeleteAfterDelay(String path, Uri contentUri, long delay, TimeUnit unit) {
        WorkRequest request = new OneTimeWorkRequest.Builder(DeleteScreenshotWorker.class)
                .setInputData(DeleteScreenshotWorker.createInput(path,
                        contentUri != null ? contentUri.toString() : null))
                .setInitialDelay(delay, unit)
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    /** Show screenshot as full-screen overlay for scanning (e.g. metro ticket). Stays for durationMinutes. */
    public void showScreenshotOnScreen(Uri imageUri, int durationMinutes) {
        if (imageUri == null) {
            Toast.makeText(context, "No image to display", Toast.LENGTH_SHORT).show();
            return;
        }
        // Use a dedicated Activity that can appear over the lockscreen,
        // instead of a raw overlay window (many devices block overlays on keyguard).
        Intent intent = new Intent(context, DisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(DisplayActivity.EXTRA_URI, imageUri.toString());
        long endTime = System.currentTimeMillis() + durationMinutes * 60L * 1000L;
        intent.putExtra(DisplayActivity.EXTRA_END_TIME, endTime);
        context.startActivity(intent);
        Toast.makeText(context, "🔒 Showing for " + durationMinutes + " min (for scanning)", Toast.LENGTH_SHORT).show();
    }

    private void removeDisplayOverlay() {
        if (displayOverlay != null) {
            try {
                windowManager.removeView(displayOverlay);
            } catch (Exception e) {
                Log.w(TAG, "Error removing display overlay", e);
            }
            displayOverlay = null;
        }
    }

    public void removeBubble() {
        if (bubbleView != null) {
            windowManager.removeView(bubbleView);
            bubbleView = null;
        }
    }

    public void destroy() {
        removeBubble();
        removeDisplayOverlay();
    }
}

