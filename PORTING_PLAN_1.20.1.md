# 火影模组 (narutomod) 1.12.2 → Forge 1.20.1 完整移植计划 v2.1

> 目标：把 `D:\mcmodding\naruto\1.12.2` 的 narutomod 0.2.10-beta **完整、保留全部功能与视觉效果**地移植到 Minecraft 1.20.1 / Forge。前提：**放弃 MCreator**，手工移植已生成的 Java 源码。
>
> **v2 说明**：本版基于 (a) Forge 1.20.x 官方文档逐页核对、(b) 官方/社区迁移 primer（1.12→1.13、1.19.3→1.19.4、1.19.4→1.20）、(c) 对 1.12.2 源码渲染层的**逐文件行号级审计**（光束/几何特效/12 种粒子/玩家渲染/87 个模型类普查）重写。视觉效果章节（§5）为本版核心，给到"逐效果配方"级别。参考链接见 §13。
>
> **v2.1 增补（外部评审交叉核验后，2026-06-12）**：修正 UV 滚动 RenderType 缓存键（§5.1-D/§5.3）、声音键实测数 98/14（§1#7/§7）、GL 耦合双口径 100/71 + `audit/render_gl_files.csv` 清单（§2.2）、M1 拆 4 道门（§8）、M3 四样本硬验收（§8）、工期前提标注（§11）、datagen 分工（§7）、`getEntityData()` 297 处迁移清单（§6.5）、56 文件专服隔离验收（§4.3）。

---

## 0. TL;DR

- **规模**：451 个 Java 文件 / 11.5 万行（`wc -l` 实测 114,775，非空行 102,632）/ 849 个资源文件；**87 个旧式模型类**（实体 64 + 物品 23），其中 42 个超 500 行、12 个层级超 200 个 `addChild`（最大 EntityBuddha1000：4555 行、550 个 addChild）。
- **渲染是最大工程，且比 v1 评估更重**：实测 **100 个文件含直接 GL/渲染状态引用**（GlStateManager/GL11/OpenGlHelper，entity+item 占 92），其中 **71 个同文件内嵌旧式模型类**——状态调用直接混进模型与渲染代码（逐文件清单：`audit/render_gl_files.csv`）。1.20.1 下所有状态必须收敛到 `RenderType`，这意味着这些文件不是"翻译"而是"几何与状态分离重构"。
- **三件渲染基建必须先建好**（§5.1/5.6/5.8）：① 本 mod 专用 `RenderType` 库（加色发光/半透明双面全亮/滚动纹理/纯色几何 4 大族）；② `SphereMesh` 球体网格工具（替代已消失的 GLU `Sphere`+显示列表）；③ 自定义粒子管线（12 种粒子、通用 `ParticleOptions` 带 `int[]` 参数、自管纹理的 `ParticleRenderType`）。
- **好消息**（官方文档确认）：1.20.1 Forge 仍是 `SimpleChannel` 网络（~60 个包近 1:1 迁）；ItemStack 仍用 NBT；OBJ 模型有内置 `forge:obj` 加载器；玩家渲染层有正式事件 `EntityRenderersEvent.AddLayers`（不必再反射 `skinMap`）。
- **v1 遗漏、v2 补上的硬变化**：数据驱动 `DamageType`（1.19.4 起）、骑乘操控 API 重做（`tickRidden`/`getRiddenInput`）、方块掉落必须写 loot table、**声音事件键名必须小写化**（现有 `sounds.json` 里是 `Amaterasu` 这类大写键）、村庄 API 消失（围攻事件需用 POI 重写）、自然刷怪数据驱动化。
- **工期校准**：单人全职约 **4–6 个月**；渲染相关（模型转换 + 特效重写 + 粒子 + 玩家渲染）约占 45%。

---

## 1. v1 → v2 勘误与升级（检查结果）

对照官方文档逐条核对后，v1 有以下错误/遗漏，本版已修正：

| # | 类型 | 内容 |
|---|---|---|
| 1 | **勘误** | 容器屏幕注册：1.20.1 用 `MenuScreens.register(...)`，且必须在 `FMLClientSetupEvent` 的 `event.enqueueWork(...)` 中调用（非线程安全）。v1 写的 `RegisterMenuScreensEvent` 是 NeoForge 1.20.4+ 才有的事件，1.20.1 没有。 |
| 2 | **勘误** | `MobEffect` 每 tick 门控方法在 1.20.1 叫 `isDurationEffectTick(duration, amplifier)`；`shouldApplyEffectTickThisTick` 是 1.20.2+ 的更名。 |
| 3 | **勘误** | OBJ 模型（`wendigo.obj`）**不需要任何代码注册**（1.12 的 `OBJLoader.addDomain` 直接删掉），只需模型 JSON 顶层写 `"loader": "forge:obj"`（注意 `flip_v`）。 |
| 4 | **新增** | **数据驱动 DamageType**（1.19.4 变更）：`DamageSource` 已是 final 类，必须为忍术/仙术伤害建 `data/narutomod/damage_type/*.json` + `ResourceKey<DamageType>`，经 `Level#damageSources` 或 registry holder 构造。 |
| 5 | **新增** | **骑乘/操控重做**（1.19.4 变更）：`Mob` 系坐骑改用 `getControllingPassenger()`（返回 `LivingEntity`）+ `tickRidden(Player, Vec3)` + `getRiddenInput(Player, Vec3)` + `getRiddenSpeed(Player)`。影响尾兽（人柱力骑乘）、`EntityShieldBase.travel()` 自定义转向、蛤蟆/凯之龟等坐骑。 |
| 6 | **新增** | 方块掉落在 1.20.1 **必须**有 `loot_tables/blocks/<块名>.json`（1.12 是 `getItemDropped` 代码逻辑），9 个方块要补数据文件。 |
| 7 | **新增** | **资源路径/注册名强制小写**（1.13+）：`sounds.json` 实测 98 个键、其中 **14 个含大写**（`Amaterasu`、`ShinraTensei`、`80GodsPunch`、`KoH_spawn`…）需小写化 + 同步代码引用 + lang 字幕键；97 个 ogg 文件名与 sounds 内部路径实测已全小写，文件本身不用动。 |
| 8 | **新增** | 1.12 `Village`/`VillageCollection` API 在 1.14+ 已删除：`ProcedureCheckVillageSize`、村庄围攻事件需改用 POI（`PoiManager`/`PoiTypes.MEETING`）或结构判定重写。 |
| 9 | **新增** | 自然刷怪与生成数据驱动化：mob 生成用 `forge:add_spawns` BiomeModifier JSON + `SpawnPlacements`；`BlockMud.generateWorld`（泥潭湖）改 `Feature` + `BiomeModifier`。 |
| 10 | **新增** | `IFuelHandler` 已删除 → `IForgeItem#getBurnTime`；自定义游戏规则 `keepNinjaXp` 用 `GameRules.register`。 |
| 11 | **升级** | §5 视觉效果章节全部重写：基于对 20+ 个渲染重点文件与 12 种粒子的行号级审计，给出"1.12 实际做了什么 → 1.20.1 等价配方"的逐效果方案与需保留的精确常数。 |
| 12 | **升级** | 模型普查实测：87 个模型类（非 v1 估计的 89），仅 1 个用 `mirror`、0 个非标准 `addBox` 变体（机械转换无阻塞），但 **71 个文件在模型类内嵌 GL 状态调用、全工程 100 个文件含 GL 引用**（真正的重构点，清单 `audit/render_gl_files.csv`），42 个模型超 500 行。 |

---

## 2. 规模与现状（实测数据）

### 2.1 代码分布（451 文件 / ~115k 行）

| 包 | 文件数 | 内容 | 移植难度 |
|---|---|---|---|
| `entity` | 123 | 实体 + 内联模型(64) + 渲染器(~69) | ★★★★★ |
| `item` | 136 | 忍术框架/武器/瞳术/盔甲 + 内联模型(23) | ★★★★☆ |
| `procedure` | 97 | `ProcedureUtils`(1132 行工具库)、`ProcedureSync`(1142 行同步原语) + 每招逻辑 | ★★★★☆ |
| `gui` | 43 | 37 个卷轴 GUI + 2 个容器 GUI + 3 个 overlay | ★★★☆☆ |
| `event` | 10 | 可持久化特殊事件（圆柱/球形爆炸、延迟生成、村庄围攻、陨石雨、SetBlocks、延迟回调） | ★★★☆☆ |
| `block`/`potion`/`keybind`/`command`/`world`/`creativetab`/`util` | 9/9/5/3/1/1/1 | 方块(含 2 流体+1 TileEntity)、效果、键位、命令、Kamui 维度、创造标签、工具 | ★★★☆☆ |
| 根目录 | 13 | `ElementsNarutomodMod`(框架)、`Chakra`、`Particles`(~1300 行)、`PlayerRender`(477 行)、`PlayerTracker`、`EntityTracker`、`SaveData`、`NarutomodModVariables` | ★★★★★ |

### 2.2 模型普查（87 个旧式模型类，转换工作量的事实基础）

| 指标 | 数值 | 含义 |
|---|---|---|
| 模型类总数 | **87**（entity 64 + item 23）；workspace `models/` 另有 13 个孤儿文件可弃 | 全部 `ModelBase`/`ModelBiped` 系 |
| 纹理尺寸分布 | 64x64×40、32x32×14、16x16×12、128x128×5、256x256×3、64x16×5、128x64×1（BijuCloak）、512x512×1、32x1024×1 | 非 2 次幂极少，1.20.1 均可直接用 |
| 用 `addChild` 层级 | 53 个（61%）；最深：Buddha1000(550)、TenTails(308)、Snake8Heads(288)、ItemBoneArmor(240)、TwoTails(230) | 层级映射到 `PartDefinition` 树 |
| 有 `setRotationAngles` 动画 | 57 个；其中 35 个含 sin/cos 程序化动画（尾兽尾巴 9×7 段波动、八岐大蛇 8×8 颈段、翅膀、触须） | 动画逐个手工核对 |
| `setLivingAnimations` | 3 个（EntityClone、EntityMightGuy、EntitySnake） | → `prepareMobModel` |
| `mirror=true` | 仅 1 处；非标准 `addBox`/负尺寸 | **0 个** → 几何可脚本化机械转换 |
| **GL 状态调用分布** | 宽口径 **100** 文件含 GlStateManager/GL11/OpenGlHelper（entity+item 92）；其中 **71** 个同文件含模型类（逐文件清单+调用行数：`audit/render_gl_files.csv`，最重 EntityPretaShield 62 处） | ★核心重构点：状态必须剥离进 RenderType，按 CSV 逐文件销账 |
| >500 行巨型模型 | 42 个 | Buddha1000 4555 行为最 |

### 2.3 视觉效果审计结论（详见 §5 逐效果配方）

- 混合模式两族：**加色发光** `blendFunc(SRC_ALPHA, ONE)`（闪电、螺旋手里剑冲击、查克拉线、特效球线）与**标准半透明** `(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)`（穹顶、水龙、影子、须佐火焰）。
- **全亮**统一用 `OpenGlHelper.setLightmapTextureCoords(240,240)`（个别 0xF0）。
- **纹理矩阵滚动**（`matrixMode(5890)` + translate）出现在 4 处：光束(U+0.01/V+0.02 每帧)、人柱力披风(V=age×0.01)、须佐火焰层(V=age×0.01)、水龙水体层(V=age×0.01)。
- **GLU Sphere + 显示列表** 2 处（EntitySpecialEffect、Particles.ExpandingSphere），均为 32×32 细分、内外两面。
- 自绘几何：闪电（`TRIANGLE_STRIP` 三道宽度 pass）、旋转线（`TRIANGLE_FAN`×120 次随机旋转）、影子贴地面片、土块群（`BlockRendererDispatcher`）、封印锁链（每 2.5 单位一节、节间转 ~85°）。
- 12 种自定义粒子使用私有 ID 段 54678400–54678411，4 张自有纹理（`particles.png` 8×8 格、`seal_black_512.png`、`white_square.png`、`swirl_white_2.png`），其中 2 种（BurningAsh/AcidSpit）带**服务端**消息回路（点燃/腐蚀），1 种（MOB_APPEARANCE）直接渲染整个实体（鼬幻象）。

