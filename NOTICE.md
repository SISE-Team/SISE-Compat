# 来源与许可

SISE Compat 原创代码和文档采用 MIT。配方规则仅用于识别上游格式及恢复原配方语义。

`src/main/java/local/sise/compat/chemical/ChemicalConnectionConfig.java`、`ChemicalScreenType.java` 及 `src/main/resources/assets/enderio/textures/` 内三档化学导管图像来源于 Team-EnderIO/EnderIO 9.0.6-alpha 的配置、GUI 和化学导管模块，已改为当前事务 API 并去除未移植的过滤器入口。上游声明的公共领域许可原文保存在 [EnderIO.txt](src/main/resources/META-INF/licenses/EnderIO.txt)，并随正式 JAR 分发。

注册表并发和化学导管修复从我们此前的独立维护工程迁入；旧工程退役后仅保留为历史证据。SISE Ores 的矿物格式、配方冲突与 RarityCore 快照修复已迁入本工程；材料注册、等价标签、统一算法和世界生成仍由 Ores 维护。

本模组不打包第三方模组 JAR、Minecraft JAR、GLFW/LWJGL DLL 或公共依赖。用户排除的窗口原生保护继续由既有独立模组维护。

Titanium 拾取库存定位和 AE2 Screenshot 与 LDLib2 的渲染冲突改由本工程的 Mixin 处理，恢复对应上游原版 JAR。截图补丁使用当前整合包已有的 MixinSquared API 取消原截图模组的冲突注入，再在渲染目标存入局部变量时调用原截图 API；不打包 MixinSquared 或第三方原类。
