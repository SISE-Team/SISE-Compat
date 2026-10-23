# Ender IO 化学导管的联机配方同步修复

版本：SISE Compat `0.2.3+26.1.2`。审核环境：Minecraft 26.1.2、NeoForge 26.1.2.114、EnderIO 9.0.6-alpha、EnderIO Evolution 4.0.1-NeoForge-26.1.2、Mekanism 10.8。

## 原因

旋律和恒星化学导管的定义原来同时检查 Mekanism 已加载，以及 `enderio:conduit_type` 注册表存在 `enderio:chemical`。

服务端加载数据包时有条件上下文，因此第二项检查通过。客户端连接时，NeoForge 会通过已知数据包机制复用本地 JSON；`NetworkRegistryLoadTask` 的读取使用普通 `RegistryOps`，条件读取器得到 `ICondition.IContext.EMPTY`，其中注册表访问为 `RegistryAccess.EMPTY`。`neoforge:registered` 因此返回 false，这两个导管被跳过。

实测旧版服务端导管注册表有 28 项，客户端只有 26 项。随后客户端收到包含恒星升级路线的 `neoforge:recipe_content`，其输入引用 `enderio_evolution:melodic_chemical_conduit`，无法解码而断开连接。单人游戏会使用本地服务端的注册表，掩盖了这个差异。

## 修复

从这两个数据定义移除 `neoforge:registered` 条件，保留 `neoforge:mod_loaded` 的 Mekanism 检查：

- `data/enderio_evolution/enderio/conduit/melodic_chemical_conduit.json`
- `data/enderio_evolution/enderio/conduit/stellar_chemical_conduit.json`

SISE Compat 的依赖元数据已要求审核版本的 EnderIO 和 Mekanism，并在公共初始化时注册化学导管类型；原注册检查在此环境中是冗余的。导管 ID、纹理、描述、传输速率及七条制作路线保持原样。修复通过现有 SISE Compat 的资源覆盖交付，客户端和服务器都更新至同一个版本。

## 验证

本地 `src/lanprobe` 只用于隔离测试，正式 JAR 不包含它。原整合包的全部 221 个模组进入新建测试世界，另加两个测试探针；测试身份不使用真实账号凭据，测试不读取原实例存档。

- 旧版对照：两端分别为 28 / 26 项，复现配方解码断线。
- 修复后：服务端、主机客户端与远程客户端实测均加载 28 项，五档化学导管的速率完全一致：750、2,000、64,000、256,000、2,147,483,647。
- 完整包双客户端测试 `pair-result-4.json` 通过，两端退出码均为 0。远程客户端完成真实登录及配方同步，WATUT 打字、界面投影、挂机、TAB 状态及物品转移消息均通过。
- 测试驱动等待远程客户端完成 JEI 初始化并开始正常客户端 tick 后，再发送打字动作，避免加载期间连续状态被后续消息覆盖；此等待逻辑仅存在于测试探针。
- 配方和材料保护回归：262 项规则断言、49 项材料保护断言及 12 条真实配方。
- 安装前校验原有模组及 Minecraft、公共库和资源的哈希；备份原 Compat JAR，只替换 Compat。

详细运行结果与安装记录保存在 `.work/enderio-lan-sync-20261011` 和 `.work/outputs/enderio-lan-sync-20261011`。一次测试客户端在进入联机前遇到既有 MixinSquared 取消注册器的并发修改崩溃；该运行未计为联机通过，已单独保留其日志。

通过上述测试的正式 JAR 为 `SISE-Compat-0.2.3+26.1.2.jar`，SHA-256：`a82b0c3da0418f23a6609a4e48e65fca243c0a57d7148737cc78d6c9cfdc4c0d`。正式 JAR 不包含联机测试探针。
