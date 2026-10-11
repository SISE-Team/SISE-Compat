# SISE Compat — NeoForge 26.1.2

整合包兼容修复模组。物品仍属于原模组；不注册替代导管、装备或机器，不更改 SISE Ores 的材料归属。客户端与服务端均安装本模组。

主分支：`26.1.2`。当前版本：`0.1.1+26.1.2`。项目使用 [MIT 协议](LICENSE)。

配方规则快照用于识别和兼容上游配方；不打包上游纹理或二进制文件，第三方模组仍遵循各自协议。

## 范围

当前 0.1.1 包含 76 条按配方 ID 限定的补丁及 5 条手动反向转换：

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

## 实现与维护

- `rules.json`：逐条记录原配方、修复后配方及理由，是构建输入。
- `RecipePatches.java`：在 NeoForge 配方 JSON 事件的 HIGH 阶段应用修复，在 KubeJS 通常解析阶段前工作；SISE Ores 随后执行材料统一。
- 每条补丁必须与审核时的原 JSON 完全一致才应用；用户数据包或新版模组改变原配方后，跳过并在日志明确记录，不强制覆盖。
- `PristineIngredient.java`：保护有额外组件的转换输入，提供网络序列化和 JEI 原料显示。
- `ManualConversionMixin.java`：仅阻止本模组手动转换进入 MI 自动切割配方。
- 运行报告：游戏目录的 `logs/sise-compat-recipes.json`。`skipped` 非空时应重新审核对应配方。

本工程依赖当前实例的 NeoForge、SISE Ores 和 MI；不是经过任意整合包验证的通用补丁集合。原模组版本更新后应检查补丁是否仍必要。数据文件保留原模组命名空间及配方 ID，不修改任何原模组 JAR、Minecraft 核心文件或公共库。

## 构建与验证

Python 构建、验证和维护脚本全部保存在本地 `tool/`；该目录及所有 `*.py` 文件均由 Git 忽略，不随仓库提交。独立克隆只包含源码、规则和文档，构建需另行准备本地工具与实例依赖，详见 [构建说明](docs/BUILDING.md)。

已准备好本地工具的维护环境中，从实例根目录运行：

```powershell
python .work\SISE-Compat\tool\build.py
python .work\SISE-Compat\tool\test.py
python .work\SISE-Compat\tool\verify.py prepare
python .work\SISE-Compat\tool\verify.py launch
python .work\SISE-Compat\tool\audit.py
python .work\SISE-Compat\tool\install.py
```

测试实例是 `.work/test-sise-compat-20261011`，使用离线身份、新建平坦世界和一次完整资源重载；不读取玩家存档。保留 SISE Ores 的回归检查，并添加导管型号、创造配方等级、加工原料消耗、变压器互转及组件网络同步测试。`verification/baseline` 保存添加本模组前的不可变导出。

本地 `tool/plan.py` 从该基线重新生成补丁并检查全部普通合成输入域；布局同时考虑平移裁边和水平镜像。`verification/verification-report.md` 记录本地完整验证结果和未穷举的机器行为边界。可随仓库查看的结果见 [0.1.1 验证摘要](docs/verification-0.1.1.md)。

本地实机记录位于 `verification/gameplay/report.md`：266 条真实容器取料、15 项实体机器加工、12 项命名变压器反例。测试辅助代码在 `src/gameplay`；本地 `tool/gameplay.py` 负责独立客户端准备及启动，`tool/gameplay_regression.py` 复查保护入口，`tool/gameplay_finalize.py` 校验最终证据和安装哈希。辅助探针只在 `.work/test-*` 中使用，不安装到正式实例。
