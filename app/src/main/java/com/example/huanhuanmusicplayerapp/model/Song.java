package com.example.huanhuanmusicplayerapp.model;

public class Song {
    private String title;
    private String artist;
    private int duration; // 歌曲时长（毫秒）
    private String albumCoverUrl; // 专辑封面URL或资源ID
    private String filePath; // 本地文件路径

    public Song(String title, String artist, int duration, String albumCoverUrl) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.albumCoverUrl = albumCoverUrl;
        this.filePath = null;
    }

    public Song(String title, String artist, int duration, String albumCoverUrl, String filePath) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.albumCoverUrl = albumCoverUrl;
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    public String getAlbumCoverUrl() {
        return albumCoverUrl;
    }

    public String getFilePath() {
        return filePath;
    }
}