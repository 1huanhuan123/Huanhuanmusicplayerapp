package com.example.huanhuanmusicplayerapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicPlayerSimulator implements MediaPlayer.OnCompletionListener {

    public enum PlayMode {
        SINGLE_LOOP,    // 单曲循环
        RANDOM,         // 随机播放
        SEQUENCE_LOOP   // 顺序循环
    }

    public interface OnPlaybackListener {
        void onProgressChanged(int currentPosition, String formattedTime);
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackCompleted();
        void onPlayModeChanged(PlayMode playMode);
    }

    private MediaPlayer mediaPlayer;
    private final int totalDuration;
    private final OnPlaybackListener listener;
    private final Handler handler;
    private boolean isPlaying = false;
    private PlayMode currentPlayMode = PlayMode.SEQUENCE_LOOP;
    private int currentPosition = 0;

    public MusicPlayerSimulator(int totalDuration, OnPlaybackListener listener) {
        this.totalDuration = totalDuration;
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setOnCompletionListener(this);
    }

    public void setDataSource(Context context, Uri uri) throws Exception {
        mediaPlayer.setDataSource(context, uri);
    }

    public void prepare() throws Exception {
        mediaPlayer.prepare();
    }

    public void start() {
        try {
            if (mediaPlayer != null) {
                android.util.Log.d("MusicPlayerSimulator", "开始播放");
                mediaPlayer.start();
                isPlaying = true;
                if (listener != null) {
                    listener.onPlaybackStateChanged(true);
                }
                startProgressUpdate();
            } else {
                android.util.Log.e("MusicPlayerSimulator", "start()失败: mediaPlayer为空");
            }
        } catch (Exception e) {
            android.util.Log.e("MusicPlayerSimulator", "start()出错: " + e.getMessage());
            isPlaying = false;
        }
    }

    public void pause() {
        try {
            if (mediaPlayer != null) {
                android.util.Log.d("MusicPlayerSimulator", "暂停播放");
                mediaPlayer.pause();
                isPlaying = false;
                if (listener != null) {
                    listener.onPlaybackStateChanged(false);
                }
                handler.removeCallbacksAndMessages(null);
            } else {
                android.util.Log.e("MusicPlayerSimulator", "pause()失败: mediaPlayer为空");
            }
        } catch (Exception e) {
            android.util.Log.e("MusicPlayerSimulator", "pause()出错: " + e.getMessage());
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            currentPosition = position;
            updateProgress();
        }
    }

    public void togglePlayback() {
        try {
            android.util.Log.d("MusicPlayerSimulator", "切换播放状态: 当前isPlaying=" + isPlaying);
            if (mediaPlayer == null) {
                android.util.Log.e("MusicPlayerSimulator", "togglePlayback失败: mediaPlayer为空");
                return;
            }
            
            if (isPlaying) {
                pause();
            } else {
                start();
            }
        } catch (Exception e) {
            android.util.Log.e("MusicPlayerSimulator", "togglePlayback()出错: " + e.getMessage());
        }
    }

    public void switchPlayMode() {
        switch (currentPlayMode) {
            case SINGLE_LOOP:
                currentPlayMode = PlayMode.RANDOM;
                break;
            case RANDOM:
                currentPlayMode = PlayMode.SEQUENCE_LOOP;
                break;
            case SEQUENCE_LOOP:
                currentPlayMode = PlayMode.SINGLE_LOOP;
                break;
        }

        if (listener != null) {
            listener.onPlayModeChanged(currentPlayMode);
        }
    }

    public PlayMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getFormattedTotalTime() {
        return formatTime(totalDuration);
    }

    private void startProgressUpdate() {
        android.util.Log.d("MusicPlayerSimulator", "开始进度更新, isPlaying=" + isPlaying + ", mediaPlayer=" + (mediaPlayer != null));
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isPlaying && mediaPlayer != null) {
                    try {
                        currentPosition = mediaPlayer.getCurrentPosition();
                        android.util.Log.d("MusicPlayerSimulator", "进度更新: position=" + currentPosition);
                        updateProgress();
                        handler.postDelayed(this, 1000);
                    } catch (Exception e) {
                        android.util.Log.e("MusicPlayerSimulator", "进度更新错误: " + e.getMessage());
                    }
                } else {
                    android.util.Log.d("MusicPlayerSimulator", "无法更新进度: isPlaying=" + isPlaying + ", mediaPlayer=" + (mediaPlayer != null));
                }
            }
        });
    }

    private void updateProgress() {
        if (listener != null) {
            android.util.Log.d("MusicPlayerSimulator", "触发进度回调: position=" + currentPosition + ", time=" + formatTime(currentPosition));
            listener.onProgressChanged(currentPosition, formatTime(currentPosition));
        } else {
            android.util.Log.e("MusicPlayerSimulator", "进度监听器为空");
        }
    }

    private String formatTime(int milliseconds) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
        );
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (listener != null) {
            listener.onTrackCompleted();
        }
    }

    // 使用真实MediaPlayer的方法
    public void setCustomMediaPlayer(MediaPlayer customPlayer, final Runnable onCompletionCallback) {
        // 释放旧的MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        
        // 设置新的MediaPlayer
        mediaPlayer = customPlayer;
        android.util.Log.d("MusicPlayerSimulator", "设置自定义MediaPlayer: " + (mediaPlayer != null) + ", isPlaying=" + (mediaPlayer != null && mediaPlayer.isPlaying()));
        
        // 设置完成监听器
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            if (onCompletionCallback != null) {
                onCompletionCallback.run();
            }
            if (listener != null) {
                listener.onTrackCompleted();
            }
        });
        
        // 确保isPlaying标志与实际状态同步
        try {
            isPlaying = mediaPlayer.isPlaying();
            
            // 通知UI更新播放状态
            if (listener != null) {
                listener.onPlaybackStateChanged(isPlaying);
            }
            
            android.util.Log.d("MusicPlayerSimulator", "设置完成，准备启动进度更新: isPlaying=" + isPlaying);
            
            // 如果已在播放，开始更新进度
            if (isPlaying) {
                startProgressUpdate();
            }
        } catch (Exception e) {
            android.util.Log.e("MusicPlayerSimulator", "设置自定义播放器时出错: " + e.getMessage());
        }
    }
}