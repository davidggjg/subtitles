# SubBurn

Native Android app (Kotlin + Jetpack Compose, dark futuristic UI) that burns an
`.srt` subtitle track into a video with FFmpeg and re-encodes to H.265, running
the job in a foreground service so it never stalls when the screen goes off.

## The command it builds

```
ffmpeg -hide_banner -y -i <input> \
  -vf "subtitles=<subs.srt>:fontsdir=<fonts>:force_style='FontName=Noto Sans Hebrew,FontSize=20,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,BackColour=&H4D000000,BorderStyle=1,Outline=1.5,Shadow=0.5,Bold=0,MarginV=24'" \
  -c:v libx265 -preset medium -crf 20 -tag:v hvc1 \
  -c:a copy -movflags +faststart <output>.mp4
```

Every value in `force_style` plus `-crf` and `-preset` is driven by the UI, and
the exact command line is shown in the app (tap **Show ffmpeg command**).

## Features

- **File pickers** — SAF `OpenDocument` for the video and the subtitle file.
  The video is handed to ffmpeg as a `saf:` path (no copying a multi-GB file);
  the subtitle is copied into the cache because libass needs a real path.
- **Foreground service + WakeLock** (`BurnService`) — `dataSync` foreground
  type, partial wake lock, ongoing notification with live speed/ETA and a
  Cancel action. The encode survives screen-off and backgrounding.
- **Compression** — libx265, CRF slider 16–32 with quality hints, preset
  picker, audio stream-copied so only the video is re-encoded.
- **Subtitle styling** — font size slider, full box vs. outline-only toggle,
  box opacity, black outline thickness, drop shadow, bottom margin, bold,
  font name and optional `fontsdir`, all with a live preview strip.
- **Output** — rendered to app storage, then published to `Movies/SubBurn`
  via MediaStore so it appears in the gallery.

## Project layout

| Path | Role |
| --- | --- |
| `core/SubtitleStyle.kt` | style model → ASS `force_style` string (incl. `&HAABBGGRR` colours) |
| `core/BurnJob.kt` | job model + `FfmpegCommand` builder and shell preview |
| `core/PickedFiles.kt` | SAF metadata, srt caching, `saf:` input path, output naming |
| `core/BurnState.kt` | shared `StateFlow` of render progress and log tail |
| `core/MediaExporter.kt` | MediaStore publish to `Movies/SubBurn` |
| `service/BurnService.kt` | foreground service, wake lock, ffmpeg session, notification |
| `ui/BurnScreen.kt` | the Compose UI |
| `ui/Theme.kt` | dark neon Material 3 theme |

## Building

```
./gradlew :app:assembleDebug
```

Requires the Android SDK (compileSdk 35, JDK 17); minSdk 24. This repo has no
Gradle wrapper jar committed — run `gradle wrapper` once, or open the project in
Android Studio.

### FFmpeg dependency note

`subtitles` (libass) plus `libx265` means a **GPL** FFmpeg build. The original
`com.arthenica:ffmpeg-kit-*` artifacts were retired by the upstream project, so
`app/build.gradle.kts` points at the republished `com.antonkarpenko:ffmpeg-kit-full-gpl:6.0.2`
fork, which keeps the same `com.arthenica.ffmpegkit` API. Swap it for any other
mirror or your own build of the same API if you prefer. Shipping the app means
complying with the GPL.
