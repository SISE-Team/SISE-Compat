# 2026-10-18 声音状态查询并发崩溃

用户报告在使用奥瑞科技粒子对撞机时崩溃，当时没有开枪。实例最新报告为 `crash-2026-10-18_16.35.15-client.txt`，异常发生在 Render thread 的 `SoundEngine.isActive`：`Integer.intValue()` 的接收值为 null。TaCZ `cleanupInvalidEntitySounds` 是调用者，不代表玩家当时开枪。

## 已确认的原因

已安装 RSLS 1.2.2 将声音处理移到声音线程，并将 `soundDeleteTime` 包装为 `Collections.synchronizedMap`。Minecraft 26.1.2 的状态查询先执行 `containsKey`，再执行 `get` 并拆箱。两次独立同步操作之间，声音线程仍可删除该条记录，因而第二次读取返回 null。

AsyncParticles 26.1.2.7 包装了 `get`，但仅在粒子线程遇到 null 时返回保护值；主线程仍得到 null。TaCZ 每 5 个客户端刻清理声音，调用这个公共查询。奥瑞科技 2.0.0-exp11 的加速器代码有粒子移动音效及碰撞音效调用。日志和代码不能重建玩家当时的全部动作，但可以确认“必须开枪才能触发”的判断不成立。

崩溃后的 `Tried to release unknown channel` 首次出现在 16:35:15.825，晚于 16:35:14.899 的首个空指针，不能用它替代首发异常的诊断。

分析依据是本机日志及已安装 JAR 的 JDK 25 `javap` 输出。REA 工具未连接，诊断显示注册配置漂移；未修改工具配置，静态证据由本地 JDK 获取。上游项目参考：[RSLS](https://github.com/RelativityMC/raise-sound-limit-simplified)、[AsyncParticles](https://github.com/Harveykang/AsyncParticles)。未假设当前上游分支与安装版本完全一致。

## 修复

SISE Compat 0.5.5 增加客户端 `SoundActivityMixin`：使用 RSLS 已有的 `soundDeleteTime` 映射锁包住整个 `isActive` 调用。RSLS 删除操作使用同一把锁，因此检查和读取之间不能再删除记录。保留原查询、AsyncParticles 的包装以及 RSLS 的待播放声音返回值修饰。

补丁只针对 RSLS 1.2.2 与 NeoForge 26.1.2.114 启用。没有修改第三方模组 JAR、Minecraft JAR、公共库、DLL、存档或声音配置，也没有关闭声音扩容或异步粒子。

## 验证方式

`src/soundprobe` 是仅用于测试的探针，不进入正式 JAR。`tool/sound_verify.py` 在两个独立完整模组客户端中运行它；不复制玩家存档，不调用开枪逻辑。探针使用真实经过全部 Mixin 变换的 SoundManager / SoundEngine 和它自己的 Sound engine 线程，在 Render thread 查询声音状态，同时由声音线程不断添加、删除同一个声音记录。

同时检查未加载、缺失、到期、到期边界、未到期、有播放通道、RSLS 待播放与取消待播放状态，以及工作线程能正常退出。

实测结果：

- 原版在完成 1 次查询后复现相同的 `asyncparticles$redirectIsActive` 空值拆箱异常，声音线程已执行 1,012 次删除；探针正确捕获预期异常，客户端正常退出。
- 修复版完成 2,000,000 次查询及 709,445 次并发删除，耗时 323 ms，16 项断言通过，客户端正常退出。运行时导出字节码确认 `monitorenter/monitorexit` 包住完整上游调用。
- 现有规则、生产、材料回归分别通过 345、417、36 项断言，合计 798 项。
- 对比安装前后的正式 JAR：仅新增 `SoundActivityMixin.class`，变更版本元数据、Mixin 注册和版本保护插件三个条目；没有其他类或资源改动。

原版本对照与修复版结果、原始字节码、安装前哈希及安装记录保存在 `verification/sound-race-20261018/`。测试不等同于在玩家原存档中重新搭建和运行整座对撞机；实际机器场景仍需重启后复核。
