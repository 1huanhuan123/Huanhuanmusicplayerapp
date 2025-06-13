# 欢欢音乐播放器 (Huanhuan Music Player)

一个功能丰富的Android音乐播放器应用，支持本地和网络音乐播放，具有美观的音乐可视化效果。

## 功能特色

- 🎵 **本地音乐播放** - 支持播放设备上的音乐文件
- 🌐 **网络音乐播放** - 可播放在线音乐资源
- 📋 **播放列表管理** - 创建和管理自己的播放列表
- 🎚️ **多种播放模式** - 支持顺序播放、单曲循环和随机播放
- 📊 **音乐可视化** - 多种音频可视化效果，包括波形、线条、波浪、圆形和条形
- 🎛️ **播放控制** - 播放/暂停、上一首/下一首、进度控制

## 截图


如何添加应用截图:
1. 在项目根目录创建一个screenshots文件夹
2. 将应用截图添加到该文件夹中
3. 使用下面的格式在README中引用这些截图:

![主界面](screenshots/main_screen.jpg)
![搜索界面](screenshots/search_screen.jpg)
![播放列表界面](screenshots/list.jpg)


## 技术栈

- Android原生开发
- Java编程语言
- Android Media Player API
- Android Visualizer API用于音频可视化
- 基于MVVM架构设计

## 项目结构

```
app/
├── src/
│   └── main/
│       ├── java/com/example/huanhuanmusicplayerapp/
│       │   ├── adapter/       # 适配器类
│       │   ├── model/         # 数据模型
│       │   ├── network/       # 网络相关
│       │   ├── ui/            # UI组件
│       │   ├── MainActivity.java      # 主界面
│       │   ├── PlaylistActivity.java  # 播放列表界面
│       │   └── MusicPlayerSimulator.java  # 音乐播放模拟器
│       └── res/
│           ├── layout/        # 布局文件
│           └── ...
└── ...
```

## 运行要求

- Android SDK 24+
- Java 17+
- Gradle 8.0+

## 如何使用

1. 克隆此仓库
2. 在Android Studio中打开项目
3. 构建并在设备上运行

## 贡献指南

1. Fork 这个项目
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m '添加了一些很棒的功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启一个 Pull Request

## 致谢

感谢所有为这个项目做出贡献的人！ 