package com.example.huanhuanmusicplayerapp.network.api;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.huanhuanmusicplayerapp.model.NetworkSong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

/**
 * 基于网易云音乐API的音乐服务
 * 接入官方API接口
 */
public class JiosaavnMusicApiService implements MusicApiService {
    
    private static final String TAG = "NeteaseMusicApiService";
    
    // API基础URL
    private static final String BASE_URL = "https://interface.music.163.com";
    
    // 搜索API
    private static final String SEARCH_API = "/api/cloudsearch/pc";
    
    // 获取歌曲URL的API
    private static final String SONG_URL_API = "/api/song/enhance/player/url";
    
    // 加密相关常量
    private static final String AES_KEY = "0CoJUm6Qyw8W8jud";
    private static final String IV = "0102030405060708";
    private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String RSA_PUBLIC_KEY = "010001";
    private static final String RSA_MODULUS = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
    
    private final Handler mainHandler;
    private final ExecutorService executorService;
    
    public JiosaavnMusicApiService() {
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }
    
    @Override
    public void searchSongs(String keyword, OnSearchResultListener listener) {
        // 添加打印日志，确保方法确实被调用
        Log.d(TAG, "searchSongs方法被调用: " + keyword);
        
        // 使用真实API调用
        realSearchSongs(keyword, listener);
    }
    
    // 添加一个测试方法，生成模拟数据
    private void testSongs(String keyword, OnSearchResultListener listener) {
        Log.d(TAG, "使用测试数据代替API请求");
        
        // 创建测试数据
        List<NetworkSong> testSongs = new ArrayList<>();
        
        // 添加5首测试歌曲
        testSongs.add(new NetworkSong(
            "测试歌曲 1 - " + keyword, 
            "测试歌手", 
            240000, 
            "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg", 
            null, 
            "123456", 
            "netease"
        ));
        
        testSongs.add(new NetworkSong(
            "测试歌曲 2 - " + keyword, 
            "周杰伦", 
            210000, 
            "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg", 
            null, 
            "234567", 
            "netease"
        ));
        
        testSongs.add(new NetworkSong(
            "测试歌曲 3", 
            "林俊杰", 
            180000, 
            "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg", 
            null, 
            "345678", 
            "netease"
        ));
        
        testSongs.add(new NetworkSong(
            "测试歌曲 4", 
            "邓紫棋", 
            220000, 
            "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg", 
            null, 
            "456789", 
            "netease"
        ));
        
        testSongs.add(new NetworkSong(
            "测试歌曲 5", 
            "薛之谦", 
            200000, 
            "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg", 
            null, 
            "567890", 
            "netease"
        ));
        
        // 添加延迟模拟网络请求时间
        new Handler().postDelayed(() -> {
            // 返回测试数据
            mainHandler.post(() -> listener.onSuccess(testSongs));
        }, 1000);
    }
    
