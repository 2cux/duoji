# 多记 / duoji

**AI 自然语言记账助手** — 像发消息一样记一笔。

## 核心功能

- **AI 智能解析**: 输入"午饭35，咖啡18，地铁6"，AI 自动识别每一笔的金额、分类和时间。
- **手动记账**: AI 识别失败时可手动输入，支持支出、收入、退款、转账、还款五种类型。
- **账单列表**: 按月查看所有账单，支持编辑、删除、按日期分组。
- **月度账单编辑**: 对已保存的账单修改金额、分类、备注等信息。
- **月度统计**: 自动生成月度支出总览、分类统计、每日趋势、最大单笔支出和高频小额消费分析。
- **AI 月度建议**: 根据消费数据生成个性化的月度分析和建议，全程可离线运行。
- **数据导出**: 支持 CSV 和 JSON 格式导出全部账单，方便备份和进一步分析。
- **设置页**: 数据管理（导出/清空）、AI 配置、偏好设置、应用信息。

## 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository 模式 |
| 本地存储 | Room 数据库 + DataStore Preferences |
| AI 接入 | OpenAI 兼容 API（可配置），含本地模拟解析器 |
| 网络 | Ktor Client (OkHttp) |
| 依赖管理 | Gradle Version Catalog |
| 最低 SDK | Android 8.0 (API 26) |

## 当前阶段

当前为 **Phase 4 — 体验增强与产品可用性打磨**。

前期已完成的阶段:

- Phase 1: 自然语言输入 → AI/本地模拟解析 → 确认页展示 → 编辑确认 → Room 保存
- Phase 2: Room 本地账本闭环、账单列表、账单编辑、首页真实数据刷新
- Phase 3: 月度统计面板、分类/趋势/高频分析、AI 月度建议（离线可用）

## 如何看到项目成果

多记是原生 Android App（Kotlin + Jetpack Compose），**不能**像网页一样直接在浏览器预览。需要通过 Android Studio 或直接安装 APK 才能运行。

### 方式一：Android Studio 运行

前置条件：
- 安装 [Android Studio](https://developer.android.com/studio)（2023.1 或更高版本）
- Android Studio 内安装 Android SDK Platform 34（通过 SDK Manager）
- JDK 17+（Android Studio 自带）

步骤：
1. **打开 Android Studio**
2. **File → Open**，选择项目根目录 `duoji/`（包含 `settings.gradle.kts` 的目录，不是 `app/` 子目录）
3. 等待 **Gradle Sync** 完成
4. 创建 Android 模拟器（Target API 26+），或连接 Android 真机
5. 点击 **Run ▶**（或 `Shift + F10`）
6. App 启动后默认进入首页

### 方式二：连接 Android 真机

1. 手机开启**开发者模式**：设置 → 关于手机 → 连续点击"版本号"7 次
2. 开启 **USB 调试**
3. 用 USB 线连接电脑
4. Android Studio 选择该设备
5. 点击 Run

### 方式三：安装 Debug APK

1. 在项目根目录执行：
   ```bash
   # macOS / Linux
   ./gradlew :app:assembleDebug

   # Windows
   gradlew.bat :app:assembleDebug
   ```
2. 构建成功后 APK 位于：
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
3. 安装到设备：
   ```bash
   # 通过 adb 安装（需要连接设备）
   adb install -r app/build/outputs/apk/debug/app-debug.apk

   # 或者把 APK 传到手机，点击文件安装
   ```

### Debug APK 路径

```
app/build/outputs/apk/debug/app-debug.apk
```

### 关于 local.properties

`local.properties` 用于告诉 Gradle Android SDK 的安装位置。

- **不需要手动提交** — 该文件已在 `.gitignore` 中，不会被 Git 跟踪
- **Android Studio 通常会自动生成** — 打开项目后会自动填写正确的 SDK 路径
- 如果命令行构建失败，提示 "SDK location not found"：
  1. 复制 `local.properties.example` 为 `local.properties`
  2. 将 `sdk.dir` 改为本机 Android SDK 实际路径
  3. 重新执行构建命令

### 常见问题排查

| 问题 | 解决方法 |
|------|----------|
| Android Studio 打开后找不到模块 | 确保打开的是项目**根目录**（含 `settings.gradle.kts`），不是 `app/` 子目录 |
| Gradle Sync 失败 | 检查网络连接，确保已安装 Android SDK Platform 34 |
| JDK 版本不匹配 | AGP 8.2.x 需要 JDK 17+，Android Studio 自带 JDK |
| SDK 未找到 | 复制 `local.properties.example` 为 `local.properties`，填入本机 SDK 路径；或设置 `ANDROID_HOME` 环境变量 |
| Compose 编译错误 | 检查 Kotlin 和 Compose Compiler 版本是否匹配（当前使用 Kotlin 1.9.22 + Compose Compiler 1.5.10） |
| Room KSP 错误 | KSP 版本必须与 Kotlin 版本一致（当前 1.9.22-1.0.17） |
| 模拟器未创建 | 在 Android Studio 的 Device Manager 中创建模拟器（API 26+） |
| 真机无法识别 | 确认 USB 调试已开启，尝试更换 USB 线或端口 |
| FileProvider 冲突 | `AndroidManifest.xml` 中 FileProvider authority 使用了 `${applicationId}`，会自动匹配 |

### 开发环境说明

当前 CI/开发环境 **没有安装 Android SDK**，因此在命令行中无法直接完成编译。

要在本地开发机运行此项目：
1. 安装 Android Studio
2. Android Studio 会自动下载所需的 Android SDK 和构建工具
3. 按照上方的"方式一"步骤操作即可

## AI 配置说明

App 默认使用**本地模拟解析模式**，即不配置任何 AI Key 也能完整使用所有功能。

如需接入真实 AI（如 OpenAI 兼容 API）：

1. 进入"设置" → "AI 设置"
2. 填写 API Base URL（默认 https://api.openai.com/v1）
3. 填写 API Key
4. 填写模型名称（如 gpt-4o-mini）
5. 点击"保存设置"

> 注意：API Key 仅保存在本机 DataStore 中，当前阶段未加密存储。正式发布时应使用 EncryptedSharedPreferences 或更安全的密钥管理方案。

## 本地模拟解析说明

当未配置 API Key 时，App 自动使用基于关键词和正则表达式的本地解析器：

- 支持提取金额（如"35"、"18.5"）
- 根据关键词自动识别类型和分类（如"工资" → 收入，"午饭" → 餐饮）
- 支持一次输入多条记录（如"午饭35，咖啡18，地铁6"）
- 支持日期表达式（如"昨天"、"前天"）
- 响应快、不依赖网络

## 数据隐私说明

- **账单数据默认保存在本机**，不会自动上传到云端。
- 只有在设置中配置了 API Key 并进行 AI 解析时，才会将必要的记账文本上传到 AI 服务端进行解析。
- **本地模拟模式下不上传任何数据**。
- 月度建议功能同样遵循上述规则：无 API Key 时使用本地生成器，不联网。
- 导出功能将数据写入 app-specific 外部存储，其他应用无法直接访问。

## 当前不做的功能

- 云同步 / 多设备数据互通
- 微信 / 支付宝账单导入
- 小票 OCR 识别
- 语音输入记账
- 多账户 / 家庭账本管理
- 复杂预算规划
- 社交分享

## 后续计划

- 语音输入记账
- 月度/年度预算设置与提醒
- 自定义分类管理
- 数据加密存储
- 数据趋势图（折线图、饼图）
- App 锁（面容 / 密码保护）
