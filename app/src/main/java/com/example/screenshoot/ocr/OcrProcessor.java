package com.example.screenshoot.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;

public class OcrProcessor {

    public interface Callback {
        void onResult(String text, String classification);
    }

    public static void recognizeText(Context context, String imagePath, Callback callback) {
        try {
            File file = new File(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            com.google.mlkit.vision.text.TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            Task<Text> task = recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        String type = classify(fullText);
                        if (callback != null) {
                            callback.onResult(fullText, type);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onResult("", "error");
                        }
                    });
        } catch (Exception e) {
            if (callback != null) {
                callback.onResult("", "error");
            }
        }
    }

    private static String classify(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("total") || lower.contains("invoice") || lower.contains("rs") || lower.contains("$")) {
            return "receipt";
        } else if (lower.contains("http://") || lower.contains("https://")) {
            return "link";
        } else if (lower.length() > 0 && lower.split("\\s+").length > 3) {
            return "text_note";
        } else {
            return "image";
        }
    }
}

