# SISE Compat — NeoForge 26.1.2

整合包兼容修复的统一入口。我们自己的 Compat 修复在本工程维护，由 `sise_compat` 一个模组、一个正式 JAR 交付。物品、导管和材料沿用原有注册 ID；客户端与服务端均安装本模组。

主分支：`26.1.2`。当前版本：`0.5.5+26.1.2`。原创代码使用 [MIT 协议](LICENSE)，EnderIO 派生部分保留上游公共领域许可，见 [来源声明](NOTICE.md)。

配方规则快照用于识别和兼容上游配方；恢复的 EnderIO 化学导管包含上游 GUI 配置代码和三档导管纹理，不包含上游模组 JAR。第三方部分遵循各自原协议。

本轮 UU、18 组流体、废料和硅晶圆统一见 [SISE Ores 0.6.0](../SISE-Ores/docs/verification-0.6.0.md)。Compat 负责原生堆栈、传输资源、精确流体比较、IC2／MI 的硬编码引用、旧桶接口、废料盒掉落和创造列表隐藏。数量、组件与原生制造成本保持。此前普通板材、塑料片与齿轮统一见 [0.5.0](../SISE-Ores/docs/verification-0.5.0.md)。

## 范围

当前保留 86 条按配方 ID 限定的补丁及 5 条手动反向转换。0.5.1 撤销 IC2 九件青铜装备、五种 Prefab 速刃及 CakestickLib 凿子的 15 条改摆法补丁；相同合成表由 SISE Ores 0.8.0 按既定优先级统一为原生装备，IC2 排在最低优先级，详见 [装备统一记录](../SISE-Ores/docs/verification-0.8.0.md)。冰火传说 2.2-beta.4 移除了旧铜装备配方，对应 9 条旧摆法补丁随之退役；Construction Wands Revived 的 6 条专属补丁也已随模组移除。保留 Construction Sticks（建筑棒）。原有规则包括：

建筑手杖移除及建筑棒 37 条配方保持不变的验证结果见 [移除记录](docs/remove-constructionwand.md)。

- EnderIO Evolution：14 条导管配方格式迁移，其中两条旧导管类型 `ender_item` / `ender_fluid` 改用当前 EnderIO 的 `vibrant_item` / `vibrant_fluid`。升级仍检查准确的导管组件，不能用任意导管顶替。
- NeoEcoAE：2 条电路配方迁移原料格式，补入已安装 ExtendedAE 配方构建器的默认能耗 2000。
- Avaritia：5 条创造物品配方迁移复合原料及 MEK 能量组件，保留原材料、图案、产出和四阶工作台要求。
- Avaritia 的 EnderIO 创造电源：在 `enderio:creative_power` 和 `enderio:vibrant_capacitor_bank` 均存在时才启用。当前版本缺失这些物品，因此正常禁用；这不等于恢复创造电源功能。
- 此前其他建筑配方的摆法补丁继续保留，每种材料数量及产出保持不变。原版摆法保留。暮色专用箱子使用 8 木板、左上角空缺的 3×3 图案；原版箱子仍可用环形摆法和混合木板合成。
- 黑曜石音符鱼钩使用黑曜石鱼钩作为基底。
- MI 四级变压器保留升压版本的原始制作配方；对应降压版本由升压版本 1:1 转换。组装机同步使用升压变压器输入，保留原 8 EU/t、200 tick，因此完整机器路线增加一次方向转换工序。切石机提供反向转换。
- JDT 与 MEK 的旧木炭块仍可手动转换；SISE Ores 0.4.0 将两者的普通产出统一为 SISE 木炭块，保留 MEK 优先来源的属性、燃烧时间及九份木炭成本。

手动切石转换不会导入 MI 切割机。机器转换只接受无额外数据组件的物品，防止丢失储能、配置或名称等状态。已有机器若带状态则不会匹配转换。0.1.1 还保护 MI 原有的 8 条 `up_to_down` / `down_to_up` 配方，避免同产物的另一条配方绕过此限制。

MI 组装机用原材料制作升压变压器时，输入也能满足原有能源仓配方，需要正常使用配方／输出槽锁定功能。实机已验证锁定后可以正常生产；四档升压转降压加工没有这项歧义。

## 统一的运行时修复

