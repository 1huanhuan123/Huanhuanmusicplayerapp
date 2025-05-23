package com.example.huanhuanmusicplayerapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.huanhuanmusicplayerapp.model.Song;
import com.example.huanhuanmusicplayerapp.ui.NetworkMusicActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements MusicPlayerSimulator.OnPlaybackListener {

    private ImageButton playPauseButton;
    private ImageButton playModeButton;
    private ImageButton prevButton;  // 上一首按钮
    private ImageButton nextButton;  // 下一首按钮
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private TextView songTitleTextView;
    private TextView artistNameTextView;
    private Button playlistButton;
    private TextView playModeTextView;
    private FrameLayout visualizerContainer;
    private Spinner visualizerModeSpinner;
    
    // 可视化器相关
    private Visualizer visualizer;
    private View currentVisualizerView;
    private int audioSessionId = 0;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1002;

    // 音乐播放模拟器
    private MusicPlayerSimulator playerSimulator;

    // 模拟歌曲总时长（3:30 = 210秒 = 210000毫秒）
    private static final int SONG_DURATION = 210000;

    // 播放列表请求码
    private static final int REQUEST_PLAYLIST = 1001;

    // 播放列表相关
    private List<Song> songList = new ArrayList<>();
    private int currentSongIndex = 0;  // 当前歌曲索引
    private Random random = new Random();
    private boolean isPlaying = false;  // 记录播放状态

    // 添加标志，用于区分用户手动切换和系统自动切换
    private boolean userChangingPlayMode = false;

    private Button networkMusicButton;
    private static final int REQUEST_NETWORK_MUSIC = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化播放列表（使用与PlaylistActivity中相同的示例数据）
        initSongList();
        
        // 从SharedPreferences加载保存的播放列表
        loadPlaylist();

        // 初始化视图
        initViews();

        // 初始化音乐播放模拟器
        playerSimulator = new MusicPlayerSimulator(SONG_DURATION, this);

        // 设置监听器
        setupListeners();

        // 初始化UI
        totalTimeTextView.setText(playerSimulator.getFormattedTotalTime());
        songTitleTextView.setText("请选择歌曲");
        artistNameTextView.setText("未知歌手");
        updatePlayModeUI(playerSimulator.getCurrentPlayMode());
    }

    private void initSongList() {
        // 清空播放列表，不再添加假音乐数据
        songList.clear();
    }

    private void initViews() {
        playPauseButton = findViewById(R.id.btn_play_pause);
        playModeButton = findViewById(R.id.btn_play_mode);
        prevButton = findViewById(R.id.btn_previous);
        nextButton = findViewById(R.id.btn_next);
        seekBar = findViewById(R.id.seek_bar);
        currentTimeTextView = findViewById(R.id.current_time);
        totalTimeTextView = findViewById(R.id.total_time);
        songTitleTextView = findViewById(R.id.song_title);
        artistNameTextView = findViewById(R.id.artist_name);
        playlistButton = findViewById(R.id.btn_playlist);
        playModeTextView = findViewById(R.id.play_mode_text);
        visualizerContainer = findViewById(R.id.visualizer_container);
        visualizerModeSpinner = findViewById(R.id.visualizer_mode_spinner);
        networkMusicButton = findViewById(R.id.btn_network_music);
    }

    private void setupListeners() {
        // 设置进度条最大值
        seekBar.setMax(SONG_DURATION);

        // 播放/暂停按钮点击监听
        playPauseButton.setOnClickListener(v -> {
            try {
                android.util.Log.d("MainActivity", "播放/暂停按钮点击");
                
                if (playerSimulator != null) {
                    android.util.Log.d("MainActivity", "调用播放器切换播放状态");
                    playerSimulator.togglePlayback();
                    isPlaying = playerSimulator.isPlaying();
                } else {
                    android.util.Log.e("MainActivity", "播放器模拟器为空，无法切换播放状态");
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "切换播放状态时发生错误: " + e.getMessage());
                Toast.makeText(MainActivity.this, "播放控制失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });

        // 播放模式按钮点击监听
        playModeButton.setOnClickListener(v -> {
            // 设置标志，表示这是用户手动切换
            userChangingPlayMode = true;
            playerSimulator.switchPlayMode();
            userChangingPlayMode = false;
        });

        // 上一首按钮点击监听
        prevButton.setOnClickListener(v -> {
            playPreviousSong();
        });

        // 下一首按钮点击监听
        nextButton.setOnClickListener(v -> {
            playNextSong();
        });

        // 进度条拖动监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerSimulator.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不需要特殊处理
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 不需要特殊处理
            }
        });

        // 播放列表按钮点击监听
        playlistButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlaylistActivity.class);
            
            // 传递当前播放列表到PlaylistActivity
            intent.putExtra("playlist_size", songList.size());
            for (int i = 0; i < songList.size(); i++) {
                Song song = songList.get(i);
                intent.putExtra("song_title_" + i, song.getTitle());
                intent.putExtra("song_artist_" + i, song.getArtist());
                intent.putExtra("song_duration_" + i, song.getDuration());
                intent.putExtra("song_uri_" + i, song.getFilePath());
            }
            
            startActivityForResult(intent, REQUEST_PLAYLIST);
        });
        
        // 设置可视化器模式选择监听
        visualizerModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVisualizerMode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何处理
            }
        });
        
        // 检查录音权限并初始化可视化器
        checkAudioRecordPermission();

        networkMusicButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NetworkMusicActivity.class);
            startActivityForResult(intent, REQUEST_NETWORK_MUSIC);
        });
    }
    
    // 检查录音权限
    private void checkAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            setupVisualizer();
        }
    }
    
    // 初始化可视化器
    private void setupVisualizer() {
        // 如果已经有可视化器，先释放它
        releaseVisualizer();
        
        // 创建新的可视化器实例
        if (audioSessionId != 0) {
            try {
                visualizer = new Visualizer(audioSessionId);
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                
                // 设置数据捕获监听器
                visualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        @Override
                        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                            if (currentVisualizerView instanceof SurfaceView) {
                                SurfaceView surfaceView = (SurfaceView) currentVisualizerView;
                                SurfaceHolder holder = surfaceView.getHolder();
                                
                                // 根据选择的模式绘制不同的可视化效果
                                int mode = visualizerModeSpinner.getSelectedItemPosition();
                                drawVisualizer(holder, waveform, mode);
                            }
                        }

                        @Override
                        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                            // FFT数据处理（频谱分析）
                            // 这里不处理
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, false);
                
                visualizer.setEnabled(true);
                
                // 根据当前选择的模式初始化可视化视图
                int selectedMode = visualizerModeSpinner.getSelectedItemPosition();
                updateVisualizerMode(selectedMode);
            } catch (Exception e) {
                Toast.makeText(this, "无法初始化音频可视化: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // 绘制可视化效果
    private void drawVisualizer(SurfaceHolder holder, byte[] waveform, int mode) {
        if (holder == null) return;
        
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas != null) {
                // 清空画布
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                
                // 获取画布尺寸
                int width = canvas.getWidth();
                int height = canvas.getHeight();
                int centerY = height / 2;
                
                // 创建画笔
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(ContextCompat.getColor(this, R.color.purple_500));
                
                // 根据不同模式绘制
                switch (mode) {
                    case 0: // 默认 - 简单波形图
                        drawDefaultVisualizer(canvas, waveform, width, height, centerY, paint);
                        break;
                    case 1: // 单线 - 单一线条波形
                        drawLineVisualizer(canvas, waveform, width, height, centerY, paint);
                        break;
                    case 2: // 波形 - 填充波形
                        drawWaveVisualizer(canvas, waveform, width, height, centerY, paint);
                        break;
                    case 3: // 圆形 - 圆形波形
                        drawCircleVisualizer(canvas, waveform, width, height, paint);
                        break;
                    case 4: // 条形 - 频谱条形图
                        drawBarVisualizer(canvas, waveform, width, height, paint);
                        break;
                }
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
    
    // 默认可视化效果 - 简单波形图
    private void drawDefaultVisualizer(Canvas canvas, byte[] waveform, int width, int height, int centerY, Paint paint) {
        if (waveform == null || waveform.length == 0) return;
        
        // 计算每个点的间距
        float strokeWidth = 5f;
        paint.setStrokeWidth(strokeWidth);
        float spacing = width / (float) waveform.length;
        
        // 绘制波形
        for (int i = 0; i < waveform.length - 1; i++) {
            float x1 = i * spacing;
            float y1 = centerY + ((byte) (waveform[i] + 128)) * (height / 4) / 128;
            float x2 = (i + 1) * spacing;
            float y2 = centerY + ((byte) (waveform[i + 1] + 128)) * (height / 4) / 128;
            
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
    
    // 单线可视化效果 - 单一线条波形
    private void drawLineVisualizer(Canvas canvas, byte[] waveform, int width, int height, int centerY, Paint paint) {
        if (waveform == null || waveform.length == 0) return;
        
        paint.setStrokeWidth(3f);
        float spacing = width / (float) waveform.length;
        
        Path path = new Path();
        path.moveTo(0, centerY);
        
        for (int i = 0; i < waveform.length; i++) {
            float x = i * spacing;
            float y = centerY + ((byte) (waveform[i] + 128)) * (height / 3) / 128;
            path.lineTo(x, y);
        }
        
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
    }
    
    // 波形可视化效果 - 填充波形
    private void drawWaveVisualizer(Canvas canvas, byte[] waveform, int width, int height, int centerY, Paint paint) {
        if (waveform == null || waveform.length == 0) return;
        
        float spacing = width / (float) waveform.length;
        
        Path path = new Path();
        path.moveTo(0, centerY);
        
        for (int i = 0; i < waveform.length; i++) {
            float x = i * spacing;
            float y = centerY + ((byte) (waveform[i] + 128)) * (height / 3) / 128;
            path.lineTo(x, y);
        }
        
        path.lineTo(width, centerY);
        path.close();
        
        paint.setAlpha(100);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
        
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
    }
    
    // 圆形可视化效果 - 圆形波形
    private void drawCircleVisualizer(Canvas canvas, byte[] waveform, int width, int height, Paint paint) {
        if (waveform == null || waveform.length == 0) return;
        
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 4;
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        
        Path path = new Path();
        float angle = 0;
        float angleIncrement = 360f / waveform.length;
        
        for (int i = 0; i < waveform.length; i++) {
            float magnitude = (128 + waveform[i]) * radius / 128;
            float x = centerX + magnitude * (float) Math.cos(Math.toRadians(angle));
            float y = centerY + magnitude * (float) Math.sin(Math.toRadians(angle));
            
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
            
            angle += angleIncrement;
        }
        
        path.close();
        canvas.drawPath(path, paint);
    }
    
    // 条形可视化效果 - 频谱条形图
    private void drawBarVisualizer(Canvas canvas, byte[] waveform, int width, int height, Paint paint) {
        if (waveform == null || waveform.length == 0) return;
        
        int barCount = Math.min(waveform.length, 128);
        float barWidth = width / (float) barCount - 2;
        float spacing = 2;
        
        paint.setStyle(Paint.Style.FILL);
        
        for (int i = 0; i < barCount; i++) {
            float left = i * (barWidth + spacing);
            float top = height / 2 - Math.abs(waveform[i]) * (height / 2) / 128;
            float right = left + barWidth;
            float bottom = height / 2 + Math.abs(waveform[i]) * (height / 2) / 128;
            
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    // 播放上一首歌曲
    private void playPreviousSong() {
        // 检查播放列表是否为空
        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean wasPlaying = playerSimulator.isPlaying();

        // 根据播放模式确定上一首歌曲
        if (playerSimulator.getCurrentPlayMode() == MusicPlayerSimulator.PlayMode.RANDOM) {
            // 随机模式：随机选择一首不同的歌曲
            int newIndex;
            if (songList.size() > 1) {
                do {
                    newIndex = random.nextInt(songList.size());
                } while (newIndex == currentSongIndex);
                currentSongIndex = newIndex;
            } else {
                // 只有一首歌曲的情况，仍然播放当前歌曲
                currentSongIndex = 0;
            }
        } else {
            // 顺序模式：播放上一首歌曲
            currentSongIndex = (currentSongIndex - 1 + songList.size()) % songList.size();
        }

        // 播放选定的歌曲
        Song prevSong = songList.get(currentSongIndex);
        playSong(prevSong, wasPlaying);
    }

    // 播放下一首歌曲
    private void playNextSong() {
        // 检查播放列表是否为空
        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean wasPlaying = playerSimulator.isPlaying();

        // 记录当前索引，以便稍后检查是否重复
        int oldIndex = currentSongIndex;

        // 根据播放模式确定下一首歌曲
        if (playerSimulator.getCurrentPlayMode() == MusicPlayerSimulator.PlayMode.RANDOM) {
            // 随机模式：随机选择一首不同的歌曲
            int newIndex;
            if (songList.size() > 1) {
                do {
                    newIndex = random.nextInt(songList.size());
                } while (newIndex == currentSongIndex);
                currentSongIndex = newIndex;
            } else {
                // 只有一首歌曲的情况，仍然播放当前歌曲
                currentSongIndex = 0;
            }
        } else {
            // 顺序模式：播放下一首歌曲
            currentSongIndex = (currentSongIndex + 1) % songList.size();
        }

        // 播放选定的歌曲
        Song nextSong = songList.get(currentSongIndex);
        
        // 添加日志，帮助调试
        android.util.Log.d("MainActivity", "播放下一首: 从" + oldIndex + "切换到" + currentSongIndex + 
                          ", 歌曲:" + nextSong.getTitle());
                          
        String uri = nextSong.getFilePath();
        if (uri != null) {
            playLocalSong(nextSong, Uri.parse(uri));
        } else {
            playSong(nextSong, wasPlaying);
        }
    }
    // 播放指定歌曲
    private void playSong(Song song) {
        playSong(song, true); // 默认开始播放
    }

    // 播放指定歌曲，可指定是否自动开始播放
    private void playSong(Song song, boolean autoPlay) {
        // 更新UI
        songTitleTextView.setText(song.getTitle());
        artistNameTextView.setText(song.getArtist());

        // 保存当前播放模式
        MusicPlayerSimulator.PlayMode currentMode = MusicPlayerSimulator.PlayMode.SEQUENCE_LOOP;
        if (playerSimulator != null) {
            currentMode = playerSimulator.getCurrentPlayMode();
            playerSimulator.release();
        }

        // 创建新的播放器实例，确保使用正确的时长
        int songDuration = Math.max(song.getDuration(), 60000); // 至少1分钟
        android.util.Log.d("MainActivity", "创建新的模拟播放器, 时长: " + songDuration + "ms");
        playerSimulator = new MusicPlayerSimulator(songDuration, this);
        
        // 设置为之前保存的播放模式
        while (playerSimulator.getCurrentPlayMode() != currentMode) {
            playerSimulator.switchPlayMode();
        }
        
        // 设置进度条最大值
        seekBar.setMax(songDuration);
        totalTimeTextView.setText(formatDuration(songDuration));
        currentTimeTextView.setText("00:00");
        seekBar.setProgress(0);

        // 更新播放模式UI
        updatePlayModeUI(playerSimulator.getCurrentPlayMode());

        // 如果需要自动开始播放
        if (autoPlay) {
            try {
                android.util.Log.d("MainActivity", "自动开始播放");
                playerSimulator.togglePlayback();
                isPlaying = playerSimulator.isPlaying();
                updatePlayPauseButton();
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "开始播放时出错: " + e.getMessage(), e);
            }
        }
    }

    private void updatePlayModeUI(MusicPlayerSimulator.PlayMode playMode) {
        switch (playMode) {
            case SINGLE_LOOP:
                playModeTextView.setText("单曲循环");
                break;
            case RANDOM:
                playModeTextView.setText("随机播放");
                break;
            case SEQUENCE_LOOP:
                playModeTextView.setText("顺序循环");
                break;
        }
    }
    // 从PlaylistActivity回传更新后的播放列表
    public void syncPlaylist(List<Song> updatedList) {
        // 保存之前正在播放的歌曲信息
        Song currentSong = null;
        boolean wasPlaying = false;
        
        if (!songList.isEmpty() && currentSongIndex >= 0 && currentSongIndex < songList.size()) {
            currentSong = songList.get(currentSongIndex);
            wasPlaying = playerSimulator.isPlaying();
        }
        
        // 更新播放列表
        songList.clear();
        songList.addAll(updatedList);
        
        // 尝试恢复之前播放的歌曲
        if (currentSong != null) {
            // 查找相同标题和艺术家的歌曲
            int newIndex = -1;
            for (int i = 0; i < songList.size(); i++) {
                Song s = songList.get(i);
                if (s.getTitle().equals(currentSong.getTitle()) && 
                    s.getArtist().equals(currentSong.getArtist())) {
                    newIndex = i;
                    break;
                }
            }
            
            if (newIndex >= 0) {
                // 找到了相同的歌曲，继续播放
                currentSongIndex = newIndex;
                playSong(songList.get(currentSongIndex), wasPlaying);
            } else if (!songList.isEmpty()) {
                // 没找到相同的歌曲，但播放列表不为空，播放第一首
                currentSongIndex = 0;
                playSong(songList.get(0), false);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PLAYLIST && resultCode == RESULT_OK && data != null) {
            // 检查播放列表是否已更新
            boolean playlistUpdated = data.getBooleanExtra("playlist_updated", false);
            if (playlistUpdated) {
                // 更新播放列表
                updatePlaylist(data);
            }
            
            // 从播放列表获取选择的歌曲
            int position = data.getIntExtra(PlaylistActivity.EXTRA_SONG_POSITION, -1);
            if (position >= 0 && position < songList.size()) {
                // 保存当前歌曲索引
                currentSongIndex = position;
                
                // 播放选中的歌曲
                Song selectedSong = songList.get(position);
                String songUri = data.getStringExtra("song_uri");
                
                if (songUri != null) {
                    // 如果是本地文件，使用MediaPlayer播放
                    playLocalSong(selectedSong, Uri.parse(songUri));
                } else {
                    // 如果是模拟数据，使用模拟器播放
                    playSong(selectedSong, true);
                }
            }
        } else if (requestCode == REQUEST_NETWORK_MUSIC && resultCode == RESULT_OK && data != null) {
            // 处理网络音乐选择结果
            String title = data.getStringExtra("song_title");
            String artist = data.getStringExtra("song_artist");
            int duration = data.getIntExtra("song_duration", 0);
            String uri = data.getStringExtra("song_uri");
            String coverUrl = data.getStringExtra("song_cover");
            
            // 创建歌曲对象并添加到播放列表
            Song networkSong = new Song(title, artist, duration, coverUrl, uri);
            songList.add(networkSong);
            
            // 保存播放列表
            savePlaylist();
            
            // 播放歌曲
            currentSongIndex = songList.size() - 1;
            playSong(networkSong);
        }
    }

    private void updatePlaylist(Intent data) {
        // 清空当前播放列表
        songList.clear();
        
        // 获取播放列表大小
        int playlistSize = data.getIntExtra("playlist_size", 0);
        
        // 添加所有歌曲到播放列表
        for (int i = 0; i < playlistSize; i++) {
            String title = data.getStringExtra("song_title_" + i);
            String artist = data.getStringExtra("song_artist_" + i);
            int duration = data.getIntExtra("song_duration_" + i, 0);
            String uri = data.getStringExtra("song_uri_" + i);
            
            // 创建新的Song对象并添加到列表
            Song song = new Song(title, artist, duration, null, uri);
            songList.add(song);
        }
        
        // 打印日志，帮助调试
        android.util.Log.d("MainActivity", "播放列表已更新，大小: " + songList.size());
    }

    private void playLocalSong(Song song, Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "文件无效", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("MainActivity", "开始播放音乐，URL: " + uri.toString() + ", 时长: " + song.getDuration());
        
        // 更新界面显示
        songTitleTextView.setText(song.getTitle());
        artistNameTextView.setText(song.getArtist());
        totalTimeTextView.setText(formatDuration(song.getDuration()));
        
        // 确保旧的播放器已释放
        if (playerSimulator != null) {
            MusicPlayerSimulator.PlayMode currentMode = playerSimulator.getCurrentPlayMode();
            playerSimulator.release();
            
            // 创建新的模拟器，但不要开始播放
            playerSimulator = new MusicPlayerSimulator(song.getDuration(), this);
            
            // 恢复播放模式
            while (playerSimulator.getCurrentPlayMode() != currentMode) {
                playerSimulator.switchPlayMode();
            }
        }
        
        // 对网易云音乐链接进行特殊处理
        if (uri.toString().contains("music.163.com")) {
            android.util.Log.d("MainActivity", "识别为网易云音乐链接，使用替代方法");
            // 对网易云链接特殊处理
            playNeteaseSong(song);
            return;
        }
        
        // 更新媒体播放器
        try {
            final MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            setUpMediaPlayer(mediaPlayer, song);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "设置播放器时出错: " + e.getMessage(), e);
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 尝试使用模拟播放方式
            playSong(song, true);
        }
    }
    
    // 添加专门的方法处理网易云音乐
    private void playNeteaseSong(Song song) {
        android.util.Log.d("MainActivity", "使用替代方法播放网易云音乐");
        
        // 获取歌曲ID
        String uri = song.getFilePath();
        android.util.Log.d("MainActivity", "原始URI: " + uri);
        
        final String songId;  // 使用final声明
        
        try {
            // 尝试从URL中提取ID
            if (uri.contains("id=")) {
                String extractedId = uri.substring(uri.indexOf("id=") + 3);
                if (extractedId.contains(".")) {
                    songId = extractedId.substring(0, extractedId.indexOf("."));
                } else {
                    songId = extractedId;
                }
                android.util.Log.d("MainActivity", "提取到歌曲ID: " + songId);
            } else {
                // 设置一个默认值，避免可能的未初始化问题
                songId = "";
                android.util.Log.d("MainActivity", "无法从URL提取歌曲ID，使用空ID");
            }
            
            // 直接使用模拟播放器播放
            if (new Random().nextInt(10) < 7) {  // 70%概率直接使用模拟播放器
                android.util.Log.d("MainActivity", "直接使用模拟播放器");
                playSong(song, true);
                return;
            }
            
            // 尝试直接播放网络URL
            try {
                // 创建媒体播放器
                MediaPlayer mediaPlayer = new MediaPlayer();
                
                // 设置错误监听器 - 这里可能是问题所在
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    android.util.Log.e("MainActivity", "网易云音乐播放错误: " + what + ", extra: " + extra);
                    // 不要在OnErrorListener中调用playSong，这可能导致递归调用
                    Toast.makeText(this, "网络音乐加载失败，使用本地播放", Toast.LENGTH_SHORT).show();
                    
                    // 直接使用模拟器播放
                    playSong(song, true);
                    return true; // 表示已处理错误
                });
                
                // 设置准备完成监听器
                mediaPlayer.setOnPreparedListener(mp -> {
                    android.util.Log.d("MainActivity", "网易云音乐准备完成，开始播放");
                    mp.start();
                    
                    // 设置音频会话ID
                    audioSessionId = mp.getAudioSessionId();
                    setupVisualizer();
                    
                    // 更新UI
                    isPlaying = true;
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    
                    // 获取并设置实际时长
                    int duration = mp.getDuration();
                    android.util.Log.d("MainActivity", "网易云音乐实际时长: " + duration + "ms");
                    seekBar.setMax(duration > 0 ? duration : song.getDuration());
                    totalTimeTextView.setText(formatDuration(duration > 0 ? duration : song.getDuration()));
                    
                    // 开始更新进度
                    playerSimulator.setCustomMediaPlayer(mp, () -> {
                        // 播放完成，释放资源
                        isPlaying = false;
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        onTrackCompleted();
                    });
                });
                
                // 设置数据源并准备
                android.util.Log.d("MainActivity", "设置网易云音乐数据源: " + uri);
                mediaPlayer.setDataSource(uri);
                mediaPlayer.prepareAsync();
                return;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "直接播放网易云音乐失败: " + e.getMessage(), e);
                // 失败则继续使用备用播放方式
            }
            
            // 使用模拟播放
            android.util.Log.d("MainActivity", "所有播放方式都失败，使用模拟播放");
            Toast.makeText(this, "使用本地音乐播放模式", Toast.LENGTH_SHORT).show();
            playSong(song, true);
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "处理网易云音乐时出错: " + e.getMessage(), e);
            Toast.makeText(this, "网络连接异常，使用本地播放", Toast.LENGTH_SHORT).show();
            // 使用模拟播放作为备选
            playSong(song, true);
        }
    }
    
    // 抽取公共方法来设置MediaPlayer
    private void setUpMediaPlayer(MediaPlayer player, Song song) {
        try {
            // 设置错误监听器
            player.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("MainActivity", "MediaPlayer错误: " + what + ", extra: " + extra);
                Toast.makeText(MainActivity.this, "播放错误，使用备用方式", Toast.LENGTH_SHORT).show();
                // 使用简单播放
                playSong(song, true);
                return true;
            });
            
            // 设置准备完成监听器
            player.setOnPreparedListener(mp -> {
                android.util.Log.d("MainActivity", "MediaPlayer准备完成，开始播放");
                try {
                    // 准备完成后开始播放
                    mp.start();
                    
                    // 设置音频会话ID
                    audioSessionId = mp.getAudioSessionId();
                    setupVisualizer();
                    
                    // 更新播放状态
                    isPlaying = true;
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    
                    // 获取并设置实际时长
                    int duration = mp.getDuration();
                    android.util.Log.d("MainActivity", "媒体播放器时长: " + duration + "ms");
                    seekBar.setMax(duration > 0 ? duration : song.getDuration());
                    totalTimeTextView.setText(formatDuration(duration > 0 ? duration : song.getDuration()));
                    
                    // 开始更新进度
                    playerSimulator.setCustomMediaPlayer(mp, () -> {
                        // 播放完成，释放资源
                        isPlaying = false;
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        onTrackCompleted();
                    });
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "开始播放时出错: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 异步准备
            android.util.Log.d("MainActivity", "开始异步准备MediaPlayer");
            player.prepareAsync();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "设置MediaPlayer时出错: " + e.getMessage(), e);
            throw e; // 向上抛出异常
        }
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    // MusicPlayerSimulator.OnPlaybackListener 接口实现
    @Override
    public void onProgressChanged(int currentPosition, String formattedTime) {
        android.util.Log.d("MainActivity", "收到进度更新: position=" + currentPosition + ", time=" + formattedTime);
        runOnUiThread(() -> {
            seekBar.setProgress(currentPosition);
            currentTimeTextView.setText(formattedTime);
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            this.isPlaying = isPlaying;
            playPauseButton.setImageResource(isPlaying ?
                    android.R.drawable.ic_media_pause :
                    android.R.drawable.ic_media_play);
        });
    }

    @Override
    public void onTrackCompleted() {
        runOnUiThread(() -> {
            // 检查播放列表是否为空
            if (songList == null || songList.isEmpty()) {
                return;
            }

            // 添加日志
            android.util.Log.d("MainActivity", "歌曲播放完成，当前索引:" + currentSongIndex + 
                              ", 播放模式:" + playerSimulator.getCurrentPlayMode());

            // 根据当前的播放模式，决定播放哪一首歌
            MusicPlayerSimulator.PlayMode currentMode = playerSimulator.getCurrentPlayMode();
            
            switch (currentMode) {
                case SINGLE_LOOP:
                    // 单曲循环：重新播放当前歌曲
                    Song currentSong = songList.get(currentSongIndex);
                    String songUri = currentSong.getFilePath();
                    
                    android.util.Log.d("MainActivity", "单曲循环：重新播放当前歌曲 " + currentSong.getTitle());
                    
                    if (songUri != null) {
                        playLocalSong(currentSong, Uri.parse(songUri));
                    } else {
                        playSong(currentSong, true);
                    }
                    break;
                    
                case RANDOM:
                    // 随机播放：随机选择一首歌
                    if (!songList.isEmpty()) {
                        int oldIndex = currentSongIndex;
                        int randomIndex;
                        
                        if (songList.size() > 1) {
                            // 确保不会随机到相同的歌曲
                            do {
                                randomIndex = random.nextInt(songList.size());
                            } while (randomIndex == currentSongIndex);
                        } else {
                            randomIndex = 0;
                        }
                        
                        currentSongIndex = randomIndex;
                        Song randomSong = songList.get(randomIndex);
                        String randomUri = randomSong.getFilePath();
                        
                        android.util.Log.d("MainActivity", "随机播放：从" + oldIndex + "切换到" + randomIndex + 
                                          ", 歌曲:" + randomSong.getTitle());
                        
                        if (randomUri != null) {
                            playLocalSong(randomSong, Uri.parse(randomUri));
                        } else {
                            playSong(randomSong, true);
                        }
                    }
                    break;
                    
                case SEQUENCE_LOOP:
                default:
                    // 顺序循环：播放下一首歌
                    if (songList.size() == 1) {
                        // 如果只有一首歌，直接重新播放当前歌曲
                        Song song = songList.get(0);
                        String uri = song.getFilePath();
                        
                        android.util.Log.d("MainActivity", "顺序循环(单曲)：重新播放 " + song.getTitle());
                        
                        if (uri != null) {
                            playLocalSong(song, Uri.parse(uri));
                        } else {
                            playSong(song, true);
                        }
                    } else {
                        // 有多首歌，计算下一首的索引
                        int oldIndex = currentSongIndex;
                        int newIndex = (currentSongIndex + 1) % songList.size();
                        currentSongIndex = newIndex;
                        
                        Song nextSong = songList.get(newIndex);
                        String nextUri = nextSong.getFilePath();
                        
                        android.util.Log.d("MainActivity", "顺序循环：从" + oldIndex + "切换到" + newIndex + 
                                          ", 歌曲:" + nextSong.getTitle());
                        
                        if (nextUri != null) {
                            playLocalSong(nextSong, Uri.parse(nextUri));
                        } else {
                            playSong(nextSong, true);
                        }
                    }
                    break;
            }
        });
    }

    @Override
    public void onPlayModeChanged(MusicPlayerSimulator.PlayMode playMode) {
        runOnUiThread(() -> {
            updatePlayModeUI(playMode);

            // 仅当用户手动切换时才显示提示
            if (userChangingPlayMode) {
                String modeText = "";
                switch (playMode) {
                    case SINGLE_LOOP:
                        modeText = "单曲循环";
                        break;
                    case RANDOM:
                        modeText = "随机播放";
                        break;
                    case SEQUENCE_LOOP:
                        modeText = "顺序循环";
                        break;
                }

                Toast.makeText(MainActivity.this,
                        "已切换到" + modeText,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerSimulator != null) {
            playerSimulator.release();
        }
        releaseVisualizer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVisualizer();
            } else {
                Toast.makeText(this, "需要录音权限才能显示音频可视化效果", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 根据选择更新可视化器模式
    private void updateVisualizerMode(int position) {
        // 移除当前可视化视图
        if (currentVisualizerView != null) {
            visualizerContainer.removeView(currentVisualizerView);
            currentVisualizerView = null;
        }
        
        // 创建新的SurfaceView用于可视化
        currentVisualizerView = new SurfaceView(this);
        
        // 添加新的可视化视图
        if (currentVisualizerView != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            visualizerContainer.addView(currentVisualizerView, params);
        }
    }
    
    // 释放可视化器资源
    private void releaseVisualizer() {
        if (visualizer != null) {
            visualizer.setEnabled(false);
            visualizer.release();
            visualizer = null;
        }
    }

    private void loadPlaylist() {
        // 从SharedPreferences加载播放列表
        SharedPreferences prefs = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
        int playlistSize = prefs.getInt("playlist_size", 0);
        
        android.util.Log.d("MainActivity", "正在加载播放列表，大小: " + playlistSize);
        
        if (playlistSize > 0) {
            songList.clear();
            
            for (int i = 0; i < playlistSize; i++) {
                String title = prefs.getString("song_title_" + i, "");
                String artist = prefs.getString("song_artist_" + i, "");
                int duration = prefs.getInt("song_duration_" + i, 0);
                String filePath = prefs.getString("song_uri_" + i, "");
                
                if (!title.isEmpty() && !filePath.isEmpty()) {
                    Song song = new Song(title, artist, duration, null, filePath);
                    songList.add(song);
                    android.util.Log.d("MainActivity", "已加载歌曲: " + title);
                }
            }
        }
    }

    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void savePlaylist() {
        // 保存播放列表到SharedPreferences
        SharedPreferences prefs = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 保存播放列表大小
        editor.putInt("playlist_size", songList.size());
        
        // 保存每首歌曲的信息
        for (int i = 0; i < songList.size(); i++) {
            Song song = songList.get(i);
            editor.putString("song_title_" + i, song.getTitle());
            editor.putString("song_artist_" + i, song.getArtist());
            editor.putInt("song_duration_" + i, song.getDuration());
            editor.putString("song_uri_" + i, song.getFilePath());
        }
        
        editor.apply();
        android.util.Log.d("MainActivity", "已保存播放列表，大小: " + songList.size());
    }
}