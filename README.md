# SubBurn

Native Android app (Kotlin + Jetpack Compose, dark futuristic UI) that burns an
`.srt` subtitle track into a video with FFmpeg and re-encodes to H.265, running
the job in a foreground service so it never stalls when the screen goes off.

## The command it builds

Out of the box the app reproduces the hand-written command it was built
around — same `force_style`, same `libx265 -preset medium -crf 20`, same
`-c:a copy`:

```
ffmpeg -hide_banner -y -i <input> \
  -vf "subtitles=<subs.srt>:fontsdir=<fonts>:force_style='FontName=Noto Sans Hebrew,FontSize=20,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,BorderStyle=1,Outline=1.5,Shadow=0.5'" \
  -c:v libx265 -preset medium -crf 20 -tag:v hvc1 \
  -c:a copy -movflags +faststart <output>.mp4
```

Optional style fields (`BackColour`, `Bold`, `Italic`, `Blur`, `MarginV`) are
only added once you change them, so the default command stays minimal.
`-tag:v hvc1` and `-movflags +faststart` are the two additions: they make the
HEVC file play on more players and start without buffering the whole moov
atom. A unit test pins the default `force_style` string.

Every value in `force_style` plus `-crf` and `-preset` is driven by the UI, and
the exact command line is shown in the app (tap **Show ffmpeg command**).

## Features

- **Hebrew, right-to-left UI** — every label, hint and notification is in
  Hebrew, the layout is forced to RTL, and each section carries a short
  explanation of what the control actually does.
- **Video-editor layout** — video on top, transport row, a scrollable timeline
  with the subtitle track under a fixed playhead, and an icon toolbar at the
  bottom (video · subtitle file · edit · style · quality) that opens each panel
  as a bottom sheet, with the burn button in the app bar.
- **Player, not just a picker** — the video plays in-app with play/pause,
  ±10s and a scrub bar, and the active subtitle is drawn over the frame in the
  chosen style, so you see the result while you work.
- **Subtitle editing** — tap a cue to jump to it, the pencil to fix a word, or
  "add from here" to create a cue at the playhead; start/end can be taken from
  the current playback position or nudged in 0.2s steps. The edited cue list is
  written back out as a clean, renumbered SRT and that file is what gets burned.
- **Few choices by default** — three style presets (classic / boxed / cinema)
  and three quality presets; everything else is folded behind "advanced".
- **File pickers** — SAF `OpenDocument` for the video and the subtitle file.
  The video is handed to ffmpeg as a `saf:` path (no copying a multi-GB file);
  the subtitle is copied into the cache because libass needs a real path.
- **Foreground service + WakeLock** (`BurnService`) — `dataSync` foreground
  type, partial wake lock, ongoing notification with live speed/ETA and a
  Cancel action. The encode survives screen-off and backgrounding.
- **Compression** — libx265, CRF slider 16–32 with quality hints, preset
  picker, audio stream-copied so only the video is re-encoded.
- **Subtitle styling** — font size, full box vs. outline-only, box opacity,
  black outline thickness, drop shadow, edge softening (libass `Blur`),
  bottom margin, bold, italic slant, font name and an optional `fontsdir`.
- **Output** — rendered to app storage, then published to `Movies/SubBurn`
  via MediaStore so it appears in the gallery.

## Project layout

| Path | Role |
| --- | --- |
| `core/SubtitleStyle.kt` | style model → ASS `force_style` string (incl. `&HAABBGGRR` colours) |
| `core/BurnJob.kt` | job model + `FfmpegCommand` builder and shell preview |
| `core/SrtDocument.kt` | SRT parse/serialize, cue lookup by playback position |
| `core/PickedFiles.kt` | SAF metadata, srt caching, `saf:` input path, output naming |
| `core/BurnState.kt` | shared `StateFlow` of render progress and log tail |
| `core/MediaExporter.kt` | MediaStore publish to `Movies/SubBurn` |
| `service/BurnService.kt` | foreground service, wake lock, ffmpeg session, notification |
| `ui/BurnScreen.kt` | the Compose UI |
| `ui/VideoStage.kt` | player surface, subtitle overlay, transport bar |
| `ui/SubtitleEditor.kt` | cue list, edit/add/delete dialog |
| `ui/Theme.kt` | dark neon Material 3 theme |

## Building the APK on GitHub

`.github/workflows/android.yml` builds the APK on GitHub's own runners — no
Android Studio, no local SDK. It runs on every push, on pull requests, and
manually from **Actions → Build APK → Run workflow**.

What it does: JDK 17 → `android-actions/setup-android` → unit tests → `lintDebug` →
`assembleDebug` → (release, if a keystore secret is set) → uploads the APKs as
the **SubBurn-apk** artifact (downloadable from the run page for 30 days).
Pushing a `v*` tag additionally attaches the APKs to a GitHub Release.

### Installable (signed) release APKs

An unsigned release APK cannot be installed, so release builds only run when a
keystore is configured. Create one and add four repository secrets
(*Settings → Secrets and variables → Actions*):

```
keytool -genkeypair -v -keystore release.jks -alias subburn \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks     # paste as KEYSTORE_BASE64
```

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of `release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `subburn` |
| `KEY_PASSWORD` | key password |

Without them the debug APK is still built and uploaded — debug APKs are signed
with the standard debug key and install fine for personal use.

### Output sizes

The bundled FFmpeg native libraries are most of the download, so the build is
split per ABI:

| APK | Size |
| --- | --- |
| `arm64-v8a` (nearly every modern phone) | ~31 MB |
| `armeabi-v7a` (older 32-bit devices) | ~50 MB |
| `x86_64` (emulator / ChromeOS) | ~32 MB |
| `universal` (all three) | ~93 MB |

### Building locally

```
./gradlew :app:assembleDebug
```

Android SDK with compileSdk 35 and JDK 17; minSdk 24. The Gradle wrapper is
committed, so no separate Gradle install is needed.

### FFmpeg dependency note

`subtitles` (libass) plus `libx265` means a **GPL** FFmpeg build. The original
`com.arthenica:ffmpeg-kit-*` artifacts were retired by the upstream project, so
`app/build.gradle.kts` points at the republished `com.antonkarpenko:ffmpeg-kit-full-gpl:6.0.2`
fork, which keeps the same `com.arthenica.ffmpegkit` API. Swap it for any other
mirror or your own build of the same API if you prefer. Shipping the app means
complying with the GPL.
