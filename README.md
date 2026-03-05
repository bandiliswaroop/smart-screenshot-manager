# Smart Screenshot Manager
Smart Screenshot Manager is an Android app that listens for new screenshots and offers **smart actions** on top of them:
- Show tickets & QR codes **full screen** (and on the **lock screen**) for scanning.
- Keep a **floating mini window** (picture‑in‑picture style) while you use other apps.
- **Delete after share** or delete after a delay.
- Extract text from screenshots using OCR.
- Customizable **sounds** for countdown and delete events.
- Beautiful **animated home screen** background.
---
## Features
### 📸 Smart screenshot bubble
When you take a screenshot, a small orange bubble appears. Tapping it opens the **Smart screenshot actions** panel with:
- **⏳ Keep for 1 hour**  
  Schedule the screenshot to be deleted later (configurable delay in code).
- **📲 Display 15 / 30 / 60 min**  
  Shows the screenshot **full screen** for the chosen time so you can:
  - Scan QR codes (e.g. metro tickets).
  - Show barcodes / passes without opening the Gallery.
  - Also appears on the **lock screen**, so you can just wake the phone and scan.
- **⏱ Display custom time**  
  Enter any number of minutes and start a display session.
- **🗑 Delete after share**  
  Opens the system share sheet with the screenshot attached, then:
  - Asks Android (via `MediaStore.createDeleteRequest`) to delete it from the gallery after sharing.
- **🧠 Extract text**  
  Opens an OCR result screen using ML Kit Text Recognition.
- **✕ Dismiss**  
  Hides the bubble without changing the screenshot.
---
### 🪟 Floating mini window (picture‑in‑picture style)
While a display session is active:
- If you press **Home** or open another app, a **small draggable mini window** stays on screen.
- You can drag it to any edge/corner.
- Tapping it re‑opens the full‑screen ticket view.
- The mini window automatically disappears when:
  - Time finishes, or
  - You explicitly close the session.
---
### 🔊 Sound settings
From the **Main screen → Sound settings** you can configure:
- **Countdown sound (last 10 seconds)**  
  - Off / Soft / Loud  
  - Controls beeps as time approaches zero.
- **Delete sound**  
  - Off / Soft / Loud  
  - Plays when a screenshot is actually deleted.
Settings are stored in `SharedPreferences` (`SoundPrefs`), so users can change them anytime without code changes.
---
### 🎬 Animated main screen
`MainActivity` has:
- A fullscreen **looping background video** (`bg_loop.mp4` in `res/raw`).
- A dark overlay to keep text readable.
- A small card with:
  - **Start Screenshot Manager**
  - **Test Bubble (Show Now)**
  - **Sound settings**
To replace the background, drop any loopable MP4 into `app/src/main/res/raw/bg_loop.mp4` (must be lowercase, no spaces).
---
## Architecture Overview
- **ScreenshotService**
  - Foreground service with a persistent notification.
  - Registers a `ScreenshotObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`.
  - On new screenshot: shows the bubble via `OverlayManager`.
- **ScreenshotObserver**
  - Queries the latest image.
  - Builds both:
    - File path (when available).
    - `contentUri` (MediaStore `_ID`) for safe delete/share on modern Android.
  - Notifies `OverlayManager`.
- **OverlayManager**
  - Adds/removes:
    - `OverlayBubbleView` (main bubble + actions).
    - `MiniDisplayOverlay` (mini PIP overlay).
  - Launches:
    - `ShareActivity` (delete after share).
    - `DisplayActivity` (full-screen ticket).
    - `OcrResultActivity` (OCR).
- **DisplayActivity**
  - Full-screen ticket with bottom timer bar.
  - Shows over lockscreen (`setShowWhenLocked`, `setTurnScreenOn`).
  - Handles countdown logic and last-10-seconds beeps.
  - When backgrounded (Home/Back), shows `MiniDisplayOverlay`.
- **MiniDisplayOverlay**
  - `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`).
  - Draggable, with small countdown text.
  - Tapping opens `DisplayActivity` again.
- **DeleteScreenshotWorker**
  - Scheduled via WorkManager for delayed delete (Keep N minutes/hours).
  - Deletes using both `contentUri` and file path where possible.
  - Plays optional delete tone based on `SoundPrefs`.
---
## Permissions
Declared in `AndroidManifest.xml`:
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` – to observe and access screenshots.
- `SYSTEM_ALERT_WINDOW` – to draw the bubble and mini overlay on top of other apps.
- `FOREGROUND_SERVICE` – for the persistent screenshot listener.
- `POST_NOTIFICATIONS` – to show the foreground service notification.
- `WAKE_LOCK` – to support lockscreen display behavior.
On first run, the app:
1. Requests image/media permission.
2. Asks user to enable **“Display over other apps”** (overlay) in system settings.
---
## Building and Running
1. Open the project in **Android Studio**.
2. Ensure min/target SDK in `app/build.gradle` match your device.
3. Optional: place a background loop video at `app/src/main/res/raw/bg_loop.mp4`.
4. Click **Run** to install on a device/emulator.
---