- RSLS 1.2.2 / NeoForge 26.1.2.114：声音状态查询在 RSLS 已有的同步映射锁内完成，避免音频线程在 contains/get 之间删除记录而导致主线程空指针。保留 AsyncParticles 和 RSLS 待播放声音的状态修饰；覆盖 TaCZ 声音清理调用路径。见[对撞机使用期间的声音崩溃诊断与回归](docs/sound-race-20261018.md)。
- Traveler’s Titles `26.1.2-NeoForge-6.1.0` 与 SISE Core `0.5.11+26.1.2` 起：私人空间进入标题和 `/dimensiontitle` 使用服务器提供的所有者名字；修正多个私人空间共享维度类型时不切换标题的问题，支持离线主人、名字更新及保存重进。保留上游的开关、黑名单和地表限制，其他维度继续使用上游逻辑。
- Ponder / Catnip / Flywheel 已随旧动画教程退役。多方块教程及 W 入口由 SISE Core 独立提供，见[多方块搭建手册](../SISE-Core-26/docs/MULTIBLOCKS.md)。
- 注册表并发：`HolderSet.Named` 的失效回调改用 `CopyOnWriteArrayList`，保留原回调次序和并行世界生成。
- Artifacts 15.1.3：通过 NeoForge `enqueueWork` 在包处理线程完成配置任务，避免与 C2ME 准备出生点的线程约束冲突。
- AAA Particles 2.2.3：恢复 Minecraft 26.1 的命名空间校验，包括拒绝 `..`；保留 AAA 的特效路径与无初始化工厂。
- TaCZ Renovated `1.1.8+neoforge.26.1.2.R3-hotfix2`：将本地玩家抛出的弹壳从手持物品渲染转入世界渲染，使用世界重力、方块碰撞、衰减弹跳和落地停止旋转。测量碰撞尺寸时排除未绘制面的占位顶点，确保可见弹壳贴地；抛壳初速度降为原值的四分之一，水平速度上限为 2 格/秒。切换武器、转头和移动后仍保留原世界位置。默认至少显示 8 秒，最多 256 枚，卸载世界或重载资源时清理。修复仅在审核版本的客户端启用，见 [弹壳修复说明](docs/tacz-shell-physics.md)。
- RarityCore 2601.14.2：稀有度同步包构造时保存不可变快照，保留原网络格式。
- Titanium 4.0.8：库存定位按真实 Inventory 索引读取和写入，修复 Industrial Foregoing 背包拾取；覆盖主栏、背包与副手。
- AE2 Screenshot 26.1.2-1.0.0 与 LDLib2：取消冲突的原截图 Redirect，在渲染目标存入局部变量时选择截图目标，保留 LDLib2 渲染流程。使用当前包内已有的 MixinSquared API。
- EnderIO 9.0.6-alpha 与 Mekanism 10.8：恢复 `enderio:chemical` 类型和三档基础导管，使用事务式化学品传输，保留侧面配置、通道、红石、插入优先级及混合导管的最低路径速率。Evolution 的旋律、恒星导管与原 7 条制作路线继续使用原 ID。
- EnderIO Evolution 4.0.1：移除两档化学导管定义中的冗余 `neoforge:registered` 条件，避免客户端复用已知数据包时因条件上下文为空而漏载导管，继而在 `neoforge:recipe_content` 解码时断线。保留 Mekanism 加载条件和全部配方，见 [联机同步修复](docs/enderio-conduit-lan-sync.md)。
- 十四组相同合成表的装备直接统一产物，不再用额外工具或新摆法区分。此前材料冲突补丁继续保留：碳纤维、青铜粉以及 CakestickLib 铁／金齿轮使用可返还的 IC2 锻造锤；Extended Crafting 红石锭采用竖排铁锭与红石粉，ProjectE 中级共价粉采用横排，保留原材料和产量。
- UU／流体：18 组流体与六组化学品采用 SISE 身份；原生制造、复制、燃料与事务式储罐继续使用原生接口。真实锅炉蒸汽及 JEI 生成配方均为 SISE 来源，普通蒸汽生产／冷凝采用 1:1 水当量。IC2 普通蒸汽适配器的 10→16 同物质路线停用，过热蒸汽保持独立身份。创造标签页在全部相对插入完成后隐藏旧材料，避免删掉其他模组需要的插入锚点。
- 生产配方：83 条带完整来源校验的规则，对同输入、同工艺阶段按既定模组优先级统一产物与每单位原料产量，IC2 最后。分数比例使用整批原料，SAG 与 Oritech 实际扣除完整批次；机器可逐个收料，但不足一批不能开工。四条槽位／概率格式受限的路线保留机器及副产物，只统一主产物比例。详见相邻 Ores 工程的 [0.7.0 联合验证](../SISE-Ores/docs/verification-0.7.0.md)。
- 材料配方：15 条矿物原料格式修复、4 条普通钢压块排除生物钢、2 条硅块拆解分流。统一后七条 Oritech 小粉拆分恢复直接工作台合成；硫粉拆小撮粉和 IC2 铀块拆铀-238 采用材料加 IC2 锻造锤，保留 1:9 产量，锤子返还并损耗 1 点耐久。旧材料迁移转换继续手动处理。