---

## 3. 工具链与工程骨架

| 项 | 1.12.2 现状 | 1.20.1 目标 | 依据 |
|---|---|---|---|
| Forge | 14.23.5.2855 | **1.20.1-47.2.0+**（建议最新 47.x） | — |
| Gradle 插件 | ForgeGradle 3 | **ForgeGradle 6** + Gradle 8.x | 官方 MDK |
| Java | 8 | **17** | 1.18+ 要求 |
| 映射 | MCP snapshot 20171003 | **official (Mojang) + Parchment 1.20.1**（参数名） | — |
| 元数据 | `mcmod.info` | **`META-INF/mods.toml`** | 文档 gettingstarted |
| AT | 无 | `src/main/resources/META-INF/accesstransformer.cfg` + `minecraft { accessTransformer = file(...) }`；条目用 **SRG 名**（字段 `f_xxxxx_`、方法 `m_xxxxx_`，用 Linkie/Parchment 查询） | 文档 accesstransformers |
| 资源包版本 | `pack_format: 3` | **15**（1.20.1 资源与数据包均为 15） | — |
| 结构 NBT | 无 DataVersion | DataVersion **3465**（1.20.1） | — |

做法：用官方 1.20.1 MDK 起新工程，旧 `src` 作只读参考逐文件搬运；**不要**在旧工程原地升级。包结构沿用 `net.narutomod.*` 以便对照。

**映射翻译辅助**：1.12 MCP 名（`world.isRemote`、`posX`、`motionX`…）→ Mojmap 名（`level.isClientSide`、`getX()`、`getDeltaMovement()`…）绝大多数是机械替换，建议整理一张本项目专用替换词典（§12 速查表为起点），用脚本做第一遍替换，再人工修语义差异。

---

## 4. 总体架构迁移决策（官方文档核对版）

### 4.1 注册：`ElementsNarutomodMod` 框架 → `DeferredRegister`
- 废弃 MCreator `ModElement`（ASM 注解扫描 + sortid 排序 + 反射实例化）。建集中注册类，全部挂到 mod 总线：
  - `ForgeRegistries` 系：`BLOCKS`、`ITEMS`、`ENTITY_TYPES`、`MOB_EFFECTS`、`SOUND_EVENTS`、`PARTICLE_TYPES`、`MENU_TYPES`、`BLOCK_ENTITY_TYPES`、`FLUIDS`、`FLUID_TYPES`(`ForgeRegistries.Keys.FLUID_TYPES`)、`ATTRIBUTES`。
  - 需要 vanilla `Registries` key 的：**`Registries.CREATIVE_MODE_TAB`**（1.20 起创造标签是注册表）、`Registries.CHUNK_GENERATOR`（Kamui 生成器 Codec）。
  - **数据包注册表**（`DamageType`、`dimension`、`dimension_type`）**不能用 DeferredRegister**——纯 JSON 文件 + 代码里只留 `ResourceKey`。
- 物品入创造标签：注册自己的 `CreativeModeTab` + 在 `BuildCreativeModeTabContentsEvent` 里 `accept(...)`（含 ~120 个 `ForgeSpawnEggItem` 刷怪蛋，对应旧 `EntityEntryBuilder.egg(primary, secondary)`）。
- 实体属性：`EntityAttributeCreationEvent` 注册 `AttributeSupplier`；自定义属性（旧 `ProcedureUtils.MAXHEALTH` 上限 1048576 的 RangedAttribute）注册到 `ForgeRegistries.ATTRIBUTES`，玩家附加用 `EntityAttributeModificationEvent`；触及距离改用 Forge 内置 `ForgeMod.ENTITY_REACH`/`BLOCK_REACH`。

### 4.2 网络：`SimpleNetworkWrapper` → `SimpleChannel`（文档确认，近 1:1）
```java
public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
    new ResourceLocation("narutomod", "main"), () -> "1", "1"::equals, "1"::equals);
// 注册（集中、顺序固定、id 自增）：
int id = 0;
CHANNEL.registerMessage(id++, ChakraMessage.class, ChakraMessage::encode, ChakraMessage::decode, ChakraMessage::handle);
// handle 内：ctx.get().enqueueWork(() -> ...); ctx.get().setPacketHandled(true);
// 发送：CHANNEL.send(PacketDistributor.NEAR.with(...), msg) / PLAYER.with(sp) / TRACKING_ENTITY.with(e) / ALL.noArg() / sendToServer(msg)
```
- 旧 62 处 `addNetworkMessage`（约 60 类消息：变量/查克拉/粒子/按键/GUI/实体控制/多部件/相机）逐一搬进单一 `NetworkHandler`，**注册顺序即协议**，写一个枚举清单防错位。
- 也可用 `messageBuilder(...).consumerMainThread(handler)`（1.20 把 `#consumer` 拆成 `consumerNetworkThread`/`consumerMainThread`），主线程消息更省样板。
- 安全提醒：旧消息大量直接信任客户端传来的 entityId（如扩展攻击距离），移植时在服务端 handler 里加距离/权属校验，行为不变但堵掉作弊面。

### 4.3 客户端隔离：`@SidedProxy` → 事件 + 隔离类
- 删 `IProxy` 三件套；客户端初始化分散到：`FMLClientSetupEvent`（`MenuScreens.register`、键位无关杂项）、`EntityRenderersEvent.RegisterRenderers`/`RegisterLayerDefinitions`/`AddLayers`、`RegisterParticleProvidersEvent`、`RegisterKeyMappingsEvent`、`RegisterGuiOverlaysEvent`。
- **专服安全**是 1.12 代码的高危区：`Minecraft.getMinecraft()` 实测散落在 **56 个文件**的公共类里（`Chakra`、`Particles`、各实体文件，靠 `world.isRemote` 分支保护）——迁移时逐文件登记去向，最终以 M1d 的"专服启动门"为硬验收。1.20.1 下统一改为"公共类只发包/读数据，渲染与 `Minecraft` 引用全部移进 `client` 子包"，杜绝专服 classloading 崩溃。`@SideOnly` → 不要照搬 `@OnlyIn`，用包隔离。

### 4.4 生命周期
- `@Mod("narutomod")` 构造器：注册所有 `DeferredRegister` → 总线；`FMLCommonSetupEvent.enqueueWork` 里做网络注册、`SpawnPlacements`、跨线程敏感初始化。
- 旧 `GameRegistry.registerWorldGenerator`/`registerFuelHandler`/`NetworkRegistry.registerGuiHandler` 全部删除，分别由 BiomeModifier(JSON)、`IForgeItem#getBurnTime`、`MenuType`+`NetworkHooks.openScreen` 接管。

---

## 5. ★ 渲染与视觉效果移植手册（v2 核心章）

### 5.0 体系变化总览：为什么 1.12 渲染代码一行都不能直接编译

| 维度 | 1.12.2（固定管线 + 全局状态机） | 1.20.1（core shader + 状态对象） |
|---|---|---|
| 矩阵 | `GlStateManager.pushMatrix/translate/rotate/scale` 操作全局 GL 矩阵 | `PoseStack`（参数传入，渲染器间不共享全局） |
| 状态 | `enableBlend/blendFunc/depthMask/disableLighting/disableCull/alphaFunc` 随处可调 | 状态被封装进 **`RenderType`**（shader+纹理+混合+剔除+写掩码+纹理变换的组合），按类型分桶绘制 |
| 顶点提交 | `Tessellator`/`BufferBuilder` 立即画，或 `ModelRenderer.render(scale)` | `MultiBufferSource.getBuffer(RenderType)` 拿 `VertexConsumer`，顶点带 `pose.last().pose()` 矩阵、light、overlay |
| 光照 | `disableLighting` + `OpenGlHelper.setLightmapTextureCoords(240,240)` 全亮 | 顶点 `packedLight`，全亮= `LightTexture.FULL_BRIGHT`(0xF000F0)，或选 emissive 类 RenderType |
| 透明度测试 | `alphaFunc(GL_GREATER, 0.003~0.1)` | alpha 测试由 shader 决定（cutout 系 RenderType），无运行时 alphaFunc |
| 纹理矩阵 | `matrixMode(5890)` + translate（UV 滚动） | `RenderSystem.setTextureMatrix(Matrix4f)`（原版附魔闪光 glint 就这么做）或顶点 UV 手动偏移 |
| GLU/显示列表 | `org.lwjgl.util.glu.Sphere` + `GLAllocation.generateDisplayLists` | **全部消失**（LWJGL3 无 GLU；core profile 无显示列表）→ 自建网格 |
| 实体渲染入口 | `doRender(entity,x,y,z,yaw,pt)`（自己 translate 到坐标） | `render(entity, yaw, pt, PoseStack, MultiBufferSource, packedLight)`（已位于实体坐标系） |
| 模型 | `ModelBase` 构造时 `new ModelRenderer().addBox()`，`render(f5)` 直接画 | 几何=静态 `LayerDefinition`，运行时 `ModelPart`；`renderToBuffer(pose, vc, light, overlay, r,g,b,a)` |
| GUI | `drawTexturedModalRect`/`fontRenderer.drawString` | **`GuiGraphics`**（1.20 新引入）：`blit/fill/fillGradient/drawString`，`Font#draw` 已删除 |

**总原则**：把审计出的每段 GL 序列归类到"4 大状态族"（§5.1），渲染代码翻译成"选 RenderType → 拿 VertexConsumer → 用 PoseStack 变换 → 喂顶点"。**禁止**在新代码里散写 `RenderSystem.blendFunc`（除 HUD 全屏特效外），否则会与原版批渲染互相污染。

### 5.1 第一项基建：`NarutoRenderTypes` 库（先写它，全 mod 复用）

审计归纳出本 mod 实际需要的状态组合只有 4 族 + 2 个特例：

| 族 | 1.12 原状态序列 | 使用者 | 1.20.1 实现 |
|---|---|---|---|
| **A. 加色发光（textured additive）** | enableBlend + blendFunc(SRC_ALPHA, **ONE**) + disableLighting + lightmap240 + depthMask(false) [+disableCull] | 螺旋手里剑(冲击/刃)、特效线、查克拉系 | 自定义：`NEW_ENTITY` 格式 + `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER` + 自定义 `TransparencyStateShard(SRC_ALPHA, ONE)` + `NO_CULL` + `COLOR_WRITE`(不写深度) |
| **B. 半透明全亮（textured translucent emissive）** | enableBlend + blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA) + disableLighting + lightmap240 [+disableCull] [+depthMask 控制] | 穹顶、八卦阵、锁链、水龙、须佐火焰、尾兽玉、Magatama | 原版现成 **`RenderType.entityTranslucentEmissive(tex)`**（双面版本需自定义加 `NO_CULL`） |
| **C. 无纹理纯色几何（position-color additive）** | disableTexture2D + blendFunc(SRC_ALPHA, ONE) + shadeModel(SMOOTH) + depthMask(false) | 闪电（TRIANGLE_STRIP）、旋转线特效（TRIANGLE_FAN×120） | 原版现成 **`RenderType.lightning()`**（就是为原版闪电设计的：POSITION_COLOR、加色、写色不写深度）——几乎零成本对位 |
| **D. 滚动纹理（UV scroll）** | matrixMode(5890) + translate(u,v) | 光束(0.01,0.02/帧)、披风/须佐火焰/水龙(V=age×0.01) | 自定义 `TexturingStateShard`：setup 里按**全局时间**现算偏移再 `RenderSystem.setTextureMatrix(...)`，clear 里 `resetTextureMatrix()`——与原版 glint 同机制。⚠ `Util.memoize` 的 key **只能是 (texture, uSpeed, vSpeed) 速度常量**（本 mod 仅 4 种组合，缓存有界），**严禁把每帧变化的总偏移量当 key**（会无限增殖 RenderType 实例）。匀速循环滚动的相位全局共享，与 1.12 各自计数器视觉等价（相位不可感知，速度才可感知）；手写顶点的几何另有零状态替法：偏移直接加进顶点 UV |
| 特例 E. 颜色反相 | enableColorLogic + colorLogicOp(INVERT) 全屏矩形 | 白眼视觉 overlay | HUD 层直调 `RenderSystem.enableColorLogicOp()` + `logicOp(INVERT)` + `GuiGraphics.fill`，画完恢复（原版文本高亮同款用法，安全） |
| 特例 F. 方块模型 | `BlockRendererDispatcher.renderModel` | 土块群、地爆天星吸附块 | `Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY)` |

