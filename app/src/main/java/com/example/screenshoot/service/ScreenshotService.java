package com.example.screenshoot.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.screenshoot.R;
import com.example.screenshoot.overlay.OverlayManager;

public class ScreenshotService extends Service {

    private static final String CHANNEL_ID = "screenshot_service_channel";
    private ContentObserver screenshotObserver;
    private OverlayManager overlayManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Screenshot Manager")
                .setContentText("Listening for screenshots…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        overlayManager = new OverlayManager(this);
        Handler handler = new Handler();
        ContentResolver resolver = getContentResolver();
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        screenshotObserver = new ScreenshotObserver(handler, getApplicationContext(), resolver, imagesUri, (path, contentUri) -> {
            Toast.makeText(getApplicationContext(), "📸 Screenshot captured", Toast.LENGTH_SHORT).show();
            overlayManager.showBubble(path, contentUri);
        });

        resolver.registerContentObserver(imagesUri, true, screenshotObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenshotObserver != null) {
            getContentResolver().unregisterContentObserver(screenshotObserver);
        }
        if (overlayManager != null) {
            overlayManager.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, "Screenshot Listener",
                            NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Listens for new screenshots");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

