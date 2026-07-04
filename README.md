# Guild - 公会管理插件

功能全面的 Minecraft 公会管理插件，支持 GUI 交互、多货币系统、等级经验、银行商店等完整生态。

## 兼容性

| 项目 | 说明 |
|------|------|
| 服务端 | Spigot / Paper / Purpur / **Folia** |
| MC 版本 | **1.8 ~ 26.1+**（全版本兼容） |
| Java | 8+ |
| API | Bukkit/Spigot API 1.13+ |

## 功能概览

- **GUI 界面** — 全图形化操作，铁砧输入创建公会，无需记忆命令
- **等级系统** — 公会最高 100 级，等级提升解锁更多成员上限
- **经验系统** — 击杀怪物、挖掘方块、PVP 均可获取公会经验
- **银行系统** — 公会成员存取资金，支持内置币/Vault/PlayerPoints
- **公会商店** — 按等级解锁专属商品（资源包、装备、技能书等）
- **聊天频道** — 独立公会聊天频道，支持格式化前缀
- **角色权限** — 会长 / 管理员 / 成员 三级权限体系
- **标签系统** — 自定义公会标签，支持颜色代码
- **上下线通知** — 成员上线/下线自动广播
- **多语言** — 内置简体中文、繁体中文、英文三套语言包

## 快速开始

1. 将 `Guild-3.0.0.jar` 放入服务端 `plugins/` 目录
2. 启动服务器（自动生成 `plugins/Guild/config.yml`）
3. 玩家使用 `/guildgui` 打开 GUI 界面创建或加入公会

## 命令列表

| 命令 | 别名 | 说明 | 权限 |
|------|------|------|------|
| `/guild` | `/gh` | 公会主指令 | guild.player |
| `/guild create <名称> [标签]` | — | 创建公会 | guild.create |
| `/guild info [公会名]` | — | 查看公会信息 | guild.info |
| `/guild invite <玩家>` | — | 邀请玩家 | guild.invite |
| `/guild kick <玩家>` | — | 踢出成员 | guild.kick |
| `/guild leave` | — | 离开公会 | guild.player |
| `/guild disband` | — | 解散公会 | guild.disband |
| `/guild chat <消息>` | — | 公会频道发言 | guild.chat |
| `/guild deposit <金额>` | — | 存入银行 | guild.bank |
| `/guild withdraw <金额>` | — | 取出银行 | guild.bank |
| `/guild top` | — | 公会排行榜 | guild.info |
| `/guild reload` | — | 重载配置 | op |
| `/guildchat <消息>` | `/gc`, `/guildc` | 快捷公会聊天 | guild.chat |
| `/guildgui` | — | 打开 GUI 界面 | guild.player |

## 权限树

```
guild.*          (op)     → 全部权限
├── guild.admin   (op)    → 管理操作（创建/邀请/踢出/升降级/解散/升级/银行/管理）
│   ├── guild.create       创建公会 (true)
│   ├── guild.invite       邀请玩家 (op)
│   ├── guild.kick         踢出成员 (op)
│   ├── guild.promote      提升职位 (op)
│   ├── guild.demote       降低职位 (op)
│   ├── guild.disband      解散公会 (op)
│   ├── guild.upgrade      升级公会 (op)
│   ├── guild.bank         银行操作 (true)
│   ├── guild.manage       管理设置 (op)
│   ├── guild.settings     个人设置 (true)
│   └── guild.chat         频道聊天 (true)
└── guild.player  (true)   → 基础权限
    ├── guild.chat         频道聊天 (true)
    └── guild.info         查看信息 (true)
```

## 配置说明

### 货币系统 (`config.yml` → `currency`)

支持三种货币类型，无需外部依赖即可运行：

```yaml
currency:
  type: GUILD_COIN        # 内置公会币（默认）
  # type: VAULT            # Vault 经济插件
  # type: PLAYERPOINTS     # PlayerPoints 积分插件
```

### 数据库 (`config.yml` → `database`)

- **SQLite**（默认）：零配置，数据存储在 `plugins/Guild/guild.db`
- **MySQL**：填写连接信息即可切换，使用 HikariCP 连接池

### 语言包 (`lang/`)

修改 `config.yml` 中的 `language` 字段切换语言：

- `zh_cn` — 简体中文（默认）
- `zh_tw` — 繁体中文
- `en_US` — English

## GUI 创建流程

```
点击"创建公会"
    ↓
铁砧界面：输入公会名称（2~20字符，支持中文/英文/数字/下划线）
    ↓
铁砧界面：输入公会标签（1~6字符）
    ↓
创建完成 → 自动打开公会主界面
```

## 软依赖

以下插件为可选依赖，缺失时核心功能正常运行：

| 插件 | 用途 |
|------|------|
| **Vault** | 接入外部经济系统（当 `currency.type: VAULT` 时） |
| **PlaceholderAPI** | 提供变量占位符（如 `%guild_name%`） |
| **ProtocolLib** | 增强 JSON 可点击消息体验 |

## 技术特性

- **HikariCP 连接池** — 高性能数据库连接管理，Shade 打包避免版本冲突
- **反射缓存** — 全版本 Material/NMS 兼容，启动时一次性初始化
- **线程安全** — ConcurrentHashMap 保护所有共享状态
- **Folia 支持** — `folia-supported: true`，兼容区域化线程模型
- **Java 8 兼容** — 无 Lambda 以外的 Java 8+ 特性要求

## 构建方式

```bash
mvn clean package
# 输出: target/Guild-3.0.0.jar（含 Shade 依赖）
```
