# 单忍术移植配方：Rasengan 垂直切片

本文档沉淀 M4 `Rasengan` 垂直切片的可复用迁移方法。后续迁移 Chidori、Rasenshuriken、元素术和同类持有型忍术时，优先按本配方拆分任务，而不是直接翻译 1.12.2 文件。

## 目标口径

一个忍术垂直切片至少包含：

- 物品入口：学习/启用状态、owner 绑定、蓄力、冷却、查克拉消耗、经验记录。
- 实体行为：生成、owner 跟随、命中、方块交互、生命周期清理。
- 视觉：模型、RenderType、第一/第三人称位置、玩家 pose、粒子、音效。
- GUI/网络：卷轴学习界面、服务端校验、客户端到服务端的最小消息。
- 验证：`compileJava`、资源校验、专服安全扫描、完整构建、dedicated server gate。

Rasengan 当前代码落点：

| 责任 | 1.20.1 文件 |
|---|---|
| 普通 Rasengan 物品 | `1.20.1/src/main/java/net/narutomod/item/NinjutsuItem.java` |
| Sage Rasengan 与 Sage Mode | `1.20.1/src/main/java/net/narutomod/item/SenjutsuItem.java` |
| Rasengan 实体 | `1.20.1/src/main/java/net/narutomod/entity/RasenganEntity.java` |
| Rasengan 模型 | `1.20.1/src/main/java/net/narutomod/client/model/RasenganModel.java` |
| Rasengan 渲染器 | `1.20.1/src/main/java/net/narutomod/client/renderer/RasenganDebugRenderer.java` |
| 强制右臂 pose | `1.20.1/src/main/java/net/narutomod/client/PlayerPoseEvents.java` |
| Sage Mode 头部层 | `1.20.1/src/main/java/net/narutomod/client/renderer/SageModeHelmetLayer.java` |
| 卷轴物品 | `1.20.1/src/main/java/net/narutomod/item/ScrollRasenganItem.java` |
| 卷轴菜单/屏幕 | `1.20.1/src/main/java/net/narutomod/menu/RasenganScrollMenu.java`, `1.20.1/src/main/java/net/narutomod/client/gui/RasenganScrollScreen.java` |
| 网络消息 | `RasenganHandPositionMessage`, `RasenganScrollLearnMessage` |

## 迁移步骤

### 1. 先定旧版行为常数

迁移前先从 1.12.2 审计出常数，不凭感觉调：

- 普通 Rasengan：基础 power `0.0`，最小释放 `0.5`，最大 `3.0`，power-up delay `200`，查克拉 `150 * power`。
- Sage Rasengan：基础 power `2.9`，最小释放 `3.0`，最大 `7.0`，power-up delay `200`，伤害走 senjutsu damage。
- Sage Mode：蓄力目标 `100`，power-up delay `20`，基础查克拉门槛 `1000`，激活后每秒扣 `50` 查克拉。
- Rasengan 模型：核心/外壳分层，使用全亮半透明渲染，第一人称贴手臂，第三人称贴右手。
- 卷轴学习：打开 scroll GUI，服务端确认玩家可学习，给或更新 `narutomod:ninjutsu`，启用 Rasengan。

这些常数应写进迁移后的类或 profile record 中，避免分散魔法数。

### 2. 注册名先稳定

Rasengan 切片只替换必要注册项：

- `ModItems.NINJUTSU` 从 placeholder 改为 `NinjutsuItem::new`。
- `ModItems.SENJUTSU` 从 placeholder 改为 `SenjutsuItem::new`。
- `ModItems.SCROLL_RASENGAN` 从 placeholder 改为 `ScrollRasenganItem::new`。
- `ModEntityTypes.RASENGAN` 从 dummy 改为真实 `EntityType<RasenganEntity>`。
- `ModMenuTypes.RASENGAN_SCROLL` 单独注册菜单，避免把 GUI 逻辑混进物品类。

