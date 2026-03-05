package com.example.screenshoot;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.screenshoot.overlay.OverlayManager;
import com.example.screenshoot.service.ScreenshotService;
import com.example.screenshoot.util.SoundPrefs;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> imagesPermissionLauncher;
    private OverlayManager overlayManager;
    private VideoView bgVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgVideo = findViewById(R.id.bgVideo);
        Button btnStart = findViewById(R.id.btnStartService);
        Button btnTestBubble = findViewById(R.id.btnTestBubble);
        Button btnSoundSettings = findViewById(R.id.btnSoundSettings);

        initBackgroundVideo();

        overlayManager = new OverlayManager(this);

        imagesPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        checkOverlayPermissionAndStartService();
                    } else {
                        Toast.makeText(this, "Image permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnStart.setOnClickListener(v -> requestPermissionsAndStart());
        btnTestBubble.setOnClickListener(v -> {
            // Test bubble without screenshot
            overlayManager.showBubble("/test/path", null);
        });
        btnSoundSettings.setOnClickListener(v -> showSoundSettings());
    }

    private void initBackgroundVideo() {
        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bg_loop);
            bgVideo.setVideoURI(uri);
            bgVideo.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                bgVideo.start();
            });
        } catch (Exception ignored) {
        }
    }

    private void requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagesPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            imagesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Please enable 'Display over other apps' permission, then come back and tap Start again", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Overlay permission OK", Toast.LENGTH_SHORT).show();
                startScreenshotService();
            }
        } else {
            startScreenshotService();
        }
    }

    private void startScreenshotService() {
        Intent serviceIntent = new Intent(this, ScreenshotService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Screenshot service started", Toast.LENGTH_SHORT).show();
    }

    private void showSoundSettings() {
        String[] modes = {"Off", "Soft", "Loud"};

        // Countdown sound dialog
        String countdown = SoundPrefs.getCountdownSound(this);
        int countdownIndex = "off".equals(countdown) ? 0 : "loud".equals(countdown) ? 2 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Countdown sound (last 10 sec)")
                .setSingleChoiceItems(modes, countdownIndex, (dialog, which) -> {
                    String val = which == 0 ? "off" : which == 1 ? "soft" : "loud";
                    SoundPrefs.setCountdownSound(this, val);
                })
                .setPositiveButton("Next", (d, w) -> showDeleteSoundDialog())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDeleteSoundDialog() {
        String[] modes = {"Off", "Soft", "Loud"};
        String delete = SoundPrefs.getDeleteSound(this);
        int deleteIndex = "off".equals(delete) ? 0 : "loud".equals(delete) ? 2 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Delete sound")
                .setSingleChoiceItems(modes, deleteIndex, (dialog, which) -> {
                    String val = which == 0 ? "off" : which == 1 ? "soft" : "loud";
                    SoundPrefs.setDeleteSound(this, val);
                })
                .setPositiveButton("Done", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bgVideo != null && !bgVideo.isPlaying()) {
            bgVideo.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bgVideo != null) {
            bgVideo.pause();
        }
    }
}

