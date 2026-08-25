# Unbirth / Vore 功能设计方案

> 基于 Lilith's Throne 0.4.11.3 汉化版源码调查，经五轮代码审计收敛（高/中危发现数 14→9→8→6→0）。
> 本文档为实施前的最终定稿，所有行号均经实际源码核实。

## 核心结论

**纯 XML mod 无法实现，必须修改 Java 源码。** 原因：

1. XML 性爱动作只能绑定一对已有部位（插入物 × 孔穴），**没有"全身"部位概念**；
2. 游戏里**完全不存在"角色存于角色体内"的数据结构**——怀孕系统存的只是 `OffspringSeed` 后代种子 ID，不是完整 NPC。

但 XML 脚本引擎（UtilText/Nashorn）可以调用 `game`/`sex`/`flags` 上任意已暴露的 public 方法（`importModdedCharacter` 就是这个模式），所以最佳路线是 **Java 做数据层和桥方法，XML 做对话和表现层**。

Unbirth（阴道→子宫）与 Vore（口→胃）共享约 80% 底层架构，实现为统一的「体内收容系统」加两种入口。

## 可复用的现有系统

| 现有系统 | 代码位置 | 在本功能中的用途 |
|----------|----------|------------------|
| 怀孕阶段状态效果 | `StatusEffect.java:4143+`（PREGNANT_0→3） | 仿造「体内有人」的分阶段腹部隆起描述 |
| 卵孵化系统 | `GameCharacter.incubatingLitters: Map<SexAreaOrifice, Litter>` | 全游戏最接近「腔内存有内容物」的现成模型；也是 ContainmentData 存档的模板 |
| 腹部外观 | `Body.java:2891-2908`（无数值 bellySize 字段） | 靠状态效果阶段 + 文本描述驱动；插入点为 2908 行 `}` 之后 |
| 体型差判定 | `GameCharacter.isSizeDifferenceTallerThan`（27194-27196，60% 身高阈值） | 「能否吞入」的前置条件；注意语义方向：调用方是较大者 |
| 角色隐藏 | `WorldType.EMPTY` + 专用 PlaceType | 猎物移出可见地图，但保留在 NPCMap 中 |
| 囚禁机制 | `RatWarrensDialogue.applyCaptivity`（370 行） | 物品备份 `addSavedInventory` 可用；但 `restoreInventories` 是玩家专用静态方法，NPC 猎物需自写参数化恢复 |
| 生产流程 | `LilayaBirthing` → `endPregnancy(true)`（230 行） | 「释放/逆生产」对话的流程模板 |
| 内容开关 | `PropertyValue` 枚举 + `OptionsDialogue` + `OptionsController`（657-679） | 新增 unbirthContent / voreContent 开关，需同时改三个文件 |
| 性癖系统 | `Fetish.java` 静态注册（2143-2161 反射扫描）+ `isContentEnabled()` 绑开关 | FETISH_UNBIRTH / FETISH_VORE（给予/接受成对）；override isContentEnabled 后生成/AI/附魔/UI 全自动门控 |
| XML 对话 | `DialogueManager` 多 scene + nextDialogue + flags | 进入 → 体内持续 → 释放 三节点结构 |

## 引擎缺口（必须新建）

- 「宿主 → 猎物」收容容器：游戏完全没有角色存于角色体内的数据结构
- 全身部位：SexArea 只有插入物 × 孔穴，无 BODY 概念
- StatusEffect 实例无法持久化角色 ID（`AppliedStatusEffect` 只有计时字段）
- 立绘过滤只有 `#preg` / `#nopreg`，无 inflation/unbirth 标签
- Nashorn 脚本引擎带 `--no-java`，XML 不能加载新类，只能调用已暴露的 public 方法（但 GameCharacter 对象可穿过引擎传递，枚举参数须收 String）

## 统一架构：体内收容系统

