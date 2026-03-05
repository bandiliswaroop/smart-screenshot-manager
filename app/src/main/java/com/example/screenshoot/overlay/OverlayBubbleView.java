package com.example.screenshoot.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.screenshoot.R;

import java.io.File;

public class OverlayBubbleView extends LinearLayout {

    public interface ActionListener {
        void onKeepForHour(String path, Uri contentUri);
        void onDeleteAfterShare(String path, Uri contentUri);
        void onDisplayOnScreen(String path, Uri contentUri, int durationMinutes);
        void onExtractText(String path);
        void onDismiss();
    }

    private final String screenshotPath;
    private final Uri contentUri;
    private final ActionListener listener;
    private View bubble;
    private View panel;

    public OverlayBubbleView(Context context, String screenshotPath, Uri contentUri, ActionListener listener) {
        super(context);
        this.screenshotPath = screenshotPath;
        this.contentUri = contentUri;
        this.listener = listener;
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_overlay_bubble, this, true);
        bubble = findViewById(R.id.bubble);
        panel = findViewById(R.id.panel);
        panel.setVisibility(GONE);

        bubble.setOnClickListener(v -> {
            panel.setVisibility(panel.getVisibility() == GONE ? VISIBLE : GONE);
        });

        TextView title = findViewById(R.id.txtTitle);
        TextView details = findViewById(R.id.txtDetails);
        TextView keepHour = findViewById(R.id.btnKeepHour);
        TextView deleteShare = findViewById(R.id.btnDeleteShare);
        TextView display15 = findViewById(R.id.btnDisplay15);
        TextView display30 = findViewById(R.id.btnDisplay30);
        TextView display60 = findViewById(R.id.btnDisplay60);
        TextView displayCustom = findViewById(R.id.btnDisplayCustom);
        android.widget.EditText editCustom = findViewById(R.id.editCustomMinutes);
        TextView extractText = findViewById(R.id.btnExtractText);
        TextView dismiss = findViewById(R.id.btnDismiss);

        // Show simple details: screenshot file name
        String name = null;
        if (screenshotPath != null) {
            name = new File(screenshotPath).getName();
        }
        if (details != null) {
            details.setText(name != null ? name : "Latest screenshot");
        }

        keepHour.setOnClickListener(v -> {
            if (listener != null) listener.onKeepForHour(screenshotPath, contentUri);
        });
        deleteShare.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAfterShare(screenshotPath, contentUri);
        });
        if (display15 != null) display15.setOnClickListener(v -> {
            if (listener != null) listener.onDisplayOnScreen(screenshotPath, contentUri, 15);
        });
        if (display30 != null) display30.setOnClickListener(v -> {
            if (listener != null) listener.onDisplayOnScreen(screenshotPath, contentUri, 30);
        });
        if (display60 != null) display60.setOnClickListener(v -> {
            if (listener != null) listener.onDisplayOnScreen(screenshotPath, contentUri, 60);
        });
        if (displayCustom != null && editCustom != null) displayCustom.setOnClickListener(v -> {
            String value = editCustom.getText() != null ? editCustom.getText().toString().trim() : "";
            if (TextUtils.isEmpty(value)) {
                android.widget.Toast.makeText(getContext(), "Enter minutes", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int mins = Integer.parseInt(value);
                if (mins <= 0) {
                    android.widget.Toast.makeText(getContext(), "Minutes must be > 0", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (listener != null) listener.onDisplayOnScreen(screenshotPath, contentUri, mins);
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(getContext(), "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        extractText.setOnClickListener(v -> {
            if (listener != null) listener.onExtractText(screenshotPath);
        });
        dismiss.setOnClickListener(v -> {
            if (listener != null) listener.onDismiss();
        });

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF9800"));
        bg.setShape(GradientDrawable.OVAL);
        bubble.setBackground(bg);

        // Make sure bubble is visible (a bit bigger now)
        int sizePx = (int) (72 * getResources().getDisplayMetrics().density / getResources().getDisplayMetrics().densityDpi * 160);
        bubble.setMinimumWidth(sizePx);
        bubble.setMinimumHeight(sizePx);
    }
}