原则：registry object 是后续 datagen、GUI、命令和测试的共同锚点。先稳定注册名，再写行为。

### 3. 物品层只做输入和资源结算

`NinjutsuItem` 的模式可以复用到下一个持有蓄力型忍术：

1. `use(...)` 判断冷却、owner、学习状态、查克拉上限，然后 `startUsingItem`。
2. `onUseTick(...)` 做充能反馈：状态栏 power、粒子、蓄力音效。
3. `releaseUsing(...)` 计算 power，扣查克拉，生成实体，加冷却，记录经验。
4. 道具只保存必要 NBT：owner、learned flag、当前实体需要的尺寸/模式。

不要在 item 类里做渲染，不要直接引用 `Minecraft` 或 client package。

### 4. 学习状态必须服务端可信

Rasengan 采用两个 NBT：

- `OwnerIdKey` / `player_id`：兼容旧版 owner 语义。
- `RasenganLearned`：替代旧版 `enableJutsu(stack, ItemNinjutsu.RASENGAN, true)`。

卷轴学习消息只在玩家确实打开 `RasenganScrollMenu` 时生效：

```java
if (sender == null || !(sender.containerMenu instanceof RasenganScrollMenu)) {
    return;
}
```

其他忍术卷轴应复用这个规则：客户端按钮只发意图，服务端重新检查菜单、玩家状态、目标物品和学习条件。

### 5. 实体层负责世界行为

`RasenganEntity` 承担旧实体逻辑：

- owner id、scale、full scale、senjutsu damage 用 `SynchedEntityData`。
- 生命周期内持续贴 owner 手部。
- 命中 living target 时按普通/仙术伤害源分流。
- 方块接触调用 `ProcedureUtils.breakBlockAndDropWithChance(...)`。
- 实体清理时移除 owner 的强制 pose 标记和 `RasenganSize`。

重要边界：实体可以写公共逻辑和服务端行为，但不能引用客户端模型/渲染器。

### 6. 第三人称手部坐标走客户端辅助、服务端校验

旧版手部位置依赖 `ModelBiped` 动画姿态。1.20 服务端没有这个模型状态，所以 Rasengan 使用 owner 客户端计算右手位置，再发给服务端：

- 客户端：`RasenganDebugRenderer` 计算右臂位置，每实体每 tick 最多发一次。
- 网络：`RasenganHandPositionMessage(entityId, x, y, z)`。
- 服务端：只接受 owner 本人、有限距离、有限坐标。
- 回退：5 tick 内没收到客户端点位时，使用服务端近似手部位置。

后续 Chidori、手持光球、掌心特效可以复用同一模式，但必须保留服务端距离/owner 校验。

### 7. 玩家 pose 走同步数据，不改 renderer 本体

Rasengan 需要旧版 `BOW_AND_ARROW` 右臂姿态：

- 服务端实体激活时写 `NarutomodModVariables.FORCE_BOW_POSE`。
- `ProcedureSync.EntityNBTTag.setAndSync(...)` 负责追踪同步。
- 客户端 `PlayerPoseEvents` 在 `RenderLivingEvent.Pre` 读取变量并改 `HumanoidModel.rightArmPose`。

后续所有“强制玩家姿态”的忍术都应走同一事件层，不要替换 vanilla player renderer。

### 8. RenderType 集中复用

Rasengan 和 Sage Mode 视觉都复用 `NarutoRenderTypes`：

- 发光/半透明/双面/不写深度等状态封装进 RenderType。
- 渲染器只选择 RenderType 和传顶点，不直接操作全局 GL 状态。
- 全亮效果用 `LightTexture.FULL_BRIGHT`。

迁移 GL 耦合模型时，先把旧 GL state 归类到 RenderType，再处理几何。

### 9. GUI 和客户端注册只放 client 包

Rasengan 卷轴迁移规则：

