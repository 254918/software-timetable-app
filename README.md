# 课表助手（Timetable Assistant）

安卓原生（Kotlin）课表应用：上传课表图片，自动识别生成课表；支持分专业课表识别与手动校正。

## 功能

- **课表展示**：自动计算当前周并高亮；上一周 / 下一周 / 回到本周 / 选择周次；本周课程速览；上午(1-5) / 下午(6-9) / 晚上(10-13) 分区周视图；课程卡片按节次精确定位，教室编号白底徽章突出显示；调课课程带「调课」标记
- **图片导入识别**：选择相册课表图片 → 选择识别引擎 → 自动识别课程名称 / 节次 / 周次 / 教室 / 教师
- **分专业识别**：自动识别课表中的专业方向（如软件开发 / 大数据技术），识别到多个时让用户选择后生成课表，未选中的专业课程自动剔除
- **手动校正**：识别结果逐条可点开修改课程名 / 星期 / 节次 / 周次 / 教室 / 教师，也可删除误识别项
- **双 OCR 引擎**：
  - 离线识别：Google ML Kit 中文识别，无需联网、无需 Key
  - 云端识别：百度通用文字识别（准确率更高，需联网，在「设置」中填写百度智能云 API Key / Secret Key，标准版有免费额度）
- **内置默认课表**：首次安装自动加载当前学期软件开发方向课表（含调课安排），可直接使用或重新导入
- **设置**：学期第 1 周周一日期（计算当前周用，默认 2026-09-07）

## 技术栈

- Kotlin 1.6 + Android Gradle Plugin 7.0.4 + Gradle 7.3.3（兼容 Gitee 在线构建环境）
- minSdk 24 / targetSdk 31 / compileSdk 31，ViewBinding
- Google ML Kit Text Recognition（中文）、OkHttp、kotlinx-coroutines

## 构建

### 方式一：Gitee 在线构建（无需本地环境）

1. 仓库「管理 → 基本信息 → 语言」设为 **Android**
2. 仓库首页会出现「下载 APK」入口，进入「流水线」页面
3. 点「新建构建」：构建版本 openjdk-11、Java 版本 OpenJDK 11、构建工具 gradle、构建类型 `release`
4. 等待构建完成，产物为 `app/build/outputs/apk/release/app-release.apk`，可在仓库页下载安装

> 说明：Gitee 在线构建环境预置 Gradle 7.3.3；若下载 Gradle 较慢，可将
> `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 改为
> `file:///data/android/gradle-zip/gradle-7.3.3-bin.zip` 加速。

### 方式二：本地 Android Studio

用 Android Studio（Flamingo+，自带 JDK 17）打开工程根目录，同步后 Run / Build APK 即可。

## 目录结构

```
app/src/main/java/com/example/timetable/
├── data/       # 课程模型、周次解析、本地持久化、默认课表
├── ocr/        # OCR 引擎（ML Kit / 百度）、版面分析（行列重建、专业识别）
└── ui/         # 主界面（课表视图）、导入识别、校正、设置
```

## 说明

- 版面分析针对「最左节次列 + 周一至周五 5 列」的常见课表格式设计；其他格式可能识别不完整，可在校正环节手动调整
- 百度 OCR Key 仅保存在手机本地，不经过任何服务器
- 识别结果的课程数据保存在手机本地（SharedPreferences），不联网上传
