# mpv-android TokyoNight Fork

这是基于 [mpv-android](https://github.com/mpv-android/mpv-android) 维护的非官方 fork。mpv-android 是一款基于 [libmpv](https://github.com/mpv-player/mpv) 的 Android 播放器。本项目保留对上游项目及其依赖的致谢，并在此基础上开发独立界面和面向 ARM64 的构建。

[English](README.md)

## 本 Fork 的改动

- 为内置文件管理器和播放控制界面设计 Neo OLED 风格，不依赖额外图片素材。
- 内置文件管理器作为应用的唯一启动入口；按需请求 Android 存储权限，并从菜单进入设置或播放 URL。
- 在各构建系统支持的范围内，为 native 依赖启用 `-O2` 和 ThinLTO。
- GitHub Actions 仅构建使用 ARMv8.2-A 指令的 `arm64-v8a`，且只在手动启动时运行。

播放控制和现有设置仍以 mpv-android 上游实现为基础。上游功能详情请参阅[原项目](https://github.com/mpv-android/mpv-android)。

## 使用 GitHub Actions 构建

进入 **Actions**，选择 **build**，再点击 **Run workflow**。工作流完成后下载 `mpv-android-arm64-v8a-debug` artifact。push 和 pull request 不会自动触发构建。

第一次运行会生成一把 JKS 签名密钥，用它签名 APK，并将密钥作为 `mpv-android-signing-keystore` artifact 上传。下载该 artifact 后，把文件加入仓库的 `.github/keys/mpv-android.jks` 路径；后续运行便会使用同一把密钥。请妥善备份，密钥丢失或替换后，旧密钥签名的安装包将无法通过更新安装。

这是有意存放在公开仓库中的公开签名密钥。任何人都能使用它签出看似来自本 fork 的 APK。密码和 alias 定义在 workflow 中，并非秘密。此方案只保证签名一致，不提供发布者身份验证或密钥保密性。

该构建要求设备支持 ARMv8.2-A。较旧的 ARM64 设备可能不支持生成的 native 库。

## 本地构建

必须先构建 native 库，再构建 Android 应用。上游构建脚本支持 Linux 和 macOS，不支持 Windows 或 WSL。

```sh
cd buildscripts
./download.sh
./buildall.sh --arch arm64
```

从仓库根目录查看 ARM64 debug APK：`app/build/outputs/apk/default/debug/app-default-arm64-v8a-debug.apk`。

## 上游与许可证

本项目是 fork，并非 mpv-android 官方发布版本。仓库保留了上游代码与版权声明，本 fork 的改动由本项目独立维护。根目录 [`LICENSE`](LICENSE) 及原有声明适用于其覆盖的文件和组件；打包的库与 Android 依赖仍遵循各自许可证。上游组件清单见 [`docs/licenses.html`](docs/licenses.html)。上游说明，组合后的应用根据具体构建选项适用 GPL-3.0-or-later。

本仓库仅用于反馈本 fork 特有的问题。