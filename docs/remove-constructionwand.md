# 移除建筑手杖

整合包保留 Construction Sticks（`constructionstick`，建筑棒），移除 Construction Wands Revived（`constructionwand`，建筑手杖复兴版）。其功能重叠，不再需要为建筑手杖维护配方兼容。

`ConstructionWandsRevived-26.1.2-4.0.2-NeoForge.jar` 及 `constructionwand-client.toml`、`constructionwand-server.toml` 已从正式实例退役。旧文件和此前的 Compat JAR 保存在实例 `.work/outputs/constructionwand-retired-20261011/`，不进入正式 `mods/`。

从 `rules.json` 删除以下六条专属布局补丁：

- `constructionwand:stone_wand`、`constructionwand:stone_wand_alt1`
- `constructionwand:iron_wand`、`constructionwand:iron_wand_alt1`
- `constructionwand:diamond_wand`、`constructionwand:diamond_wand_alt1`

其余 87 条配方补丁和 5 条手动转换与此前完全相同；其中布局调整由 36 条减至 30 条。未修改 Construction Sticks 发行 JAR。

## 验证

2026-10-11 使用 `SISE-Compat-0.2.1+26.1.2.jar` 在独立 `.work/test-sise-compat-without-wand-20261011` 验证，采用离线身份及新建世界。测试不读取玩家存档，探针不进入正式 JAR。

| 检查 | 结果 |
|---|---:|
| 配方原始 JSON、幂等及用户修改保护 | 262 项通过 |
| 12 条材料冲突保护 | 49 项通过 |
| 完整客户端、新世界及重载 | 13,150 项通过，退出码 0 |
| 当前加载配方 | 17,625 |
| 匹配及产出调用 | 12,732 |
| 建筑手杖配方移除 | 19 条 |
| 其他旧配方丢失或变化 | 0 |
| 建筑棒配方 | 37 条，全部与移除前相同 |
| 配方加载、编码及执行错误 | 0 |
| 新合成、自动机器冲突及可计量材料增殖 | 0 |
| 全部配方重载 | 一致 |
| 原生窗口保护 JAR、游戏及公共库文件 | 哈希保持一致 |

建筑手杖的 8 个物品已不再注册。87 条补丁全部通过原始 JSON 保护并应用，没有跳过。当前 3 对普通合成冲突候选和 73 对自动机器候选均为原有情况，移除前后没有增加。

已测试并安装的产物 SHA-256：`31f547360ab26074362c4e8bd7a0b9748791d3a2259c531df12a27cb15a68b21`。本地逐条对照、测试结果、安装哈希及备份记录保存在 `verification/remove-constructionwand/`。此前 0.2.0 的验证记录保持原样。

已准备本地工具的维护环境可运行：

```powershell
python tool/remove_constructionwand.py prepare
python tool/remove_constructionwand.py test
python tool/remove_constructionwand.py launch
python tool/remove_constructionwand.py audit
python tool/remove_constructionwand.py install
```

该工具验证正式构建已经包含 87 条规则，再复制候选至独立实例；不构建或打包其他兼容模组。完整客户端退出并完成配方对照后，安装步骤才允许替换 Compat。模组移除及新 Compat 安装需重启游戏生效。
