<div align="center">

<img src="app/src/main/res/drawable/brand.png" width="96" alt="Meridian 图标" />

# Meridian

**连接每一次出发 · Xiaomi EV Mate 的非官方 Android 客户端**

把车辆状态、行程、充电、足迹、用车报告和 AI 助手装进口袋。

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#安装与开始使用)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?logo=kotlin&logoColor=white)](#技术栈)
[![License](https://img.shields.io/badge/Code_License-MIT-blue.svg)](LICENSE)
[![AI](https://img.shields.io/badge/Developed_with-AI-orange)](#全程-ai-辅助开发)

[下载安装](https://github.com/zeyu8023/Meridian/releases) · [前置服务](https://github.com/Ponyix/xiaomiev-mate) · [问题反馈](https://github.com/zeyu8023/Meridian/issues) · [本地构建](docs/BUILDING.md)

</div>

> **使用前必须先部署 [Xiaomi EV Mate（xiaomiev-mate）](https://github.com/Ponyix/xiaomiev-mate)，并在其网页端完成账号与车辆配置。Meridian 是连接该服务的手机客户端，单独安装 APK 无法获取车辆数据。**

## 项目介绍

Meridian 基于 Xiaomi EV Mate 提供的服务接口开发，面向已经部署该项目的小米汽车车主，将网页端常用数据和操作适配为 Android 手机上的交互界面。

项目以「爱车、行程、AI 助手、统计、我的」为主导航，结合玻璃质感胶囊、明暗主题、可编辑首页卡片和原生数据图表，让查看车辆、回顾行程和整理日常用车开支更加方便。

Meridian 与 Xiaomi EV Mate、Xiaomi/小米汽车不存在官方隶属或背书关系。车辆数据获取、同步任务及服务端 AI 能力由你部署的后端提供；可用功能以实际后端版本、车辆授权及返回数据为准。

当前公开版本为 **0.52.0-preview**，仍处于持续迭代阶段。

## 界面预览

以下为作者提供的真实 App 截图，部分地址已通过隐私模式隐藏；截图展示的数据和车图不代表所有车辆的可用字段或外观适配。

<table>
  <tr>
    <td align="center"><b>爱车首页</b><br/><img src="docs/screenshots/home.webp" width="245" alt="爱车首页、卡片和玻璃导航栏"/></td>
    <td align="center"><b>用车账单</b><br/><img src="docs/screenshots/ledger.webp" width="245" alt="费用总览、分类和账单明细"/></td>
    <td align="center"><b>行程详情</b><br/><img src="docs/screenshots/trip.webp" width="245" alt="行程地图、概览和手记"/></td>
  </tr>
  <tr>
    <td align="center"><b>AI 助手</b><br/><img src="docs/screenshots/assistant.webp" width="245" alt="AI助手欢迎页和推荐问题"/></td>
    <td align="center"><b>用车统计</b><br/><img src="docs/screenshots/statistics.webp" width="245" alt="用车概况和多彩数据卡片"/></td>
    <td align="center"><b>主题分享</b><br/><img src="docs/screenshots/sharing.webp" width="245" alt="统计报告分享和图片预览"/></td>
  </tr>
  <tr>
    <td align="center"><b>管理与设置</b><br/><img src="docs/screenshots/settings.webp" width="245" alt="车辆、头像、外观和功能管理"/></td>
    <td colspan="2">支持浅色 / 深色、不同卡片分色主题、玻璃导航外观，以及首页卡片排序与显示设置。具体效果会随系统版本、设备和设置变化。</td>
  </tr>
</table>

## 功能概览

| 模块 | 已接入的主要能力 |
| --- | --- |
| 爱车 | 电量、续航、车辆状态、温度、近期里程与驾驶时长、最近行程/充电、今日行车简报、综合分析、趋势与分布 |
| 首页编辑 | 长按卡片拖动排序、角落移除、隐藏与恢复、布局预设、主题快捷切换 |
| 行程与充电 | 左上角切换记录类型，搜索与筛选、日期范围、分页、详情、真实轨迹地图、起终点与相关指标 |
| 行程手记 | 本机收藏、标签、备注、卡片内编辑；从收藏库回看记录、常用路线对比 |
| 足迹 | 行驶轨迹、高频路线、起终点/全国足迹等视图及到访地区明细，具体数据依赖后端 |
| AI 助手 | 对话、推荐问题、Markdown 排版、历史会话、图片附件及服务返回的关联证据/媒体 |
| 统计 | 用车概况、能耗分析、月度报告、年度报告，趋势/构成图及数据交互 |
| 图片分享 | 行程、统计和报告生成图片；主题配色、精简/完整报告、分享隐私选项 |
| 用车账单 | 充电、高速、停车、维修、保养、配件、保险等分类；明细、周/月/年统计、分类构成、每公里费用 |
| 充电费用 | 自定义电价，支持按充电记录估算费用或从费用反算电量，手动修正及历史单价保留 |
| 远程查看 | 对接后端远程视频会话；能否开启取决于后端、车辆和授权条件 |
| 桌面卡片 | Android 桌面车辆卡片、字段选择与外观设置；内容来自最近同步的数据 |
| 管理与设置 | 账号与服务连接、车辆管理、同步、头像、隐私模式、外观与动效、消息、备份与本机资料管理 |

没有返回或尚未同步的数据会显示为空、未知或提示信息，不把缺失指标当作真实的 0。功能清单描述当前客户端实现范围，并非对所有服务版本的兼容保证。

### 用车账单的数据口径

- 账单主要保存在当前设备，独立于车辆服务端数据。
- 自动充电费属于 **充电量 × 自定义电价的估算**，不等同于充电运营商实际账单；分时电价、服务费等可能造成差异。
- 每公里费用按同一统计期的已记录费用与有效行驶里程计算，未记录的支出不纳入；缺少有效里程时不计算。
- 高速费采用手动记账，当前版本**没有高德路线费用估算功能**，无需额外部署路线服务。

## 安装与开始使用

### 1. 先部署 Xiaomi EV Mate

前往 [Ponyix/xiaomiev-mate](https://github.com/Ponyix/xiaomiev-mate)，按照该项目说明部署服务。

完成后，先在浏览器确认能够：登录自己的实例、查看车辆，以及读取行程和相关数据。车辆授权、地图配置、同步和 AI 服务配置请遵循前置项目的说明。

### 2. 安装 Meridian

从本仓库 [Releases](https://github.com/zeyu8023/Meridian/releases) 下载 APK。

- `Meridian-v0.52-preview.apk`：ARM64，适用于大多数现代 Android 手机。
- `Meridian-v0.52-armeabi-v7a-preview.apk`：32位 ARM 兼容包，按设备实际架构选择。
- 最低 Android 8.0（API 26）。玻璃折射、模糊和系统返回动效会受系统版本及设备能力影响。

### 3. 手动填写自己的服务信息

在登录页填写 **自己的 Xiaomi EV Mate HTTPS 服务根地址、账号和密码**。应用不预填作者的私有地址或账号，可选择记住登录信息和自动登录。

服务地址示例：`https://your-evmate.example.com`，不要填写网页的 `/app/dashboard` 路径。

网页登录与车辆读取正常后，再排查 App 连接问题。开放 API Key 管理属于服务端功能；当前 App 主登录流程使用账号会话，**并非仅填一个开放 API Key 就能替代账号登录**。

### 4. 按需要配置能力

AI、地图和远程查看依赖原服务端能力与相应配置。App 不内置可供他人共用的 AI、地图或视频服务 Key。

离线时仅能浏览已经保存在本机的部分数据；实时车辆状态、聊天、远程查看和服务端同步需要联网。

## 全程 AI 辅助开发

**这是一个采用全程 AI 辅助方式推进的项目。**

Meridian 自身的功能代码、界面实现、问题修复、测试和文档由 AI（Codex）根据需求与反馈撰写、迭代；引用的第三方依赖、地图数据和复用组件保留各自来源，不属于「由本项目 AI 原创」的范围。

项目发起者负责提出需求、决定设计方向、提供截图与素材、在真实手机上测试并持续反馈。你在截图中看到的界面，就是这个协作过程逐步打磨的结果。

AI 生成并不意味着代码已经完整审计，或所有设备上的效果都已验证。欢迎提交可复现的问题、改进建议与代码贡献。

## 技术栈

- **Kotlin + Jetpack Compose**：原生 Android 界面。
- **Coroutines**：网络、数据读取与后台任务。
- **Haze 与自有玻璃组件**：模糊、胶囊、底栏与材质效果，部分 UI 复用自作者的流影项目。
- **Markwon**：AI 对话 Markdown 排版。
- **osmdroid + 高德底图**：行程轨迹与地图展示。
- **Agora SDK**：后端授权的视频会话接收。
- **Android Keystore**：本地登录和资料加密存储相关能力。
- **JUnit**：数据处理、统计口径和部分交互计算的单元测试。

## 本地构建

需要 JDK 17、Android SDK 35 和 Gradle 8.9。仓库附带 Gradle Wrapper。

```bash
git clone https://github.com/zeyu8023/Meridian.git
cd Meridian
./gradlew :app:assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

单元测试和静态检查：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

具体环境配置、构建类型、签名和产物说明见 [构建文档](docs/BUILDING.md)。本仓库不包含维护者的签名私钥。自行构建的 APK 通常不能覆盖安装维护者发布的 APK，原因是签名不同。

当前 0.52 版本发布前的本地结果：**168项 JVM 单元测试通过，Lint 0错误、13警告**。这不是完整的 UI 自动化、硬件兼容或安全审计报告。

## 隐私与数据

服务端账号和车辆数据来自你指定的 Xiaomi EV Mate 实例。账号、车辆位置、行程轨迹和远程画面均属于敏感资料，请只连接你信任的实例。

隐私模式主要隐藏文字地址的详细部分，**不会自动抹去地图和轨迹上的地理线索**。分享时还应根据内容选择隐藏地址或路线等选项。本机手记、账单、头像和缓存的保留范围见 [隐私与数据说明](docs/PRIVACY.md)。

提 Issue 或贡献截图前，请移除密码、Cookie、Token、API Key、车架号、精确地址及其他不想公开的信息。不要把真实备份文件或完整配置直接上传到公开 Issue。

## 已知边界与后续方向

- 预览版仍在打磨页面切换、系统侧滑返回、键盘和大字号布局；具体机型可能出现不同表现。
- 当前展示车图为现有 SU7 素材；没有可交互3D车模，也不承诺所有车型/年款的准确车图适配。
- 这是对现有服务接口的客户端适配，不对未公开接口的长期稳定性作保证。
- 本机账单和手记不会自动成为后端的跨设备同步数据；重装或清理应用前请确认备份。
- 后续重点：更多真实设备验证、持续完善后端功能适配、界面细节、可访问性和数据导出体验。

## 参与贡献

欢迎在 [Issues](https://github.com/zeyu8023/Meridian/issues) 描述问题，或提交 Pull Request。反馈请附 App 版本、Android版本/机型、后端版本、复现步骤，以及已脱敏截图/录屏。

开发原则和提交建议见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 致谢与许可

- [Xiaomi EV Mate](https://github.com/Ponyix/xiaomiev-mate)：车辆数据与服务能力，是使用本 App 的前置项目。
- 流影：作者自有项目的设计语言及部分玻璃/交互组件。
- Jetpack Compose、Haze、Markwon、osmdroid 等依赖的开发者。
- 地图、视频 SDK、车图及其他第三方内容的提供者。

本项目自有源码采用 [MIT License](LICENSE)。第三方库遵循各自许可证；小米汽车车图、品牌标识、地图数据/底图和其他第三方素材**不因本仓库源码使用 MIT 就自动成为 MIT 素材**，具体来源与范围见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