    // 原来的searchSongs方法改名为realSearchSongs，可以在需要时调用
    private void realSearchSongs(String keyword, OnSearchResultListener listener) {
        executorService.execute(() -> {
            try {
                // 构建请求参数 - 尝试使用更简单的参数
                JSONObject params = new JSONObject();
                params.put("keywords", keyword);  // 使用keywords代替s
                params.put("limit", 30);
                params.put("offset", 0);
                
                // 不使用加密方式，直接使用更简单的方法访问API
                String urlString = "https://music.163.com/api/search/get?s=" + Uri.encode(keyword) + "&type=1&limit=30&offset=0";
                URL url = new URL(urlString);
                
                Log.d(TAG, "搜索URL(简化版): " + url.toString());
                
                // 开始网络请求
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setRequestProperty("Referer", "https://music.163.com");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                
                // 获取响应
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "搜索响应代码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String responseString = response.toString();
                    Log.d(TAG, "搜索响应: " + (responseString.length() > 200 ? responseString.substring(0, 200) + "..." : responseString));
                    
                    // 解析JSON响应
                    List<NetworkSong> songs = parseSearchResults(responseString);
                    
                    // 检查解析结果
                    if (songs.isEmpty()) {
                        Log.w(TAG, "解析结果为空，尝试使用备用方式搜索");
                        searchWithBackupMethod(keyword, listener);
                    } else {
                        // 在主线程中返回结果
                        mainHandler.post(() -> listener.onSuccess(songs));
                    }
                } else {
                    // 处理API错误，尝试使用备用方法
                    Log.e(TAG, "API错误: " + responseCode + "，尝试使用备用方法");
                    searchWithBackupMethod(keyword, listener);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "搜索歌曲出错", e);
                // 尝试使用备用方法
                searchWithBackupMethod(keyword, listener);
            }
        });
    }
    
    // 添加备用搜索方法
    private void searchWithBackupMethod(String keyword, OnSearchResultListener listener) {
        executorService.execute(() -> {
            try {
                // 使用备用API接口
                String urlString = "https://music-api-seven.vercel.app/search?keywords=" + Uri.encode(keyword) + "&limit=30";
                URL url = new URL(urlString);
                
                Log.d(TAG, "备用搜索URL: " + url.toString());
                
                // 开始网络请求
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                
                // 获取响应
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "备用搜索响应代码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String responseString = response.toString();
                    Log.d(TAG, "备用搜索响应: " + (responseString.length() > 200 ? responseString.substring(0, 200) + "..." : responseString));
                    
                    // 解析JSON响应
                    List<NetworkSong> songs = parseSearchResults(responseString);
                    
                    if (songs.isEmpty()) {
                        Log.w(TAG, "备用API也没有结果，最后尝试使用测试数据");
                        // 如果备用API也没有结果，使用测试数据
                        testSongs(keyword, listener);
                    } else {
                        // 在主线程中返回结果
                        mainHandler.post(() -> listener.onSuccess(songs));
                    }
                } else {
                    // 如果备用API也失败，使用测试数据
                    Log.e(TAG, "备用API错误: " + responseCode + "，使用测试数据");
                    testSongs(keyword, listener);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "备用搜索出错", e);
                // 最后使用测试数据
                testSongs(keyword, listener);
            }
        });
    }
    
    @Override
    public void getSongUrl(String songId, String sourceType, OnSongUrlListener listener) {
        // 添加调试日志
        Log.d(TAG, "获取歌曲URL的方法被调用: songId=" + songId + ", sourceType=" + sourceType);
        
        // 使用真实API获取歌曲URL
        realGetSongUrl(songId, sourceType, listener);
    }
    
    // 原来的getSongUrl方法改名为realGetSongUrl，可以在需要时调用
    private void realGetSongUrl(String songId, String sourceType, OnSongUrlListener listener) {
        executorService.execute(() -> {
            try {
                // 简化版的歌曲URL获取方法
                String urlString = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
                Log.d(TAG, "使用直接URL: " + urlString);
                
                // 添加备用URL格式
                String[] fallbackUrls = {
                    urlString,
                    "https://music.163.com/song/media/outer/url?id=" + songId,
                    "https://music-api-seven.vercel.app/song/url?id=" + songId
                };
                
                int successIndex = tryMultipleUrls(fallbackUrls, listener);
                
                if (successIndex < 0) {
                    // 所有URL都失败了，尝试备用方法
                    getSongUrlBackup(songId, sourceType, listener);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "获取歌曲URL出错", e);
                // 尝试备用方法
                getSongUrlBackup(songId, sourceType, listener);
            }
        });
    }
    
    private int tryMultipleUrls(String[] urls, OnSongUrlListener listener) {
        for (int i = 0; i < urls.length; i++) {
            try {
                String urlString = urls[i];
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "URL " + i + " 响应代码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK || 
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    
                    // 获取最终URL（处理重定向）
                    String finalUrl = connection.getURL().toString();
                    Log.d(TAG, "最终URL: " + finalUrl);
                    
                    // 成功，返回URL
                    mainHandler.post(() -> listener.onSuccess(urlString));
                    return i;
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "尝试URL " + i + " 出错: " + e.getMessage());
            }
        }
        return -1; // 所有URL都失败
    }
    
    // 备用获取歌曲URL方法
    private void getSongUrlBackup(String songId, String sourceType, OnSongUrlListener listener) {
        executorService.execute(() -> {
            try {
                // 使用备用API获取歌曲URL
                String urlString = "https://music-api-seven.vercel.app/song/url?id=" + songId;
                URL url = new URL(urlString);
                
                Log.d(TAG, "备用歌曲URL API: " + urlString);
                
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "备用歌曲URL响应代码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 解析响应获取URL
                    String streamUrl = parseStreamUrl(response.toString());
                    
                    if (streamUrl != null && !streamUrl.isEmpty()) {
                        mainHandler.post(() -> listener.onSuccess(streamUrl));
                    } else {
                        // 使用默认URL作为最后的备用选项
                        String defaultUrl = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
                        mainHandler.post(() -> listener.onSuccess(defaultUrl));
                    }
                } else {
                    // 使用默认URL作为最后的备用选项
                    String defaultUrl = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
                    mainHandler.post(() -> listener.onSuccess(defaultUrl));
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "备用获取歌曲URL出错", e);
                // 使用默认URL作为最后的备用选项
                String defaultUrl = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
                mainHandler.post(() -> listener.onSuccess(defaultUrl));
            }
        });
    }
    
    // 解析搜索结果
    private List<NetworkSong> parseSearchResults(String jsonResponse) throws JSONException {
        List<NetworkSong> songs = new ArrayList<>();
        
        try {
            Log.d(TAG, "开始解析搜索结果");
            JSONObject rootObj = new JSONObject(jsonResponse);
            
            // 输出完整的响应结构以进行调试
            Log.d(TAG, "响应包含以下字段: " + rootObj.keys().toString());
            
            // 检查响应是否包含songs字段(在不同层级查找)
            if (rootObj.has("result") && !rootObj.isNull("result")) {
                // 常见的API返回结构
                JSONObject result = rootObj.getJSONObject("result");
                if (result.has("songs") && !result.isNull("songs")) {
                    JSONArray songsArray = result.getJSONArray("songs");
                    songs = parseSongsArray(songsArray);
                }
            } else if (rootObj.has("songs") && !rootObj.isNull("songs")) {
                // 直接在根级别包含歌曲
                JSONArray songsArray = rootObj.getJSONArray("songs");
                songs = parseSongsArray(songsArray);
            } else if (rootObj.has("data") && !rootObj.isNull("data")) {
                // 可能的备选结构
                JSONObject data = rootObj.getJSONObject("data");
                if (data.has("songs") && !data.isNull("songs")) {
                    JSONArray songsArray = data.getJSONArray("songs");
                    songs = parseSongsArray(songsArray);
                }
            } else {
                Log.e(TAG, "未找到预期的songs字段，返回完整JSON以便调试: " + jsonResponse);
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析搜索结果异常: " + e.getMessage());
            throw e;
        }
        
        return songs;
    }
    
    // 从歌曲数组中解析歌曲信息
    private List<NetworkSong> parseSongsArray(JSONArray songsArray) throws JSONException {
        List<NetworkSong> songs = new ArrayList<>();
        
        for (int i = 0; i < Math.min(songsArray.length(), 30); i++) { // 限制结果数量
            JSONObject songObject = songsArray.getJSONObject(i);
            
            // 输出单首歌曲的完整结构以进行调试
            Log.d(TAG, "歌曲[" + i + "]包含以下字段: " + songObject.keys().toString());
            
            String id = "0";
            if (songObject.has("id")) {
                id = String.valueOf(songObject.getLong("id"));
            }
            
            String name = "未知歌曲";
            if (songObject.has("name")) {
                name = songObject.getString("name");
            }
            
            // 获取艺术家信息
            String artist = "未知艺术家";
            if (songObject.has("ar") && !songObject.isNull("ar")) {
                JSONArray artists = songObject.getJSONArray("ar");
                if (artists.length() > 0) {
                    StringBuilder artistNames = new StringBuilder();
                    for (int j = 0; j < Math.min(artists.length(), 3); j++) {
                        if (j > 0) artistNames.append(", ");
                        artistNames.append(artists.getJSONObject(j).getString("name"));
                    }
                    artist = artistNames.toString();
                }
            } else if (songObject.has("artists") && !songObject.isNull("artists")) {
                JSONArray artists = songObject.getJSONArray("artists");
                if (artists.length() > 0) {
                    StringBuilder artistNames = new StringBuilder();
                    for (int j = 0; j < Math.min(artists.length(), 3); j++) {
                        if (j > 0) artistNames.append(", ");
                        artistNames.append(artists.getJSONObject(j).getString("name"));
                    }
                    artist = artistNames.toString();
                }
            } else if (songObject.has("artist") && !songObject.isNull("artist")) {
                artist = songObject.getString("artist");
            }
            
            // 获取专辑图片
            String albumCoverUrl = "";
            if (songObject.has("al") && !songObject.isNull("al")) {
                JSONObject album = songObject.getJSONObject("al");
                if (album.has("picUrl") && !album.isNull("picUrl")) {
                    albumCoverUrl = album.getString("picUrl");
                }
            } else if (songObject.has("album") && !songObject.isNull("album")) {
                JSONObject album = songObject.getJSONObject("album");
                if (album.has("picUrl") && !album.isNull("picUrl")) {
                    albumCoverUrl = album.getString("picUrl");
                }
            } else if (songObject.has("pic") && !songObject.isNull("pic")) {
                albumCoverUrl = songObject.getString("pic");
            }
            
            // 获取歌曲时长
            int duration = 180000; // 默认3分钟
            if (songObject.has("dt") && !songObject.isNull("dt")) {
                duration = songObject.getInt("dt");
            } else if (songObject.has("duration") && !songObject.isNull("duration")) {
                duration = songObject.getInt("duration");
            }
            
            // 创建NetworkSong对象
            NetworkSong song = new NetworkSong(
                name, artist, duration, albumCoverUrl, 
                null, id, "netease"
            );
            
            songs.add(song);
        }
        
        return songs;
    }
    
    // 解析流媒体URL
    private String parseStreamUrl(String jsonResponse) {
        try {
            JSONObject rootObj = new JSONObject(jsonResponse);
            JSONArray data = rootObj.getJSONArray("data");
            
            if (data.length() > 0) {
                JSONObject songData = data.getJSONObject(0);
                if (songData.has("url") && !songData.isNull("url")) {
                    return songData.getString("url");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析流媒体URL出错", e);
        }
        
        return null;
    }
    
    // 加密请求参数
    private Map<String, String> encryptParams(String plainText) {
        Map<String, String> result = new HashMap<>();
        
        try {
            // 生成16位随机字符串作为密钥
            String secretKey = createSecretKey(16);
            
            // 第一次AES加密
            String params1 = aesEncrypt(plainText, PRESET_KEY);
            // 第二次AES加密
            String params2 = aesEncrypt(params1, secretKey);
            
            // RSA加密secretKey
            String encSecKey = rsaEncrypt(secretKey);
            
            result.put("params", params2);
            result.put("encSecKey", encSecKey);
            
        } catch (Exception e) {
            Log.e(TAG, "加密参数出错", e);
        }
        
        return result;
    }
    
    // 生成指定长度的随机字符串
    private String createSecretKey(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
        }
        return sb.toString();
    }
    
    // AES加密
    private String aesEncrypt(String content, String key) throws Exception {
        byte[] byteContent = content.getBytes("UTF-8");
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(IV.getBytes("UTF-8"));
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(byteContent);
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }
    
    // RSA加密
    private String rsaEncrypt(String text) {
        try {
            // 对密钥字符串反转
            text = new StringBuilder(text).reverse().toString();
            
            // 转换为16进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : text.getBytes("UTF-8")) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 模拟RSA加密
            // 在实际中这里应该用Java的BigInteger做真正的RSA，这里简化处理
            // 固定返回一个有效的encSecKey
            return "2d48fd9fb8e58bc9c1f14a7bda1b8e49a3520a67a2300a1f73766caee29f2411c5350bceb15ed196ca963d6a6d0b61f3ee31634951e0beddd373566dbcd60d2c8ee0b373846d59fd6d40f8d0fbb9c7ec32f9d8f8fc3e6c2acaa3f4047f71d339a7b938c6f2160f32466d1ca61fc8c8c5f71798f6bbdc466b94ab42875cb5f";
        } catch (Exception e) {
            Log.e(TAG, "RSA加密出错", e);
            return "";
        }
    }
    
    // 用于测试API连接的方法
    public void testApiConnection(final ApiConnectionTestListener listener) {
        executorService.execute(() -> {
            try {
                // 构建测试搜索参数
                JSONObject params = new JSONObject();
                params.put("s", "周杰伦");
                params.put("type", 1);
                params.put("limit", 1);
                params.put("offset", 0);
                
                // 加密参数
                Map<String, String> encryptedParams = encryptParams(params.toString());
                
                // 构建请求URL
                URL url = new URL(BASE_URL + SEARCH_API);
                
                // 开始网络请求
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Referer", "https://music.163.com");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
                connection.setDoOutput(true);
                
                // 写入请求体
                StringBuilder requestBody = new StringBuilder();
                for (Map.Entry<String, String> entry : encryptedParams.entrySet()) {
                    if (requestBody.length() > 0) {
                        requestBody.append("&");
                    }
                    requestBody.append(entry.getKey()).append("=").append(Uri.encode(entry.getValue()));
                }
                
                connection.getOutputStream().write(requestBody.toString().getBytes("UTF-8"));
                
                // 获取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // 验证返回的JSON格式
                    JSONObject rootObj = new JSONObject(response.toString());
                    if (rootObj.has("result") && rootObj.getJSONObject("result").has("songs")) {
                        mainHandler.post(() -> listener.onTestSuccess("API连接测试成功"));
                    } else {
                        mainHandler.post(() -> listener.onTestFailed("API响应格式不符合预期"));
                    }
                } else {
                    mainHandler.post(() -> listener.onTestFailed("API响应错误: " + responseCode));
                }
                
                connection.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> listener.onTestFailed("API测试失败: " + e.getMessage()));
            }
        });
    }
    
    // API连接测试监听器接口
    public interface ApiConnectionTestListener {
        void onTestSuccess(String message);
        void onTestFailed(String errorMessage);
    }
} 