```
表现层（XML / 资源）
  对话场景、状态效果图标与文案、性癖图标、中文描述文本 —— 全部走 res/mods/(作者)/ 目录
脚本桥（Java 暴露给 UtilText）
  game.canBeSwallowed(prey) / addContainedCharacter(prey, "WOMB") / releaseContainedCharacter(id)
  / getContainedCharacterCount(host) / getContainedCharacter(host, index) —— 仿 importModdedCharacter 模式
数据层（Java + 存档）
  isContained 标志 + Map<ID, ContainmentData>（type/entryTime/stage）+ 猎物侧 containmentHostId 反向引用
  + 专用 GENERIC_CONTAINMENT_CELL 地块 + 存档按 captive 模式（仅真读档时加载，导出/导入强制清空）
```

**关键约束**：不要把完整 NPC 塞进 `Litter.offspring`（语义是后代种子）；不要用 `banishNPC` 隐藏猎物（会从存档删除）。

## isContained 短路策略（五轮审计定稿）

EMPTY 世界不是时间静止区，七套定时系统会穿透作用于猎物。最终短路方案：

- **Game.java 三循环守卫**：2979 主循环 `continue` 一刀切 / 3269 小时循环 `continue` / 3330 turnUpdate 循环跳过（三循环是平级兄弟结构）；3095/3128 分娩产蛋墙钟守卫显式保留在 continue 之前
- **两方法内短路**：`calculateStatusEffects`（GameCharacter.java:7616）/ `performHourlyFluidsCheck`（20879）——方法内短路是奴隶猎物的唯一有效防线（奴隶 SE 计时走 Game.java:2896/2903 而非主循环）
- **OccupancyUtil 五处**：299/329/388/401 四个循环 + 237 dailyOccupantUpdate（Game.java:2931 调用）
- **7 处奴隶选择器过滤**：Encounter.java:93/153、AbstractEncounter.java:214/255/272/319/338、RoomPlayer.java:1215/1232

已验证：SE 纯冻结无残留、附魔衣物 ItemEffect tick 同覆盖、读档流程在专用地块前提下干净。

**注意**：NPC 基类短路无效——80+ 子类重写 update 方法且不调 super，必须在 Game.java 调用点守卫。

## 五轮审计发现汇总

### 第一轮

| 严重度 | 问题 | 证据 | 修复 |
|--------|------|------|------|
| 高 | 猎物在体内会照常分娩/产蛋 | Game.java:3094-3125、3022-3024 | 冻结怀孕计时 + 3095/3128 加 `!isContained` 排除 |
| 高 | 同伴猎物每回合被强制传送回玩家身边 | Game.java:2877-2881 | 吞入时 `removeCompanion(prey)`，记录原关系以便释放恢复 |
| 高 | 奴隶/室友猎物被小时更新拉出收容 | OccupancyUtil.java:299-374；SlaveJob.java:112-119 | 各循环加 isContained 跳过 |
| 高 | 30+ 独特 NPC 的 turnUpdate 会自己逃出收容 | Game.java:3274-3332；Lilaya.java:419-427 | 调用点守卫 + MVP 禁吞独特 NPC |
| 高 | 玩家作为猎物时读档 NPE 坏档 | PlaceType.java:125-126（dialogue=null）；Game.java:2192-2195 | 玩家被吞功能暂缓，待专用「体内」对话节点 |
| 中 | GENERIC_EMPTY_TILE 每次读档都被清理 | Game.java:1447-1456 | 【已废止】→ 二轮起改用专用 CONTAINMENT_CELL |
| 中 | 猎物背包每日被清空重生 | Game.java:3288-3298 | 释放时从 getSavedInventories() 按 ID 恢复 |
| 中 | 消化后宿主列表残留悬空 ID | Game.java:5347-5375 getNPCById 抛 NPE | 消化结算先清宿主列表；isReadyToBeDeleted() 保护 unique |
| 中 | UI 穿帮：联系人/奴隶管理/事件日志 | PhoneDialogue.java:2851-2853 等 | 各 UI 列表过滤 isContained |
| 中 | 性爱中吞入的顺序问题 | Sex.java:5457-5482 | 先 setSexManager 转 spectator 再 setLocation；MVP 禁性爱中吞入 |

### 第二轮

