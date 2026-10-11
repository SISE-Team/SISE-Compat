# SISE Compat — NeoForge 26.1.2

整合包兼容修复的统一入口。我们自己的 Compat 修复在本工程维护，由 `sise_compat` 一个模组、一个正式 JAR 交付。物品、导管和材料沿用原有注册 ID；客户端与服务端均安装本模组。

主分支：`26.1.2`。当前版本：`0.2.0+26.1.2`。原创代码使用 [MIT 协议](LICENSE)，EnderIO 派生部分保留上游公共领域许可，见 [来源声明](NOTICE.md)。

配方规则快照用于识别和兼容上游配方；恢复的 EnderIO 化学导管包含上游 GUI 配置代码和三档导管纹理，不包含上游模组 JAR。第三方部分遵循各自原协议。

## 范围

0.2.0 保留原有 76 条配方补丁及 5 条手动反向转换，增加迁入的 15 条矿物格式修复和 2 条化学导管配方修复，共 93 条按配方 ID 限定的补丁。原有规则包括：

- EnderIO Evolution：14 条导管配方格式迁移，其中两条旧导管类型 `ender_item` / `ender_fluid` 改用当前 EnderIO 的 `vibrant_item` / `vibrant_fluid`。升级仍检查准确的导管组件，不能用任意导管顶替。
- NeoEcoAE：2 条电路配方迁移原料格式，补入已安装 ExtendedAE 配方构建器的默认能耗 2000。
- Avaritia：5 条创造物品配方迁移复合原料及 MEK 能量组件，保留原材料、图案、产出和四阶工作台要求。
- Avaritia 的 EnderIO 创造电源：在 `enderio:creative_power` 和 `enderio:vibrant_capacitor_bank` 均存在时才启用。当前版本缺失这些物品，因此正常禁用；这不等于恢复创造电源功能。
- 36 条装备、工具及建筑配方调整摆法，每种材料数量及产出保持不变。原版摆法保留。暮色专用箱子使用 8 木板、左上角空缺的 3×3 图案；原版箱子仍可用环形摆法和混合木板合成。
- 黑曜石音符鱼钩使用黑曜石鱼钩作为基底。
- MI 四级变压器保留升压版本的原始制作配方；对应降压版本由升压版本 1:1 转换。组装机同步使用升压变压器输入，保留原 8 EU/t、200 tick，因此完整机器路线增加一次方向转换工序。切石机提供反向转换。
- JDT 木炭块从 MEK 木炭块在切石机 1:1 转换，切石机也提供反向转换。两种方块各自属性、燃烧行为及原来的九份木炭成本保持。

手动切石转换不会导入 MI 切割机。机器转换只接受无额外数据组件的物品，防止丢失储能、配置或名称等状态。已有机器若带状态则不会匹配转换。0.1.1 还保护 MI 原有的 8 条 `up_to_down` / `down_to_up` 配方，避免同产物的另一条配方绕过此限制。

MI 组装机用原材料制作升压变压器时，输入也能满足原有能源仓配方，需要正常使用配方／输出槽锁定功能。实机已验证锁定后可以正常生产；四档升压转降压加工没有这项歧义。

## 统一的运行时修复

- 注册表并发：`HolderSet.Named` 的失效回调改用 `CopyOnWriteArrayList`，保留原回调次序和并行世界生成。
- Artifacts 15.1.3：通过 NeoForge `enqueueWork` 在包处理线程完成配置任务，避免与 C2ME 准备出生点的线程约束冲突。
- AAA Particles 2.2.3：恢复 Minecraft 26.1 的命名空间校验，包括拒绝 `..`；保留 AAA 的特效路径与无初始化工厂。
- RarityCore 2601.14.2：稀有度同步包构造时保存不可变快照，保留原网络格式。
- Titanium 4.0.8：库存定位按真实 Inventory 索引读取和写入，修复 Industrial Foregoing 背包拾取；覆盖主栏、背包与副手。
- AE2 Screenshot 26.1.2-1.0.0 与 LDLib2：取消冲突的原截图 Redirect，在渲染目标存入局部变量时选择截图目标，保留 LDLib2 渲染流程。使用当前包内已有的 MixinSquared API。
- EnderIO 9.0.6-alpha 与 Mekanism 10.8：恢复 `enderio:chemical` 类型和三档基础导管，使用事务式化学品传输，保留侧面配置、通道、红石、插入优先级及混合导管的最低路径速率。Evolution 的旋律、恒星导管与原 7 条制作路线继续使用原 ID。
- 材料配方：从 SISE Ores 迁入 15 条矿物原料格式修复、7 条小粉手动选择、3 条普通钢压块排除生物钢、2 条硅块拆解分流，以及禁止手动转换进入 MI 自动切割机的保护。

