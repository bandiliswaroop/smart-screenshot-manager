package com.example.screenshoot.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

public class ScreenshotObserver extends ContentObserver {

    public interface OnScreenshotListener {
        /** path is for display; contentUri is for share/delete (use this on Android 10+) */
        void onScreenshot(String path, Uri contentUri);
    }

    private static final String TAG = "ScreenshotObserver";

    private final ContentResolver contentResolver;
    private final Context context;
    private final Uri contentUri;
    private final OnScreenshotListener listener;
    private long lastScreenshotTime = 0L;

    public ScreenshotObserver(Handler handler,
                              Context context,
                              ContentResolver resolver,
                              Uri uri,
                              OnScreenshotListener listener) {
        super(handler);
        this.context = context.getApplicationContext();
        this.contentResolver = resolver;
        this.contentUri = uri;
        this.listener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        queryLatestImage();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        queryLatestImage();
    }

    private void queryLatestImage() {
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,           // deprecated on newer Android, may be null
                MediaStore.Images.Media.RELATIVE_PATH,  // e.g. "DCIM/Screenshots/"
                MediaStore.Images.Media.DISPLAY_NAME,   // file name
                MediaStore.Images.Media.DATE_ADDED
        };
        // Some devices/versions don't support "LIMIT" in sortOrder; just sort DESC and read first row.
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = contentResolver.query(contentUri, projection,
                null, null, sortOrder)) {

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int relIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH);
                int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);

                long id = idIndex >= 0 ? cursor.getLong(idIndex) : -1;
                String dataPath = dataIndex >= 0 ? cursor.getString(dataIndex) : null;
                String relativePath = relIndex >= 0 ? cursor.getString(relIndex) : null;
                String displayName = nameIndex >= 0 ? cursor.getString(nameIndex) : null;
                long dateAdded = cursor.getLong(dateIndex);

                String pathForCheck = buildPathInfo(dataPath, relativePath, displayName);

                Log.d(TAG, "Latest image pathInfo=" + pathForCheck + " dateAdded=" + dateAdded);

                if (isNewScreenshot(dateAdded)) {
                    lastScreenshotTime = dateAdded;
                    if (listener != null) {
                        String bestPath = !TextUtils.isEmpty(dataPath) ? dataPath : pathForCheck;
                        Uri contentUri = id >= 0
                                ? android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                : null;
                        listener.onScreenshot(bestPath, contentUri);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to query latest image", e);
        }
    }

    private String buildPathInfo(String dataPath, String relativePath, String displayName) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(dataPath)) {
            sb.append(dataPath);
        }
        if (!TextUtils.isEmpty(relativePath)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(relativePath);
        }
        if (!TextUtils.isEmpty(displayName)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(displayName);
        }
        return sb.toString();
    }

    private boolean isScreenshotPath(String text) {
        if (TextUtils.isEmpty(text)) return false;
        String lower = text.toLowerCase();
        return lower.contains("screenshot")
                || lower.contains("screen_shot")
                || lower.contains("screenshots");
    }

    private boolean isNewScreenshot(long dateAdded) {
        return dateAdded > lastScreenshotTime;
    }
}