代码骨架（一次写好，全 mod 引用）：
```java
public final class NarutoRenderTypes extends RenderType {
    private NarutoRenderTypes(...) { super(...); } // 仅为继承受保护 shard 常量

    private static final TransparencyStateShard ADDITIVE_TRANSPARENCY = new TransparencyStateShard(
        "naruto_additive",
        () -> { RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); },
        () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); });

    /** A 族：加色发光，双面，不写深度，全亮由顶点 light 传 0xF000F0 */
    public static final Function<ResourceLocation, RenderType> ENERGY_ADDITIVE = Util.memoize(tex ->
        create("naruto_energy_additive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setOverlayState(OVERLAY)
                .createCompositeState(false)));

    /** B 族双面版 */
    public static final Function<ResourceLocation, RenderType> TRANSLUCENT_EMISSIVE_2SIDE = Util.memoize(tex -> ...同上，
        TRANSLUCENT_TRANSPARENCY 替换 ADDITIVE...);

    /** D 族：滚动纹理。memoize key 只含 (纹理, 速度)——有界；偏移在 shard 内按全局时间现算 */
    private record ScrollKey(ResourceLocation tex, float uPerTick, float vPerTick) {}
    private static final Function<ScrollKey, RenderType> SCROLLING = Util.memoize(k ->
        create("naruto_scroll", ..., CompositeState.builder()
            .setTexturingState(new TexturingStateShard("naruto_scroll",
                () -> { float t = (Util.getMillis() % 3_600_000L) / 50f; // 先取模再转 float，避免精度灾难
                        RenderSystem.setTextureMatrix(new Matrix4f().translation(
                            (t * k.uPerTick()) % 1f, (t * k.vPerTick()) % 1f, 0)); },
                RenderSystem::resetTextureMatrix))
            ...));
    public static RenderType scrollingEmissive(ResourceLocation tex, float uPerTick, float vPerTick) {
        return SCROLLING.apply(new ScrollKey(tex, uPerTick, vPerTick));
    }
    // 同一 RenderType 的整批几何共享同一纹理矩阵（绘制期才 setup）。若哪天需要"各实体不同相位"，
    // 不要走矩阵（那得 endBatch 局部刷新，破坏批渲染）——把偏移写进顶点 UV。本 mod 4 处滚动均为匀速循环，全局相位即可。
}
```
> 实现提示：`RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER`、`NO_CULL`、`COLOR_WRITE`、`TRANSLUCENT_TRANSPARENCY` 都是 `RenderType`/`RenderStateShard` 里现成的受保护常量；C 族直接用 `RenderType.lightning()` 不必自建。深度写入策略沿用审计结论：发光体一律不写深度（`COLOR_WRITE`），实体本体写深度。

### 5.2 GL 调用 → 1.20.1 翻译对照表（渲染代码逐行替换用）

| 1.12.2 | 1.20.1 |
|---|---|
| `GlStateManager.pushMatrix()/popMatrix()` | `poseStack.pushPose()/popPose()` |
| `translate(x,y,z)` | `poseStack.translate(x,y,z)` |
| `rotate(deg, 0,1,0)` / `(1,0,0)` / `(0,0,1)` | `poseStack.mulPose(Axis.YP.rotationDegrees(deg))` / `XP` / `ZP`；任意轴 `new Quaternionf().rotationAxis(rad, x,y,z)`（JOML，注意 1.19.3+ 数学库从自带换成 JOML） |
| `scale(s,s,s)` | `poseStack.scale(s,s,s)` |
| `color(r,g,b,a)` | 顶点 `.color(r,g,b,a)`；模型整体 → `renderToBuffer(..., r,g,b,a)` 实参 |
| `enableBlend+blendFunc+depthMask+lighting+cull` 组合 | 选 §5.1 的 RenderType（禁止散调） |
| `OpenGlHelper.setLightmapTextureCoords(240,240)` | 顶点 light 传 `LightTexture.FULL_BRIGHT`，或用 emissive RenderType |
| `Tessellator.getInstance(); buffer.begin(GL_QUADS, POSITION_TEX_COLOR)` | `VertexConsumer vc = bufferSource.getBuffer(type);` 顶点：`vc.vertex(pose.pose(), x,y,z).color(...).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,1,0).endVertex();`（NEW_ENTITY 格式要素齐全；POSITION_COLOR 格式只需 vertex+color） |
| `GL_TRIANGLE_STRIP`/`GL_TRIANGLE_FAN` | `VertexFormat.Mode.TRIANGLE_STRIP`/`TRIANGLE_FAN`（自定义 RenderType 可声明；`lightning()` 是 QUADS——闪电改用四边形条带，见 §5.5） |
| `bindTexture(res)` | RenderType 的 `TextureStateShard` 携带；HUD 上 `RenderSystem.setShaderTexture(0, res)` |
| `matrixMode(5890)+translate` | `RenderSystem.setTextureMatrix` / `TexturingStateShard`（§5.1 D 族） |
| `GLAllocation.generateDisplayLists`+`callList` | 删除：网格每帧重建（≤数千顶点无压力）或缓存到 `float[]` 顶点数组循环喂入 |
| `shadeModel(7425/7424)` | 无对应（core shader 默认平滑插值），删除 |
| `alphaFunc(0x204, 0.01f)` | 无运行时 alphaFunc：要"近乎不裁剪"用 translucent 系，要裁剪用 cutout 系 RenderType |
| `RenderHelper.enable/disableStandardItemLighting` | 删除（光照走顶点 light/overlay） |
| `entity.getBrightnessForRender()` | `LevelRenderer.getLightColor(level, pos)` 或渲染器入参 `packedLight` |
| `Render<T>#doRender/bindEntityTexture/getEntityTexture` | `EntityRenderer<T>#render(...)`/`getTextureLocation(entity)` |
| `RenderingRegistry.registerEntityRenderingHandler` | `EntityRenderersEvent.RegisterRenderers#registerEntityRenderer` |
| `shouldRenderInPass/isMultipass` | 删除（半透明排序交给 RenderType 桶 + `MultiBufferSource`） |
| `renderItem(stack, TransformType.HEAD)` | `Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.HEAD, light, overlay, poseStack, bufferSource, level, seed)`（1.20 `TransformType`→`ItemDisplayContext`） |

### 5.3 配方 A：光束族（`EntityBeamBase` 及其全部子类）

**1.12 实况**（审计 EntityBeamBase.java:180-206）：模型=2 根细长盒（高度=16×束长）；渲染序列=平移→yaw→`90°-pitch`→**纹理矩阵滚动（每帧 U+0.01、V+0.02，静态递减计数器）**→enableAlpha/Blend→disableLighting→lightmap(240,240)→画模型→复位纹理矩阵→`depthMask(false)`（注意：它在收尾时关深度写，依赖下一个渲染器再开，是个老代码坏习惯，移植时不要照抄）。纹理 `textures/beam.png`。

**1.20.1 配方**：
- 模型转 `LayerDefinition`（束长是动态的：保留旧思路——按 `BEAM_LENGTH` DataParameter 在 `setupAnim`/渲染时 `poseStack.scale(1, length, 1)` 拉伸单位长度盒，或分段重复；推荐前者，UV 拉伸观感与原版一致，因为原模型本来就是按长度重建盒子）。
- RenderType 用 §5.1 **D 族** `scrollingEmissive(BEAM_TEX, 0.01f, 0.02f)`——参数是**速度常量**（memoize 安全），偏移由 shard 内全局时间现算；旧"静态递减计数器"换成全局时间相位，行为一致且所有光束实例共享同一个 RenderType（批渲染友好）。
- 全亮：顶点 light 传 `0xF000F0`。旋转：`mulPose(Axis.YP.rotationDegrees(yaw)); mulPose(Axis.XP.rotationDegrees(90 - pitch))`。
- 子类（雷遁光束、水束等）只换纹理与颜色 → RenderType 参数化后子类零渲染代码。

### 5.4 配方 B：发光叠层族（螺旋丸 / 螺旋手里剑 / 求道玉 / 勾玉）

**螺旋丸**（EntityRasengan.java:318-372, 405-493）：核心=3 层嵌套盒(1³/2³/3³)×4 组旋转骨 + 外壳 4 个 4³ 盒；每帧 `rotate(ticksExisted*30°, 轴(1,1,0))`，然后**同一模型随机旋转重画 10 遍**，颜色=白 0.3α(核)/青(0.66,0.87,1.0) 0.3α(壳)，标准半透明混合+全亮+双面。第一人称时贴在自己手臂上（详见 §5.12）。
→ **配方**：B 族双面 RenderType；10 遍重画原样保留（每遍 `pushPose+mulPose(随机)+renderToBuffer+popPose`，注意随机数用 `entity.getId()+tick` 做种子保证两帧间稳定，否则会闪烁——1.12 用全局 `rand` 实际上每帧都在闪，这是它"能量感"的来源，**保真选择：保留每帧随机**）。任意轴旋转用 `new Quaternionf().rotationAxis((ticks+pt)*30*Mth.DEG_TO_RAD, axisNormalized)`。

**螺旋手里剑**（EntityRasenshuriken.java:586-613）：球=25+个旋转 6³ 盒（rotateAngleY=-age×0.8、X=age×0.6）；刃=8 片**零厚度面片**（Y=age×0.6、X=sin(age×0.2)×0.1 摆动、scale 1.5+sin×2 脉动）；混合模式**动态切换**：α>0.8 用标准半透明、否则加色；命中后（impactTicks>0）全部加色+白 0.2α。
→ **配方**：两个 RenderType（A 族 + B 族）按状态选择；零厚度面片必须双面（NO_CULL 已含）。模型内的 GlStateManager 调用全部上移到渲染器（这是 §2.2 GL 内嵌清单（71 文件）的典型样例）。

**求道玉**（EntityTruthSeekerBall.java:418-433）：黑色 4³ 盒，自旋 `(age+pt)*60°` 轴(1,1,0)（持盾状态停转），disableLighting 但无混合。→ 普通 `entityCutoutNoCull` + 顶点全亮即可。

**须佐勾玉**（EntitySusanooClothed.java:459-532）：5 遍叠画（1 遍全α + 3 遍 0.5α + 1 遍黄色(1,1,0) 0.5α 高光），B 族；位移旋转按速度/age。原样直翻。

### 5.5 配方 C：闪电（`EntityLightningArc`，被千鸟/麒麟/各雷遁广泛复用）

**1.12 实况**（264-349）：递归分形（默认 4 层，中点高斯抖动 `±lengthVector×0.1`，20% 概率分叉、叉细 0.6×）；每段画 3 道宽度（内白 α0xF0 → 色 α0x80 → 色 α0x20），`GL_TRIANGLE_STRIP` + `POSITION_COLOR` + **加色混合** + depthMask(false)，主干 lightmap 240/分支 160。
**1.20.1 配方**：
- RenderType 直接用 **`RenderType.lightning()`**（原版闪电专用：POSITION_COLOR、加色、写色不写深度——状态与 1.12 序列逐项吻合）。
- `lightning()` 是 QUADS 模式：把原 TRIANGLE_STRIP 改为逐段发 4 顶点四边形条（原代码本来就是"沿段拉宽度条"，QUADS 重写是 1:1 几何换皮）。三道宽度 pass、内白外色、α 梯度、分形参数（0.1 抖动、20% 分叉、0.6 细化、4 层）**全部原值保留**。
- 递归结构保留为纯几何函数 `emitArc(VertexConsumer, PoseStack.Pose, from, to, depth, seed)`；种子取实体 id+段索引，避免每帧形态跳变除非有意（原版每帧重抖——保真则保留每帧 `rand`）。
- `spawnAsParticle` 路径（CPacketSpawnLightning）保留：包到客户端后往粒子引擎丢一个短寿命"闪电粒子"，其 `render` 用 `ParticleRenderType.CUSTOM` 调用同一个 `emitArc`（§5.8）。

