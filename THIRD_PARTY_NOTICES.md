# 第三方组件与许可

本项目的 MIT 许可证适用于项目原创内容。第三方组件保留各自的版权和许可条款。

## Gradle Wrapper

`gradlew`、`gradlew.bat` 和 `gradle/wrapper/gradle-wrapper.jar` 为 Gradle Wrapper，采用 Apache License 2.0。脚本中的原版权及许可声明保留，许可证全文见 [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。

Gradle 项目：https://github.com/gradle/gradle

## 构建及运行依赖

AndroidX、Jetpack Compose、Room、WorkManager、Glance、Kotlin 和 kotlinx.coroutines 等依赖由 Gradle 下载，不以项目 MIT 许可重新授权；具体版本见 [app/build.gradle.kts](app/build.gradle.kts)，使用和分发时遵守各组件自己的许可。

Google ML Kit 的使用与分发遵循其[条款](https://developers.google.com/ml-kit/terms)，模型和 SDK 不因本项目开源而改用 MIT 许可。

## 参考项目

教务适配设计参考了 [WakeUp CourseAdapter](https://github.com/YZune/CourseAdapter) 和 [拾光课程表适配仓库](https://github.com/XingHeYuZhuan/shiguang_warehouse)。相关说明保留在 README 中；本项目的 MIT 许可不覆盖这些独立项目。