不再安装 `registry_concurrency_fix` 或 `enderio_chemical_compat` 独立模组；Artifacts、AAA Particles、EnderIO Evolution、Titanium 和 AE2 Screenshot 使用原版发行 JAR。窗口原生保护按用户要求保持现有独立实现，排除在这次归并之外。详见 [维护约定](docs/COMPAT_POLICY.md)。

## 实现与维护

- `rules.json`：逐条记录原配方、修复后配方及理由，是构建输入。
- `production.json` 与 `ProductionPatches.java`：在身份统一和既有冲突处理之后校准生产比例；每条规则记录参考配方、完整来源与结果，运行报告为 `logs/sise-compat-production.json`。
- `RecipePatches.java`：在 NeoForge 配方 JSON 事件的 HIGH 阶段应用修复；SISE Ores 在 LOW 阶段执行材料统一，Compat 在 LOWEST 阶段处理材料配方冲突。
- 每条补丁必须与审核时的原 JSON 完全一致才应用；用户数据包或新版模组改变原配方后，跳过并在日志明确记录，不强制覆盖。
- `PristineIngredient.java`：保护有额外组件的转换输入，提供网络序列化和 JEI 原料显示。
- `ManualConversionMixin.java`：阻止 Compat 和 Ores 的手动转换进入 MI 自动切割配方。
- 运行报告：游戏目录的 `logs/sise-compat-recipes.json`。`skipped` 非空时应重新审核对应配方。

本工程依赖当前实例的 NeoForge、SISE Ores 0.8.0、MI、EnderIO 9.0.6-alpha 和 Mekanism 10.8；不是经过任意整合包验证的通用补丁集合。可选运行时修复仅对审核版本启用；原模组版本更新后应检查补丁是否仍必要。数据文件保留原模组命名空间及配方 ID，不修改原模组发行 JAR、Minecraft 核心文件或公共库。

## 构建与验证

0.5.2 同步识别 Industrial Foregoing `26.1.2-3.6.39-viking602.3` 的原有资源 SHA-256 保护，以及 ProjectE Reforged `26.1.2-1.1.2` 的冰火 EMC 兼容保护。两个 Curios 图标的 GUI 图集修正由 ProjectE 自身交付，`example` 槽分配由 Industrial Foregoing 自身移除；Compat 只维护已有补丁对新发行版的识别。

0.5.0 增加 TaCZ 配方 JSON 的无损编解码及延迟产物初始化，保留预装弹药、附件、原料数量、分组、配方 ID 和自定义物品组件。网络协议沿用上游格式。厨房酒柜保存使用可空槽位编码；出货箱补齐实际使用的战利品上下文；暮色拆解预览和 JEI 页面移植官方的显示接口实现，避免用空工作台调用动态配方。

资源层按原文件 SHA-256 启用已审核的音频、模型、图集、动画、指南和配方数据修复。流体别名复用原生流体模型及着色。源文件改变时跳过对应修复，避免覆盖新版内容。旧指南指向已移除方块或物品的条目与页面，仅在该内容未注册时停用；改名物品与配方引用迁移到当前 ID，现有锻造配方使用锻造页。TaCZ 动画保留真实分段音效，修正路径，并移除已有完整动作录音覆盖的无效旧引用；默认枪包的外部文件也校验 SHA-256，保留用户自行修改的枪包。审阅来源和具体变更原因位于 JAR 的 `sise_compat/resource_repairs.json`、`resource_filters.json`、`atlas_stack_repairs.json` 和 `tacz_sound_audit.json`。

本轮完整测试使用 `.work/test-kaleidoscope-resources-20261017` 与 `.work/test-kaleidoscope-tacz-flow-20261017` 两个隔离实例，测试源分别保存在 `src/updateprobe`、`src/flowprobe`，正式 JAR 不打包探针。详细日志、安装校验和、旧版备份及验收结果保存在 `.work/resource-tacz-repair-20261017`。

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
