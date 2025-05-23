package com.example.huanhuanmusicplayerapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.huanhuanmusicplayerapp.R;
import com.example.huanhuanmusicplayerapp.adapter.NetworkSongAdapter;
import com.example.huanhuanmusicplayerapp.model.NetworkSong;
import com.example.huanhuanmusicplayerapp.network.NetworkMusicManager;
import com.example.huanhuanmusicplayerapp.network.api.MusicApiService;

import java.util.ArrayList;
import java.util.List;

public class NetworkMusicActivity extends AppCompatActivity implements NetworkSongAdapter.OnSongClickListener {

    private RecyclerView recyclerView;
    private NetworkSongAdapter adapter;
    private List<NetworkSong> songResults;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ProgressBar loadingProgressBar;
    private TextView emptyTextView;
    
    private NetworkMusicManager musicManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_music);
        
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.network_music_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("网络音乐");
        
        // 初始化视图
        recyclerView = findViewById(R.id.network_music_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        loadingProgressBar = findViewById(R.id.loading_progress);
        emptyTextView = findViewById(R.id.empty_text_view);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songResults = new ArrayList<>();
        adapter = new NetworkSongAdapter(songResults, this);
        recyclerView.setAdapter(adapter);
        
        // 初始化网络音乐管理器
        musicManager = NetworkMusicManager.getInstance();
        
        // 设置搜索按钮点击事件
        searchButton.setOnClickListener(v -> {
            String keyword = searchEditText.getText().toString().trim();
            if (!keyword.isEmpty()) {
                searchMusic(keyword);
            } else {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void searchMusic(String keyword) {
        // 显示加载中
        loadingProgressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        
        // 调用API搜索
        musicManager.searchSongs(keyword, new MusicApiService.OnSearchResultListener() {
            @Override
            public void onSuccess(List<NetworkSong> songs) {
                // 更新UI
                loadingProgressBar.setVisibility(View.GONE);
                
                if (songs.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    // 更新数据
                    songResults.clear();
                    songResults.addAll(songs);
                    adapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public void onError(String message) {
                // 显示错误
                loadingProgressBar.setVisibility(View.GONE);
                emptyTextView.setText("搜索出错: " + message);
                emptyTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                
                Toast.makeText(NetworkMusicActivity.this, "搜索失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onSongClick(int position, NetworkSong song) {
        // 获取歌曲URL
        loadingProgressBar.setVisibility(View.VISIBLE);
        
        // 设置列表项不可点击，避免重复点击
        recyclerView.setEnabled(false);
        
        android.util.Log.d("NetworkMusicActivity", "点击歌曲: " + song.getTitle() + ", ID: " + song.getSongId());
        
        // 显示选中提示
        Toast.makeText(this, "正在准备: " + song.getTitle(), Toast.LENGTH_SHORT).show();
        
        musicManager.getSongUrl(song, new MusicApiService.OnSongUrlListener() {
            @Override
            public void onSuccess(String url) {
                loadingProgressBar.setVisibility(View.GONE);
                recyclerView.setEnabled(true);
                
                android.util.Log.d("NetworkMusicActivity", "获取到歌曲URL: " + url);
                
                // 检查URL是否为空
                if (url == null || url.trim().isEmpty()) {
                    android.util.Log.e("NetworkMusicActivity", "获取到的URL为空");
                    Toast.makeText(NetworkMusicActivity.this, "获取播放链接失败，请尝试其他歌曲", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 更新歌曲URL
                song.setStreamUrl(url);
                
                try {
                    // 返回选中的歌曲到播放列表
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("song_title", song.getTitle());
                    resultIntent.putExtra("song_artist", song.getArtist());
                    resultIntent.putExtra("song_duration", song.getDuration());
                    resultIntent.putExtra("song_uri", song.getStreamUrl());
                    resultIntent.putExtra("song_cover", song.getAlbumCoverUrl());
                    resultIntent.putExtra("song_source", song.getSourceType());
                    
                    android.util.Log.d("NetworkMusicActivity", "设置结果并返回");
                    setResult(RESULT_OK, resultIntent);
                    
                    // 平滑退出
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } catch (Exception e) {
                    android.util.Log.e("NetworkMusicActivity", "设置结果时出错: " + e.getMessage(), e);
                    Toast.makeText(NetworkMusicActivity.this, "处理歌曲数据时出错", Toast.LENGTH_SHORT).show();
                    recyclerView.setEnabled(true);
                }
            }
            
            @Override
            public void onError(String message) {
                loadingProgressBar.setVisibility(View.GONE);
                recyclerView.setEnabled(true);
                
                android.util.Log.e("NetworkMusicActivity", "获取URL失败: " + message);
                
                // 尝试使用一个备用的URL格式
                String backupUrl = "https://music.163.com/song/media/outer/url?id=" + song.getSongId() + ".mp3";
                android.util.Log.d("NetworkMusicActivity", "尝试使用备用URL: " + backupUrl);
                
                song.setStreamUrl(backupUrl);
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("song_title", song.getTitle());
                resultIntent.putExtra("song_artist", song.getArtist());
                resultIntent.putExtra("song_duration", song.getDuration());
                resultIntent.putExtra("song_uri", backupUrl);
                resultIntent.putExtra("song_cover", song.getAlbumCoverUrl());
                resultIntent.putExtra("song_source", song.getSourceType());
                
                setResult(RESULT_OK, resultIntent);
                
                // 平滑退出
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
