package com.example.huanhuanmusicplayerapp.network;

import android.util.Log;

import com.example.huanhuanmusicplayerapp.model.NetworkSong;
import com.example.huanhuanmusicplayerapp.model.Song;
import com.example.huanhuanmusicplayerapp.network.api.JiosaavnMusicApiService;
import com.example.huanhuanmusicplayerapp.network.api.MusicApiService;

import java.util.List;

public class NetworkMusicManager {
    private static final String TAG = "NetworkMusicManager";
    private static NetworkMusicManager instance;
    
    private MusicApiService apiService;
    
    private NetworkMusicManager() {
        // 初始化API服务
        apiService = new JiosaavnMusicApiService();
    }
    
    public static synchronized NetworkMusicManager getInstance() {
        if (instance == null) {
            instance = new NetworkMusicManager();
        }
        return instance;
    }
    
    public void searchSongs(String keyword, MusicApiService.OnSearchResultListener listener) {
        Log.d(TAG, "搜索歌曲: " + keyword);
        apiService.searchSongs(keyword, listener);
    }
    
    public void getSongUrl(NetworkSong song, MusicApiService.OnSongUrlListener listener) {
        Log.d(TAG, "获取歌曲URL: " + song.getTitle());
        apiService.getSongUrl(song.getSongId(), song.getSourceType(), listener);
    }
    
    // 将NetworkSong转换为常规Song以便添加到播放列表
    public Song convertToSong(NetworkSong networkSong) {
        return new Song(
                networkSong.getTitle(),
                networkSong.getArtist(),
                networkSong.getDuration(),
                networkSong.getAlbumCoverUrl(),
                networkSong.getStreamUrl()
        );
    }
}