不再安装 `registry_concurrency_fix` 或 `enderio_chemical_compat` 独立模组；Artifacts、AAA Particles、EnderIO Evolution、Titanium 和 AE2 Screenshot 使用原版发行 JAR。窗口原生保护按用户要求保持现有独立实现，排除在这次归并之外。详见 [维护约定](docs/COMPAT_POLICY.md)。

## 实现与维护

- `rules.json`：逐条记录原配方、修复后配方及理由，是构建输入。
- `RecipePatches.java`：在 NeoForge 配方 JSON 事件的 HIGH 阶段应用修复；SISE Ores 在 LOW 阶段执行材料统一，Compat 在 LOWEST 阶段处理材料配方冲突。
- 每条补丁必须与审核时的原 JSON 完全一致才应用；用户数据包或新版模组改变原配方后，跳过并在日志明确记录，不强制覆盖。
- `PristineIngredient.java`：保护有额外组件的转换输入，提供网络序列化和 JEI 原料显示。
- `ManualConversionMixin.java`：阻止 Compat 和 Ores 的手动转换进入 MI 自动切割配方。
- 运行报告：游戏目录的 `logs/sise-compat-recipes.json`。`skipped` 非空时应重新审核对应配方。

本工程依赖当前实例的 NeoForge、SISE Ores 0.3.1、MI、EnderIO 9.0.6-alpha 和 Mekanism 10.8；不是经过任意整合包验证的通用补丁集合。可选运行时修复仅对审核版本启用；原模组版本更新后应检查补丁是否仍必要。数据文件保留原模组命名空间及配方 ID，不修改原模组发行 JAR、Minecraft 核心文件或公共库。

## 构建与验证

Python 构建、验证和维护脚本全部保存在本地 `tool/`；该目录及所有 `*.py` 文件均由 Git 忽略，不随仓库提交。独立克隆只包含源码、规则和文档，构建需另行准备本地工具与实例依赖，详见 [构建说明](docs/BUILDING.md)。

已准备好本地工具的维护环境中，从实例根目录运行：

```powershell
python .work\SISE-Compat\tool\build.py
python .work\SISE-Compat\tool\test.py
python .work\SISE-Compat\tool\verify.py prepare
python .work\SISE-Compat\tool\verify.py launch
python .work\SISE-Compat\tool\audit.py
python .work\SISE-Compat\tool\consolidation.py chemical
python .work\SISE-Compat\tool\consolidation.py resume
python .work\SISE-Compat\tool\consolidation.py screenshot
python .work\SISE-Compat\tool\install.py
```

完整归并测试实例为 `.work/test-sise-compat-consolidated-20261011`，化学导管测试为 `.work/test-sise-compat-chemical-final-20261011`，截图测试为 `.work/test-sise-compat-screenshot-20261011`，均使用离线身份及新建世界，不读取玩家存档。测试探针保留材料、配方、组件同步与重载回归，并检查真实注册表并发、AAA 路径工厂、Titanium 拾取、真实 MEK 储罐传输及保存重启，以及截图的 GPU 渲染和导出。`verification/baseline` 保存添加本模组前的不可变导出。

本地 `tool/plan.py` 从该基线重新生成补丁并检查全部普通合成输入域；布局同时考虑平移裁边和水平镜像。`verification/verification-report.md` 记录本地完整验证结果和未穷举的机器行为边界。当前归并结果见 [0.2.0 验证摘要](docs/verification-0.2.0.md)；此前实体机器实测见 [0.1.1 验证摘要](docs/verification-0.1.1.md)。

本地实机记录位于 `verification/gameplay/report.md`：266 条真实容器取料、15 项实体机器加工、12 项命名变压器反例。测试辅助代码在 `src/gameplay`；本地 `tool/gameplay.py` 负责独立客户端准备及启动，`tool/gameplay_regression.py` 复查保护入口，`tool/gameplay_finalize.py` 校验最终证据和安装哈希。辅助探针只在 `.work/test-*` 中使用，不安装到正式实例。
