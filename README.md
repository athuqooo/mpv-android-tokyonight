# mpv-android-tokyonight

基于 [mpv-android](https://github.com/mpv-android/mpv-android) 修改，应用 Tokyo Night Moon 暗色主题与播放界面修复。

## 修改内容

### 播放界面修复
- 修复触摸视频时状态栏/导航栏异常弹出的问题，现在触控响应更直接，无需多次点击
- 播放界面自动保持全屏沉浸模式，状态栏和导航栏始终隐藏

### Tokyo Night Moon 主题
- 全局纯黑背景 (`#000000`)，更省电、更沉浸
- 控件文字与图标色: `#c8d3f5`
- 强调色: `#82aaff`
- 控件背景: 深黑半透明 (`#d9000000`)

### 构建
- GitHub Actions 仅在 push 到 master 分支时触发构建

## 下载

从 [Releases](https://github.com/mpv-android/mpv-android/releases) 下载原版，或自行构建。

## 构建

参见 [buildscripts/README.md](buildscripts/README.md)。
