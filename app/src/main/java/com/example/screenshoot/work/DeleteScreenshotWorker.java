package com.example.screenshoot.work;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.screenshoot.util.FileDeleteUtils;
import com.example.screenshoot.util.SoundPrefs;

public class DeleteScreenshotWorker extends Worker {

    public static final String KEY_PATH = "screenshot_path";
    public static final String KEY_URI = "screenshot_uri";

    public DeleteScreenshotWorker(@NonNull Context context,
                                  @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String path = getInputData().getString(KEY_PATH);
        String uriStr = getInputData().getString(KEY_URI);
        Uri uri = uriStr != null ? Uri.parse(uriStr) : null;

        boolean attempted = false;
        try {
            if (uri != null) {
                FileDeleteUtils.deleteImageByUri(getApplicationContext(), uri);
                attempted = true;
            }
            if (path != null) {
                FileDeleteUtils.deleteImage(getApplicationContext(), path);
                attempted = true;
            }

            if (attempted) {
                playDeleteTone();
                return Result.success();
            } else {
                return Result.failure();
            }
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void playDeleteTone() {
        try {
            String mode = SoundPrefs.getDeleteSound(getApplicationContext());
            if ("off".equals(mode)) return;

            int vol = "loud".equals(mode) ? 100 : 60;
            int tone = "loud".equals(mode)
                    ? ToneGenerator.TONE_PROP_ACK
                    : ToneGenerator.TONE_PROP_BEEP;
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, vol);
            tg.startTone(tone, 200);
        } catch (Exception ignored) {
        }
    }

    public static Data createInput(String path, String uriString) {
        Data.Builder builder = new Data.Builder();
        if (path != null) {
            builder.putString(KEY_PATH, path);
        }
        if (uriString != null) {
            builder.putString(KEY_URI, uriString);
        }
        return builder.build();
    }

    // Backwards-compatible helper if only path is available
    public static Data createInput(String path) {
        return createInput(path, null);
    }
}