| 严重度 | 问题 | 证据 | 修复 |
|--------|------|------|------|
| 高 | NPC 基类短路无效（80+ 子类重写不调 super） | NPC.java:425-447 | 改在 Game.java 调用点守卫 |
| 高 | 第七套穿透：猎物在体内可受孕（inGame 门控不覆盖 HOLDING_CELL） | Game.java:3276 → GameCharacter.java:20879 | performHourlyFluidsCheck 方法内短路 |
| 高 | 墙钟 12h 分娩守卫不可省 | StatusEffect.java:4458-4460；Game.java:3095 | SE 冻结 + 墙钟守卫双保险；宿主 PREGNANT_3 禁吞 |
| 高 | NPC 宿主被 banishNPC 时猎物永久丢失 | Game.java:5508-5571 | banishNPC 开头加「释放全部猎物」钩子（阶段 2 硬性前置） |
| 中 | 猎物的同伴/元素体被递归拖入收容格 | GameCharacter.java:22256-22262 | 吞入时 `prey.removeAllCompanions(true)` |
| 中 | HOLDING_CELL 是共享地块，现存循环会反向拉出猎物 | ImpFortressDialogue.java:252-278；Quest.java:351-367 | 新增专用 GENERIC_CONTAINMENT_CELL |
| 中 | 纯 ID 列表无法支撑多猎物 per-prey 阶段 | 设计层面 | Map<ID, ContainmentData>（type/entryTime/stage） |
| 中 | 嵌套收容未禁止 | 设计层面 | canBeSwallowed 硬门控：猎物体内为空 |
| 中 | 每小时钩子落点 | Game.java:2887-2900 | 阶段推进寄生宿主 SE 时长链（零新钩子）；事件钩子落 2893 之后 |

### 第三轮

| 严重度 | 问题 | 证据 | 修复 |
|--------|------|------|------|
| 高 | OccupancyUtil 是四个循环（388/401 好感/服从/收入/事件穿透） | OccupancyUtil.java:297-706 | 四循环全部加跳过 |
| 高 | endTurn 主循环 inGame 区块整体穿透（换装/刷武器/服药/回满） | Game.java:2979-3092 | 2979 循环开头 continue 一刀切 |
| 高 | 管理界面可远程卖掉/放生/玩偶化收容中的猎物 | OccupantController.java:807-820；CompanionManagement.java:2148；MiscDialogue.java:1109-1114 | `isAbleToBeSold()` 改 `return !isContained()` + UI 过滤 + 前置强制释放 |
| 中 | Java 交叉调用与 XML 脚本直调绕过守卫 | Scarlett.java:375；tavern_f0.xml 等 12 处 | MVP 禁吞独特 NPC |
| 中 | 角色导出/导入携带收容状态 | Game.java:485 导出；589/632/674/721 导入 | containment 节点仅 settings.length==0 时加载（仿 noCompanions 3044） |
| 中 | 猎物 partyLeader 成员身份未解除 | Game.java:3315-3317 | 吞入时 `prey.getPartyLeader().removeCompanion(prey)`；canBeSwallowed 补 !isContained + 排除 Elemental |
| 中 | banishNPC 需双向清理 | Game.java:5508-5571 | 猎物侧 containmentHostId 反向引用 |
| 中 | 宿主怀孕×收容文本叠加 | GameCharacter.java:20897-20938 | 宿主带猎物时禁止受孕（rollForPregnancy 一处检查） |

### 第四轮

| 严重度 | 问题 | 证据 | 修复 |
|--------|------|------|------|
| 高 | turnUpdate 循环（3330）在三循环之外，守卫遗漏 | Game.java:3330-3332 | 3330 同样加跳过 |
| 高 | 7 处奴隶遭遇/睡奸选择器按 slavesOwned 无位置检查选人 | Encounter.java:93/153；AbstractEncounter.java:214/255/272/319/338；RoomPlayer.java:1215/1232 | 各加一行 `!isContained()` 过滤 |
| 中 | empty.png 仅 3×2 像素五色全占 + 世界网格序列化进存档，旧档无新地块会 NPE | World.java:43-88；GameCharacter.java:22336-22338；先例 Game.java:1153-1183（0.2.8.1 重建 EMPTY） | 版本门控 EMPTY 世界重建迁移（`isVersionOlderThan("0.4.11.4")`）；PNG 只尾部追加像素 |
| 中 | SE 的 isConditionsMet 覆写陷阱：覆写它但不覆写 getApplicationLength 会卡死阶段链 | GameCharacter.java:7655-7656；AbstractStatusEffect.java:347-350/163 | UNBIRTH_CARRYING 纯计时，不覆写 isConditionsMet，只由桥方法添加 |
| 中 | UNBIRTH_CARRYING SE 随角色导出/导入泄漏 | GameCharacter.java:1161-1174 存 / 2558-2591 读 | 2579-2587 加载条件追加：settings.length>0 时跳过 |
| 低 | WEATHER_SNOW 迁移与 savedEnforcers 召回可波及猎物 | 版本迁移与巡警召回逻辑 | 两处加 isContained 过滤 |

