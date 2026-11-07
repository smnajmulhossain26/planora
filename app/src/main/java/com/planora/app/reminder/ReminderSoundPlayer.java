package com.planora.app.reminder;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class ReminderSoundPlayer {
    private static MediaPlayer mediaPlayer;
    private static Vibrator vibrator;
    private static final Handler stopHandler = new Handler(Looper.getMainLooper());
    private static Runnable stopRunnable;
    private static String currentPlayingTaskId;

    public static synchronized void play(Context context, String taskId, int durationMs) {
        stopSoundAndVibration(); // Stop any currently playing sound & vibration first

        currentPlayingTaskId = taskId;
        final Context app = context.getApplicationContext();

        // 1. Play Sound
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(app, alarmUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Play Vibration
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vibrator = vm.getDefaultVibrator();
                }
            } else {
                vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 500, 500}; // 0.5s vibrate, 0.5s pause
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Auto-stop after durationMs
        stopRunnable = ReminderSoundPlayer::stopSoundAndVibration;
        stopHandler.postDelayed(stopRunnable, durationMs);
    }

    public static synchronized void stopSoundAndVibration() {
        if (stopRunnable != null) {
            stopHandler.removeCallbacks(stopRunnable);
            stopRunnable = null;
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {}
            vibrator = null;
        }
        currentPlayingTaskId = null;
    }

    public static synchronized void stopIfMatching(String taskId) {
        if (taskId != null && taskId.equals(currentPlayingTaskId)) {
            stopSoundAndVibration();
        }
    }
}
