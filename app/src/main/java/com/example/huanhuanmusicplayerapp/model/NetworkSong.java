// app/src/main/java/com/example/huanhuanmusicplayerapp/model/NetworkSong.java
package com.example.huanhuanmusicplayerapp.model;

public class NetworkSong extends Song {
    private String streamUrl;
    private String songId;
    private String sourceType;
    
    public NetworkSong(String title, String artist, int duration, String albumCoverUrl, 
                      String streamUrl, String songId, String sourceType) {
        super(title, artist, duration, albumCoverUrl);
        this.streamUrl = streamUrl;
        this.songId = songId;
        this.sourceType = sourceType;
    }
    
    public String getStreamUrl() {
        return streamUrl;
    }
    
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
    
    public String getSongId() {
        return songId;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    @Override
    public String getFilePath() {
        return streamUrl; // 对于网络歌曲，使用流媒体URL作为filePath
    }
}