### 第五轮（收敛验证）

- 高/中危：**0**。前四轮所有关键修正描述与源码实测一致。
- 低危：`OccupantDialogue.java:1174` 室友「谈论奴隶」随机抽选未过滤（纯文案穿帮）→ 并入 p1-6。
- 文档修正：选择器实为 7 处；dailyOccupantUpdate 在 OccupancyUtil.java:237（Game.java:2931 调用）。
- 重要佐证：`calculateStatusEffects` 方法内短路是奴隶猎物的唯一有效防线（奴隶 SE 计时走 Game.java:2896/2903）。
- 实施顺序依赖：p1-1（字段）→ p1-4（地块）→ p1-0（守卫）→ p1-3（桥方法）→ 其余。

## Unbirth 与 Vore 的差异设计

| 维度 | Unbirth（逆生产） | Vore（吞噬） |
|------|-------------------|--------------|
| 入口孔穴 | 阴道 → 子宫（ORIFICE_VAGINA） | 口 → 胃（ORIFICE_MOUTH） |
| 最近现有模型 | incubatingLitters 子宫卵孵化 | incubatingLitters 肛/口（胃）分支 |
| 典型走向 | 长期携带、变身、重生为宿主后代 | 消化（可选致命）、吐出、囚禁 |
| 时间推进 | 变身阶段（仿 PREGNANT_1→3 文本） | 消化阶段（仿 CUM_INFLATION 分级） |
| 致命性 | 通常无 | 可选，需独立内容开关，unique NPC 必须保护 |
| 性癖设计 | FETISH_UNBIRTH_GIVING / _RECEIVING | FETISH_VORE_PRED / _PREY |
| 释放场景 | 逆生产（复用 LilayaBirthing 流程） | 吐出 / 反刍对话 |

## 已验证可行的新组件设计（四轮终审通过）

- **ContainmentData 存档**：仿 `incubatingLitters`（GameCharacter.java:1260-1269 存 / 2717-2728 读）复合值模式：`<containment><entry id="preyId" type="WOMB" entryTime="..." stage="1"/></containment>`；读取用 captive 模式守卫（1708-1710）；用 LinkedHashMap 保序
- **桥方法签名**：`boolean canBeSwallowed(GameCharacter prey)` / `boolean addContainedCharacter(GameCharacter prey, String type)` / `boolean releaseContainedCharacter(String preyId)`（先 isCharacterExisting 守卫）/ `int getContainedCharacterCount(GameCharacter host)` / `GameCharacter getContainedCharacter(GameCharacter host, int index)`；猎物经 `game.setParserTarget(tag, npc)`（Game.java:6272-6274）成为解析目标
- **XML 对话**：条件 Response（`availabilityConditional` 写 `[#game.canBeSwallowed(npc)]`）、nextDialogue 串联、effects 调桥方法全部实证可行（glory_hole.xml）；唯一硬编码点是入口 Response 注入（用 `DialogueManager.getDialogueFromId` 衔接 XML）
- **腹部文本**：Body.java:2908 后加 `hasStatusEffect(UNBIRTH_CARRYING_x)` 兄弟分支；`getPregnancyDetails`（5930+）加平行「体内」章节（仿 getIncubationPregnancyDetails 6042-6053 多条目聚合）
- **SE 链**：UNBIRTH_CARRYING_1→2→3 用 extraRemovalEffects 时长链（仿 PREGNANT 4458-4460）；secondsRemaining 持久化（1168-1170），读档链不断；宿主在 EMPTY 世界 SE 也照常推进（3023 在 inGame 区块外）

