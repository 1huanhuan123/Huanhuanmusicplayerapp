package com.example.huanhuanmusicplayerapp.network.api;

import com.example.huanhuanmusicplayerapp.model.NetworkSong;
import java.util.List;

public interface MusicApiService {
    interface OnSearchResultListener {
        void onSuccess(List<NetworkSong> songs);
        void onError(String message);
    }
    
    interface OnSongUrlListener {
        void onSuccess(String url);
        void onError(String message);
    }
    
    void searchSongs(String keyword, OnSearchResultListener listener);
    void getSongUrl(String songId, String sourceType, OnSongUrlListener listener);
}
