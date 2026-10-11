# 构建与验证

主分支为 `26.1.2`，模组版本为 `0.1.1+26.1.2`，采用 MIT 协议。

运行时代码位于 `src/main/java`，审核规则位于 `rules.json`。资源文件和模组元数据由维护者本地 `tool/build.py` 生成。全部 Python 脚本保存在本地 `tool/`；`/tool/` 和 `*.py` 均已加入 `.gitignore`，不随仓库上传。仅克隆本仓库无法直接执行以下构建或验证命令。

## 本地构建条件

- Windows、Python 3.10 或更新版本、JDK 25。
- 仓库根目录下已另行准备好本地 `tool/` 脚本。
- project02 实例中的 `mods/` 和已解析的 `libraries/`，包括 NeoForge 26.1.2.114、SISE Ores、Modern Industrialization 及整合包配套模组。

从仓库根目录执行：

```powershell
$env:SISE_INSTANCE_DIR = 'D:\Minecraft\project02'
$env:SISE_JDK_DIR = 'C:\Java\jdk-25'
python tool/build.py
python tool/test.py
```

产物位于 `build/libs/SISE-Compat-0.1.1+26.1.2.jar`。源码放在原实例 `.work/SISE-Compat` 时无需设置实例路径。JDK 依次使用 `SISE_JDK_DIR`、实例 `.work/devtools/jdk-25`、`JAVA_HOME`。

构建仅清理本工程 `build/classes` 和 `build/resources`，使用已安装依赖，不下载或重新分发第三方模组 JAR。脚本移入 `tool/` 后，规则、源码、构建产物及验证数据仍使用工程根目录下的原路径。

## 集成及实机验证

`src/test` 检查补丁的原始配方保护、幂等性和用户改动保护。`src/probe` 与 `src/gameplay` 分别用于完整客户端回归和真实容器、实体机器测试，探针不进入正式模组 JAR。

完整验证还需要原实例配置、本地 `verification/baseline/` 配方基线、相邻 SISE Ores 工程的材料清单，以及实例 `.work/instance-maintenance-20261008/verify_pcl_launch.py` 启动参数生成器。这些环境和工具不随仓库提交。

```powershell
python tool/verify.py prepare
python tool/verify.py launch
python tool/audit.py
python tool/gameplay.py prepare
python tool/gameplay.py launch
```

客户端与实机测试分别使用独立 `.work/test-sise-compat-20261011`、`.work/test-sise-compat-gameplay-20261011`，采用离线测试身份及新建世界。实机批量操作需要测试探针与独立客户端配合，启动本身不代表完成验证。

`tool/install.py` 和 `tool/gameplay_finalize.py` 使用原维护环境的测试记录及哈希核验，不能当作任意实例的通用安装程序。`tool/plan.py` 是基于冻结基线的配方规划辅助工具，重新生成后需要审核，不能替代已核准的 `rules.json`。

已完成的游戏验证结果见 [0.1.1 验证摘要](verification-0.1.1.md)。完整日志、存档、第三方配方导出和本机启动参数保留在本地，仓库只公开摘要。