## 分阶段路线图

### 阶段 1：Unbirth MVP（对话驱动）

实施顺序：p1-1 → p1-4 → p1-0 → p1-3 → p1-2/p1-5/p1-6 → p1-7 → p1-8（编译期依赖）

- **p1-0**【最高优先】isContained 短路：Game.java 三循环（2979/3269/3330）+ 两方法内短路（7616/20879）+ OccupancyUtil 五处（299/329/388/401/237）+ 7 处奴隶选择器过滤 + 分娩墙钟 3095/3128 显式排除
- **p1-1** Map<ID, ContainmentData> + 猎物侧 containmentHostId 反向引用 + 存档 captive 模式（仅 settings 为空时加载，导出/导入强制清空，SE 块 2579-2587 同样门控）
- **p1-2** PropertyValue.unbirthContent 开关 + 设置界面入口（默认关闭）
- **p1-3** 脚本桥方法（签名见上节）
- **p1-4** GENERIC_CONTAINMENT_CELL：PlaceType 字段 + EMPTY placesMap 颜色映射 + empty.png 尾部追加像素 + 版本门控 EMPTY 世界重建迁移
- **p1-5** canBeSwallowed 门控：体型差 60% + 开关 + 猎物体内为空 + !isContained + 非 Elemental + 非独特 NPC（MVP）+ 宿主非 PREGNANT_3
- **p1-6** 防远程处置：isAbleToBeSold() + 管理 UI 4 处过滤（OccupantManagementDialogue:1163/1239/1100/410）+ OccupantDialogue:1174 谈资过滤 + 放生/玩偶化前置强制释放
- **p1-7** UNBIRTH_CARRYING 状态效果链（纯计时，不得单独覆写 isConditionsMet）+ Body.java:2908 后腹部分支 + getPregnancyDetails「体内」章节 + 宿主带猎物时禁止受孕
- **p1-8** Java 入口 Response 注入 + 吞入/持续/释放三场景 XML（释放：宿主在真实世界放宿主格 / 在 EMPTY 则 returnToHome；先清 isContained；物品从备份恢复；重置 XML SE 墙钟）

### 阶段 2：Vore + 消化 + 性爱动作入口

- **p2-0**【硬性前置】banishNPC 开头加「释放全部猎物」钩子
- **p2-1** Vore 变体：STOMACH 类型 + 吞咽入口
- **p2-2** 消化阶段：寄生宿主 SE 时长链推进；事件/结算钩子落 Game.endTurn:2893 之后
- **p2-3** 吐出 / 消化分支结算（先清宿主列表再 banish 猎物；isReadyToBeDeleted() 保护 unique NPC）
- **p2-4** 硬编码 SPECIAL 性爱动作入口（先 setSexManager 转 spectator 再 setLocation）
- **p2-5** FETISH_UNBIRTH / FETISH_VORE 成对性癖（isContentEnabled 绑开关，自动门控）

### 阶段 3：打磨

- 体内变身选项：猎物缩小 TF / 重生为宿主后代（接 OffspringSeed）
- 立绘 #unbirth / #vore 文件标签过滤（仿 #preg）
- NPC 反应：目击者对话、猎物体内挣扎文本事件
- 战斗特殊招式：体型差吞咽（仿 CMSpecialAttack 1.5 倍身高暴击判定）
- 手机/属性面板显示「体内」状态与猎物列表
- 玩家作为猎物（第一人称被吞）：需先做专用「体内」对话节点解决读档 NPE

## 实施备注

- 新增字段按 captive 模式（非空才写、读取守卫）保证旧档零风险
- 所有新文本遵循现有汉化风格（长文本可放 res/txt 引用）
- 新代码集中在独立包（如 game/character/containment/）减少与上游合并冲突
- 消化等致命分支默认关闭，用 isReadyToBeDeleted() 保护 unique NPC
- 未来新增版本迁移代码必须感知收容系统
- mod 被移除后读档靠 getClosestStringMatch 模糊匹配降级（不崩但语义漂移），需写入用户文档
