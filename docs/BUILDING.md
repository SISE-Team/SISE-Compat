# 构建与验证

主分支为 `26.1.2`，模组版本为 `0.2.0+26.1.2`。原创代码采用 MIT，EnderIO 派生部分保留上游公共领域许可。

运行时代码位于 `src/main/java`，审核规则位于 `rules.json`，化学导管资源位于 `src/main/resources`。元数据和配方规则资源由维护者本地 `tool/build.py` 生成。全部 Python 脚本保存在本地 `tool/`；`/tool/` 和 `*.py` 均已加入 `.gitignore`，不随仓库上传。仅克隆本仓库无法直接执行以下构建或验证命令。

## 本地构建条件

- Windows、Python 3.10 或更新版本、JDK 25。
- 仓库根目录下已另行准备好本地 `tool/` 脚本。
- 目标整合包实例中的 `mods/` 和已解析的 `libraries/`，包括 NeoForge 26.1.2.114、SISE Ores 0.3.1、Modern Industrialization、EnderIO 9.0.6-alpha、Mekanism 10.8 及配套模组。

从仓库根目录执行：

```powershell
$env:SISE_INSTANCE_DIR = 'D:\Minecraft\SISE'
$env:SISE_JDK_DIR = 'C:\Java\jdk-25'
python ../SISE-Ores/tool/build.py
python tool/build.py
python ../SISE-Ores/tool/test.py
python tool/test.py
```

产物位于 `build/libs/SISE-Compat-0.2.0+26.1.2.jar`。源码放在原实例 `.work/SISE-Compat` 时无需设置实例路径。JDK 依次使用 `SISE_JDK_DIR`、实例 `.work/devtools/jdk-25`、`JAVA_HOME`。

构建仅清理本工程 `build/classes` 和 `build/resources`，使用已安装依赖，不下载或重新分发第三方模组 JAR。脚本移入 `tool/` 后，规则、源码、构建产物及验证数据仍使用工程根目录下的原路径。

## 集成及实机验证

`src/test` 检查补丁的原始配方保护、幂等性、用户改动保护及迁入的材料冲突处理。`src/probe`、`src/chemicalprobe`、`src/screenshotprobe`、`src/serverprobe` 和 `src/gameplay` 分别用于完整客户端、化学导管、GPU 截图、专用服务端及实体机器回归，探针不进入正式模组 JAR。

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
