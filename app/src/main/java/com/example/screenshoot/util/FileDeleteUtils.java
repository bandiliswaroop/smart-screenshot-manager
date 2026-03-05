package com.example.screenshoot.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

public class FileDeleteUtils {

    private static final String TAG = "FileDeleteUtils";

    public static void deleteImage(Context context, String path) {
        if (path == null) return;

        boolean fileDeleted = false;
        try {
            File file = new File(path);
            if (file.exists()) {
                fileDeleted = file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "File delete error", e);
        }

        // Also try deleting via MediaStore by file name (for scoped storage devices)
        try {
            String fileName = new File(path).getName();
            int rows = context.getContentResolver().delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{fileName}
            );
            Log.d(TAG, "MediaStore delete rows=" + rows + " for name=" + fileName);
        } catch (Exception e) {
            Log.e(TAG, "MediaStore delete error", e);
        }

        // Ask media scanner to rescan so gallery updates its view
        if (fileDeleted) {
            try {
                MediaScannerConnection.scanFile(
                        context,
                        new String[]{path},
                        null,
                        (scanPath, uri) -> Log.d(TAG, "Rescanned after delete: " + scanPath + " uri=" + uri)
                );
            } catch (Exception e) {
                Log.e(TAG, "MediaScanner error", e);
            }
        }

        Log.d(TAG, "deleteImage path=" + path + " fileDeleted=" + fileDeleted);
    }

    /** Prefer this on Android 10+ when you have the MediaStore content URI. */
    public static void deleteImageByUri(Context context, android.net.Uri uri) {
        if (uri == null) return;
        try {
            int rows = context.getContentResolver().delete(uri, null, null);
            Log.d(TAG, "deleteImageByUri uri=" + uri + " rows=" + rows);
        } catch (Exception e) {
            Log.e(TAG, "deleteImageByUri error", e);
        }
    }
}

