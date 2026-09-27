# FFmpegKit loads its native classes reflectively from JNI.
-keep class com.antonkarpenko.ffmpegkit.** { *; }
-dontwarn com.antonkarpenko.ffmpegkit.**