### 5.6 配方 D：球体/穹顶族 + `SphereMesh` 基建（替代 GLU）

**第二项基建**：写一个 `SphereMesh` 工具类，一次解决 EntitySpecialEffect、Particles.ExpandingSphere、以及任何后续球形术：
```java
public final class SphereMesh {
    /** 经纬球（slices×stacks=32×32 与 GLU 相同），QUADS 输出；inside=true 时反绕序+法线取反（替代 GLU_INSIDE） */
    public static void render(VertexConsumer vc, PoseStack.Pose pose, float radius,
                              int slices, int stacks, float r, float g, float b, float a,
                              int packedLight, boolean inside) {
        for (int i = 0; i < stacks; i++) {
            float phi0 = (float)Math.PI * i / stacks, phi1 = (float)Math.PI * (i+1) / stacks;
            for (int j = 0; j < slices; j++) {
                float th0 = (float)(2*Math.PI) * j / slices, th1 = (float)(2*Math.PI) * (j+1) / slices;
                // 4 个顶点 p(phi,theta)=(r sinφ cosθ, r cosφ, r sinφ sinθ)，UV=(θ/2π, φ/π)
                // inside 时按 p00,p01,p11,p10 顺序反向输出
                ...
            }
        }
    }
}
```
> 32×32 球 ≈ 1024 quads ≈ 4096 顶点/次——每帧直接重建毫无压力，无需缓存（显示列表的替代品就是"不缓存"）。若以后要更高细分再考虑 `VertexBuffer` 缓存。

**EntitySpecialEffect 两模式**（215-354）：
- `EXPANDING_SPHERES_FADE_TO_BLACK`：同心壳层循环（scale=(age-i)×0.7、灰度=1-0.05i、α=0.101 固定、标准半透明、全亮、depthMask off、**内外两面都画**）→ B 族 RenderType（白色方块纹理可以省去——改用 `lightning()`? 不行，它是标准半透明非加色；用 B 族 + `white_square.png` 或顶点纯色 + `entityTranslucentEmissive(白图)`，保留原值即可）。
- `ROTATING_LINES_COLOR_END`：120 次随机旋转 TRIANGLE_FAN（中心白→端点色、加色、smooth shading、depthMask off、enableCull）→ `RenderType.lightning()` + 把 fan 改 quads（每扇 3 三角→3 四边形退化或直接 4 顶点两两重复），随机轴旋转在 PoseStack 上做。

