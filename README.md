# mpv-android TokyoNight Fork

An unofficial, community-maintained fork of [mpv-android](https://github.com/mpv-android/mpv-android), the Android video player powered by [libmpv](https://github.com/mpv-player/mpv). This fork keeps the upstream project and its dependencies credited while developing a distinct interface and ARM64-focused builds.

[简体中文](README.zh-CN.md)

## What this fork changes

- A Neo OLED interface for the built-in file browser and playback controls, designed without additional image assets.
- The built-in file manager is the single app-launch entry point. It requests the Android storage permissions it needs and offers settings and URL playback from its menu.
- mpv configuration defaults to the app's `Android/media/<package>` directory and can be changed in Advanced settings.
- TokyoNight surfaces are the default; a pure OLED-black theme is available in User interface settings.
- Native dependencies use `-O2` and ThinLTO where supported by their build systems.
- GitHub Actions builds only `arm64-v8a` with ARMv8.2-A instructions and runs only when started manually.

Playback controls and the existing settings remain based on upstream mpv-android. For the full upstream feature set, see the [upstream project](https://github.com/mpv-android/mpv-android).

## GitHub Actions build

Open **Actions**, select **build**, then choose **Run workflow**. Download the `mpv-android-arm64-v8a-debug` artifact when the run completes. The workflow does not run on pushes or pull requests.

The first run creates a JKS signing key, uses it to sign the APK, and uploads the key as the `mpv-android-signing-keystore` artifact. Download that artifact and add the file to this repository at `.github/keys/mpv-android.jks`; subsequent runs will use the same key. Keep a backup: losing or replacing it prevents future APKs from updating installations signed with the old key.

This is an intentionally public signing key stored in a public repository. Anyone can use it to sign an APK that appears to come from this fork. Its password and alias are defined in the workflow and are not secrets. This setup provides signature consistency, not publisher authenticity or key confidentiality.

This build requires an ARMv8.2-A-capable device. Older ARM64 devices may not support the generated native libraries.

## Build locally

Native libraries must be built before the Android app. The build scripts support Linux and macOS; Windows and WSL are not supported by the upstream build scripts.

```sh
cd buildscripts
./download.sh
./buildall.sh --arch arm64
```

The ARM64 debug APK is written to `app/build/outputs/apk/default/debug/app-default-arm64-v8a-debug.apk` from the repository root.

## Upstream and licensing

This project is a fork, not an official mpv-android release. Upstream code and copyright notices are retained; changes in this repository are maintained separately. The root [`LICENSE`](LICENSE) and the original notices apply to the files and components they cover. Bundled libraries and Android dependencies retain their own licenses; see [`docs/licenses.html`](docs/licenses.html) for the upstream component list. The upstream project describes the combined application as GPL-3.0-or-later, depending on the selected build options.

Please report issues specific to this fork in this repository.