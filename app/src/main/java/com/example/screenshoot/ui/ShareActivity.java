package com.example.screenshoot.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.screenshoot.util.FileDeleteUtils;

import java.io.File;
import java.util.ArrayList;

public class ShareActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "extra_path";
    public static final String EXTRA_URI = "extra_uri";
    private static final int REQ_SHARE = 1001;
    private static final int REQ_DELETE = 1002;
    private String screenshotPath;
    private Uri contentUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenshotPath = getIntent().getStringExtra(EXTRA_PATH);
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr != null) {
            contentUri = Uri.parse(uriStr);
        }

        if (screenshotPath == null && contentUri == null) {
            finish();
            return;
        }

        Uri shareUri;
        if (contentUri != null) {
            shareUri = contentUri;
        } else {
            File file = new File(screenshotPath);
            shareUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");
        share.putExtra(Intent.EXTRA_STREAM, shareUri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(Intent.createChooser(share, "Share screenshot"), REQ_SHARE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SHARE) {
            if (resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED) {
                finish();
                return;
            }

            // On Android 11+ ask the system to delete this image from MediaStore with user consent.
            if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    ArrayList<Uri> targets = new ArrayList<>();
                    targets.add(contentUri);
                    IntentSender sender = MediaStore.createDeleteRequest(
                            getContentResolver(), targets
                    ).getIntentSender();
                    startIntentSenderForResult(sender, REQ_DELETE, null, 0, 0, 0, null);
                    return; // wait for REQ_DELETE result
                } catch (Exception ignored) {
                    // Fall through to best-effort direct delete below.
                }
            }

            performDirectDelete();
            finish();
        } else if (requestCode == REQ_DELETE) {
            // System delete dialog finished; also run direct cleanup in case a file still exists.
            performDirectDelete();
            finish();
        }
    }

    private void performDirectDelete() {
        if (contentUri != null) {
            FileDeleteUtils.deleteImageByUri(getApplicationContext(), contentUri);
        }
        if (screenshotPath != null) {
            FileDeleteUtils.deleteImage(getApplicationContext(), screenshotPath);
        }
    }
}

