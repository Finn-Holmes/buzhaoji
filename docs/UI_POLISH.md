# 0.8.1 · 现有功能 UI 与动效打磨

2026-09-29，versionCode 40。

## 本轮变化

- 今天页移除收工大卡片，标题下显示日期；搜索直接放在顶部，回顾与一周足迹收进更多菜单。
- 今天与任务清单共用任务行：两行标题、下方日期和课程、22dp 圆角方勾选框与 48dp 触控区域。保留优先级颜色、任务菜单、编辑、删除与完成防重。新建任务短暂显示浅主题色背景。
- 快速添加等待面板展开及窗口获得焦点再打开键盘；从日期选择器返回时恢复键盘。发送与键盘完成键走同一个保存入口，锁定草稿后收起面板，避免重复提交。发送按钮固定位置，日期标签淡换。
- 习惯页缩小装饰图标，日期选中底色、打卡浅底色和完成计数使用短动画；勾选区域仍为 48dp。
- 倒数日加强天数字号与基线对齐，长标题最多两行；大字号时改为上下布局。天数变化使用淡换。
- 专注页使用真实完成比例的细进度环、稳定数字和暂停状态；正计时使用静态刻度。
- 按压反馈更轻，展开箭头旋转，完成描边与划线保持短动画，遵循系统动画缩放。

## 验证

命令：`build-local.cmd assembleDebug testDebugUnitTest lintDebug --max-workers=1 --no-daemon --console=plain`

- 最终构建成功；55 项单元测试，0 失败、0 错误。
- Lint：0 错误，81 个警告、6 个提示，未对全部已有报告项做清理。
- APK 已覆盖安装在 Pixel 7 / API 35 模拟器，包名 `com.kexu.app`，版本已核对为 `0.8.1-ui-polish`。
- 实际操作：新增任务、日期选择后恢复键盘、连续点击发送仅生成一条记录、键盘完成键保存备注、完成与恢复任务、展开与收起完成组、任务删除。
- 实际操作：习惯打卡与取消、切换日期；倒数日长标题／当天／过去／未来状态及进入编辑；倒计时开始与暂停、正计时开始与暂停。
- 深色模式、1.5 倍系统字号和关闭系统动画做了抽查；本轮未做全部设备、主题与桌面小组件矩阵测试。

测试前备份了数据库和 DataStore。测试后清理全部临时任务、习惯、倒数日及关联记录，逐表比较 10 张业务表与完整设置映射，均与测试前相同。原计时器恢复为 34 分钟设定、暂停在 33:12；系统字号、动画比例与深色开关也已恢复。

构建日志：[ui-polish-build-final.log](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-build-final.log>)。

数据核对：[preservation.json](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/preservation.json>)。

## 模拟器截图

带 `[QA]` 的内容为临时测试数据，已清理。

- [今天页](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/today-final.png>)
- [日期选择后恢复键盘](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/quick-return-fixed.png>)
- [习惯页](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/habits-before.png>)
- [倒数日](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/countdowns.png>)
- [大字号深色倒数日](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/countdown-large-dark.png>)
- [专注页，原计时器已恢复](<C:/Users/Finn/Documents/to do list/tmp/ui-polish-qa/focus-final.png>)

APK SHA-256：`A51DF8B808C57BB648B0EA12D0A22F67948F992932F02B7857BE4E643A74005E`。