- 物品右键在服务端 `NetworkHooks.openScreen(...)`。
- 菜单是 common 类：只承载 container id 和服务端上下文。
- 屏幕是 client 类：`AbstractContainerScreen` + `GuiGraphics`。
- `MenuScreens.register(...)` 只能在 `FMLClientSetupEvent.enqueueWork(...)`。

任何 `Screen`、`Minecraft`、`PlayerRenderer`、`RenderLayer` 都不能被 common 类静态引用。

### 10. Sage Mode 作为附属状态迁移

Rasengan 的 Senjutsu 变体暴露出一类常见问题：某个忍术依赖另一个系统状态。

当前处理方式：

- `SenjutsuItem` 继承 `NinjutsuItem`，复用 Rasengan 释放路径。
- `isRasenganLearnedForUse(...)` 改成检查玩家是否拥有已学普通 Rasengan。
- 潜行右键用于 M4 临时输入分流：蓄力激活 Sage Mode，普通右键释放 Sage Rasengan。
- `SageModeActivated` 和 `SageType` 通过玩家变量同步给客户端。
- `SageModeHelmetLayer` 只读同步状态选择 toad/snake/slug 纹理。

后续 M5 做通用 `ItemJutsu` 框架时，应把“潜行右键分流”收回到正式的切术/键位系统。

## 验证门

每完成一个同类忍术切片，至少跑以下命令：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon -D "net.minecraftforge.gradle.check.certs=false" compileJava
```

```powershell
python tools\validate_port_resources.py
python tools\validate_dedicated_server_safety.py
```

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon -D "net.minecraftforge.gradle.check.certs=false" build
```

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
powershell -ExecutionPolicy Bypass -File tools\run_dedicated_server_gate.ps1 -TimeoutSeconds 180
```

## 实机检查清单

Rasengan 切片的客户端验证项：

- `/narutoport rasengan_ready` 后，右键长按 `narutomod:ninjutsu` 可充能并释放。
- 未学会 Rasengan 时普通玩家不能使用，卷轴 Learn 后可以使用。
- `scroll_rasengan` 打开 GUI，按钮能学习，卷轴不消耗。
- 第一人称 Rasengan 贴右手，第三人称 Rasengan 贴右臂姿态位置。
- 右臂 pose 在实体存在时生效，实体清理后消失。
- 粒子、`charging_chakra`、`rasengan_start`、`rasengan_during`、爆炸/命中音效可听。
- 命中 living target 后造成伤害、击退、轨迹粒子，并清理实体。
- 接触可破坏方块时按硬度/概率/mobGriefing 规则破坏。
- `/narutoport senjutsu_ready` 后，潜行右键长按 `senjutsu` 可激活 Sage Mode，再普通右键释放 Sage Rasengan。
- Sage Mode 头部层在第三人称和其他客户端可见，并按 `SageType` 选择 toad/snake/slug 纹理。
- dedicated server 不加载任何 client 类。

## 下一个忍术复用模板

迁移下一个忍术时按这个顺序推进：

1. 写旧版审计小节：常数、输入、实体、渲染、粒子、音效、GUI/学习依赖。
2. 先接 registry 和 placeholder 替换，保证注册名稳定。
3. 迁物品输入与资源结算，先不碰视觉。
4. 迁实体世界行为，补同步数据和保存/读取。
5. 迁模型和 RenderType，把 GL 状态归类。
6. 接玩家 pose、第一人称、第三人称定位。
7. 接卷轴/GUI/学习或依赖状态。
8. 跑五项验证门。
9. 写进 `MIGRATION_PROGRESS.md`，列出剩余实机项。

## 当前未完成项

- M4 尚缺实际客户端截图/多人视角验证。
- `senjutsu` 目前用潜行右键作为 M4 临时输入分流；正式切术键位要在 M5/M7 通用框架里恢复。
- Rasengan 切片只覆盖一个忍术族，不能直接证明 M5/M6 全量内容已完成。
