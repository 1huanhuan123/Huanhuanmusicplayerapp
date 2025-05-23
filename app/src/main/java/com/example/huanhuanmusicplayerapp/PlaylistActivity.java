package com.example.huanhuanmusicplayerapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.huanhuanmusicplayerapp.model.Song;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlaylistActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnSongActionListener {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private List<Song> songList;
    private FloatingActionButton fabAddMusic;
    private boolean isMultiSelectMode = false;
    private Set<Integer> selectedItems = new HashSet<>();
    private int currentSongIndex = -1;
    private MediaPlayer mediaPlayer;
    
    // 用于保存播放列表的常量
    private static final String PREFS_NAME = "PlaylistPrefs";
    private static final String KEY_PLAYLIST_SIZE = "playlist_size";
    private static final String KEY_SONG_TITLE = "song_title_";
    private static final String KEY_SONG_ARTIST = "song_artist_";
    private static final String KEY_SONG_DURATION = "song_duration_";
    private static final String KEY_SONG_URI = "song_uri_";

    public static final String EXTRA_SONG_POSITION = "song_position";
    public static final String EXTRA_SONG_TITLE = "song_title";
    public static final String EXTRA_SONG_ARTIST = "song_artist";
    public static final String EXTRA_SONG_DURATION = "song_duration";

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.playlist_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("播放列表");

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化添加音乐按钮
        fabAddMusic = findViewById(R.id.fab_add_music);
        fabAddMusic.setOnClickListener(v -> checkPermissionAndPickFile());

        // 初始化文件选择器
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData());
                }
            }
        );

        // 初始化播放列表
        songList = new ArrayList<>();
        
        // 首先尝试从SharedPreferences加载保存的播放列表
        loadPlaylist();
        
        // 如果从SharedPreferences加载的列表为空，则尝试从Intent获取
        if (songList.isEmpty()) {
            // 尝试从Intent中获取播放列表数据
            Intent intent = getIntent();
            if (intent != null) {
                int playlistSize = intent.getIntExtra("playlist_size", 0);
                android.util.Log.d("PlaylistActivity", "从MainActivity接收到播放列表，大小: " + playlistSize);
                
                // 如果有播放列表数据，加载它
                if (playlistSize > 0) {
                    for (int i = 0; i < playlistSize; i++) {
                        String title = intent.getStringExtra("song_title_" + i);
                        String artist = intent.getStringExtra("song_artist_" + i);
                        int duration = intent.getIntExtra("song_duration_" + i, 0);
                        String uri = intent.getStringExtra("song_uri_" + i);
                        
                        // 创建新的Song对象并添加到列表
                        Song song = new Song(title, artist, duration, null, uri);
                        songList.add(song);
                    }
                } else {
                    // 如果没有播放列表数据，创建空列表
                    createEmptySongList();
                }
            } else {
                // 如果没有Intent，创建空列表
                createEmptySongList();
            }
        }

        // 设置适配器
        adapter = new SongAdapter(songList, this);
        adapter.setOnSongActionListener(this);
        recyclerView.setAdapter(adapter);

        // 注册上下文菜单
        registerForContextMenu(recyclerView);
    }

    private void createEmptySongList() {
        // 创建空的播放列表
        songList = new ArrayList<>();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongClick(int position, Song song) {
        if (isMultiSelectMode) {
            // 多选模式下，切换选中状态
            if (selectedItems.contains(position)) {
                selectedItems.remove(position);
            } else {
                selectedItems.add(position);
            }
            adapter.notifyItemChanged(position);
        } else {
            // 单选模式下，直接返回选中的歌曲
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SONG_POSITION, position);
            resultIntent.putExtra(EXTRA_SONG_TITLE, song.getTitle());
            resultIntent.putExtra(EXTRA_SONG_ARTIST, song.getArtist());
            resultIntent.putExtra(EXTRA_SONG_DURATION, song.getDuration());
            resultIntent.putExtra("song_uri", song.getFilePath());
            
            // 同步播放列表到MainActivity
            syncPlaylistToMainActivity(resultIntent);
            
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onSongEdit(int position, Song song) {
        showEditSongDialog(position, song);
    }

    @Override
    public void onSongDelete(int position, Song song) {
        if (position < 0 || position >= songList.size()) {
            Toast.makeText(this, "无效的歌曲位置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Song songToDelete = songList.get(position);
        new AlertDialog.Builder(this)
                .setTitle("删除歌曲")
                .setMessage("确定要将 \"" + songToDelete.getTitle() + "\" 从播放列表中删除吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 从列表中删除歌曲
                    songList.remove(position);
                    adapter.notifyItemRemoved(position);
                    
                    // 如果删除的是当前播放的歌曲，需要更新播放状态
                    if (position == currentSongIndex) {
                        if (songList.isEmpty()) {
                            // 如果播放列表为空，停止播放
                            if (mediaPlayer != null) {
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                            currentSongIndex = -1;
                        } else {
                            // 如果还有歌曲，播放下一首
                            if (currentSongIndex >= songList.size()) {
                                currentSongIndex = 0;
                            }
                            playSong(songList.get(currentSongIndex));
                        }
                    } else if (position < currentSongIndex) {
                        // 如果删除的是当前播放歌曲之前的歌曲，需要调整当前播放索引
                        currentSongIndex--;
                    }
                    
                    // 立即保存到SharedPreferences
                    savePlaylist();
                    
                    // 同步播放列表到MainActivity
                    Intent intent = new Intent();
                    syncPlaylistToMainActivity(intent);
                    intent.putExtra("playlist_updated", true);
                    setResult(RESULT_OK, intent);
                    
                    Toast.makeText(this, "已从播放列表中删除", Toast.LENGTH_SHORT).show();
                    
                    // 检查播放列表是否为空
                    if (songList.isEmpty()) {
                        finish();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void checkPermissionAndPickFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openFilePicker();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Intent data) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            int addedCount = 0;
            
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) {
                    if (processAudioFile(uri)) {
                        addedCount++;
                    }
                }
            }
            
            if (addedCount > 0) {
                Toast.makeText(this, "已添加 " + addedCount + " 首歌曲", Toast.LENGTH_SHORT).show();
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            if (processAudioFile(uri)) {
                Toast.makeText(this, "已添加歌曲", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private boolean processAudioFile(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null) {
                fileName = "未知歌曲";
            }

            String title = fileName;
            if (fileName.contains(".")) {
                title = fileName.substring(0, fileName.lastIndexOf("."));
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);

            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist == null || artist.isEmpty()) {
                artist = "未知艺术家";
            }

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int duration = 0;
            if (durationStr != null) {
                duration = Integer.parseInt(durationStr);
            }

            retriever.release();

            String newUri = copyFileToPrivateStorage(uri, fileName);
            if (newUri == null) {
                Toast.makeText(this, "无法保存音乐文件: " + title, Toast.LENGTH_SHORT).show();
                return false;
            }

            Song newSong = new Song(title, artist, duration, null, newUri);
            songList.add(newSong);
            adapter.notifyItemInserted(songList.size() - 1);
            
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "无法读取文件信息: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private String copyFileToPrivateStorage(Uri sourceUri, String fileName) {
        try {
            File musicDir = new File(getFilesDir(), "music");
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }

            File destFile = new File(musicDir, fileName);
            
            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            return Uri.fromFile(destFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "需要存储权限才能添加音乐文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void syncPlaylistToMainActivity(Intent intent) {
        intent.putExtra("playlist_updated", true);
        intent.putExtra("playlist_size", songList.size());
        
        for (int i = 0; i < songList.size(); i++) {
            Song song = songList.get(i);
            intent.putExtra("song_title_" + i, song.getTitle());
            intent.putExtra("song_artist_" + i, song.getArtist());
            intent.putExtra("song_duration_" + i, song.getDuration());
            intent.putExtra("song_uri_" + i, song.getFilePath());
        }
        
        setResult(RESULT_OK, intent);
    }

    private void showEditSongDialog(int position, Song song) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_song, null);

        EditText titleEdit = dialogView.findViewById(R.id.edit_song_title);
        EditText artistEdit = dialogView.findViewById(R.id.edit_artist_name);
        EditText durationEdit = dialogView.findViewById(R.id.edit_duration);

        titleEdit.setText(song.getTitle());
        artistEdit.setText(song.getArtist());

        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(song.getDuration());
        int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(song.getDuration()) -
                TimeUnit.MINUTES.toSeconds(minutes));
        durationEdit.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));

        new AlertDialog.Builder(this)
                .setTitle("编辑歌曲")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newTitle = titleEdit.getText().toString().trim();
                    String newArtist = artistEdit.getText().toString().trim();
                    String durationStr = durationEdit.getText().toString().trim();

                    if (TextUtils.isEmpty(newTitle) || TextUtils.isEmpty(newArtist) || TextUtils.isEmpty(durationStr)) {
                        Toast.makeText(this, "所有字段都需要填写", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newDuration = 0;
                    try {
                        String[] parts = durationStr.split(":");
                        if (parts.length == 2) {
                            int min = Integer.parseInt(parts[0]);
                            int sec = Integer.parseInt(parts[1]);
                            newDuration = (min * 60 + sec) * 1000;
                        } else {
                            throw new NumberFormatException("格式错误");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "时长格式无效，请使用分:秒格式", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Song updatedSong = new Song(newTitle, newArtist, newDuration, song.getAlbumCoverUrl(), song.getFilePath());
                    songList.set(position, updatedSong);
                    adapter.notifyItemChanged(position);
                    
                    // 立即保存到SharedPreferences
                    savePlaylist();

                    Toast.makeText(this, "歌曲已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        syncPlaylistToMainActivity(intent);
        intent.putExtra("playlist_updated", true);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 不再使用item.getOrder()获取位置
        // 直接让adapter处理菜单点击，它已经知道正确的位置
        return adapter.onContextItemSelected(item);
    }
    
    // 保存播放列表到SharedPreferences
    private void savePlaylist() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // 保存播放列表大小
        editor.putInt(KEY_PLAYLIST_SIZE, songList.size());
        
        // 保存每首歌曲的信息
        for (int i = 0; i < songList.size(); i++) {
            Song song = songList.get(i);
            editor.putString(KEY_SONG_TITLE + i, song.getTitle());
            editor.putString(KEY_SONG_ARTIST + i, song.getArtist());
            editor.putInt(KEY_SONG_DURATION + i, song.getDuration());
            editor.putString(KEY_SONG_URI + i, song.getFilePath());
        }
        
        editor.apply();
        android.util.Log.d("PlaylistActivity", "已保存播放列表，大小: " + songList.size());
    }
    
    // 从SharedPreferences加载播放列表
    private void loadPlaylist() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int playlistSize = prefs.getInt(KEY_PLAYLIST_SIZE, 0);
        
        android.util.Log.d("PlaylistActivity", "正在加载播放列表，大小: " + playlistSize);
        
        if (playlistSize > 0) {
            songList.clear();
            
            for (int i = 0; i < playlistSize; i++) {
                String title = prefs.getString(KEY_SONG_TITLE + i, "");
                String artist = prefs.getString(KEY_SONG_ARTIST + i, "");
                int duration = prefs.getInt(KEY_SONG_DURATION + i, 0);
                String filePath = prefs.getString(KEY_SONG_URI + i, "");
                
                if (!title.isEmpty() && !filePath.isEmpty()) {
                    Song song = new Song(title, artist, duration, null, filePath);
                    songList.add(song);
                    android.util.Log.d("PlaylistActivity", "已加载歌曲: " + title);
                }
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        savePlaylist();
    }

    private void playSong(Song song) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            // 更新当前播放索引
            currentSongIndex = songList.indexOf(song);
            
            // 同步播放状态到MainActivity
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SONG_POSITION, currentSongIndex);
            intent.putExtra(EXTRA_SONG_TITLE, song.getTitle());
            intent.putExtra(EXTRA_SONG_ARTIST, song.getArtist());
            intent.putExtra(EXTRA_SONG_DURATION, song.getDuration());
            intent.putExtra("song_uri", song.getFilePath());
            syncPlaylistToMainActivity(intent);
            setResult(RESULT_OK, intent);
            
        } catch (Exception e) {
            Toast.makeText(this, "无法播放歌曲: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 