**地爆天星**（EntityChibakuTenseiBall）：核心球=单盒模型 scale 0.2→80 增长 + `rotate(age*10°, 轴(1,1,0))` + 40% 进度切换纹理（空白→求道玉纹理）；吸附的方块实体用特例 F（`renderSingleBlock`）。
**冰穹顶**（EntityIceDome）：16 片零厚度面片组成的穹顶模型、整体 scale 8、α 随 ticksExisted 渐入、B 族双面。
**八卦阵**（EntityEightTrigrams）：16×16 零厚度面片 + 180° 翻转 + B 族全亮，按 age 自转。
**回天**(Kaiten)/**水龙**：见 §5.7 水龙双 pass。

### 5.7 配方 E：影子模仿 / 土块群 / 封印锁链 / 水龙

- **影子模仿术**（EntityShadowImitation.java:291-375）：沿"施术者→目标"射线在地形方块顶面+侧面贴 `black.png` 四边形，各面外扩 0.01 防 Z-fighting，α=0.5，标准半透明、depthMask(false)。→ B 族（或 `entityTranslucent`）原样直翻；注意 1.20.1 里拿方块 AABB 用 `state.getShape(level, pos).bounds()`。
- **土块群**（EntityEarthBlocks.java:810-867）：`Map<Vec3d, IBlockState>` 逐块 `BlockRendererDispatcher.renderModel`，"被完全包围的块不画"剔除。→ `renderSingleBlock(state, poseStack, bufferSource, packedLight, NO_OVERLAY)` 循环；剔除逻辑照搬；光照用实体所在处 `packedLight`（与 1.12 的 disableLighting 近似）。
- **封印锁链**（EntitySealingChains.java:211-329）：链=程序生成 ModelRenderer 数组（`ceil(len×6.4)-1` 节、每节 2 盒、节距 2.5、相邻节绕 Y 转 ~85°（`i×π×0.472`）、链头 4 盒菱形），金色纹理、B 族全亮、指向目标 pitch+90°。→ 模型改"运行时按长度生成 `PartDefinition` 不现实"——改为**单节 `ModelPart` 循环渲染 N 次**（每次 `pushPose+translate(0, i*2.5/16, 0)+mulPose(Y, i*85°)`），观感一致且更省。
- **水龙**（EntityWaterDragon.java:241-306, 555-578）：**双 pass**：pass1 绑 `gas256.png`、纹理矩阵 V=age×0.01 滚动、整体水色 (0.04,0.325,0.733,1.0)、隐藏牙齿/眼睛部件；pass2 绑 `dragon_blue.png`、α0.8、显示全部件。动画：100 节脊柱从 `partRot` 列表读 yaw/pitch 级联、胡须 sin 摆动、张口 30°。→ 两个 RenderType（D 族滚动 + B 族），`renderToBuffer` 调两次，部件可见性用 `ModelPart.visible`；脊柱级联在 `setupAnim` 里照搬（数据已在实体侧同步，模型只读）。

### 5.8 粒子系统移植（12 种，含完整参数语义表）

**1.20.1 注册管线**（官方文档确认）：
1. `DeferredRegister<ParticleType<?>>`；带参粒子自实现 `ParticleOptions`（`writeToNetwork`/`writeToString` + `ParticleOptions.Deserializer`）——**设计一个通用 `NarutoParticleOptions`（携带 `int[] args`）+ 每种一个 `ParticleType<NarutoParticleOptions>`**，旧 `Message` 的 int 数组语义零改动。
2. 客户端 `RegisterParticleProvidersEvent`：精灵图集粒子用 `registerSpriteSet`，**自管纹理/自绘的用 `registerSpecial`**。
3. 服务器触发：旧 `Particles.spawnParticle` 服务端分支改 `ServerLevel.sendParticles(options, x,y,z, count, dx,dy,dz, speed)`（原版包替代自定义 Message——**旧 Message 可以整个删掉**，count/offset/speed 语义原版包都有；个别带 `renderDistance=64+` 的远距粒子保留自定义包或用 `ServerLevel#sendParticles(ServerPlayer,...force)` 强制版）。
4. `ignoreRange=true` 对应 `addAlwaysVisibleParticle`/`force` 标志。

**纹理策略**：`particles.png`(8×8 格)、`seal_black_512.png`、`white_square.png`、`swirl_white_2.png` 这 4 张**不进原版图集**，用**自定义 `ParticleRenderType`**（`begin` 里 `setShaderTexture(0, ...)`+`depthMask(false)`+混合设置，`end` 里 `tesselator.end()`+恢复）——保真且省去切图；MOB_APPEARANCE 用 `ParticleRenderType.CUSTOM` 自己整段渲染。

**逐粒子移植表**（参数语义为审计实测，必须保留）：

| 粒子 | args 语义 | 1.12 渲染要点 | 1.20.1 做法 |
|---|---|---|---|
| SMOKE | [ARGB 色, 缩放×10, 寿命, 亮度, **viewerId**, 漂浮速度×1000] | particles.png 8 帧倒放动画；尺寸 32 倍渐入；α=(1-f²·0.5)；**viewerId==相机实体且第一人称时隐藏**；地面摩擦 0.7、阻力 0.96 | 自定义 RenderType(自管纹理)；viewer 判定改 `Minecraft.getInstance().cameraEntity` |
| SUSPENDED | [ARGB, 缩放×10, 寿命] | 原版贴图静浮、阻力 0.8 | `registerSpriteSet` + `TextureSheetParticle`（用原版 generic 精灵） |
| FALLING_DUST | [ARGB] | `ParticleSimpleAnimated`(176,4,-0.025) | 1.20.1 无该类：`TextureSheetParticle`+`SpriteSet` 动画帧重写（重力 -0.025、寿命 60±12 保留） |
| FLAME | [ARGB, 缩放×10] | particles.png Y=1 行 8 帧(每 2tick 换帧)；α=1-(f-0.5)²×3.5 抛物线；全亮 | 同 SMOKE 自管纹理 |
| MOB_APPEARANCE | [实体类型 id] | **渲染整个实体**（默认鼬，幻象）：scale 1+f×1.5、α=0.05+0.5sin(fπ)、面向相机 | `ParticleRenderType.CUSTOM`：`EntityRenderDispatcher#render` + 临时实体实例缓存；α 经 `renderToBuffer` 实参传入需该实体用 translucent RenderType——简单可靠的替法：保留为"短寿命客户端实体"而非粒子 |
| BURNING_ASH | [排除实体 id] | 继承 SMOKE(深灰 0xFF606060、scale 5±5)；**碰撞活物→发服务端包 `setFire(sec)`** | 粒子继承同前；**服务端 Message 保留**（`SimpleChannel` C2S，校验距离） |
| HOMING_ORB | [半径, 缩放×10] | 原版贴图(49/97 号)；从球面随机点直线归心、全亮 | `registerSpriteSet`；归心运动照搬 |
| EXPANDING_SPHERE | [尺寸×10, 寿命, ARGB] | **GLU 球壳**层层外扩（α0.05、RGB 按层渐暗） | `ParticleRenderType.CUSTOM` + `SphereMesh`（§5.6） |
| PORTAL_SPIRAL | [半径, ARGB, 缩放×10] | 原版贴图；按初始 yaw/pitch 螺旋向心（rotatePitch/rotateYaw 数学） | `registerSpriteSet`；运动数学照搬（`Vec3#xRot/yRot` 对应 rotatePitch/rotateYaw） |
| SEAL_FORMULA | [尺寸×10, Y 旋转×10, 寿命] | 512² 封印贴图平铺四边形，UV 从中心向外"展开"（0.5(1±f8)），20tick 展开期，固定 Y 旋转，不动 | 自定义 RenderType(自管纹理)；UV 数学照搬 |
| ACID_SPIT | [排除 id, ARGB(默认 0x80ffd6ba)] | 继承 SMOKE；**黏附**到碰到的实体上下沉；每帧发服务端包上腐蚀药水；附带方块破坏进度显示 | 同 BURNING_ASH：粒子+保留服务端 Message（`MobEffectInstance(CORROSION)`）；`level.destroyBlockProgress` 仍在 |
| WHIRLPOOL | [ARGB, 缩放×10, 寿命, 亮度] | swirl 贴图、朝向取运动向量(pitch/yaw)、Z 自旋 -30°×age、α 抛物线、depthMask off | 自定义 RenderType(自管纹理)；自定义朝向=覆写 `Particle#render` 自构四顶点（不用 `SingleQuadParticle` 的强制面向相机） |

**EntityParticle.java**（实体型粒子基建）：审计确认 12 种粒子都没用它——**移植时直接删除**，省一个实体注册位。

### 5.9 模型转换流水线（87 个 `ModelBase`/`ModelBiped` → `LayerDefinition`）

**普查结论决定策略**：0 个非标准 addBox、仅 1 处 mirror → **几何部分可以写脚本全自动转换**；真正要人工的是 (a) 71 个文件里混进模型类的 GL 调用上移（清单 `audit/render_gl_files.csv`），(b) 57 个 `setRotationAngles` 动画核对。

**转换器设计**（建议 Python/正则 + 少量手修，1.12 MCreator/Blockbench 导出格式高度规整）：
```
输入模式：
  this.X = new ModelRenderer(this, U, V);
  this.X.setRotationPoint(px, py, pz);
  this.X.addBox(ox, oy, oz, w, h, d, delta);
  setRotateAngle(X, rx, ry, rz);   this.P.addChild(this.X);
输出：
  PartDefinition X = P.addOrReplaceChild("X",
      CubeListBuilder.create().texOffs(U, V).addBox(ox, oy, oz, w, h, d, new CubeDeformation(delta)),
      PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
+ 模型类骨架：字段 ModelPart、构造器 root.getChild(...) 逐级取、静态 createBodyLayer() 返回
  LayerDefinition.create(mesh, texW, texH)、注册 ModelLayerLocation。
```
要点与陷阱：
1. **同名子部件**：旧代码字段名可作 part 名，但同一父下重名要加后缀（Buddha1000 这类 550-child 模型必查）。
2. **`render(f5)` 内直接画+变换的模型**（部分模型在 render 里 push/translate/rotate 后分别 render 子部件）：这些"手动层级"要转成真正的父子 `PartPose`，或保留为渲染器里的多次 `renderToBuffer`。
3. **动画**：`setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity)` → `setupAnim(T entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)`；字段名 `rotateAngleX→xRot`、`rotationPointX→x`；`MathHelper→Mth`。35 个三角函数动画模型逐个目检（尾兽 9×7 尾段波、八岐 8×8 颈段、七尾翅膀为最难三件）。
4. `setLivingAnimations` → `prepareMobModel`（3 个模型）。
5. **`ModelBiped` 系** → `HumanoidModel<T>`（字段 `bipedHead→head` 等自动映射表）；玩家形 64×64 含外层（`bipedBodyWear→jacket` 等）。`ArmPose` 枚举仍在。
6. 备选路径：Blockbench 可导入 1.12 Java 模型再导出 1.17+ Mojmap 格式——对个别脚本啃不动的巨型模型做兜底，但 87 个全走 Blockbench 不现实（动画不跟随）。
7. **验收**：每个模型转换后在"模型展厅"测试世界（一排刷怪蛋）与 1.12 截图对照（§5.14）。

**优先级分批**（按普查复杂度）：
- T1 验证批（先打通管线）：ModelHelmetSnug、ModelAnbuMask、ModelGourd、EntityTruthSeekerBall、EntityEightTrigrams 等 ≤100 行模型。
- T2 人形批：ModelClone（64×64+slim 变体）、ModelNinjaArmor、ModelAkatsukiRobe、忍者 NPC 系。
- T3 动画巨兽批：9 个尾兽 + TenTails + Snake8Heads + SusanooWinged + Buddha1000（各预留 0.5–1.5 天/个，含渲染状态剥离）。
- T4 装备特效批：ItemBoneArmor(1868 行)、ItemSteamArmor、ItemBijuCloak、ItemEightGates。

### 5.10 玩家渲染重构（`PlayerRender` 477 行 → 事件化）

1.12 用反射替换 `RenderManager.skinMap`；**1.20.1 有正式入口，全部不再反射**：

| 1.12 功能（审计行号） | 1.20.1 实现 |
|---|---|
| 反射安装自定义 `RenderPlayer`（84-92） | **删除**。分发到下面各项 |
| 身上挂物层 `LayerInventoryItem`（296-373）：扫主背包 `ItemOnBody.Interface`，按 BodyPart `postRender(0.0625)` 后 translate `(off.x, -0.25+off.y, off.z)`、rotY 180°、scale `(0.625,-0.625,-0.625)`、`TransformType.HEAD` 画物品；腿部偏移 `(±0.125,-0.6875,0)` | `EntityRenderersEvent.AddLayers`：`for (String skin : event.getSkins()) { PlayerRenderer r = event.getSkin(skin); r.addLayer(new InventoryItemLayer(r)); }`。部件跟随= `model.rightLeg.translateAndRotate(poseStack)`（即旧 postRender）；物品= `ItemRenderer.renderStatic(..., ItemDisplayContext.HEAD, ...)`；**所有常数原值保留** |
| 皮肤克隆（变身术）：`getEntityTexture` 返回目标皮肤 + 名牌换名 + 潜行高度补偿 ±0.2（213-228, 255-270） | `RenderPlayerEvent.Pre` 取消原渲染→用自备的"克隆渲染器"按目标皮肤画；或更简：克隆/变身走 **EntityClone 同款独立渲染器**。皮肤获取：`((AbstractClientPlayer)target).getSkinTextureLocation()`、瘦臂判定 `getModelName().equals("slim")` |
| 半透明（隐身术）：`enableBlendProfile(TRANSPARENT_MODEL)`（193-199） | `RenderPlayerEvent.Pre` 里改不了模型 alpha——做法：取消事件+自渲染 `RenderType.entityTranslucent(skin)` + `renderToBuffer(..., 1,1,1,0.15f)`；或给玩家加 `Invisibility` + 自定义幽灵层。推荐前者（保真） |
| 颜色乘子（死亡变黑等）：NBT `SkinColorMultiplier`（273-279） | 同上自渲染路径把 ARGB 拆成 `renderToBuffer` 的 r,g,b,a 实参 |
| 鸣人跑姿势：`shouldNarutoRun` 时 `isSneak=true`（247-252） | `RenderPlayerEvent.Pre` 里 `event.getRenderer().getModel().crouching = true`（渲染期临时改，Post 恢复） |
| 强制拉弓姿势 / 死亡动画配色（`ProcedureDeathAnimations` type2 给 0x30000000 乘子） | 同乘子路径；死亡尘化粒子公式 `(200-t)×h/2` 原样 |

> 结论：**整渲染器替换降级为最后手段**。若克隆+透明+乘子三件用事件取消式自渲染嫌散，可保留一个 `NarutoPlayerRenderer extends PlayerRenderer`，仅在需要时经 `RenderPlayerEvent.Pre`(cancel) 委托给它——既不反射也不 Mixin。
> `ItemOnBody.InventoryTracker` 的背包同步包原样保留（其他玩家的身上挂物依赖它）。

### 5.11 盔甲 / 瞳术 / 人柱力披风渲染

- **挂接点**：`IForgeItem` 已无 `getArmorModel` → 物品里 `initializeClient(consumer)` + `IClientItemExtensions#getHumanoidArmorModel(living, stack, slot, original)` 返回 `HumanoidModel`；贴图仍走 `IForgeItem#getArmorTexture`。普通盔甲（NinjaArmor、AkatsukiRobe、AnbuMask、Gourd、SizPathRobe…）走这条即可，注意 `copyPropertiesTo` 保持基础姿态同步。
- **发光部件问题**：vanilla 盔甲层只用一个 `armorCutoutNoCull` RenderType，**做不了局部全亮/滚动纹理**。审计确认 3 件需要：
  - `ModelHelmetSnug`（瞳术，64×16）：headwear 全亮 + 面部高光片（z=-4.15 零厚度quad）全亮 + 额痕第二 pass；alphaFunc 0.01。
  - `ModelBijuCloak`（128×64）：纹理矩阵 V 滚动(age×0.01)、α=wearingTicks/80 渐入、bodyShine/layerShine 全亮、9 条尾×8 段 sway 动画（随机相位数组）、tailShowMap 位掩码控制显示几条尾。
  - 须佐火焰罩层（gas256.png 滚动 + α=age/60 渐入，0.99 缩放套壳）。
  → **方案**：这 3 件放弃 vanilla 盔甲管线，做成 **AddLayers 自定义 `RenderLayer`**（自由选 §5.1 的 B/D 族 RenderType、自由多 pass）；物品的 `getHumanoidArmorModel` 返回空模型或不实现。这是保真且最省心的路线。
- 非标纹理尺寸（128×64、64×16、16×16）在 `LayerDefinition.create(mesh, w, h)` 声明即可，无限制。

### 5.12 第一人称与手部绑定（螺旋丸/千鸟/查克拉刀）

1.12 实况：Rasengan 在 `viewer==owner && thirdPersonView==0` 时改为"画手臂+把球贴到手上"（`rotateArmIn1stPerson`，translate(0,1.925,0) 后按视角旋转）；Chidori 给手臂改 pose + 沿手臂生成闪电粒子；SMOKE 粒子按 viewerId 在第一人称隐藏。
1.20.1 配方：
- 实体渲染器里判定：`Minecraft mc = Minecraft.getInstance(); boolean fp = mc.options.getCameraType().isFirstPerson() && entity.getOwner() == mc.getCameraEntity();`
- 第一人称分支不再自己画手臂——**挂 `RenderHandEvent`**：取消原手渲染或在其后用 `PlayerRenderer#renderRightHand(poseStack, buffer, light, player)` 画臂，再在掌心位置渲染球体模型（旧 translate/rotate 常数照搬）。
- 第三人称贴手：渲染时把坐标换到 owner 模型臂骨——用 `AddLayers` 给玩家加"手持忍术层"更稳（层内天然有 `model.rightArm.translateAndRotate(poseStack)`），实体本体渲染器在 owner 存活时跳过自画。千鸟的"手臂姿势强制"用 `RenderPlayerEvent.Pre` 设 `rightArmPose`/直接改 `model.rightArm.xRot`。
- 相机实体判定（白眼 `setRenderViewEntity`）：`mc.setCameraEntity(e)`，恢复 `mc.setCameraEntity(mc.player)`。

### 5.13 HUD / 全屏特效 / 相机与雾

| 功能（1.12 实现） | 1.20.1 |
|---|---|
| 查克拉条 overlay（`RenderGameOverlayEvent` HELMET 阶段，纯文字/贴图） | `RegisterGuiOverlaysEvent` 注册 `IGuiOverlay`（id 排序可挂在 `VanillaGuiOverlay.PLAYER_HEALTH` 上下），绘制改 `GuiGraphics.blit/drawString` |
| 白眼视觉：全屏**颜色反相**矩形（`enableColorLogic+colorLogicOp(INVERT)`）+ 改 `renderDistanceChunks` + `setRenderViewEntity` 相机实体 | overlay 里 `RenderSystem.enableColorLogicOp(); RenderSystem.logicOp(GlStateManager.LogicOp.INVERT); guiGraphics.fill(...); RenderSystem.disableColorLogicOp();`；渲染距离 `mc.options.renderDistance().set(...)`（记得恢复）；相机实体见 §5.12 |
| 雾色/雾浓度（`EntityViewRenderEvent.FogColors/FogDensity`，ItemSuiton 水雾 den=255） | `ViewportEvent.ComputeFogColor`（setRed/Green/Blue）；`ViewportEvent.RenderFog`：`setNearPlaneDistance/setFarPlaneDistance/setFogShape` + `setCanceled(true)` 生效——"浓度"概念换算成近远平面收紧 |
| FOV（`FOVModifier`） | `ViewportEvent.ComputeFov`（含/不含 FOV 设置缩放两种，按旧行为选 `usedConfiguredFov`） |
| 相机震动（`CameraSetup` 随机角度） | `ViewportEvent.ComputeCameraAngles#setYaw/setPitch/setRoll`（旧震动幅值照搬） |
| 包驱动的持续时间（Message 带 tick 数） | 包与客户端状态机原样保留 |

### 5.14 视觉回归基准（保证"视觉等价"的方法论）

1. **移植前**在 1.12.2 实例逐招录制基准：固定种子超平坦世界 + 固定时间(`/time set noon`)/天气，每个忍术/实体/粒子 3 角度截图 + 10 秒视频（建议直接按 §10 回归清单顺序录，文件名=注册名）。
2. 移植后同条件复刻对比。重点核对：发光强度（加色 vs 半透明选错最常见）、双面是否缺面、depth 排序（透明体互相遮挡）、UV 滚动方向与速度、粒子密度/寿命/颜色、第一人称隐藏逻辑。
3. 已知**允许的差异**记录在案：核心 shader 下颜色插值与固定管线 smooth shading 的微小差别；雾形状（球面雾 vs 指数雾）观感差异。

---

## 6. 非渲染系统移植要点

### 6.1 实体（123 文件）
- **注册**：`EntityEntryBuilder` → `EntityType.Builder.of(factory, MobCategory).sized(w,h).clientTrackingRange(chunks).updateInterval(ticks)`；旧 `tracker(64,1,true)` → `clientTrackingRange(4).updateInterval(1)` + `setShouldReceiveVelocityUpdates(true)`。蛋 → `ForgeSpawnEggItem`。
- **数据同步**：`EntityDataManager.createKey` → `SynchedEntityData.defineId`（**宿主类必须写实际定义参数的类**，`EntityScalableProjectile`/`EntityBeamBase`/`EntityTailedBeast` 这类共享基类是踩坑高发区）；`entityInit` → `defineSynchedData`；`notifyDataManagerChange` → `onSyncedDataUpdated`（包围盒变化后 `refreshDimensions()`）。生成附加数据：Forge `IEntityAdditionalSpawnData` + `NetworkHooks.getEntitySpawningPacket(this)`（覆写 `getAddEntityPacket`）。
- **伤害**：`ItemJutsu.NINJUTSU_TYPE/SENJUTSU_TYPE` → `data/narutomod/damage_type/ninjutsu.json`（`message_id/scaling/exhaustion`）+ `ResourceKey<DamageType>`；构造 `new DamageSource(holder, direct, owner)`；旧 `setDamageBypassesArmor`/`setMagicDamage` 等→ `data/minecraft/tags/damage_type/bypasses_armor.json` 等标签加入自己的类型。
- **骑乘/操控**（§1#5）：尾兽人柱力骑乘、`EntityShieldBase.travel()` 读乘客输入、蛤蟆/犬等坐骑 → `getControllingPassenger()` 返回 `LivingEntity` + `tickRidden/getRiddenInput/getRiddenSpeed`（Mob 系），或非 Mob 的 LivingEntity 继续覆写 `travel()` 读 `passenger.xxa/zza`（两条路都通，按基类选）。`getMountedYOffset` → `getPassengerAttachmentPoint`/`positionRider`；`shouldRiderSit` 仍在（Forge IForgeEntity）。
- **多部件**（蛇/八岐/尾兽尾巴判定 + `MultiPartsPacket`）：Forge `PartEntity`（参照 EnderDragon），父实体 `isMultipartEntity/getParts`；自定义部件同步包保留。
- **AI**：`EntityAIBase` → `Goal`；自定义 AILeap/RangedTactical/DefendEntity 直翻（`goalSelector.addGoal`）；`SwimHelper`→`setMaxUpStep`/`MoveControl` 对应项核对。
- **Boss 条**：`BossInfoServer` → `ServerBossEvent`（`startSeeing/stopSeeing` 对应 `addPlayer/removePlayer`，进入/离开跟踪事件 `startSeenByPlayer/stopSeenByPlayer`）。
- **其他**：`noClip`→`noPhysics`；`isImmuneToFire`→`fireImmune()`(Builder)；`onUpdate`→`tick`；`onLivingUpdate`→`aiStep`；`attackEntityAsMob`→`doHurtTarget`；`setDead`→`discard`；爆炸 `world.createExplosion` 签名+`ExplosionInteraction`；`EntityFallingBlock.shouldDropItem` 反射 → AT `FallingBlockEntity#dropItem` 或 `cancelDrop`；Nuibari 反射写 `xTile/inTile` → 这些字段 1.16 已删，按 `AbstractArrow.inGround` 思路重写"钉入方块"状态机。

### 6.2 物品（136 文件）
- `ItemJutsu.Base` NBT 框架（JUTSU_INDEX/CDMAP/XPMAP/OWNER_ID/AFFINITY）**原样保留**（1.20.1 仍是 NBT）；冷却参照 `world_tick` 改为 `level.getGameTime()`。
- 使用流程改名：`onItemRightClick→use`、`onUsingTick→onUseTick`、`onPlayerStoppedUsing→releaseUsing`、`getMaxItemUseDuration→getUseDuration`、`EnumAction→UseAnim`；`onUpdate→inventoryTick`；`onArmorTick`（Forge 1.20.1 仍在 IForgeItem）。
- metadata 清零：BijuCloak 10 变体→10 个注册物品（推荐，配方/模型最直观）或 NBT+`CustomModelData`；Sharingan/Tenseigan damage-as-state→NBT；Kunai 变体判定→NBT/独立物品。
- 武器：`isShield`→`canPerformAction(stack, ToolActions.SHIELD_BLOCK)`；工具/剑改 `Tier`/`SwordItem`；鲛肌吸查克拉、八门、Kabutowari 自定义包等逻辑层直翻。
- `ItemArmor`→`ArmorItem`(+`ArmorMaterial` 枚举重建)、食物→`FoodProperties`、燃料→`getBurnTime`。

### 6.3 方块与流体（9 块）
- `Material` 删除 → `BlockBehaviour.Properties.of().mapColor(...).strength(h,r).sound(...).noCollission().noOcclusion()...`；`ITileEntityProvider`→`EntityBlock`+`BlockEntityType`（爆炸符）；`PropertyDirection`→`BlockStateProperties.FACING`+`createBlockStateDefinition`。
- 回调改名：`onBlockAdded→onPlace`、`updateTick→tick`(+`randomTick`)、`onEntityCollidedWithBlock→entityInside`、被爆破→Forge `onBlockExploded`、放置者→`setPlacedBy`。
- **流体重做**（mud、water_still）：`ForgeFlowingFluid.Properties` + **`FluidType`**（注册到 `ForgeRegistries.Keys.FLUID_TYPES`，客户端贴图/雾色走 `IClientFluidTypeExtensions`）+ `LiquidBlock`；旧 `forge_marker` 流体 blockstate 删除。BlockMud 的湖泊生成 → `Feature`(可用原版 `LakeFeature` 配置) + `forge:add_features` BiomeModifier JSON。

### 6.4 GUI / 键位 / 输入
- 37 个卷轴 GUI：移植 `GuiNinjaScroll` 一次（`AbstractContainerMenu` + `IForgeMenuType.create`（带 buf）+ `AbstractContainerScreen` + `GuiGraphics`），37 个子类只剩"按钮清单+GUIID"参数化数据；按钮/槽位包（GUIButtonPressed/GUISlotChanged）保留。
- 打开：`player.openGui(...)` → `NetworkHooks.openScreen(serverPlayer, menuProvider, buf -> ...)`；屏幕注册 `MenuScreens.register` 于 `FMLClientSetupEvent.enqueueWork`（§1#1）。
- 键位：5 个 `KeyMapping` + `RegisterKeyMappingsEvent`；按文档建议**在 `ClientTickEvent` 用 `consumeClick()` 轮询**（替代旧 KeyInputEvent 混用），按下/松开状态机+发包逻辑保留；鼠标滚轮切忍术→`InputEvent.MouseScrollingEvent`（按住特定键时 `setCanceled(true)`+发包）；扩展攻击距离鼠标事件→`InputEvent.InteractionKeyMappingTriggered` 取消+自定义攻击包（服务端校验 `ForgeMod.ENTITY_REACH`）。
- `PlayerInput`（夺心术等"操控实体"的输入转发）：`MovementInputUpdateEvent` 采集 + 自定义包，结构保留。

### 6.5 存档 / 变量 / 调度
- `WorldSavedData`+`MapStorage` → `SavedData`+`DimensionDataStorage.computeIfAbsent(load, create, name)`；跨维度全局数据挂 `server.overworld()`（官方文档明确 Overworld 永不卸载）。涉及：`MapVariables`、`WorldVariables`、`SaveData`(11 尾兽)、`SpecialEvent.Save`、`PlayerTracker.Deaths`。
- 玩家持久变量（battle_experience 等 `getEntityData()`，全工程实测 **297 处调用**）：1.20.1 `getPersistentData()` 仍在，但**死亡重生不自动拷贝**——在 `PlayerEvent.Clone` 里 `event.getOriginal().reviveCaps()` 后整块复制（旧 PlayerTracker 已有 clone 逻辑，对位迁移）。**先用脚本扫出全部 NBT 键生成迁移清单**（哪些是持久进度、哪些是临时缓存、哪些挂非玩家实体），不要只靠 Clone 一条路兜底；`keepNinjaXp` 规则 → `GameRules.register("keepNinjaXp", Category.PLAYER, BooleanValue.create(false))`。
- 全局 `world_tick` 调度器与 `SpecialEvent` 8 类事件：逻辑直翻（`ServerTickEvent`/`LevelTickEvent`），时间基准建议换 `overworld.getGameTime()` 消除自增 double。
- 进度授予：`player.getAdvancements().award(adv, criterion)`，查询经 `server.getAdvancements().getAdvancement(id)`。

### 6.6 维度（Kamui）与世界生成
- `dimension_type/kamuidimension.json`（无雨雪/无床/固定 0.12 亮度曲线/`has_skylight:false` 等映射旧 WorldProvider）+ `dimension/kamuidimension.json` 引用自定义生成器。
- `KamuiChunkGenerator extends ChunkGenerator`：实现 `codec()`、`fillFromNoise`（搬旧 Simplex 浮岛算法）、`buildSurface`、`getBaseHeight/getBaseColumn`、空 `applyCarvers/spawnOriginalMobs`；Codec 注册到 `Registries.CHUNK_GENERATOR`。固定生成 Kamui 方块岛屿、Y=69 出生逻辑放传送器。
- 雾/天空：`DimensionSpecialEffects` 子类（黑雾、无云无日月）经 `RegisterDimensionSpecialEffectsEvent` 注册，`dimension_type` 的 `effects` 字段指向它。
- 传送：神威程序 → `entity.changeDimension(targetLevel, new ITeleporter(){ placeEntity(...) })`；入维事件 `PlayerChangedDimensionEvent`/`EntityTravelToDimensionEvent` 替代旧 `WorldProvider#onPlayerAdded` 钩子；旧反射 `invulnerableDimensionChange` → AT `ServerPlayer#isChangingDimension`。
- 结构放置（神树 4 段堆叠 + 树叶层 + 陨石 + 木屋）：`serverLevel.getStructureManager().get(id)` → `StructureTemplate.placeInWorld(level, pos, pos, new StructurePlaceSettings(), random, Block.UPDATE_CLIENTS)`；NBT 文件移到 **`data/narutomod/structures/`**（1.13+ 数据包路径，不再是 assets）。
- 村庄围攻/规模检测：1.12 `Village` API 已死 → `serverLevel.getPoiManager().getInRange(t -> t.is(PoiTypes.MEETING), pos, r, ...)` 数职业方块/会面点估规模，或 `structureManager().getStructureAt(pos, BuiltinStructures.VILLAGE_*)`。

### 6.7 命令 / 药水 / 队伍
- 3 个命令 → Brigadier（`RegisterCommandsEvent`；`EntityArgument.player()`、`IntegerArgumentType` 等）。
- 9 个效果 → `MobEffect`（`MobEffectCategory.HARMFUL/BENEFICIAL` + 颜色；tick 逻辑进 `applyEffectTick`，门控 `isDurationEffectTick`；图标若需自定义渲染用 `IClientMobEffectExtensions`，否则把 `textures/mob_effect/*.png` 挪到约定路径自动用）。
- 飞行/麻痹这类"过期回调"：1.20.1 无 `PotionExpires` 事件入口差异不大——继续用 `MobEffectEvent.Expired`（Forge 有）对位旧 `ProcedureXxxPotionExpires`。
- 队伍：`scoreboard` API 小改（`PlayerTeam`/`Scoreboard#addPlayerToTeam`）。

---

## 7. 资源与数据文件转换

| 资源 | 数量 | 处理 | 自动化 |
|---|---|---|---|
| `lang/*.lang`（en 577 + zh_cn 507 条） | 2 | → `en_us.json`/`zh_cn.json`；**键名前缀重写**：`item.X.name→item.narutomod.X`、`entity.X.name→entity.narutomod.X`、`tile.→block.`、`potion.→effect.narutomod.`、容器标题、键位类别 `key.categories.*` 保留 | ✅ 脚本 |
| `recipes/*.json` | 42（31 个含 `"data":N`） | 元数据扁平化映射（`wool:15→white_wool`、`dye:N→*_dye`…），自产物品的 meta 变体跟随 §6.2 拆分改名 | ✅ 脚本+映射表 |
| `blockstates` | 9 | 去 `forge_marker`；流体 blockstate 删除重做（§6.3）；multipart 语法核对 | 半自动 |
| `models/item` + `models/block` | 176+33 | 基本原样；**新约束：物品模型文件名必须=注册名**（旧 `setCustomModelResourceLocation` 任意名已死）——做一张"模型文件↔注册名"审计表，缺的补 `models/item/<reg>.json`；含 meta 变体的改 `overrides`+`custom_model_data` 或拆分 | 半自动 |
| `models/custom/*.json`（Blockbench） | 80 | **原样可用**（elements/display 格式未变） | ✅ |
| `wendigo.obj` | 1 | 模型 JSON 加 `"loader":"forge:obj"`+`flip_v`，删 addDomain | 手工 |
| 纹理 + `.mcmeta` 动画 | 346+16 | 原样；逐个检查文件名小写；盔甲贴图路径 `textures/models/armor/` 不变 | ✅ |
| `sounds.json` + ogg | 98 键 + 97 ogg | **14 个含大写的键小写化**（`Amaterasu→amaterasu`…，其余 84 键已合规；ogg 文件名与内部路径实测全小写，不动文件）+ 代码引用同步 + `SoundEvent.createVariableRangeEvent` 注册；类别保留 | ✅ 脚本 |
| **粒子定义（新增）** | 12 | 每种建 `assets/narutomod/particles/<name>.json`（用图集的写 textures；自管纹理/CUSTOM 的也要占位文件） | ✅ |
| `loot_tables` | 1 → 10 | 旧 `jutsu_loot_table` 升格式（entry type `minecraft:item` 等）；**新增 9 个方块掉落表** `loot_tables/blocks/*.json` | 半自动 |
| `advancements` | 21 | 基本兼容；icon `{"item":}` 字段核对；`rewards.recipes` 指向的配方 id 跟随改名 | 半自动 |
| `structures/*.nbt` | 22 | 移到 `data/narutomod/structures/`；**升 DataVersion**：在 1.20.1 开发实例写一段一次性代码逐个 `load→save`（DFU 升级 1.12 调色板，含 `minecraft:log[variant=…]` 等扁平化），或结构方块手动重存；mod 自方块 id 不变则保留 | 工具 |
| `pack.mcmeta` / `mcmod.info` | 2 | `pack_format: 15`；`mcmod.info` 删除 → `mods.toml` | ✅ |

**需要编写的一次性脚本清单**：lang 转换器、配方扁平化器、sounds 小写化器（含 Java 源引用替换）、模型文件名审计器、结构重存器（游戏内跑）、**模型类转换器**（§5.9）、注册表抽取器（从旧源码扫所有注册名生成 DeferredRegister 骨架）。

**Datagen 与一次性脚本的分工**：上表"脚本"产物是一次性转换出的最终 JSON（转完即基线）。**新生内容**——9 个方块 loot table、`DamageType` JSON、damage_type 等 tags、BiomeModifier、（可选）advancements——建议改走 **Forge datagen**（`GatherDataEvent` + 各 `*Provider`，`runData` 生成）：代码直接引用 `RegistryObject`，日后改注册名不会漂移，还能在 CI 校验。存量 lang/配方/sounds **不必强行 datagen 化**（为 1000+ 存量键手写 provider 的成本高于收益），脚本转一次即可。

---

## 8. 分阶段实施计划

> 原则不变：地基 → 无渲染逻辑 → **渲染基建** → 内容批量 → 集成。每阶段可编译、可进游戏验证。

- **M0 工程骨架**（1–2 天）：MDK/FG6/Java17/Parchment、mods.toml、AT 文件、包结构、空注册类。验收：空 mod 进主菜单。
- **M1 核心基建**（1.5–2.5 周，**拆 4 道门、每道有独立可运行验收**，防止两周无产出）：
  - **M1a 注册+网络最小闭环**（2–3 天）：DeferredRegister 全套骨架（脚本抽取注册名）+ `NetworkHandler` 通道框架 + 1 物品/1 实体/1 消息回环自测命令。验收：进世界拿到物品、消息双向回环通过。
  - **M1b 玩家变量同步**（2–3 天）：`NarutomodModVariables` 附加/同步包/`PlayerEvent.Clone`。验收：重生与跨维后变量保留、第二客户端可见。
  - **M1c 查克拉+存档**（3–4 天）：`Chakra`、`SavedData` 四件、`PlayerTracker`/`EntityTracker`。验收：消耗/再生/上限双客户端一致、读档不丢。
  - **M1d 工具库+专服门**（3–5 天）：`ProcedureUtils`（AT 替换全部反射）、`ProcedureSync` 同步原语；**专服启动零客户端类加载崩溃**（56 个 `Minecraft.getMinecraft()` 文件的隔离在此一并验收）。
- **M2 资源批转换**（并行，3–5 天）：§7 全部脚本 + 结构重存。验收：资源加载零报错、语言可显示。
- **M3 渲染基建**（1.5–2 周，**新设里程碑**）：`NarutoRenderTypes` 4 族、`SphereMesh`、模型转换器跑通 T1 批、粒子管线（Options/Provider/自定义 RenderType）+ 12 种粒子、`GuiGraphics` 工具。验收（**硬门槛：4 类代表样本齐过，只跑小件不算过**）：① 小件 `ModelHelmetSnug`；② 深层级巨型一个（TenTails 308 addChild 或 Buddha1000 节选）；③ 含 sin/cos 程序化动画的 Biped 人形一个；④ GL 耦合特效模型一个（从 `audit/render_gl_files.csv` 取，如 EntityPretaShield/EntityRasengan）。每个样本与 1.12 同机位截图对照；转换器对 87 类全量空跑无异常；粒子测试命令逐个对照 1.12 基准。
- **M4 垂直切片**（1 周）：**螺旋丸全链路**（ItemNinjutsu→键位→EntityRasengan→发光叠层渲染→SMOKE 粒子→音效→卷轴 GUI→冷却/经验/查克拉消耗），第一/三人称都过。产出《单忍术移植配方》文档，后续内容批量照抄。
- **M5 物品层**（3–4 周）：ItemJutsu 框架→元素术→武器→盔甲（含 §5.11 三件特效盔甲）→杂项；metadata 拆分；39 个卷轴物品。
- **M6 实体层**（5–8 周，最重）：基类→投射物批→分身/护盾批→忍者 NPC 批→召唤兽批→尾兽批（骑乘+Boss 条+SaveData）→须佐批→特效实体批；模型 T2–T4 批随各实体走。
- **M7 GUI/HUD/键位**（1.5 周）：菜单/屏幕、37 卷轴参数化、3 个 overlay、5 键位+PlayerInput、滚轮切招。
- **M8 维度/世界/事件**（1.5–2 周）：Kamui 生成器+传送、结构放置、泥潭 Feature、8 类 SpecialEvent、村庄 POI 重写、命令。
- **M9 集成与 QA**（2–3 周）：§10 全量回归（单人+专服）、视觉基准比对、性能（粒子风暴/尾兽大战/Buddha1000）、多语言、崩溃清理。

## 9. 风险登记（v2 更新）

| 风险 | 等级 | 应对 |
|---|---|---|
| 71/100 个文件 GL 状态与几何耦合，重构量超预期 | **高** | M3 先定 4 族 RenderType 词汇表，重构=归类填空（按 `audit/render_gl_files.csv` 逐文件销账）；T1 小模型先行校准节奏 |
| 透明体排序问题（多个半透明实体互叠：穹顶里的须佐里的玩家） | 高 | 统一"发光不写深度、本体写深度"约定；问题场景逐个调 RenderType 顺序/`bufferSource.endBatch` |
| 87 模型转换的动画走样（尾兽尾波、八岐颈段） | 高 | 转换器只管几何；35 个三角函数动画人工逐个对照视频核对 |
| 玩家渲染（克隆/透明/乘子）与他模组冲突 | 中 | 全部走事件+层，不替换渲染器、不 Mixin；冲突面最小化 |
| 结构 NBT DFU 升级失败/调色板丢块 | 中 | 重存脚本 + 逐结构生成验收；保底 1.20.1 手动重建 |
| ~60 网络包 id 错位/语义漂移 | 中 | 单文件顺序注册+协议版本号；做一个"回环自测"调试命令 |
| Kamui 生成器 Codec/区块管线不熟 | 中 | 先空岛假实现打通注册，再迁噪声；参考原版 `FlatLevelSource` 写法 |
| 声音/资源大小写遗漏导致静默缺资源 | 中 | 小写化脚本 + 启动日志 missing-resource 扫描脚本 |
| 性能：每帧重建大网格（球体/闪电/120 扇形） | 低-中 | 量级评估都在数千顶点内；若掉帧再上 `VertexBuffer` 缓存 |
| Optifine/Embeddium 下自定义 RenderType 异常 | 低 | 列为"已知兼容性"，QA 各跑一遍；不为其改架构 |
| 许可：README 声明版权归原作者 (ahznb)，禁止未授权商用 | — | 发布移植版前取得许可/遵循其条款 |

## 10. 回归验证清单（节选骨架，QA 时按全忍术清单展开）

- 查克拉：升级解锁/消耗/再生/不足警告/死亡清零/跨维度与重生保留/睡觉回复。
- 基础忍术：替身、影分身（**他人视角皮肤克隆+挂物**）、变身术（皮肤+名牌）、螺旋丸（一/三人称、10 层闪烁感）、螺旋手里剑（飞行血刃+命中白爆）。
- 元素术：火/水/土/雷/风代表招（光束滚动纹理、水龙双 pass、土块群、闪电三宽度白芯）。
- 瞳术：佩戴渲染+面部高光全亮、白眼反相视觉+相机实体+渲染距离恢复、非主人致盲、写轮眼系火焰色须佐。
- 尾兽：召唤/骑乘操控（**1.19.4 新骑乘 API 路径**）/Boss 条/尾兽玉/九尾 9×7 尾波动画/SaveData 持久化。
- 须佐：骨→衣→翼分层、火焰罩滚动+渐入、第一人称隐藏本体、勾玉 5 层叠加。
- 人柱力披风：尾数位掩码、sway 动画、渐入 α、全亮 shine、128×64 贴图。
- 特效：地爆天星（吸块+球体增长+纹理切换）、神威（粒子螺旋+维度往返）、封印锁链、影子模仿、八卦、冰穹、特效球（双模式）。
- 粒子：12 种逐个（密度/颜色/寿命/第一人称隐藏/BurningAsh 点燃/AcidSpit 黏附腐蚀+方块裂纹）。
- 系统：37 卷轴 GUI、医疗卷轴 3 槽、队伍管理、5 键位+滚轮、3 命令、9 效果(含过期回调)、8 类特殊事件、神树/陨石/木屋生成、泥潭、燃料、进度、战利品。
- 联机：专服启动零客户端类加载崩溃；同步/Boss 条/克隆皮肤/挂物在第二客户端正确。

## 11. 工作量估算（v2 按普查校准）

| 阶段 | 估时（单人全职） |
|---|---|
| M0+M1 基建 | 2–3 周（M1 拆 4 道门验收） |
| M2 资源（并行） | 0.5–1 周 |
| M3 渲染基建 | 1.5–2 周 |
| M4 垂直切片 | 1 周 |
| M5 物品 | 3–4 周 |
| M6 实体+模型 | 5–8 周 |
| M7 GUI/HUD | 1.5 周 |
| M8 维度/世界/事件 | 1.5–2 周 |
| M9 QA | 2–3 周 |
| **合计** | **约 4–6 个月**（渲染相关 ≈45%） |

> 相比 v1 的 3–5 个月上调：实测 71/100 个文件 GL 耦合 + 42 个巨型模型是主要增量。**估算前提：单人全职，视觉验收口径为"同机位截图高相似"**。多人并行（渲染/实体/资源三线）或大量使用 AI 辅助批量转换可显著压缩；若接受"视觉近似而非逐帧等价"，M6 可砍 1–2 周；反之若对外承诺"逐帧等价 + 专服长稳"，**口径应放到 ≥6 个月**。

## 12. 通用 API 速查表（渲染专表见 §5.2）

| 1.12.2 | 1.20.1 |
|---|---|
| `World`/`WorldServer`/`world.isRemote` | `Level`/`ServerLevel`/`level.isClientSide` |
| `EntityPlayer(MP)`/`EntityLivingBase` | `Player`/`ServerPlayer`/`LivingEntity` |
| `posX`、`motionX`、`rotationYaw` | `getX()`、`getDeltaMovement()`、`getYRot()` |
| `NBTTagCompound`/`Vec3d`/`RayTraceResult`/`AxisAlignedBB` | `CompoundTag`/`Vec3`/`HitResult`/`AABB` |
| `IBlockState`/`Material.X` | `BlockState`/（删除→`Properties`） |
| `RegistryEvent.Register`/`GameRegistry` | `DeferredRegister`+`RegistryObject` |
| `@SidedProxy` | 事件 + client 包隔离 |
| `SimpleNetworkWrapper`/`IMessage` | `SimpleChannel.registerMessage(id,enc,dec,handle)`/`consumerMainThread` |
| `WorldSavedData`+`MapStorage` | `SavedData`+`DimensionDataStorage` |
| `Potion`/`PotionEffect` | `MobEffect`/`MobEffectInstance`（tick 门控 `isDurationEffectTick`） |
| `SharedMonsterAttributes`/`REACH_DISTANCE` | `Attributes`/`ForgeMod.ENTITY_REACH·BLOCK_REACH` |
| `DamageSource` 子类/工厂 | 数据驱动 `DamageType` JSON + Holder |
| `EntityEntryBuilder`+`egg()` | `EntityType.Builder`+`ForgeSpawnEggItem` |
| `EntityDataManager`/`DataParameter` | `SynchedEntityData`/`EntityDataAccessor` |
| `BossInfoServer` | `ServerBossEvent` |
| `IGuiHandler`+`openGui` | `MenuType`(+`IForgeMenuType`)+`NetworkHooks.openScreen`+`MenuScreens.register`(client setup) |
| `GuiScreen`/`GuiContainer`/`drawTexturedModalRect` | `Screen`/`AbstractContainerScreen`/**`GuiGraphics`**`.blit` |
| `RenderGameOverlayEvent` | `RegisterGuiOverlaysEvent`+`IGuiOverlay` |
| `KeyBinding`+`ClientRegistry` | `KeyMapping`+`RegisterKeyMappingsEvent`+`ClientTickEvent#consumeClick` |
| `EntityViewRenderEvent.*` | `ViewportEvent.*`/`ComputeFovModifierEvent` |
| `mc.setRenderViewEntity` | `mc.setCameraEntity` |
| `CommandBase` | Brigadier+`RegisterCommandsEvent` |
| `getEntityData()` 持久化 | `getPersistentData()`+`PlayerEvent.Clone`(`reviveCaps`) |
| `CreativeTabs` | `Registries.CREATIVE_MODE_TAB` 注册+`BuildCreativeModeTabContentsEvent` |
| 1.12 模板 `Template` | `StructureTemplate`+`StructureTemplateManager`（数据包路径） |
| `Village` API | POI（`PoiManager`/`PoiTypes`）或结构判定 |

## 13. 参考资料

**Forge 1.20.x 官方文档（已逐页核对本计划相关章节）**
- 网络：https://docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/
- 粒子：https://docs.minecraftforge.net/en/1.20.x/gameeffects/particles/
- 模型加载器(OBJ)：https://docs.minecraftforge.net/en/1.20.x/rendering/modelloaders/
- Access Transformer：https://docs.minecraftforge.net/en/1.20.x/advanced/accesstransformers/
- 菜单/屏幕：https://docs.minecraftforge.net/en/1.20.x/gui/menus/ 、https://docs.minecraftforge.net/en/1.20.x/gui/screens/
- SavedData：https://docs.minecraftforge.net/en/1.20.x/datastorage/saveddata/
- 注册表：https://docs.minecraftforge.net/en/1.20.x/concepts/registries/
- 键位：https://docs.minecraftforge.net/en/1.20.x/misc/keymappings/

**版本变更 primer（1.12 → 1.20 变化链）**
- 官方 1.12→1.13/1.14 移植指南：https://docs.minecraftforge.net/en/1.14.x/legacy/porting1214/
- williewillus《1.13/1.14 update primer》（扁平化圣经）：https://gist.github.com/williewillus/353c872bcf1a6ace9921189f6100d09a
- ChampionAsh5357《1.19.3→1.19.4》（DamageType 数据驱动）：https://gist.github.com/ChampionAsh5357/163a75e87599d19ee6b4b879821953e8
- ChampionAsh5357《1.19.4→1.20》（GuiGraphics/创造标签注册表/网络 consumer 拆分）：https://gist.github.com/ChampionAsh5357/cf818acc53ffea6f4387fe28c2977d56
- primer 总索引（1.17–1.20 各代渲染/注册变化）：https://github.com/neoforged/.github/blob/main/primers/1.20/index.md

**玩家渲染层模式**
- NeoForged 实体渲染文档（AddLayers 玩家多渲染器处理模式，1.20.1 Forge 同构）：https://docs.neoforged.net/docs/entities/renderer/

**本计划的代码审计依据**（1.12.2 源内行号见 §5 各配方）：EntityBeamBase/EntityRasengan/EntityRasenshuriken/EntityChidori/EntityTruthSeekerBall/EntityLightningArc/EntitySpecialEffect/EntityShadowImitation/EntityEarthBlocks/EntitySealingChains/EntityEightTrigrams/EntityChibakuTenseiBall/EntityIceDome/EntityWaterDragon/EntityWoodPrison/Particles(12 类)/PlayerRender/ItemOnBody/EntityClone/ItemDojutsu/ItemBijuCloak/EntitySusanoo 系/ProcedureDeathAnimations + 87 模型类普查。

## 14. 新会话接手卡（2026-06-14）

> 给新的 Codex/Claude 窗口用：不要从 M0 重新开始。本文档是主计划与验收口径，**真实已完成进度以 `MIGRATION_PROGRESS.md` 末尾为准**。

### 14.1 新窗口启动时先读

1. 先读本节。
2. 再读 `MIGRATION_PROGRESS.md` 最后 200–300 行，确认最近完成的 slice。
3. 如需看全局审计，读这些文件：
   - `audit/resource_validation_summary.json`
   - `audit/m0_audit_summary.json`
   - `audit/entity_persistent_data_key_summary.json`
   - `audit/legacy_model_manifest_summary.json`
4. 当前 Forge 1.20.1 工程在 `D:\mcmodding\naruto\1.20.1`；旧版只作为对照源在 `D:\mcmodding\naruto\1.12.2`。

### 14.2 当前关键状态

- 资源贴图问题的主修复已经落地：`assets/minecraft/atlases/blocks.json` 把旧版 `textures/items` 与 `textures/blocks` 纳入 1.20 block atlas。不要先大规模改几百个模型 JSON；若贴图仍异常，先看客户端日志和 `tools\validate_port_resources.py` 的 `texture_not_in_block_atlas` 类问题。
- 最近一次贴图/崩溃修复还包括：
  - 显式注册 `NarutomodModVariables.ForgeEvents`。
  - 玩家变量增加 UUID fallback。
  - 客户端变量同步改为写入 `NarutomodModVariables.get(player)`。
  - 简化 `amaterasublock`、`explosive_tag`、`water_still` 的过时 blockstate variant。
- `ModEntityTypes.dummyTypes()` 当前应为空；不要把 `PortingDummyEntity` 当作仍在使用的占位实体清单。
- `ProcedureSync` 的动态 `tagName/message.tag` 行是同步传输 API 的设计，不要为了让旧审计 CSV 归零而硬编码所有动态键。
- `CTRL_pressed` 是旧 `ProcedureSpecialJutsu2OnKeyPressed` 的真实固定键；当前 1.20 入口应在 `SpecialJutsuKeyMessage` 统一维护 `JutsuKey2Pressed` 并在 key 2 路由后复位 `CTRL_pressed`。

### 14.3 标准验证命令

从仓库根目录 `D:\mcmodding\naruto`：

```powershell
python tools\validate_port_resources.py
python tools\validate_dedicated_server_safety.py
powershell -ExecutionPolicy Bypass -File tools\run_dedicated_server_gate.ps1
```

从 `D:\mcmodding\naruto\1.20.1`：

```powershell
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' --no-daemon compileJava
.\gradlew.bat '-Dnet.minecraftforge.gradle.check.certs=false' --no-daemon build
```

若用户报告“贴图不正常”或“崩溃”，优先检查：

```powershell
Get-ChildItem 1.20.1\run\crash-reports -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 3
Select-String -Path 1.20.1\run\logs\latest.log -Pattern "Missing textures|Missing model|Missing blockstate|Missing Narutomod player variables|ERROR|FATAL|Exception|Crash"
```

### 14.4 下一步优先级

1. 用户当前优先级是“实际贴图/崩溃”而不是抽象路线图。任何新工作前先复现或读取最新 client log/crash report。
2. 若资源校验为 0 但游戏中仍有紫黑贴图，优先定位是 atlas/stitch、模型引用路径、还是 blockstate/item model variant，而不是直接重命名资源文件。
3. 若继续 §6.5 动态键迁移，先处理能证明语义的固定键；`ProcedureAoeCommand` 的 `key` 参数和 `ProcedureSync` 的 `tagName/message.tag` 属于动态传输场景，要结合调用方迁移，不要凭审计行数机械修改。
4. 每完成一个 slice，必须追加 `MIGRATION_PROGRESS.md`，并记录至少 `compileJava`、资源校验、专服安全校验；影响运行或发布包时再跑完整 `build` 与专服启动门。

### 14.5 可直接贴给新窗口的开场提示

```text
请继续 D:\mcmodding\naruto 的 narutomod 1.12.2 -> Forge 1.20.1 移植。先读 PORTING_PLAN_1.20.1.md 的“新会话接手卡”和 MIGRATION_PROGRESS.md 末尾，不要从 M0 重启。用户当前优先级是贴图异常与崩溃；先用 validate_port_resources、latest.log、crash-reports 定位。改动后按计划里的标准验证命令跑门禁，并把 slice 追加到 MIGRATION_PROGRESS.md。
```

## 15. 立即可做的下一步

1. **M0 起工程**：1.20.1 MDK + mods.toml + AT 文件 + 注册骨架。
2. **写注册表抽取脚本**跑出全部注册名清单（物品/实体/音效/粒子），同时产出"声音大写键"与"模型文件名≠注册名"两张问题清单。
3. **M3 三件渲染基建先行**：`NarutoRenderTypes`、`SphereMesh`、模型转换器（拿 ModelHelmetSnug 当第一个样本）。
4. **垂直切片选螺旋丸**，打通后以它为模板批量化。

> 本文档为移植期间的主 backlog；§5 各配方中的常数（混合模式、α 值、偏移、速度）均来自源码审计，移植时**直接照抄数值**，不要凭感觉调。
