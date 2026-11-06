# 构建 Meridian

## 环境

- JDK 17。
- Android SDK Platform 35、Build Tools 35.0.0。
- Gradle Wrapper 8.9；首次运行会下载 Gradle 和依赖。
- Android Studio 可以直接打开仓库根目录。

在项目根目录创建本机 `local.properties`，例如：

```properties
sdk.dir=/your/local/Android/Sdk
```

Windows 路径请使用正斜杠，或由 Android Studio 自动生成。本文件已忽略，不应提交。

## 构建类型

| 类型 | 用途 | 应用ID | 签名 |
| --- | --- | --- | --- |
| debug | 开发调试 | app.meridian.mobile.preview | 本机 debug key |
| performance | 非调试预览/性能观察 | app.meridian.mobile.preview | 本机 debug key或显式本机覆盖 |
| release | 自行发行 | app.meridian.mobile | 仓库没有内置发布签名 |

```bash
./gradlew :app:assembleDebug
./gradlew :app:assemblePerformance
./gradlew :app:testDebugUnitTest :app:lintPerformance
```

Windows 将 `./gradlew` 替换为 `.\gradlew.bat`。

产物位于 `app/build/outputs/apk/`，配置有 arm64-v8a、armeabi-v7a 和 universal 分包。体积受视频 SDK 影响。

## 签名

维护者既有预览版使用私有保管的签名文件，本仓库不提供该文件或密码。

`local.properties` 可用 `meridian.debugKeystore` 指定自己的本机预览签名路径；不配置则使用 Android 默认 debug 签名。不要把别人的签名写进配置，也不要把 keystore 提交到 Git。

自行构建 APK 与 Releases APK 签名不同，不能直接覆盖安装；卸载会影响本机账单、手记和资料，请先处理好自己的数据。正式发行应使用自己的 release 签名方案。

## 网络与后端

构建无需车辆账号、地图Key或AIKey。运行时输入自己的 HTTPS 服务地址和账号。

前置项目：[Ponyix/xiaomiev-mate](https://github.com/Ponyix/xiaomiev-mate)。Meridian 调用登录会话接口和相应业务接口；开放 API Key 不是全部业务接口的替代凭据。AI、远程视频、地图和授权配置均受后端能力约束。

## 目录

```text
app/src/main/java/app/meridian/mobile/  功能页面、数据处理与主题
app/src/main/java/app/meridian/mobile/glass/  玻璃材质组件
app/src/main/res/                      图标、车图、桌面卡片资源
app/src/main/assets/maps/              足迹边界数据
app/src/test/                         JVM单元测试
docs/screenshots/                     README展示截图
```

开发阶段尝试过的3D模型、未使用车图目录、后端前端压缩包、NAS配置、构建缓存和私人调试文件不属于公开源码包，也不属于当前App功能。
