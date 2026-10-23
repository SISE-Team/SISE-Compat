# 来源与许可

SISE Compat 原创代码和文档采用 MIT。配方规则仅用于识别上游格式及恢复原配方语义。

`src/main/java/local/sise/compat/chemical/ChemicalConnectionConfig.java`、`ChemicalScreenType.java` 及 `src/main/resources/assets/enderio/textures/` 内三档化学导管图像来源于 Team-EnderIO/EnderIO 9.0.6-alpha 的配置、GUI 和化学导管模块，已改为当前事务 API 并去除未移植的过滤器入口。上游声明的公共领域许可原文保存在 [EnderIO.txt](src/main/resources/META-INF/licenses/EnderIO.txt)，并随正式 JAR 分发。

注册表并发和化学导管修复从我们此前的独立维护工程迁入；旧工程退役后仅保留为历史证据。SISE Ores 的矿物格式、配方冲突与 RarityCore 快照修复已迁入本工程；材料注册、等价标签、统一算法和世界生成仍由 Ores 维护。

本模组不打包第三方模组 JAR、Minecraft JAR、GLFW/LWJGL DLL 或公共依赖。用户排除的窗口原生保护继续由既有独立模组维护。

Titanium 拾取库存定位和 AE2 Screenshot 与 LDLib2 的渲染冲突改由本工程的 Mixin 处理，恢复对应上游原版 JAR。截图补丁使用当前整合包已有的 MixinSquared API 取消原截图模组的冲突注入，再在渲染目标存入局部变量时调用原截图 API；不打包 MixinSquared 或第三方原类。

0.5.0 的资源兼容层只在对应上游文件的 SHA-256 与审阅记录一致时启用。暮色森林缺失音频来自 TeamTwilight/twilightforest 提交 `f69f4e1dfa170a648ac81eb3661b3a49637353da`，代码和素材许可分别保存在正式 JAR 的 `META-INF/licenses/TwilightForest-LICENSE.txt` 与 `TwilightForest-ASSET_LICENSE.txt`。拆解预览采用该提交的配方显示接口逻辑。其他修复资源由已安装模组的原素材和原模型派生，保留原命名空间和材质，具体来源文件、校验和及修改原因记录在 `sise_compat/resource_repairs.json`。豆腐地下城尚无专属上游音频，使用该模组现有地下音乐。已移除方块的旧指南条目仅在方块确实未注册且条目原文匹配时过滤，其他指南内容保留。
