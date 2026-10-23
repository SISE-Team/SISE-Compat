# 构建与验证

主分支为 `26.1.2`，模组版本为 `0.5.5+26.1.2`。原创代码采用 MIT，EnderIO 派生部分保留上游公共领域许可。

运行时代码位于 `src/main/java`，审核规则位于 `rules.json`，化学导管资源位于 `src/main/resources`。元数据和配方规则资源由维护者本地 `tool/build.py` 生成。全部 Python 脚本保存在本地 `tool/`；`/tool/` 和 `*.py` 均已加入 `.gitignore`，不随仓库上传。仅克隆本仓库无法直接执行以下构建或验证命令。

## 本地构建条件

- Windows、Python 3.10 或更新版本、JDK 25。
- 仓库根目录下已另行准备好本地 `tool/` 脚本。
- 目标整合包实例中的 `mods/` 和已解析的 `libraries/`，包括 NeoForge 26.1.2.114、SISE Ores 0.8.0、Modern Industrialization、EnderIO 9.0.6-alpha、Mekanism 10.8、IndustrialCraft2-CE 1.0.1、TaCZ Renovated `1.1.8+neoforge.26.1.2.R3-hotfix2` 及配套模组。TaCZ 只在编译其客户端兼容类时需要；运行时修复由版本保护和客户端 Mixin 限定。

从仓库根目录执行：

```powershell
$env:SISE_INSTANCE_DIR = 'D:\Minecraft\SISE'
$env:SISE_JDK_DIR = 'C:\Java\jdk-25'
python ../SISE-Ores/tool/build.py
python tool/build.py
python ../SISE-Ores/tool/test.py
python tool/test.py
```

产物位于 `build/libs/SISE-Compat-0.5.5+26.1.2.jar`。源码放在原实例 `.work/SISE-Compat` 时无需设置实例路径。JDK 依次使用 `SISE_JDK_DIR`、实例 `.work/devtools/jdk-25`、`JAVA_HOME`。

构建仅清理本工程 `build/classes` 和 `build/resources`，使用已安装依赖，不下载或重新分发第三方模组 JAR。脚本移入 `tool/` 后，规则、源码、构建产物及验证数据仍使用工程根目录下的原路径。

## 集成及实机验证

`src/test` 检查补丁的原始配方保护、幂等性、用户改动保护及迁入的材料冲突处理。`src/probe`、`src/chemicalprobe`、`src/screenshotprobe`、`src/serverprobe`、`src/gameplay` 和 `src/taczprobe` 分别用于完整客户端、化学导管、GPU 截图、专用服务端、实体机器及弹壳碰撞回归，探针不进入正式模组 JAR。Mixin 配置在 `src/main/resources/sise_compat.mixins.json` 维护。

完整验证还需要原实例配置、本地 `verification/baseline/` 配方基线、相邻 SISE Ores 工程的材料清单，以及实例 `.work/instance-maintenance-20261008/verify_pcl_launch.py` 启动参数生成器。这些环境和工具不随仓库提交。

```powershell
python tool/verify.py prepare
python tool/verify.py launch
python tool/audit.py
python tool/consolidation.py chemical
python tool/consolidation.py resume
python tool/consolidation.py screenshot
python tool/consolidation.py server
python tool/gameplay.py prepare
python tool/gameplay.py launch
```

归并客户端使用 `.work/test-sise-compat-consolidated-20261011`，导管使用 `.work/test-sise-compat-chemical-final-20261011`，服务端使用 `.work/test-sise-compat-server-20261011`，均为独立新建测试世界。原容器实机测试保留在 `.work/test-sise-compat-gameplay-20261011`。启动本身不代表完成验证。

`tool/install.py` 和 `tool/gameplay_finalize.py` 使用原维护环境的测试记录及哈希核验，不能当作任意实例的通用安装程序。`tool/plan.py` 是基于冻结基线的配方规划辅助工具，重新生成后需要审核，不能替代已核准的 `rules.json`。

归并验证结果与专用服务端的既有阻挡见 [0.2.0 验证摘要](verification-0.2.0.md)，此前实体机器实测见 [0.1.1](verification-0.1.1.md)。完整日志、存档、第三方配方导出和本机启动参数保留在本地，仓库只公开摘要。

建筑手杖已移除，当前 87 条规则的独立客户端、全量配方对照与安装验证见 [移除记录](remove-constructionwand.md)。此项验证使用本地 `tool/remove_constructionwand.py`，不覆盖此前归并测试实例。

0.3.0 与 SISE Ores 0.6.0 联合验证使用相邻工程的 `tool/verify.py`、物品／流体覆盖检查与 `tool/install_joint.py`，目录为 `.work/test-sise-ores-fluids-20261016`。完整客户端通过后，联合安装器核对两份产物与测试哈希再一起升级。

0.4.0 与 SISE Ores 0.7.0 使用 `.work/test-sise-ores-production-20261016`。`production.json` 是审核后的生产比例构建输入；`ProductionTests` 校验来源、幂等和用户数据包保护，Ores 的 `ProductionChecks` 验证真实扣料、锅炉、冷凝和 JEI，`tool/production_audit.py` 对照实际解码后的全部同类生产。联合安装还要求生产审计通过且两份产物哈希匹配。

0.5.1 与 SISE Ores 0.8.0 使用 `.work/test-sise-equipment-20261017`。移除十五条装备改摆法补丁，沿用 Ores 的统一身份规则，按既定优先级保留十四组原生装备，IC2 最后。`EquipmentChecks` 执行原合成网格并核对全部匹配配方、组件与 JEI；原生配方编解码还逐项证明，除恢复原装备布局外的变更仅涉及已审核装备身份，其余字段和既有生产规则不变。`tool/equipment_audit.py` 扫描实际装备配方及标签输入域交集，并对照完整导出。联合安装要求本轮装备审计及原有材料、流体、生产和世界生成回归通过；升级前既有的非装备候选需明确保留记录。
