# mpv-android-tokyonight

**一个基于 libmpv 的 Android 视频播放器**，专注**纯黑 Tokyo Night Moon 主题** + **实用修复**。

[![GitHub stars](https://img.shields.io/github/stars/athuqooo/mpv-android-tokyonight)](https://github.com/athuqooo/mpv-android-tokyonight/stargazers)
[![Release](https://img.shields.io/github/v/release/athuqooo/mpv-android-tokyonight)](https://github.com/athuqooo/mpv-android-tokyonight/releases)
[Issues](https://github.com/athuqooo/mpv-android-tokyonight/issues)

![Screenshot](https://github.com/athuqooo/mpv-android-tokyonight/raw/master/screenshots/player(1).jpg)

![Screenshot](https://github.com/athuqooo/mpv-android-tokyonight/raw/master/screenshots/player(2).jpg)

## ✨ 主要特性

- **纯黑 Tokyo Night Moon 主题**：OLED 全黑背景，视觉舒适，色彩协调。
- **播放界面交互修复**：修复原版 Bug —— 首次点击会先唤出状态栏，操作更流畅自然。
- **存储路径优化**：默认配置目录从 `/data/data/...` 改为 `/storage/emulated/0/Android/media/is.xyz.mpv/`（符合 Android 规范，可直接用文件管理器访问）。
  - `mpv.conf`、`input.conf`、`fonts.conf` 现已完美支持 `~~` 别名。

## 📥 下载

前往 [Releases 页面](https://github.com/athuqooo/mpv-android-tokyonight/releases) 下载最新 APK。

- 目前提供 `arm64-v8a` 版本（推荐主流设备）

**注意**：这是社区维护的 fork，请从本仓库 Releases 下载。

## ⚙️ 配置说明

**配置目录**：`/storage/emulated/0/Android/media/is.xyz.mpv/`

在此目录下可直接编辑：
- `mpv.conf`
- `input.conf`
- `fonts.conf`

**配置示例**（`mpv.conf`）：

```conf
# 视频输出
vo=gpu-next
gpu-api=vulkan
hwdec=mediacodec-copy

# 着色器（需在 ~~/shaders/ 目录下放置对应文件）
glsl-shaders='~~/shaders/KrigBilateral.glsl:~~/shaders/FSRCNNX_x2_8-0-4-1.glsl'

# 去色带
deband=yes
deband-iterations=2
deband-threshold=32
deband-range=16
deband-grain=32

# 抗振铃
scale-antiring=0.4
cscale-antiring=0.4
dscale-antiring=0.4
```

## 📋 Changelog

- 详见 Releases 页面

## 🛠️ 构建从源代码

- 详见 .github/workflow/build.yml

## 🙏 致谢 & 声明

- 基于 [mpv-android](https://github.com/mpv-android/mpv-android) 官方项目。
- 使用 [pi-coding-agent](github.com/earendil-works/pi)完成工作。
- 主题灵感来自 Tokyo Night Moon 变体。
