package com.example.screenshoot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.screenshoot.R;
import com.example.screenshoot.ocr.OcrProcessor;

public class OcrResultActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "extra_path";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        TextView txt = findViewById(R.id.txtResult);
        ProgressBar progress = findViewById(R.id.progress);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        txt.setText("");

        OcrProcessor.recognizeText(this, path, (text, classification) -> {
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                txt.setText(text.isEmpty() ? "No text detected" : text);
                setTitle("Type: " + classification);
            });
        });
    }
}

