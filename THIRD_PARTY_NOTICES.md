# 来源、依赖与素材许可范围

本仓库 MIT LICENSE 适用于 Meridian 作者有权授权的自有源码与文档。它不替代任何第三方库、SDK、数据、品牌或素材的条款。

## 前置服务与复用设计

- [Ponyix/xiaomiev-mate](https://github.com/Ponyix/xiaomiev-mate)：独立的前置车辆数据服务。Meridian使用其服务接口，公开仓库不包含其后端、数据库或导出的网页包；请遵循上游自己的发布与使用条件。
- 流影：作者自有项目的UI语言和部分玻璃、动效组件在本项目复用；如文件中另有第三方声明，以该声明为准。

## 主要软件依赖

| 组件 | 项目/说明 |
| --- | --- |
| Kotlin | https://github.com/JetBrains/kotlin |
| AndroidX / Jetpack Compose | https://android.googlesource.com/platform/frameworks/support/ |
| Coroutines | https://github.com/Kotlin/kotlinx.coroutines |
| Haze | https://github.com/chrisbanes/haze |
| Markwon | https://github.com/noties/Markwon |
| osmdroid | https://github.com/osmdroid/osmdroid |
| JUnit4 | https://github.com/junit-team/junit4 |
| JSON-java | https://github.com/stleary/JSON-java |
| Agora Android SDK | https://docs.agora.io/ ，商业SDK/服务，遵循其自己的许可与服务协议 |
| Gradle Wrapper | https://github.com/gradle/gradle ，Apache License 2.0；随附启动脚本保留上游声明 |

依赖由Gradle解析，不将它们重新许可为MIT。请查阅对应版本发布物中的完整LICENSE/NOTICE。

## 地图

- `app/src/main/assets/maps/china.json` 与 `china-provinces.json`：来自 [cn-atlas](https://github.com/BarbarossaWang/cn-atlas) 及其GeoJSON处理数据。上游 `package.json` 声明 ISC，数据链路及原始来源见上游README，作者信息为Amll。请同时保留该数据来源，不将地图数据视为本项目原创。
- 区县边界运行时使用 [zhChuXiao/ChinaGeoJson](https://github.com/zhChuXiao/ChinaGeoJson) 的固定版本CDN路径；该项目提供MIT许可，见 [上游LICENSE](https://github.com/zhChuXiao/ChinaGeoJson/blob/master/LICENSE)。其数据说明提及DataV.GeoAtlas，数据自身的上游要求仍适用。
- 高德底图和地图标识遵循高德的服务条款、归属标注和授权规则，地图内容不属于 Meridian MIT 授权范围。

地图仅用于车辆记录展示，不保证边界数据实时、测绘精度或行政区更新。

## 图片、品牌与截图

- Meridian图标在本项目开发过程中通过AI工具生成、由作者选定。
- 车辆展示图来自作者提供的SU7截图/车辆素材及其裁切处理，涉及Xiaomi/小米汽车的产品外观、图片及标识权益仍归相应权利人；不授予独立商用、商标或官方背书许可。`car_hero`等开发期车图也不按MIT重新授权。
- `docs/screenshots/` 为作者主动提供的App展示截图，用于项目介绍；其中出现的地图、车辆、头像及其他第三方内容不因此变成MIT素材。
- 当前公开代码不包含开发期尝试的3D模型、HDR环境贴图或未使用官方展厅车图目录。当前App没有可交互3D车模功能。

如对具体素材的公开展示有异议，请通过Issue说明文件与来源；维护者可替换或移除相关素材。
