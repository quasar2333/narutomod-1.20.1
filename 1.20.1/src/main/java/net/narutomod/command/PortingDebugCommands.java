package net.narutomod.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.Chakra;
import net.narutomod.NarutomodSavedData;
import net.narutomod.entity.AsakujakuFireballEntity;
import net.narutomod.entity.AltCamViewEntity;
import net.narutomod.entity.AsuraCannonballEntity;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.BiggerMeEntity;
import net.narutomod.entity.BrackenDanceEntity;
import net.narutomod.entity.BugSwarmEntity;
import net.narutomod.entity.Buddha1000Entity;
import net.narutomod.entity.BuddhaArmEntity;
import net.narutomod.entity.CellularActivationEntity;
import net.narutomod.entity.ChibakuSatelliteEntity;
import net.narutomod.entity.ChibakuTenseiBallEntity;
import net.narutomod.entity.ChidoriEntity;
import net.narutomod.entity.ChidoriSpearEntity;
import net.narutomod.entity.CrowEntity;
import net.narutomod.entity.EarthBlocksEntity;
import net.narutomod.entity.EarthSandwichEntity;
import net.narutomod.entity.EarthSpearsEntity;
import net.narutomod.entity.EarthWallEntity;
import net.narutomod.entity.EightTrigramsEntity;
import net.narutomod.entity.GroundShockEntity;
import net.narutomod.entity.HakkeshoKeitenEntity;
import net.narutomod.entity.EightyGodsEntity;
import net.narutomod.entity.ExplosiveClayEntity;
import net.narutomod.entity.ExplosiveCloneEntity;
import net.narutomod.entity.FalseDarknessEntity;
import net.narutomod.entity.FingerBoneEntity;
import net.narutomod.entity.FoldingFanProjectileEntity;
import net.narutomod.entity.FutonChakraFlowEntity;
import net.narutomod.entity.FutonGreatBreakthroughEntity;
import net.narutomod.entity.FutonVacuumEntity;
import net.narutomod.entity.FuttonMistEntity;
import net.narutomod.entity.GamabuntaEntity;
import net.narutomod.entity.GedoStatueEntity;
import net.narutomod.entity.GiantDog2hEntity;
import net.narutomod.entity.HirudoraEntity;
import net.narutomod.entity.HiramekareiEffectEntity;
import net.narutomod.entity.HidingInAshEntity;
import net.narutomod.entity.HidingInRockEntity;
import net.narutomod.entity.IceDomeEntity;
import net.narutomod.entity.IcePrisonEntity;
import net.narutomod.entity.IceSpearEntity;
import net.narutomod.entity.IceSpikeEntity;
import net.narutomod.entity.IntonRaihaEntity;
import net.narutomod.entity.JinchurikiCloneEntity;
import net.narutomod.entity.JintonBeamEntity;
import net.narutomod.entity.JintonCubeEntity;
import net.narutomod.entity.KageBunshinEntity;
import net.narutomod.entity.KagutsuchiFireballEntity;
import net.narutomod.entity.KamuiShurikenEntity;
import net.narutomod.entity.KarasuScrollProjectileEntity;
import net.narutomod.entity.KatonFireballEntity;
import net.narutomod.entity.KatonFireStreamEntity;
import net.narutomod.entity.KibaBladeAuraEntity;
import net.narutomod.entity.KingOfHellEntity;
import net.narutomod.entity.KirinEntity;
import net.narutomod.entity.KusanagiSwordEntity;
import net.narutomod.entity.LaserCircusEntity;
import net.narutomod.entity.LaserRingEntity;
import net.narutomod.entity.LavaChakraModeEntity;
import net.narutomod.entity.LightningArcEntity;
import net.narutomod.entity.LightningBeastEntity;
import net.narutomod.entity.LimboCloneEntity;
import net.narutomod.entity.MagmaBallEntity;
import net.narutomod.entity.MandaEntity;
import net.narutomod.entity.MeltingJutsuEntity;
import net.narutomod.entity.MindTransferEntity;
import net.narutomod.entity.MindTransferSelfEntity;
import net.narutomod.entity.MightGuyEntity;
import net.narutomod.entity.NightGuyDragonEntity;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.entity.NuibariSwordEntity;
import net.narutomod.entity.PortingDummyEntity;
import net.narutomod.entity.PretaShieldEntity;
import net.narutomod.entity.SpecialEffectEntity;
import net.narutomod.entity.SpikeEntity;
import net.narutomod.entity.PoisonMistEntity;
import net.narutomod.entity.PuppetHirukoEntity;
import net.narutomod.entity.PuppetKarasuEntity;
import net.narutomod.entity.PuppetSanshouoEntity;
import net.narutomod.entity.PurpleDragonEntity;
import net.narutomod.entity.RasenganEntity;
import net.narutomod.entity.RasenshurikenEntity;
import net.narutomod.entity.RaitonChakraModeEntity;
import net.narutomod.entity.RantonCloudEntity;
import net.narutomod.entity.RantonKogaEntity;
import net.narutomod.entity.ReplacementCloneEntity;
import net.narutomod.entity.SandBindEntity;
import net.narutomod.entity.SandBulletEntity;
import net.narutomod.entity.SandLevitationEntity;
import net.narutomod.entity.SandShieldEntity;
import net.narutomod.entity.SanshouoScrollProjectileEntity;
import net.narutomod.entity.ScorchOrbEntity;
import net.narutomod.entity.SekizoEntity;
import net.narutomod.entity.SealingChainsEntity;
import net.narutomod.entity.SealingEntity;
import net.narutomod.entity.SenjutsuSitPlatformEntity;
import net.narutomod.entity.ShadowImitationEntity;
import net.narutomod.entity.SlugSummonEntity;
import net.narutomod.entity.Snake8HeadEntity;
import net.narutomod.entity.Snake8HeadsEntity;
import net.narutomod.entity.SnakeSummonEntity;
import net.narutomod.entity.SwampPitEntity;
import net.narutomod.entity.AbstractSusanooEntity;
import net.narutomod.entity.SusanooClothedEntity;
import net.narutomod.entity.SusanooSkeletonEntity;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.entity.SuitonMistEntity;
import net.narutomod.entity.SuitonStreamEntity;
import net.narutomod.entity.TailBeastBallEntity;
import net.narutomod.entity.TailedBeastEntity;
import net.narutomod.entity.TenTailsEntity;
import net.narutomod.entity.TenseiBakuGoldEntity;
import net.narutomod.entity.TenseiBakuSilverEntity;
import net.narutomod.entity.TenseiganOrbEntity;
import net.narutomod.entity.ToadSummonEntity;
import net.narutomod.entity.ThrownNinjaToolEntity;
import net.narutomod.entity.ThrownSpecialWeaponEntity;
import net.narutomod.entity.TransformationJutsuEntity;
import net.narutomod.entity.TruthSeekerBallEntity;
import net.narutomod.entity.UnrivaledStrengthEntity;
import net.narutomod.entity.WaterDragonEntity;
import net.narutomod.entity.WaterPrisonEntity;
import net.narutomod.entity.WaterSharkEntity;
import net.narutomod.entity.WaterShockwaveEntity;
import net.narutomod.entity.WoodArmEntity;
import net.narutomod.entity.WoodBurialEntity;
import net.narutomod.entity.WoodGolemEntity;
import net.narutomod.entity.WoodPrisonEntity;
import net.narutomod.entity.YasakaMagatamaEntity;
import net.narutomod.event.SpecialEvent;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.PortingPongMessage;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureMeteorStrike;
import net.narutomod.procedure.ProcedureSpawnGodTree;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.item.AdvancedNatureJutsuItem;
import net.narutomod.item.BasicMeleeWeaponItem;
import net.narutomod.item.BijuCloakItem;
import net.narutomod.item.BoneArmorItem;
import net.narutomod.item.ByakuganHandler;
import net.narutomod.item.ByakuganHelmetItem;
import net.narutomod.item.DotonItem;
import net.narutomod.item.EightGatesItem;
import net.narutomod.item.FutonItem;
import net.narutomod.item.IceSenbonItem;
import net.narutomod.item.IryoJutsuItem;
import net.narutomod.item.IntonItem;
import net.narutomod.item.JutsuItem;
import net.narutomod.item.JutsuScrollItem;
import net.narutomod.item.KagutsuchiSwordItem;
import net.narutomod.item.KatonItem;
import net.narutomod.item.MedicalScrollItem;
import net.narutomod.item.NarutoConsumableItem;
import net.narutomod.item.NinjutsuItem;
import net.narutomod.item.ObitoKamuiHandler;
import net.narutomod.item.ObitoMangekyoHelmetItem;
import net.narutomod.item.PowerIncreaseKeyHandler;
import net.narutomod.item.PuppetScrollItem;
import net.narutomod.item.RaitonItem;
import net.narutomod.item.RinneganHelmetItem;
import net.narutomod.item.RinneganSpecialJutsuHandler;
import net.narutomod.item.RyoItem;
import net.narutomod.item.SenjutsuItem;
import net.narutomod.item.SixPathSenjutsuItem;
import net.narutomod.item.SusanooPowerIncreaseHandler;
import net.narutomod.item.SuitonItem;
import net.narutomod.item.SummoningContractItem;
import net.narutomod.item.TeamScrollItem;
import net.narutomod.item.TenseiganChakraModeItem;
import net.narutomod.item.ZzzItem;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModRegistries;
import net.narutomod.registry.ModSounds;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.world.KamuiChunkGenerator;
import net.narutomod.world.KamuiDimension;
import net.narutomod.world.VillagePoiHelper;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class PortingDebugCommands {
    private static final ResourceLocation MUD_LAKE_ID = NarutomodMod.location("mud_lake");
    private static final int LEGACY_ITACHI_ENTITY_ID = 117;
    private static final List<String> M8_STRUCTURE_TEMPLATE_IDS = List.of(
            "meteor",
            "wood_house_2",
            "world_tree_1",
            "world_tree_2",
            "world_tree_3",
            "world_tree_4",
            "world_tree_5",
            "world_tree_6",
            "world_tree_7",
            "world_tree_8",
            "world_tree_9",
            "world_tree_10",
            "world_tree_11",
            "world_tree_12",
            "world_tree_13",
            "world_tree_14",
            "world_tree_15",
            "world_tree_16",
            "world_tree_17",
            "world_tree_18",
            "world_tree_19",
            "world_tree_20");

    private PortingDebugCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("narutoport")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ping")
                        .executes(context -> sendPing(context.getSource())))
                .then(Commands.literal("spawn_dummy")
                        .executes(context -> spawnDummy(context.getSource())))
                .then(Commands.literal("spawn_entity")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(context -> spawnEntity(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type")))))
                .then(Commands.literal("m3_smoke")
                        .executes(context -> runM3SmokeAll(context.getSource()))
                        .then(Commands.literal("all")
                                .executes(context -> runM3SmokeAll(context.getSource())))
                        .then(Commands.literal("models")
                                .executes(context -> runM3SmokeModels(context.getSource())))
                        .then(Commands.literal("particles")
                                .executes(context -> runM3SmokeParticles(context.getSource())))
                        .then(Commands.literal("mob_appearance_itachi")
                                .executes(context -> runM3MobAppearanceItachiSmoke(context.getSource()))))
                .then(Commands.literal("effects")
                        .then(Commands.literal("legacy_ready")
                                .executes(context -> prepareLegacyEffectsTest(context.getSource())))
                        .then(Commands.literal("state")
                                .executes(context -> reportLegacyEffectsState(context.getSource()))))
                .then(Commands.literal("m8_world")
                        .then(Commands.literal("state")
                                .executes(context -> reportM8WorldState(context.getSource())))
                        .then(Commands.literal("validation_suite")
                                .executes(context -> runM8WorldValidationSuite(context.getSource())))
                        .then(Commands.literal("kamui_ready")
                                .executes(context -> prepareKamuiDimensionTest(context.getSource())))
                        .then(Commands.literal("kamui_toggle")
                                .executes(context -> toggleKamuiDimension(context.getSource())))
                        .then(Commands.literal("kamui_terrain_probe")
                                .executes(context -> probeKamuiTerrain(context.getSource())))
                        .then(Commands.literal("mud_lake_probe")
                                .executes(context -> probeMudLakeWorldgen(context.getSource())))
                        .then(Commands.literal("natural_spawns_probe")
                                .executes(context -> probeNaturalSpawns(context.getSource())))
                        .then(Commands.literal("structures_probe")
                                .executes(context -> probeM8Structures(context.getSource())))
                        .then(Commands.literal("god_tree_place_probe")
                                .executes(context -> placeGodTreeProbe(context.getSource())))
                        .then(Commands.literal("god_tree_trigger_ready")
                                .executes(context -> placeGodTreeProbe(context.getSource())))
                        .then(Commands.literal("meteor_structure_probe")
                                .executes(context -> placeMeteorStructureProbe(context.getSource())))
                        .then(Commands.literal("special_events_state")
                                .executes(context -> reportSpecialEventsState(context.getSource())))
                        .then(Commands.literal("special_events_tick")
                                .executes(context -> runSpecialEventsTick(context.getSource())))
                        .then(Commands.literal("delayed_spawn_ready")
                                .executes(context -> prepareDelayedSpawnTest(context.getSource())))
                        .then(Commands.literal("vanilla_explosion_ready")
                                .executes(context -> prepareVanillaExplosionTest(context.getSource())))
                        .then(Commands.literal("set_blocks_ready")
                                .executes(context -> prepareSetBlocksTest(context.getSource())))
                        .then(Commands.literal("cylindrical_explosion_ready")
                                .executes(context -> prepareCylindricalExplosionTest(context.getSource())))
                        .then(Commands.literal("spherical_explosion_ready")
                                .executes(context -> prepareSphericalExplosionTest(context.getSource())))
                        .then(Commands.literal("meteor_shower_ready")
                                .executes(context -> prepareMeteorShowerTest(context.getSource())))
                        .then(Commands.literal("village_siege_ready")
                                .executes(context -> prepareVillageSiegeTest(context.getSource())))
                        .then(Commands.literal("might_guy_siege_ready")
                                .executes(context -> prepareMightGuySiegeQuestTest(context.getSource())))
                        .then(Commands.literal("might_guy_natural_ready")
                                .executes(context -> prepareMightGuyNaturalSpawnTest(context.getSource())))
                        .then(Commands.literal("delayed_callback_ready")
                                .executes(context -> prepareDelayedCallbackTest(context.getSource())))
                        .then(Commands.literal("transfer_smoke")
                                .executes(context -> runKamuiTransferSmokeTest(context.getSource())))
                        .then(Commands.literal("obito_ready")
                                .executes(context -> prepareObitoKamuiTest(context.getSource())))
                        .then(Commands.literal("obito_block_ready")
                                .executes(context -> prepareObitoKamuiBlockTest(context.getSource())))
                        .then(Commands.literal("obito_reverse_block_ready")
                                .executes(context -> prepareObitoKamuiReverseBlockTest(context.getSource())))
                        .then(Commands.literal("obito_grab_ready")
                                .executes(context -> prepareObitoKamuiGrabTest(context.getSource())))
                        .then(Commands.literal("eternal_kamui_ready")
                                .executes(context -> prepareEternalKamuiTest(context.getSource())))
                        .then(Commands.literal("obito_state")
                                .executes(context -> reportObitoKamuiState(context.getSource())))
                        .then(Commands.literal("obito_intangible_press")
                                .executes(context -> debugObitoKamuiPress(context.getSource(), false)))
                        .then(Commands.literal("obito_teleport_press")
                                .executes(context -> debugObitoKamuiPress(context.getSource(), true)))
                        .then(Commands.literal("obito_grab_press")
                                .executes(context -> debugObitoKamuiGrabPress(context.getSource())))
                        .then(Commands.literal("obito_key1_release")
                                .executes(context -> debugObitoKamuiRelease(context.getSource())))
                        .then(Commands.literal("obito_blind")
                                .executes(context -> setObitoKamuiBlindness(context.getSource(), true)))
                        .then(Commands.literal("obito_unblind")
                                .executes(context -> setObitoKamuiBlindness(context.getSource(), false))))
                .then(Commands.literal("m4_rasengan")
                        .executes(context -> prepareM4RasenganScene(context.getSource()))
                        .then(Commands.literal("scene")
                                .executes(context -> prepareM4RasenganScene(context.getSource())))
                        .then(Commands.literal("state")
                                .executes(context -> reportM4RasenganState(context.getSource()))))
                .then(Commands.literal("m5_jutsu")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5JutsuState(context.getSource())))
                        .then(Commands.literal("senjutsu_rasenshuriken_ready")
                                .executes(context -> prepareSenjutsuRasenshurikenTest(context.getSource())))
                        .then(Commands.literal("senjutsu_wood_buddha_ready")
                                .executes(context -> prepareSenjutsuWoodBuddhaTest(context.getSource())))
                        .then(Commands.literal("senjutsu_wood_buddha_shoot")
                                .executes(context -> shootSenjutsuWoodBuddhaArms(context.getSource())))
                        .then(Commands.literal("senjutsu_snake_8_heads_ready")
                                .executes(context -> prepareSnake8HeadsEntityTest(context.getSource())))
                        .then(Commands.literal("senjutsu_snake_8_heads_shoot")
                                .executes(context -> shootSenjutsuSnake8HeadsLookTarget(context.getSource())))
                        .then(Commands.literal("senjutsu_snake_8_heads_dismiss")
                                .executes(context -> dismissSenjutsuSnake8Heads(context.getSource())))
                        .then(Commands.literal("powerincrease_jutsu_ready")
                                .executes(context -> preparePowerIncreaseJutsuCycleTest(context.getSource())))
                        .then(Commands.literal("powerincrease_jutsu_cycle")
                                .executes(context -> cyclePowerIncreaseJutsuItem(context.getSource())))
                        .then(Commands.literal("powerincrease_byakugan_ready")
                                .executes(context -> preparePowerIncreaseByakuganFovTest(context.getSource())))
                        .then(Commands.literal("powerincrease_byakugan_press")
                                .executes(context -> pressPowerIncreaseByakuganFov(context.getSource())))
                        .then(Commands.literal("powerincrease_byakugan_release")
                                .executes(context -> releasePowerIncreaseByakugan(context.getSource())))
                        .then(Commands.literal("byakugan_visual_ready")
                                .executes(context -> prepareByakuganVisualTest(context.getSource())))
                        .then(Commands.literal("byakugan_64_ready")
                                .executes(context -> prepareByakugan64PalmsTest(context.getSource())))
                        .then(Commands.literal("byakugan_64_release")
                                .executes(context -> releaseByakugan64Palms(context.getSource())))
                        .then(Commands.literal("byakugan_kaiten_ready")
                                .executes(context -> prepareByakuganKaitenTest(context.getSource())))
                        .then(Commands.literal("byakugan_kaiten_press")
                                .executes(context -> pressByakuganKaiten(context.getSource())))
                        .then(Commands.literal("byakugan_kaiten_release")
                                .executes(context -> releaseByakuganKaiten(context.getSource())))
                        .then(Commands.literal("byakurin_yomotsu_ready")
                                .executes(context -> prepareByakurinYomotsuTest(context.getSource())))
                        .then(Commands.literal("byakurin_yomotsu_release")
                                .executes(context -> releaseByakurinYomotsu(context.getSource())))
                        .then(Commands.literal("byakurin_shockwave_ready")
                                .executes(context -> prepareByakurinShockwaveTest(context.getSource())))
                        .then(Commands.literal("byakurin_shockwave_press")
                                .executes(context -> pressByakurinShockwave(context.getSource())))
                        .then(Commands.literal("byakurin_shockwave_charge")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 200))
                                        .executes(context -> chargeByakurinShockwave(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(Commands.literal("byakurin_shockwave_release")
                                .executes(context -> releaseByakurinShockwave(context.getSource())))
                        .then(Commands.literal("powerincrease_susanoo_ready")
                                .executes(context -> preparePowerIncreaseSusanooUpgradeTest(context.getSource())))
                        .then(Commands.literal("powerincrease_susanoo_upgrade")
                                .executes(context -> upgradePowerIncreaseSusanoo(context.getSource())))
                        .then(Commands.literal("replacement_ready")
                                .executes(context -> prepareReplacementItemTest(context.getSource())))
                        .then(Commands.literal("kage_bunshin_ready")
                                .executes(context -> prepareKageBunshinItemTest(context.getSource())))
                        .then(Commands.literal("limbo_clone_ready")
                                .executes(context -> prepareLimboCloneItemTest(context.getSource())))
                        .then(Commands.literal("rinnegan_deva_ready")
                                .executes(context -> prepareRinneganDevaSpecialJutsu2Test(context.getSource())))
                        .then(Commands.literal("rinnegan_asura_ready")
                                .executes(context -> prepareRinneganAsuraSpecialJutsu2Test(context.getSource())))
                        .then(Commands.literal("rinnegan_asura_maintain")
                                .executes(context -> maintainRinneganAsuraPath(context.getSource())))
                        .then(Commands.literal("rinnegan_animal_ready")
                                .executes(context -> prepareRinneganPathSpecialJutsu2Test(context.getSource(),
                                        RinneganSpecialJutsuHandler.ANIMAL_PATH, "Animal")))
                        .then(Commands.literal("rinnegan_preta_ready")
                                .executes(context -> prepareRinneganPathSpecialJutsu2Test(context.getSource(),
                                        RinneganSpecialJutsuHandler.PRETA_PATH, "Preta")))
                        .then(Commands.literal("rinnegan_naraka_ready")
                                .executes(context -> prepareRinneganPathSpecialJutsu2Test(context.getSource(),
                                        RinneganSpecialJutsuHandler.NARAKA_PATH, "Naraka")))
                        .then(Commands.literal("rinnegan_outer_ready")
                                .executes(context -> prepareRinneganOuterSpecialJutsu2Test(context.getSource())))
                        .then(Commands.literal("rinnegan_path_cycle")
                                .executes(context -> cycleRinneganPowerIncreasePath(context.getSource())))
                        .then(Commands.literal("rinnegan_path_release")
                                .executes(context -> triggerRinneganSelectedPathRelease(context.getSource())))
                        .then(Commands.literal("rinnegan_deva_chibaku_release")
                                .executes(context -> triggerRinneganDevaChibakuRelease(context.getSource())))
                        .then(Commands.literal("rinnegan_deva_meteor_release")
                                .executes(context -> triggerRinneganDevaMeteorRelease(context.getSource())))
                        .then(Commands.literal("sealing_chains_ready")
                                .executes(context -> prepareSealingChainsItemTest(context.getSource())))
                        .then(Commands.literal("puppet_ready")
                                .executes(context -> preparePuppetItemTest(context.getSource())))
                        .then(Commands.literal("bug_swarm_ready")
                                .executes(context -> prepareBugSwarmItemTest(context.getSource())))
                        .then(Commands.literal("transformation_ready")
                                .executes(context -> prepareTransformationItemTest(context.getSource())))
                        .then(Commands.literal("hiding_camouflage_ready")
                                .executes(context -> prepareHidingCamouflageItemTest(context.getSource())))
                        .then(Commands.literal("healing_ready")
                                .executes(context -> prepareHealingItemTest(context.getSource())))
                        .then(Commands.literal("poison_mist_ready")
                                .executes(context -> preparePoisonMistItemTest(context.getSource())))
                        .then(Commands.literal("cellular_activation_ready")
                                .executes(context -> prepareCellularActivationItemTest(context.getSource())))
                        .then(Commands.literal("enhanced_strength_ready")
                                .executes(context -> prepareEnhancedStrengthItemTest(context.getSource())))
                        .then(Commands.literal("genjutsu_ready")
                                .executes(context -> prepareGenjutsuItemTest(context.getSource())))
                        .then(Commands.literal("mind_transfer_ready")
                                .executes(context -> prepareMindTransferItemTest(context.getSource())))
                        .then(Commands.literal("shadow_imitation_ready")
                                .executes(context -> prepareShadowImitationItemTest(context.getSource())))
                        .then(Commands.literal("chidori_ready")
                                .executes(context -> prepareChidoriItemTest(context.getSource())))
                        .then(Commands.literal("chakra_mode_ready")
                                .executes(context -> prepareChakraModeItemTest(context.getSource())))
                        .then(Commands.literal("lightning_beast_ready")
                                .executes(context -> prepareLightningBeastItemTest(context.getSource())))
                        .then(Commands.literal("false_darkness_ready")
                                .executes(context -> prepareFalseDarknessItemTest(context.getSource())))
                        .then(Commands.literal("futon_chakra_flow_ready")
                                .executes(context -> prepareFutonChakraFlowItemTest(context.getSource())))
                        .then(Commands.literal("rasenshuriken_ready")
                                .executes(context -> prepareRasenshurikenItemTest(context.getSource())))
                        .then(Commands.literal("futon_vacuum_ready")
                                .executes(context -> prepareFutonVacuumItemTest(context.getSource())))
                        .then(Commands.literal("big_blow_ready")
                                .executes(context -> prepareBigBlowItemTest(context.getSource())))
                        .then(Commands.literal("kirin_ready")
                                .executes(context -> prepareKirinItemTest(context.getSource())))
                        .then(Commands.literal("great_fireball_ready")
                                .executes(context -> prepareGreatFireballItemTest(context.getSource())))
                        .then(Commands.literal("fire_annihilation_ready")
                                .executes(context -> prepareFireAnnihilationItemTest(context.getSource())))
                        .then(Commands.literal("hiding_in_ash_ready")
                                .executes(context -> prepareHidingInAshItemTest(context.getSource())))
                        .then(Commands.literal("great_flame_ready")
                                .executes(context -> prepareGreatFlameItemTest(context.getSource())))
                        .then(Commands.literal("hiding_in_rock_ready")
                                .executes(context -> prepareHidingInRockItemTest(context.getSource())))
                        .then(Commands.literal("earth_spears_ready")
                                .executes(context -> prepareEarthSpearsItemTest(context.getSource())))
                        .then(Commands.literal("earth_wall_ready")
                                .executes(context -> prepareEarthWallItemTest(context.getSource())))
                        .then(Commands.literal("swamp_pit_ready")
                                .executes(context -> prepareSwampPitItemTest(context.getSource())))
                        .then(Commands.literal("earth_sandwich_ready")
                                .executes(context -> prepareEarthSandwichItemTest(context.getSource())))
                        .then(Commands.literal("hiding_in_mist_ready")
                                .executes(context -> prepareHidingInMistItemTest(context.getSource())))
                        .then(Commands.literal("water_stream_ready")
                                .executes(context -> prepareWaterStreamItemTest(context.getSource())))
                        .then(Commands.literal("water_dragon_ready")
                                .executes(context -> prepareWaterDragonItemTest(context.getSource())))
                        .then(Commands.literal("water_prison_ready")
                                .executes(context -> prepareWaterPrisonItemTest(context.getSource())))
                        .then(Commands.literal("water_shark_ready")
                                .executes(context -> prepareWaterSharkItemTest(context.getSource())))
                        .then(Commands.literal("water_shockwave_ready")
                                .executes(context -> prepareWaterShockwaveItemTest(context.getSource()))))
                .then(Commands.literal("m5_tools")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5ToolsState(context.getSource())))
                        .then(Commands.literal("ninja_tools_ready")
                                .executes(context -> prepareNinjaToolsTest(context.getSource())))
                        .then(Commands.literal("special_weapons_ready")
                                .executes(context -> prepareSpecialWeaponsTest(context.getSource())))
                        .then(Commands.literal("folding_fan_ready")
                                .executes(context -> prepareFoldingFanTest(context.getSource())))
                        .then(Commands.literal("kiba_blades_ready")
                                .executes(context -> prepareKibaBladesTest(context.getSource())))
                        .then(Commands.literal("kusanagi_ready")
                                .executes(context -> prepareKusanagiTest(context.getSource())))
                        .then(Commands.literal("hiramekarei_ready")
                                .executes(context -> prepareHiramekareiTest(context.getSource())))
                        .then(Commands.literal("asura_cannon_ready")
                                .executes(context -> prepareAsuraCannonTest(context.getSource())))
                        .then(Commands.literal("kamui_shuriken_ready")
                                .executes(context -> prepareKamuiShurikenTest(context.getSource())))
                        .then(Commands.literal("nuibari_ready")
                                .executes(context -> prepareNuibariTest(context.getSource()))))
                .then(Commands.literal("m5_armor")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5ArmorState(context.getSource())))
                        .then(Commands.literal("basic_armor_ready")
                                .executes(context -> prepareBasicArmorTest(context.getSource()))))
                .then(Commands.literal("m5_weapons")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5WeaponsState(context.getSource())))
                        .then(Commands.literal("basic_melee_ready")
                                .executes(context -> prepareBasicMeleeWeaponsTest(context.getSource())))
                        .then(Commands.literal("ice_senbon_ready")
                                .executes(context -> prepareIceSenbonTest(context.getSource())))
                        .then(Commands.literal("kagutsuchi_ready")
                                .executes(context -> prepareKagutsuchiSwordTest(context.getSource()))))
                .then(Commands.literal("m5_kagutsuchi")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5KagutsuchiState(context.getSource())))
                        .then(Commands.literal("kagutsuchi_ready")
                                .executes(context -> prepareKagutsuchiSwordTest(context.getSource()))))
                .then(Commands.literal("m5_medical_scroll")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5MedicalScrollState(context.getSource())))
                        .then(Commands.literal("medical_scroll_ready")
                                .executes(context -> prepareMedicalScrollTest(context.getSource())))
                        .then(Commands.literal("tenseigan_evolution_ready")
                                .executes(context -> prepareTenseiganEvolutionTest(context.getSource())))
                        .then(Commands.literal("tenseigan_evolution_finish")
                                .executes(context -> finishTenseiganEvolutionTest(context.getSource()))))
                .then(Commands.literal("m5_consumables")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5ConsumablesState(context.getSource())))
                        .then(Commands.literal("consumables_ready")
                                .executes(context -> prepareConsumablesAndCurrencyTest(context.getSource()))))
                .then(Commands.literal("m5_eight_gates")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5EightGatesState(context.getSource())))
                        .then(Commands.literal("eight_gates_ready")
                                .executes(context -> prepareEightGatesTest(context.getSource())))
                        .then(Commands.literal("asakujaku_ready")
                                .executes(context -> prepareEightGatesAsakujakuTest(context.getSource())))
                        .then(Commands.literal("hirudora_ready")
                                .executes(context -> prepareEightGatesHirudoraTest(context.getSource())))
                        .then(Commands.literal("sekizo_ready")
                                .executes(context -> prepareEightGatesSekizoTest(context.getSource())))
                        .then(Commands.literal("night_guy_ready")
                                .executes(context -> prepareEightGatesNightGuyTest(context.getSource()))))
                .then(Commands.literal("m6_entities")
                        .then(Commands.literal("state")
                                .executes(context -> reportM6EntitiesState(context.getSource())))
                        .then(Commands.literal("crow_ready")
                                .executes(context -> prepareCrowEntityTest(context.getSource())))
                        .then(Commands.literal("alt_cam_ready")
                                .executes(context -> prepareAltCamViewEntityTest(context.getSource())))
                        .then(Commands.literal("buddha_arm_ready")
                                .executes(context -> prepareBuddhaArmEntityTest(context.getSource())))
                        .then(Commands.literal("ground_shock_ready")
                                .executes(context -> prepareGroundShockEntityTest(context.getSource())))
                        .then(Commands.literal("earth_blocks_ready")
                                .executes(context -> prepareEarthBlocksEntityTest(context.getSource())))
                        .then(Commands.literal("spike_ready")
                                .executes(context -> prepareSpikeEntityTest(context.getSource())))
                        .then(Commands.literal("eight_trigrams_ready")
                                .executes(context -> prepareEightTrigramsEntityTest(context.getSource())))
                        .then(Commands.literal("hakkesho_keiten_ready")
                                .executes(context -> prepareHakkeshoKeitenEntityTest(context.getSource())))
                        .then(Commands.literal("special_effect_lines_ready")
                                .executes(context -> prepareSpecialEffectLinesTest(context.getSource())))
                        .then(Commands.literal("special_effect_sphere_ready")
                                .executes(context -> prepareSpecialEffectSphereTest(context.getSource())))
                        .then(Commands.literal("tail_beast_ball_ready")
                                .executes(context -> prepareTailBeastBallEntityTest(context.getSource())))
                        .then(Commands.literal("tailed_beasts_ready")
                                .executes(context -> prepareTailedBeastsEntityTest(context.getSource())))
                        .then(Commands.literal("yasaka_magatama_ready")
                                .executes(context -> prepareYasakaMagatamaEntityTest(context.getSource())))
                        .then(Commands.literal("preta_shield_ready")
                                .executes(context -> preparePretaShieldEntityTest(context.getSource())))
                        .then(Commands.literal("ninja_mobs_ready")
                                .executes(context -> prepareNinjaMobsEntityTest(context.getSource())))
                        .then(Commands.literal("named_ninja_ready")
                                .executes(context -> prepareNamedNinjaEntityTest(context.getSource())))
                        .then(Commands.literal("kankuro_ready")
                                .executes(context -> prepareKankuroEntityTest(context.getSource())))
                        .then(Commands.literal("tenten_ready")
                                .executes(context -> prepareTentenEntityTest(context.getSource())))
                        .then(Commands.literal("iruka_ready")
                                .executes(context -> prepareIrukaEntityTest(context.getSource())))
                        .then(Commands.literal("sakura_ready")
                                .executes(context -> prepareSakuraEntityTest(context.getSource())))
                        .then(Commands.literal("might_guy_ready")
                                .executes(context -> prepareMightGuyEntityTest(context.getSource())))
                        .then(Commands.literal("giant_dog_ready")
                                .executes(context -> prepareGiantDog2hEntityTest(context.getSource())))
                        .then(Commands.literal("jinchuriki_clone_ready")
                                .executes(context -> prepareJinchurikiCloneEntityTest(context.getSource())))
                        .then(Commands.literal("purple_dragon_ready")
                                .executes(context -> preparePurpleDragonEntityTest(context.getSource())))
                        .then(Commands.literal("gedo_statue_ready")
                                .executes(context -> prepareGedoStatueEntityTest(context.getSource())))
                        .then(Commands.literal("gedo_ten_tails_ready")
                                .executes(context -> prepareGedoTenTailsActivationTest(context.getSource())))
                        .then(Commands.literal("ten_tails_chain_selftest")
                                .executes(context -> runTenTailsServerChainSelfTest(context.getSource())))
                        .then(Commands.literal("snake_8_heads_ready")
                                .executes(context -> prepareSnake8HeadsEntityTest(context.getSource())))
                        .then(Commands.literal("ten_tails_ready")
                                .executes(context -> prepareTenTailsEntityTest(context.getSource())))
                        .then(Commands.literal("chibaku_tensei_ready")
                                .executes(context -> prepareChibakuTenseiEntityTest(context.getSource())))
                        .then(Commands.literal("king_of_hell_ready")
                                .executes(context -> prepareKingOfHellEntityTest(context.getSource())))
                        .then(Commands.literal("susanoo_skeleton_ready")
                                .executes(context -> prepareSusanooSkeletonEntityTest(context.getSource())))
                        .then(Commands.literal("susanoo_clothed_ready")
                                .executes(context -> prepareSusanooClothedEntityTest(context.getSource(), false)))
                        .then(Commands.literal("susanoo_clothed_full_ready")
                                .executes(context -> prepareSusanooClothedEntityTest(context.getSource(), true)))
                        .then(Commands.literal("susanoo_winged_ready")
                                .executes(context -> prepareSusanooWingedEntityTest(context.getSource()))))
                .then(Commands.literal("m5_advanced_jutsu")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5AdvancedNatureState(context.getSource())))
                        .then(Commands.literal("advanced_jutsu_ready")
                                .executes(context -> prepareAdvancedNatureJutsuTest(context.getSource())))
                        .then(Commands.literal("yooton_magma_ready")
                                .executes(context -> prepareYootonMagmaBallTest(context.getSource())))
                        .then(Commands.literal("yooton_melting_ready")
                                .executes(context -> prepareYootonMeltingJutsuTest(context.getSource())))
                        .then(Commands.literal("yooton_lava_chakra_ready")
                                .executes(context -> prepareYootonLavaChakraModeTest(context.getSource())))
                        .then(Commands.literal("yoton_biggerme_ready")
                                .executes(context -> prepareYotonBiggerMeTest(context.getSource())))
                        .then(Commands.literal("yoton_sealing_ready")
                                .executes(context -> prepareYotonSealingTest(context.getSource())))
                        .then(Commands.literal("bakuton_jiraiken_ready")
                                .executes(context -> prepareBakutonJiraikenTest(context.getSource())))
                        .then(Commands.literal("bakuton_explosive_clay_ready")
                                .executes(context -> prepareBakutonExplosiveClayTest(context.getSource())))
                        .then(Commands.literal("bakuton_explosive_clone_ready")
                                .executes(context -> prepareBakutonExplosiveCloneTest(context.getSource())))
                        .then(Commands.literal("futton_mist_ready")
                                .executes(context -> prepareFuttonMistTest(context.getSource())))
                        .then(Commands.literal("futton_unrivaled_strength_ready")
                                .executes(context -> prepareFuttonUnrivaledStrengthTest(context.getSource())))
                        .then(Commands.literal("hyoton_ice_spike_ready")
                                .executes(context -> prepareHyotonIceSpikeTest(context.getSource())))
                        .then(Commands.literal("hyoton_ice_spear_ready")
                                .executes(context -> prepareHyotonIceSpearTest(context.getSource())))
                        .then(Commands.literal("hyoton_ice_dome_ready")
                                .executes(context -> prepareHyotonIceDomeTest(context.getSource())))
                        .then(Commands.literal("hyoton_ice_prison_ready")
                                .executes(context -> prepareHyotonIcePrisonTest(context.getSource())))
                        .then(Commands.literal("mokuton_wood_burial_ready")
                                .executes(context -> prepareMokutonWoodBurialTest(context.getSource())))
                        .then(Commands.literal("mokuton_wood_prison_ready")
                                .executes(context -> prepareMokutonWoodPrisonTest(context.getSource())))
                        .then(Commands.literal("mokuton_wood_house_ready")
                                .executes(context -> prepareMokutonWoodHouseTest(context.getSource())))
                        .then(Commands.literal("mokuton_wood_golem_ready")
                                .executes(context -> prepareMokutonWoodGolemTest(context.getSource())))
                        .then(Commands.literal("mokuton_wood_arm_ready")
                                .executes(context -> prepareMokutonWoodArmTest(context.getSource())))
                        .then(Commands.literal("jinton_beam_ready")
                                .executes(context -> prepareJintonBeamTest(context.getSource())))
                        .then(Commands.literal("jinton_cube_ready")
                                .executes(context -> prepareJintonCubeTest(context.getSource())))
                        .then(Commands.literal("ranton_cloud_ready")
                                .executes(context -> prepareRantonCloudTest(context.getSource())))
                        .then(Commands.literal("ranton_laser_circus_ready")
                                .executes(context -> prepareRantonLaserCircusTest(context.getSource())))
                        .then(Commands.literal("shakuton_scorch_orb_ready")
                                .executes(context -> prepareShakutonScorchOrbTest(context.getSource())))
                        .then(Commands.literal("shakuton_scorch_kill_ready")
                                .executes(context -> prepareShakutonScorchKillTest(context.getSource())))
                        .then(Commands.literal("shakuton_scorch_blast_ready")
                                .executes(context -> prepareShakutonScorchBlastTest(context.getSource())))
                        .then(Commands.literal("jiton_sand_shield_ready")
                                .executes(context -> prepareJitonSandShieldTest(context.getSource())))
                        .then(Commands.literal("jiton_sand_bullet_ready")
                                .executes(context -> prepareJitonSandBulletTest(context.getSource())))
                        .then(Commands.literal("jiton_sand_bind_ready")
                                .executes(context -> prepareJitonSandBindTest(context.getSource())))
                        .then(Commands.literal("jiton_sand_levitation_ready")
                                .executes(context -> prepareJitonSandLevitationTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_larch_dance_ready")
                                .executes(context -> prepareShikotsumyakuLarchDanceTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_willow_dance_ready")
                                .executes(context -> prepareShikotsumyakuWillowDanceTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_camellia_dance_ready")
                                .executes(context -> prepareShikotsumyakuCamelliaDanceTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_finger_bone_ready")
                                .executes(context -> prepareShikotsumyakuFingerBoneTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_clematis_flower_ready")
                                .executes(context -> prepareShikotsumyakuClematisFlowerTest(context.getSource())))
                        .then(Commands.literal("shikotsumyaku_bracken_dance_ready")
                                .executes(context -> prepareShikotsumyakuBrackenDanceTest(context.getSource())))
                        .then(Commands.literal("kekkei_mora_eighty_gods_ready")
                                .executes(context -> prepareKekkeiMoraEightyGodsTest(context.getSource())))
                        .then(Commands.literal("kekkei_mora_yomotsu_hirasaka_ready")
                                .executes(context -> prepareKekkeiMoraYomotsuHirasakaTest(context.getSource())))
                        .then(Commands.literal("kekkei_mora_expansive_tsb_ready")
                                .executes(context -> prepareKekkeiMoraExpansiveTruthSeekingBallTest(context.getSource())))
                        .then(Commands.literal("kekkei_mora_ash_bones_ready")
                                .executes(context -> prepareKekkeiMoraAshBonesTest(context.getSource()))))
                .then(Commands.literal("m5_six_path")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5SixPathState(context.getSource())))
                        .then(Commands.literal("six_path_ready")
                                .executes(context -> prepareSixPathSenjutsuTest(context.getSource())))
                        .then(Commands.literal("six_path_shoot_ready")
                                .executes(context -> prepareSixPathShootTest(context.getSource())))
                        .then(Commands.literal("six_path_shield_ready")
                                .executes(context -> prepareSixPathShieldTest(context.getSource())))
                        .then(Commands.literal("six_path_inton_raiha_ready")
                                .executes(context -> prepareSixPathIntonRaihaTest(context.getSource())))
                        .then(Commands.literal("six_path_ranton_koga_ready")
                                .executes(context -> prepareSixPathRantonKogaTest(context.getSource())))
                        .then(Commands.literal("six_path_rasenshuriken_ready")
                                .executes(context -> prepareSixPathRasenshurikenTest(context.getSource())))
                        .then(Commands.literal("six_path_outer_path_ready")
                                .executes(context -> prepareSixPathOuterPathTest(context.getSource())))
                        .then(Commands.literal("rinnegan_helmet_tick_ready")
                                .executes(context -> prepareRinneganHelmetTickTest(context.getSource())))
                        .then(Commands.literal("tenseigan_helmet_tick_ready")
                                .executes(context -> prepareTenseiganHelmetTickTest(context.getSource()))))
                .then(Commands.literal("m5_tenseigan")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5TenseiganState(context.getSource())))
                        .then(Commands.literal("tenseigan_chakra_ready")
                                .executes(context -> prepareTenseiganChakraModeTest(context.getSource())))
                        .then(Commands.literal("tenseigan_orbs_ready")
                                .executes(context -> prepareTenseiganOrbsTest(context.getSource())))
                        .then(Commands.literal("tenseigan_silver_ready")
                                .executes(context -> prepareTenseiganSilverTest(context.getSource())))
                        .then(Commands.literal("tenseigan_gold_ready")
                                .executes(context -> prepareTenseiganGoldTest(context.getSource()))))
                .then(Commands.literal("m5_summoning")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5SummoningState(context.getSource())))
                        .then(Commands.literal("summoning_contract_ready")
                                .executes(context -> prepareSummoningContractTest(context.getSource())))
                        .then(Commands.literal("summon_toad_ready")
                                .executes(context -> prepareSummoningContractTest(context.getSource(), SummoningContractItem.SUMMON_TOAD)))
                        .then(Commands.literal("summon_snake_ready")
                                .executes(context -> prepareSummoningContractTest(context.getSource(), SummoningContractItem.SUMMON_SNAKE)))
                        .then(Commands.literal("summon_slug_ready")
                                .executes(context -> prepareSummoningContractTest(context.getSource(), SummoningContractItem.SUMMON_SLUG)))
                        .then(Commands.literal("spawn_gamabunta")
                                .executes(context -> spawnGamabuntaSummon(context.getSource())))
                        .then(Commands.literal("spawn_manda")
                                .executes(context -> spawnMandaSummon(context.getSource()))))
                .then(Commands.literal("m5_team")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5TeamScrollState(context.getSource())))
                        .then(Commands.literal("team_scroll_ready")
                                .executes(context -> prepareTeamScrollTest(context.getSource())))
                        .then(Commands.literal("join")
                                .executes(context -> joinTeamScroll(context.getSource())))
                        .then(Commands.literal("leave")
                                .executes(context -> leaveTeamScroll(context.getSource()))))
                .then(Commands.literal("m5_misc")
                        .then(Commands.literal("state")
                                .executes(context -> reportM5MiscState(context.getSource())))
                        .then(Commands.literal("zzz_ready")
                                .executes(context -> prepareZzzTest(context.getSource()))))
                .then(Commands.literal("rasengan_ready")
                        .executes(context -> prepareRasenganItemTest(context.getSource())))
                .then(Commands.literal("senjutsu_ready")
                        .executes(context -> prepareSenjutsuRasenganItemTest(context.getSource())))
                .then(Commands.literal("vars")
                        .then(Commands.literal("get")
                                .executes(context -> getVariables(context.getSource())))
                        .then(Commands.literal("set_battle_xp")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 100000.0D))
                                        .executes(context -> setBattleExperience(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "value")))))
                        .then(Commands.literal("add_battle_xp")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(-100000.0D, 100000.0D))
                                        .executes(context -> addBattleExperience(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "value"))))))
                .then(Commands.literal("chakra")
                        .then(Commands.literal("get")
                                .executes(context -> getChakra(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D))
                                        .executes(context -> setChakra(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "value")))))
                        .then(Commands.literal("consume")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(context -> consumeChakra(
                                                context.getSource(),
                                                DoubleArgumentType.getDouble(context, "value")))))
                        .then(Commands.literal("fill")
                                .executes(context -> fillChakra(context.getSource()))))
                .then(Commands.literal("saveddata")
                        .then(Commands.literal("load_all")
                                .executes(context -> loadSavedData(context.getSource())))
                        .then(Commands.literal("summary")
                                .executes(context -> savedDataSummary(context.getSource())))
                        .then(Commands.literal("tailed_beasts")
                                .then(Commands.literal("list")
                                        .executes(context -> listSavedTailedBeasts(context.getSource())))
                                .then(Commands.literal("gedo_list")
                                        .executes(context -> listGedoSealedTails(context.getSource())))
                                .then(Commands.literal("gedo_clear")
                                        .executes(context -> clearGedoSealedTails(context.getSource())))
                                .then(Commands.literal("gedo_mark")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, 9))
                                                .then(Commands.argument("sealed", BoolArgumentType.bool())
                                                        .executes(context -> setGedoSealedTail(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "tails"),
                                                                BoolArgumentType.getBool(context, "sealed"))))))
                                .then(Commands.literal("save_nearby")
                                        .executes(context -> saveNearbyTailedBeasts(context.getSource())))
                                .then(Commands.literal("restore")
                                        .executes(context -> restoreSavedTailedBeasts(context.getSource())))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                                .executes(context -> clearSavedTailedBeast(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails"))))))
                        .then(Commands.literal("jinchuriki")
                                .then(Commands.literal("list")
                                        .executes(context -> listJinchurikiAssignments(context.getSource())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                                .executes(context -> setCurrentPlayerAsJinchuriki(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails")))))
                                .then(Commands.literal("fuuin_ready")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                                .executes(context -> prepareFuuinSealingTest(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails")))))
                                .then(Commands.literal("cloak_ready")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, 9))
                                                .executes(context -> prepareBijuCloakTest(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails")))))
                                .then(Commands.literal("toggle_cloak")
                                        .executes(context -> toggleCurrentPlayerBijuCloak(context.getSource())))
                                .then(Commands.literal("increase_cloak")
                                        .executes(context -> increaseCurrentPlayerBijuCloak(context.getSource())))
                                .then(Commands.literal("powerincrease_cloak_ready")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, 9))
                                                .executes(context -> preparePowerIncreaseBijuCloakTest(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails")))))
                                .then(Commands.literal("powerincrease_cloak")
                                        .executes(context -> powerIncreaseCurrentPlayerBijuCloak(context.getSource())))
                                .then(Commands.literal("special2_release")
                                        .executes(context -> triggerSpecialJutsu2Release(context.getSource())))
                                .then(Commands.literal("special3_ball_ready")
                                        .executes(context -> prepareSpecialJutsu3TailBeastBallTest(context.getSource())))
                                .then(Commands.literal("natural_grant_try")
                                        .executes(context -> tryNaturalJinchurikiGrant(context.getSource(), false)))
                                .then(Commands.literal("natural_grant_force")
                                        .executes(context -> tryNaturalJinchurikiGrant(context.getSource(), true)))
                                .then(Commands.literal("revoke_player")
                                        .executes(context -> revokeCurrentPlayerJinchuriki(context.getSource())))
                                .then(Commands.literal("revoke_tail")
                                        .then(Commands.argument("tails", IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                                .executes(context -> revokeTailJinchuriki(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "tails")))))
                                .then(Commands.literal("revoke_all")
                                        .executes(context -> revokeAllJinchuriki(context.getSource())))
                                .then(Commands.literal("resolve")
                                        .executes(context -> resolveCurrentPlayerJinchuriki(context.getSource())))))
                .then(Commands.literal("particle")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(context -> spawnParticle(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        20))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                        .executes(context -> spawnParticle(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("syncdata")
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .executes(context -> getSyncData(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "key")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .executes(context -> removeSyncData(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "key")))))
                        .then(Commands.literal("set_int")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setSyncDataInt(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "key"),
                                                        IntegerArgumentType.getInteger(context, "value"))))))
                        .then(Commands.literal("set_double")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(context -> setSyncDataDouble(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "key"),
                                                        DoubleArgumentType.getDouble(context, "value"))))))
                        .then(Commands.literal("set_bool")
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> setSyncDataBoolean(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "key"),
                                                        BoolArgumentType.getBool(context, "value"))))))));
    }

    private static int sendPing(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long nonce = System.nanoTime();
        NetworkHandler.sendToPlayer(player, new PortingPongMessage(nonce));
        source.sendSuccess(() -> Component.literal("Narutomod porting pong packet sent: " + nonce), false);
        return 1;
    }

    private static int reportM8WorldState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        boolean mudConfigured = hasMudConfiguredFeature(player.server);
        boolean mudPlaced = hasMudPlacedFeature(player.server);
        int naturalSpawnEntries = countNaturalSpawnBiomeEntries(player.server);
        StructureLoadSummary structures = structureLoadSummary(player.serverLevel());
        boolean platformReady = kamuiLevel != null
                && kamuiLevel.getBlockState(platform).is(ModBlocks.KAMUIBLOCK.get())
                && kamuiLevel.isEmptyBlock(platform.above())
                && kamuiLevel.isEmptyBlock(platform.above(2));
        source.sendSuccess(() -> Component.literal("M8 world state: kamui_dimension=" + KamuiDimension.ID
                + ", loaded=" + (kamuiLevel != null)
                + ", current_dimension=" + player.level().dimension().location()
                + ", in_kamui=" + KamuiDimension.isKamui(player.level())
                + ", platform_center=" + describeBlockPos(platform)
                + ", platform_ready=" + platformReady
                + ", mud_lake_configured=" + mudConfigured
                + ", mud_lake_placed=" + mudPlaced
                + ", natural_spawn_biome_entries=" + naturalSpawnEntries
                + ", structures_loaded=" + structures.loaded() + "/" + M8_STRUCTURE_TEMPLATE_IDS.size()
                + ", kamuidimension_stack=" + findItem(player, ModItems.KAMUIDIMENSION.get()).getCount()
                + ", return=" + KamuiDimension.describeReturn(player)), false);
        return 1;
    }

    private static int runM8WorldValidationSuite(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        MinecraftServer server = player.server;
        StructureLoadSummary structures = structureLoadSummary(level);
        NaturalSpawnSummary naturalSpawns = naturalSpawnSummary(server);
        boolean mudConfigured = hasMudConfiguredFeature(server);
        boolean mudPlaced = hasMudPlacedFeature(server);
        ServerLevel kamuiLevel = KamuiDimension.level(server);
        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        boolean platformChanged = kamuiLevel != null && KamuiDimension.ensureEntryPlatform(kamuiLevel, platform);
        boolean kamuiReady = kamuiLevel != null
                && kamuiLevel.getBlockState(platform).is(ModBlocks.KAMUIBLOCK.get())
                && kamuiLevel.isEmptyBlock(platform.above())
                && kamuiLevel.isEmptyBlock(platform.above(2));
        int pendingBefore = SpecialEvent.pendingCount(server);
        VillagePoiTestScene scene = prepareVillagePoiTestScene(player, VillagePoiHelper.MIN_NATURAL_VILLAGE_POIS,
                VillagePoiHelper.MIN_NATURAL_VILLAGERS, true);
        VillagePoiHelper.Context naturalVillage = VillagePoiHelper.findNaturalMightGuyContext(level, scene.center()).orElse(null);
        VillagePoiHelper.Context siegeVillage = VillagePoiHelper.findSiegeContext(level, scene.center(), 36).orElse(null);
        ProcedureUtils.grantAdvancement(player, "narutomod:ninjaachievement", false);
        MightGuyEntity.NaturalVillageSpawnResult guySpawn = MightGuyEntity.tryNaturalVillageSpawn(player, true);
        boolean mightGuyRouteOk = guySpawn.spawned()
                || ("nearby_might_guy".equals(guySpawn.reason()) && guySpawn.nearbyMightGuys() > 0);
        boolean ok = structures.missing().isEmpty()
                && mudConfigured
                && mudPlaced
                && naturalSpawns.missing().isEmpty()
                && naturalSpawns.mismatched().isEmpty()
                && kamuiReady
                && naturalVillage != null
                && siegeVillage != null
                && mightGuyRouteOk;
        int pendingAfter = SpecialEvent.pendingCount(server);
        source.sendSuccess(() -> Component.literal("M8 validation suite: ok=" + ok
                + ", structures=" + structures.loaded() + "/" + M8_STRUCTURE_TEMPLATE_IDS.size()
                + ", missing_structures=" + (structures.missing().isEmpty() ? "none" : String.join(",", structures.missing()))
                + ", mud_lake_configured=" + mudConfigured
                + ", mud_lake_placed=" + mudPlaced
                + ", natural_spawn_entries=" + naturalSpawns.biomeEntries()
                + ", natural_missing=" + (naturalSpawns.missing().isEmpty() ? "none" : String.join(",", naturalSpawns.missing()))
                + ", natural_mismatched=" + (naturalSpawns.mismatched().isEmpty() ? "none" : String.join(",", naturalSpawns.mismatched()))
                + ", kamui_loaded=" + (kamuiLevel != null)
                + ", kamui_platform_ready=" + kamuiReady
                + ", kamui_platform_changed=" + platformChanged
                + ", poi_scene_center=" + describeBlockPos(scene.center())
                + ", poi_blocks=" + scene.poiBlocks()
                + ", villagers=" + scene.villagers()
                + ", natural_village=" + (naturalVillage != null)
                + ", siege_village=" + (siegeVillage != null)
                + ", might_guy_route=" + guySpawn.reason()
                + ", might_guy_spawn_pos=" + describeNullableBlockPos(guySpawn.spawnPos())
                + ", pending_events=" + pendingBefore + "->" + pendingAfter
                + ", destructive_structure_placement=not_run"), false);
        return ok ? 1 : 0;
    }

    private static int probeM8Structures(CommandSourceStack source) {
        StructureLoadSummary structures = structureLoadSummary(source.getLevel());
        source.sendSuccess(() -> Component.literal("M8 structures: loaded=" + structures.loaded()
                + "/" + M8_STRUCTURE_TEMPLATE_IDS.size()
                + ", missing=" + (structures.missing().isEmpty() ? "none" : String.join(",", structures.missing()))), false);
        return structures.missing().isEmpty() ? 1 : 0;
    }

    private static StructureLoadSummary structureLoadSummary(ServerLevel level) {
        List<String> missing = new ArrayList<>();
        for (String id : M8_STRUCTURE_TEMPLATE_IDS) {
            if (level.getStructureManager().get(NarutomodMod.location(id)).isEmpty()) {
                missing.add(id);
            }
        }
        return new StructureLoadSummary(M8_STRUCTURE_TEMPLATE_IDS.size() - missing.size(), missing);
    }

    private static int placeGodTreeProbe(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(player.getX() + horizontal.x() * 18.0D, player.getY(), player.getZ() + horizontal.z() * 18.0D);
        Map<String, Object> dependencies = Map.of(
                "world", level,
                "x", origin.getX(),
                "y", origin.getY(),
                "z", origin.getZ());
        ProcedureSpawnGodTree.GodTreePlacementResult result = ProcedureSpawnGodTree.executeProcedure(dependencies);
        if (result.missing().size() > 0) {
            source.sendFailure(Component.literal("M8 God Tree placement failed: missing=" + String.join(",", result.missing())));
            return 0;
        }
        if (result.outOfBounds()) {
            source.sendFailure(Component.literal("M8 God Tree placement failed: highest_y=" + result.highestY()
                    + ", max_y=" + result.maxY()
                    + ", move to lower ground first."));
            return 0;
        }
        if (!result.success()) {
            source.sendFailure(Component.literal("M8 God Tree placement failed: reason=" + result.failureReason()
                    + ", origin=" + describeBlockPos(origin)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("M8 God Tree placement: placed=" + result.placed()
                + "/" + result.expected()
                + ", origin=" + describeBlockPos(origin)
                + ", duplicate_world_tree_15_preserved=true"
                + ", legacy_executeProcedure=true"), false);
        return 1;
    }

    private static int placeMeteorStructureProbe(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        horizontal = horizontal.normalize();
        BlockPos target = BlockPos.containing(player.getX() + horizontal.x() * 18.0D, player.getY(), player.getZ() + horizontal.z() * 18.0D);
        int satellitesBefore = countNearbyChibakuSatellites(player);
        ProcedureMeteorStrike.MeteorStrikeResult result = ProcedureMeteorStrike.strike(level, player, target);
        int satellitesAfter = countNearbyChibakuSatellites(player);
        if (!result.success()) {
            if (!result.missingTemplate().isEmpty()) {
                source.sendFailure(Component.literal("M8 meteor placement failed: missing_template=" + result.missingTemplate()));
            } else if ("out_of_bounds".equals(result.failureReason())) {
                source.sendFailure(Component.literal("M8 meteor placement failed: spawn_to=" + describeBlockPos(result.spawnTo())
                        + ", highest_y=" + result.highestY()
                        + ", max_y=" + result.maxY()
                        + ", move to lower ground first."));
            } else {
                source.sendFailure(Component.literal("M8 meteor placement failed: reason=" + result.failureReason()
                        + ", spawn_to=" + describeNullableBlockPos(result.spawnTo())
                        + ", captured_blocks=" + result.capturedBlocks()));
            }
            return 0;
        }
        source.sendSuccess(() -> Component.literal("M8 meteor placement: target=" + describeBlockPos(target)
                + ", spawn_to=" + describeBlockPos(result.spawnTo())
                + ", reused_satellite=" + result.reusedSatellite()
                + ", spawned_satellite=" + result.spawnedSatellite()
                + ", satellite_id=" + result.satelliteId()
                + ", captured_blocks=" + result.capturedBlocks()
                + ", nearby_satellites=" + satellitesBefore + "->" + satellitesAfter), false);
        return 1;
    }

    private static int probeMudLakeWorldgen(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        boolean configured = hasMudConfiguredFeature(server);
        boolean placed = hasMudPlacedFeature(server);
        source.sendSuccess(() -> Component.literal("M8 mud lake worldgen: configured_feature=" + configured
                + ", placed_feature=" + placed
                + ", biome_modifier=narutomod:add_mud_lake"
                + ", biomes=minecraft:swamp"
                + ", chance=1/10"), false);
        return configured && placed ? 1 : 0;
    }

    private static int probeNaturalSpawns(CommandSourceStack source) {
        NaturalSpawnSummary summary = naturalSpawnSummary(source.getServer());
        source.sendSuccess(() -> Component.literal("M8 natural spawns: configured_entities="
                + (naturalSpawnTargets().size() - summary.missing().size()) + "/" + naturalSpawnTargets().size()
                + ", biome_entries=" + summary.biomeEntries()
                + ", ambient_entities=4"
                + ", monster_entities=4"
                + ", missing=" + (summary.missing().isEmpty() ? "none" : String.join(",", summary.missing()))
                + ", mismatched=" + (summary.mismatched().isEmpty() ? "none" : String.join(",", summary.mismatched()))
                + ", dungeon_replacement=natural_monster_biome_modifiers"), false);
        return summary.missing().isEmpty() && summary.mismatched().isEmpty() ? 1 : 0;
    }

    private static int countNaturalSpawnBiomeEntries(MinecraftServer server) {
        return naturalSpawnSummary(server).biomeEntries();
    }

    private static NaturalSpawnSummary naturalSpawnSummary(MinecraftServer server) {
        var biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        int biomeEntries = 0;
        List<String> missing = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        for (NaturalSpawnTarget target : naturalSpawnTargets()) {
            EntityType<?> type = target.type().get();
            int targetEntries = 0;
            boolean targetMismatch = false;
            for (var biomeEntry : biomeRegistry.entrySet()) {
                for (var spawner : biomeEntry.getValue().getMobSettings().getMobs(target.category()).unwrap()) {
                    if (spawner.type == type) {
                        targetEntries++;
                        if (spawner.getWeight().asInt() != target.weight()
                                || spawner.minCount != target.minCount()
                                || spawner.maxCount != target.maxCount()) {
                            targetMismatch = true;
                        }
                        break;
                    }
                }
            }
            if (targetEntries == 0) {
                missing.add(target.name());
            } else if (targetMismatch) {
                mismatched.add(target.name() + "[" + target.weight() + "/" + target.minCount() + "-" + target.maxCount() + "]");
            }
            biomeEntries += targetEntries;
        }
        return new NaturalSpawnSummary(biomeEntries, missing, mismatched);
    }

    private static List<NaturalSpawnTarget> naturalSpawnTargets() {
        return List.of(
                new NaturalSpawnTarget("iruka_sensei", ModEntityTypes.IRUKA_SENSEI, MobCategory.AMBIENT, 1, 1, 1),
                new NaturalSpawnTarget("sakura_haruno", ModEntityTypes.SAKURA_HARUNO, MobCategory.AMBIENT, 1, 1, 1),
                new NaturalSpawnTarget("tenten", ModEntityTypes.TENTEN, MobCategory.AMBIENT, 1, 1, 1),
                new NaturalSpawnTarget("mightguy", ModEntityTypes.MIGHTGUY, MobCategory.AMBIENT, 20, 1, 1),
                new NaturalSpawnTarget("itachi", ModEntityTypes.ITACHI, MobCategory.MONSTER, 1, 1, 1),
                new NaturalSpawnTarget("kisame_hoshigaki", ModEntityTypes.KISAME_HOSHIGAKI, MobCategory.MONSTER, 1, 1, 1),
                new NaturalSpawnTarget("zabuza_momochi", ModEntityTypes.ZABUZA_MOMOCHI, MobCategory.MONSTER, 20, 4, 4),
                new NaturalSpawnTarget("whitezetsu", ModEntityTypes.WHITEZETSU, MobCategory.MONSTER, 10, 1, 1));
    }

    private static boolean hasMudConfiguredFeature(MinecraftServer server) {
        return server.registryAccess()
                .registry(Registries.CONFIGURED_FEATURE)
                .map(registry -> registry.containsKey(MUD_LAKE_ID))
                .orElse(false);
    }

    private static boolean hasMudPlacedFeature(MinecraftServer server) {
        return server.registryAccess()
                .registry(Registries.PLACED_FEATURE)
                .map(registry -> registry.containsKey(MUD_LAKE_ID))
                .orElse(false);
    }

    private record NaturalSpawnTarget(String name, RegistryObject<? extends EntityType<?>> type, MobCategory category,
            int weight, int minCount, int maxCount) {
    }

    private record NaturalSpawnSummary(int biomeEntries, List<String> missing, List<String> mismatched) {
    }

    private record StructureLoadSummary(int loaded, List<String> missing) {
    }

    private record VillagePoiTestScene(BlockPos center, int poiBlocks, int villagers, int preparedBlocks) {
    }

    private static int reportSpecialEventsState(CommandSourceStack source) {
        String summary = SpecialEvent.debugSummary(source.getServer());
        int dataKeys = NarutomodSavedData.specialEvents(source.getServer()).data().size();
        source.sendSuccess(() -> Component.literal("M8 special events: " + summary
                + ", saveddata_keys=" + dataKeys), false);
        return 1;
    }

    private static int runSpecialEventsTick(CommandSourceStack source) {
        int executed = SpecialEvent.executeDueEvents(source.getServer());
        source.sendSuccess(() -> Component.literal("M8 special events tick: executed=" + executed
                + ", " + SpecialEvent.debugSummary(source.getServer())), false);
        return executed > 0 ? 1 : 0;
    }

    private static int prepareDelayedSpawnTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        if (target == null) {
            source.sendFailure(Component.literal("Could not create delayed spawn target."));
            return 0;
        }
        Vec3 targetPos = player.position().add(player.getLookAngle().normalize().scale(3.0D));
        target.moveTo(targetPos.x(), player.getY(), targetPos.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M8 delayed spawn target"));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        long start = player.server.overworld().getGameTime() + 20L;
        int before = SpecialEvent.pendingCount(player.server);
        boolean scheduled = SpecialEvent.setDelayedSpawnEvent(level, target, 0, 0, 0, start);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 delayed spawn test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", target_pos=" + describeBlockPos(BlockPos.containing(targetPos.x(), player.getY(), targetPos.z()))
                + ", start=" + start
                + ", use /narutoport m8_world special_events_state or wait 1 second."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareVanillaExplosionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 targetPos = player.position().add(player.getLookAngle().normalize().scale(5.0D)).add(0.0D, 1.0D, 0.0D);
        long start = player.server.overworld().getGameTime() + 20L;
        int before = SpecialEvent.pendingCount(player.server);
        boolean scheduled = SpecialEvent.setVanillaExplosionEvent(
                level,
                null,
                BlockPos.containing(targetPos).getX(),
                BlockPos.containing(targetPos).getY(),
                BlockPos.containing(targetPos).getZ(),
                1.5F,
                start,
                false,
                false);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 vanilla explosion test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", target_pos=" + describeBlockPos(BlockPos.containing(targetPos))
                + ", strength=1.5"
                + ", damages_terrain=false"
                + ", start=" + start
                + ", use /narutoport m8_world special_events_state or wait 1 second."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareSetBlocksTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.position().add(player.getLookAngle().normalize().scale(5.0D))).above();
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                blocks.put(center.offset(x, y, 0), Blocks.YELLOW_WOOL.defaultBlockState());
            }
        }
        long start = player.server.overworld().getGameTime() + 20L;
        int lifespan = 80;
        int before = SpecialEvent.pendingCount(player.server);
        boolean scheduled = SpecialEvent.setBlocksEvent(level, blocks, start, lifespan, true, true);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 set blocks test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", center=" + describeBlockPos(center)
                + ", blocks=" + blocks.size()
                + ", start=" + start
                + ", lifespan=" + lifespan
                + ", use /narutoport m8_world special_events_state or wait for placement/removal."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareCylindricalExplosionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.position().add(player.getLookAngle().normalize().scale(6.0D))).above();
        int radius = 3;
        int yBottom = center.getY();
        int yTop = Math.min(level.getMaxBuildHeight() - 1, yBottom + 5);
        int prepared = 0;
        for (int y = yBottom; y <= yTop; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x + z * z <= 6) {
                        BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
                            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                            prepared++;
                        }
                    }
                }
            }
        }
        long start = player.server.overworld().getGameTime() + 20L;
        int before = SpecialEvent.pendingCount(player.server);
        int preparedBlocks = prepared;
        boolean scheduled = SpecialEvent.setMassExplosionEvent(
                level,
                center.getX(),
                yTop,
                center.getZ(),
                yBottom,
                radius,
                start,
                true,
                true,
                true);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 cylindrical explosion test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", center=" + describeBlockPos(center)
                + ", radius=" + radius
                + ", y_bottom=" + yBottom
                + ", y_top=" + yTop
                + ", prepared_blocks=" + preparedBlocks
                + ", use /narutoport m8_world special_events_state or wait for the staged column blast."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareSphericalExplosionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.position().add(player.getLookAngle().normalize().scale(6.0D))).above();
        int radius = 4;
        int prepared = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x + y * y + z * z <= 7) {
                        BlockPos pos = center.offset(x, y, z);
                        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
                            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                            prepared++;
                        }
                    }
                }
            }
        }
        long start = player.server.overworld().getGameTime() + 20L;
        int before = SpecialEvent.pendingCount(player.server);
        int preparedBlocks = prepared;
        boolean scheduled = SpecialEvent.setSphericalExplosionEvent(
                level,
                null,
                center.getX(),
                center.getY(),
                center.getZ(),
                radius,
                start,
                false,
                0.0F,
                true,
                true,
                true);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 spherical explosion test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", center=" + describeBlockPos(center)
                + ", radius=" + radius
                + ", prepared_blocks=" + preparedBlocks
                + ", use /narutoport m8_world special_events_state or wait for the staged blast."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareMeteorShowerTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.position().add(player.getLookAngle().normalize().scale(10.0D)));
        long start = player.server.overworld().getGameTime() + 20L;
        int radius = 6;
        int strikeInterval = 8;
        int duration = 80;
        int before = SpecialEvent.pendingCount(player.server);
        boolean scheduled = SpecialEvent.setMeteorShowerEvent(
                level,
                center.getX(),
                center.getY(),
                center.getZ(),
                start,
                radius,
                strikeInterval,
                duration);
        int after = SpecialEvent.pendingCount(player.server);
        int meteorY = Math.min(250, level.getMaxBuildHeight() - 1);
        source.sendSuccess(() -> Component.literal("M8 meteor shower test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", center=" + describeBlockPos(center)
                + ", meteor_y=" + meteorY
                + ", radius=" + radius
                + ", strike_interval=" + strikeInterval
                + ", duration=" + duration
                + ", use /narutoport m8_world special_events_state or wait for falling meteors."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareVillageSiegeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        VillagePoiTestScene scene = prepareVillagePoiTestScene(player, 6, 6, false);
        BlockPos center = scene.center();
        VillagePoiHelper.Context village = VillagePoiHelper.findSiegeContext(level, center, 36).orElse(null);
        level.setDayTime(14000L);
        long start = player.server.overworld().getGameTime() + 20L;
        int radius = 36;
        int spawnInterval = 20;
        int before = SpecialEvent.pendingCount(player.server);
        boolean scheduled = SpecialEvent.setVillageSiegeEvent(level, center.getX(), center.getY(), center.getZ(), start,
                radius, null, spawnInterval);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 village siege test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", center=" + describeBlockPos(center)
                + ", radius=" + radius
                + ", spawn_interval=" + spawnInterval
                + ", poi_blocks=" + scene.poiBlocks()
                + ", villagers=" + scene.villagers()
                + ", village_context=" + (village != null)
                + ", meeting_pois=" + (village == null ? 0 : village.meetingPoiCount())
                + ", village_pois=" + (village == null ? 0 : village.villagePoiCount())
                + ", day_time=14000"
                + ", use /narutoport m8_world special_events_state or wait for siege mobs."), false);
        return scheduled ? 1 : 0;
    }

    private static int prepareMightGuySiegeQuestTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        BlockPos center = BlockPos.containing(
                player.getX() + look.x() * 7.0D,
                player.getY() - 1.0D,
                player.getZ() + look.z() * 7.0D);
        int y = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 4, center.getY()));
        center = new BlockPos(center.getX(), y, center.getZ());

        int preparedBlocks = 0;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos floor = center.offset(x, 0, z);
                level.setBlock(floor, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                preparedBlocks++;
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        BlockPos bellPos = center.above();
        level.setBlock(bellPos, Blocks.BELL.defaultBlockState(), 3);

        int villagers = 0;
        int[][] offsets = {
                {2, 0},
                {-2, 0},
                {0, 2},
                {0, -2},
                {2, 2},
                {-2, -2}
        };
        for (int[] offset : offsets) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) {
                continue;
            }
            villager.moveTo(center.getX() + offset[0] + 0.5D, center.getY() + 1.0D,
                    center.getZ() + offset[1] + 0.5D, player.getYRot(), 0.0F);
            villager.setPersistenceRequired();
            villager.setCustomName(Component.literal("M8 siege villager"));
            level.addFreshEntity(villager);
            villagers++;
        }

        MightGuyEntity guy = ModEntityTypes.MIGHTGUY.get().create(level);
        if (guy == null) {
            source.sendFailure(Component.literal("Could not create Might Guy siege quest entity."));
            return 0;
        }
        guy.moveTo(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 3.5D,
                player.getYRot() + 180.0F, 0.0F);
        guy.setPersistenceRequired();
        guy.setCustomName(Component.literal("M8 Might Guy siege quest"));
        guy.setCustomNameVisible(true);
        level.addFreshEntity(guy);

        level.setDayTime(14000L);
        player.getPersistentData().putInt(MightGuyEntity.VILLAGE_REPUTATION_TAG, 0);
        int spawnedVillagers = villagers;
        int preparedFloorBlocks = preparedBlocks;
        source.sendSuccess(() -> Component.literal("M8 Might Guy siege quest ready: spawned=true"
                + ", villagers=" + spawnedVillagers
                + ", bell=" + describeBlockPos(bellPos)
                + ", prepared_blocks=" + preparedFloorBlocks
                + ", day_time=14000"
                + ", reputation_reset=0"
                + ", right-click Might Guy to schedule the village defense."), false);
        return 1;
    }

    private static int prepareMightGuyNaturalSpawnTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VillagePoiTestScene scene = prepareVillagePoiTestScene(player, VillagePoiHelper.MIN_NATURAL_VILLAGE_POIS,
                VillagePoiHelper.MIN_NATURAL_VILLAGERS, true);
        ProcedureUtils.grantAdvancement(player, "narutomod:ninjaachievement", false);
        MightGuyEntity.NaturalVillageSpawnResult result = MightGuyEntity.tryNaturalVillageSpawn(player, true);
        source.sendSuccess(() -> Component.literal("M8 Might Guy natural spawn ready: spawned=" + result.spawned()
                + ", reason=" + result.reason()
                + ", force_spawn=true"
                + ", center=" + describeBlockPos(scene.center())
                + ", village_center=" + describeBlockPos(result.villageCenter())
                + ", spawn_pos=" + describeNullableBlockPos(result.spawnPos())
                + ", prepared_poi_blocks=" + scene.poiBlocks()
                + ", prepared_villagers=" + scene.villagers()
                + ", detected_village_pois=" + result.villagePoiCount()
                + ", detected_meeting_pois=" + result.meetingPoiCount()
                + ", detected_villagers=" + result.villagerCount()
                + ", nearby_might_guys=" + result.nearbyMightGuys()
                + ", ninjaachievement=" + ProcedureUtils.advancementAchieved(player, "narutomod:ninjaachievement")), false);
        return result.spawned() ? 1 : 0;
    }

    private static VillagePoiTestScene prepareVillagePoiTestScene(ServerPlayer player, int poiCount, int villagerCount,
            boolean large) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        BlockPos center = BlockPos.containing(
                player.getX() + look.x() * (large ? 12.0D : 7.0D),
                player.getY() - 1.0D,
                player.getZ() + look.z() * (large ? 12.0D : 7.0D));
        int y = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 5, center.getY()));
        center = new BlockPos(center.getX(), y, center.getZ());

        int halfSize = large ? 10 : 5;
        int preparedBlocks = 0;
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                BlockPos floor = center.offset(x, 0, z);
                level.setBlock(floor, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                preparedBlocks++;
                for (int dy = 1; dy <= 4; dy++) {
                    level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        BlockState[] poiStates = {
                Blocks.BELL.defaultBlockState(),
                Blocks.COMPOSTER.defaultBlockState(),
                Blocks.BARREL.defaultBlockState(),
                Blocks.BLAST_FURNACE.defaultBlockState(),
                Blocks.SMOKER.defaultBlockState(),
                Blocks.CARTOGRAPHY_TABLE.defaultBlockState(),
                Blocks.FLETCHING_TABLE.defaultBlockState(),
                Blocks.GRINDSTONE.defaultBlockState(),
                Blocks.LECTERN.defaultBlockState(),
                Blocks.SMITHING_TABLE.defaultBlockState(),
                Blocks.STONECUTTER.defaultBlockState(),
                Blocks.BREWING_STAND.defaultBlockState(),
                Blocks.CAULDRON.defaultBlockState(),
                Blocks.LOOM.defaultBlockState()
        };
        int poiPlaced = 0;
        int columns = large ? 5 : 3;
        int spacing = large ? 4 : 3;
        int zRows = Math.max(1, (poiCount + columns - 1) / columns);
        for (int i = 0; i < poiCount; i++) {
            int gridX = i % columns;
            int gridZ = i / columns;
            int x = (gridX - columns / 2) * spacing;
            int z = (gridZ - zRows / 2) * spacing;
            BlockPos poiPos = center.offset(x, 1, z);
            level.setBlock(poiPos, poiStates[i % poiStates.length], 3);
            poiPlaced++;
        }

        int villagers = 0;
        for (int i = 0; i < villagerCount; i++) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) {
                continue;
            }
            int x = (i % 5 - 2) * 2;
            int z = (i / 5 - 1) * 2;
            villager.moveTo(center.getX() + x + 0.5D, center.getY() + 1.0D,
                    center.getZ() + z + 0.5D, player.getYRot(), 0.0F);
            villager.setPersistenceRequired();
            villager.setCustomName(Component.literal("M8 POI village villager"));
            level.addFreshEntity(villager);
            villagers++;
        }
        return new VillagePoiTestScene(center, poiPlaced, villagers, preparedBlocks);
    }

    private static int prepareDelayedCallbackTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(player.position().add(player.getLookAngle().normalize().scale(4.0D))).above();
        long start = player.server.overworld().getGameTime() + 20L;
        int ticks = 60;
        int before = SpecialEvent.pendingCount(player.server);
        int scheduled = SpecialEvent.setSmokeBombCallbackEvents(level, center.getX(), center.getY(), center.getZ(), start, ticks);
        int after = SpecialEvent.pendingCount(player.server);
        source.sendSuccess(() -> Component.literal("M8 delayed callback test: scheduled=" + scheduled
                + ", pending_before=" + before
                + ", pending_after=" + after
                + ", callback_id=681"
                + ", center=" + describeBlockPos(center)
                + ", ticks=" + ticks
                + ", use /narutoport m8_world special_events_state or wait for smoke callbacks."), false);
        return scheduled > 0 ? 1 : 0;
    }

    private static int prepareKamuiDimensionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        ItemStack igniter = getOrGiveItem(player, ModItems.KAMUIDIMENSION.get());
        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        boolean changed = KamuiDimension.ensureEntryPlatform(kamuiLevel, platform);
        source.sendSuccess(() -> Component.literal("Kamui dimension test ready: kamuidimension=" + igniter.getCount()
                + ", dimension=" + KamuiDimension.ID
                + ", platform_center=" + describeBlockPos(platform)
                + ", platform_changed=" + changed
                + ", use /narutoport m8_world kamui_toggle or right-click the igniter to enter/return."), false);
        return 1;
    }

    private static int toggleKamuiDimension(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!KamuiDimension.toggle(player)) {
            source.sendFailure(Component.literal("Kamui dimension toggle failed."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Kamui dimension toggled: current_dimension="
                + player.level().dimension().location()
                + ", return=" + KamuiDimension.describeReturn(player)), false);
        return 1;
    }

    private static int probeKamuiTerrain(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        int radius = 24;
        int maxForcedChunks = 64;
        ChunkPos center = new ChunkPos(KamuiDimension.platformCenter(player.getX(), player.getZ()));
        int planned = 0;
        int forced = 0;
        int chunksWithBlocks = 0;
        int blockCount = 0;
        String firstPlanned = "none";
        String firstGenerated = "none";
        long seed = kamuiLevel.getSeed();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (KamuiChunkGenerator.plannedIslands(seed, x, z).isEmpty()) {
                    continue;
                }
                planned++;
                if ("none".equals(firstPlanned)) {
                    firstPlanned = x + "," + z;
                }
                if (forced >= maxForcedChunks) {
                    continue;
                }
                kamuiLevel.getChunk(x, z);
                forced++;
                int blocks = countKamuiBlocksInChunk(kamuiLevel, x, z);
                if (blocks > 0) {
                    chunksWithBlocks++;
                    blockCount += blocks;
                    if ("none".equals(firstGenerated)) {
                        firstGenerated = x + "," + z;
                    }
                }
            }
        }
        int checkedChunks = (radius * 2 + 1) * (radius * 2 + 1);
        int plannedCount = planned;
        int forcedCount = forced;
        int generatedCount = chunksWithBlocks;
        int generatedBlocks = blockCount;
        String plannedChunk = firstPlanned;
        String generatedChunk = firstGenerated;
        source.sendSuccess(() -> Component.literal("Kamui terrain probe: checked_chunks=" + checkedChunks
                + ", planned_island_chunks=" + plannedCount
                + ", forced_planned_chunks=" + forcedCount
                + ", generated_chunks_with_kamui=" + generatedCount
                + ", generated_kamui_blocks_y1_70=" + generatedBlocks
                + ", first_planned_chunk=" + plannedChunk
                + ", first_generated_chunk=" + generatedChunk), false);
        return generatedCount > 0 ? 1 : 0;
    }

    private static int countKamuiBlocksInChunk(ServerLevel level, int chunkX, int chunkZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int minY = Math.max(1, level.getMinBuildHeight());
        int maxY = Math.min(70, level.getMaxBuildHeight() - 1);
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (level.getBlockState(pos.set(x, y, z)).is(ModBlocks.KAMUIBLOCK.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int runKamuiTransferSmokeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel sourceLevel = player.serverLevel();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        Vec3 targetPos = player.position().add(player.getLookAngle().normalize().scale(3.0D));
        ArmorStand target = EntityType.ARMOR_STAND.create(sourceLevel);
        if (target == null) {
            source.sendFailure(Component.literal("Could not create Kamui transfer target."));
            return 0;
        }
        target.moveTo(targetPos.x(), player.getY(), targetPos.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M8 Kamui transfer target"));
        target.setCustomNameVisible(true);
        sourceLevel.addFreshEntity(target);
        boolean entityMoved = KamuiDimension.enterEntity(target);

        BlockPos sourceBlock = player.blockPosition().offset(2, 0, 0);
        sourceLevel.setBlock(sourceBlock, Blocks.STONE.defaultBlockState(), 3);
        int blockDropsMoved = KamuiDimension.transferBlockDropsToKamui(sourceLevel, sourceBlock);

        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        int transferredTargets = countKamuiTransferTargets(kamuiLevel, platform);
        int transferredItems = countKamuiTransferItems(kamuiLevel, platform);
        source.sendSuccess(() -> Component.literal("Kamui transfer smoke: entity_moved=" + entityMoved
                + ", block_drops_moved=" + blockDropsMoved
                + ", platform_center=" + describeBlockPos(platform)
                + ", kamui_transfer_targets=" + transferredTargets
                + ", kamui_nearby_items=" + transferredItems), false);
        return entityMoved && blockDropsMoved > 0 ? 1 : 0;
    }

    private static int countKamuiTransferTargets(ServerLevel kamuiLevel, BlockPos center) {
        AABB area = AABB.ofSize(new Vec3(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D),
                16.0D, 16.0D, 16.0D);
        return kamuiLevel.getEntitiesOfClass(ArmorStand.class, area,
                entity -> entity.hasCustomName() && "M8 Kamui transfer target".equals(entity.getCustomName().getString())).size();
    }

    private static int countKamuiTransferItems(ServerLevel kamuiLevel, BlockPos center) {
        AABB area = AABB.ofSize(new Vec3(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D),
                16.0D, 16.0D, 16.0D);
        return kamuiLevel.getEntitiesOfClass(ItemEntity.class, area).size();
    }

    private static int prepareObitoKamuiTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareKamuiHelmetTest(source, ModItems.MANGEKYOSHARINGANOBITOHELMET.get(), "Obito Kamui");
    }

    private static int prepareEternalKamuiTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareKamuiHelmetTest(source, ModItems.MANGEKYOSHARINGANETERNALHELMET.get(), "Eternal Mangekyo Kamui");
    }

    private static int prepareObitoKamuiBlockTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        prepareJutsuResourcePool(player, 50000.0D);
        player.setItemSlot(EquipmentSlot.HEAD, ownedEye(ModItems.MANGEKYOSHARINGANOBITOHELMET.get(), player.getUUID()));
        BlockPos targetBlock = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().normalize().scale(4.0D)));
        player.serverLevel().setBlock(targetBlock, Blocks.STONE.defaultBlockState(), 3);
        BlockPos platform = KamuiDimension.platformCenter(targetBlock.getX(), targetBlock.getZ());
        KamuiDimension.ensureEntryPlatform(kamuiLevel, platform);
        int beforeItems = countKamuiTransferItems(kamuiLevel, platform);
        source.sendSuccess(() -> Component.literal("Obito Kamui block test ready: helmet=narutomod:mangekyosharinganobitohelmet"
                + ", target_block=" + describeBlockPos(targetBlock)
                + ", kamui_platform=" + describeBlockPos(platform)
                + ", kamui_nearby_items_before=" + beforeItems
                + ", use /narutoport m8_world obito_teleport_press then /narutoport m8_world obito_key1_release while aiming at the block."), false);
        return 1;
    }

    private static int prepareObitoKamuiReverseBlockTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        prepareJutsuResourcePool(player, 50000.0D);
        player.setItemSlot(EquipmentSlot.HEAD, ownedEye(ModItems.MANGEKYOSHARINGANOBITOHELMET.get(), player.getUUID()));
        if (!KamuiDimension.isKamui(player.level()) && !KamuiDimension.enter(player)) {
            source.sendFailure(Component.literal("Could not enter Kamui dimension for reverse block test."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        BlockPos targetBlock = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().normalize().scale(4.0D)));
        level.setBlock(targetBlock, Blocks.STONE.defaultBlockState(), 3);
        source.sendSuccess(() -> Component.literal("Obito Kamui reverse block test ready: in_kamui=" + KamuiDimension.isKamui(player.level())
                + ", target_block=" + describeBlockPos(targetBlock)
                + ", return=" + KamuiDimension.describeReturn(player)
                + ", use /narutoport m8_world obito_teleport_press then /narutoport m8_world obito_key1_release while aiming at the block."), false);
        return 1;
    }

    private static int prepareObitoKamuiGrabTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        prepareJutsuResourcePool(player, 50000.0D);
        player.setItemSlot(EquipmentSlot.HEAD, ownedEye(ModItems.MANGEKYOSHARINGANOBITOHELMET.get(), player.getUUID()));
        if (!KamuiDimension.isKamui(player.level()) && !KamuiDimension.enter(player)) {
            source.sendFailure(Component.literal("Could not enter Kamui dimension for grab test."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        if (target == null) {
            source.sendFailure(Component.literal("Could not create Obito Kamui grab target."));
            return 0;
        }
        Vec3 targetPos = player.position().add(player.getLookAngle().normalize().scale(3.0D));
        target.moveTo(targetPos.x(), player.getY(), targetPos.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M8 Obito Kamui grab target"));
        target.setCustomNameVisible(true);
        level.addFreshEntity(target);
        source.sendSuccess(() -> Component.literal("Obito Kamui grab test ready: in_kamui=" + KamuiDimension.isKamui(player.level())
                + ", target_ready=true"
                + ", target_pos=" + describeBlockPos(target.blockPosition())
                + ", use /narutoport m8_world obito_grab_press then /narutoport m8_world obito_key1_release."), false);
        return 1;
    }

    private static int prepareKamuiHelmetTest(CommandSourceStack source, Item helmetItem, String label) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        if (kamuiLevel == null) {
            source.sendFailure(Component.literal("Kamui dimension is not loaded: " + KamuiDimension.ID));
            return 0;
        }
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack helmet = ownedEye(helmetItem, player.getUUID());
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        KamuiDimension.ensureEntryPlatform(kamuiLevel, platform);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        boolean spawnedTarget = false;
        if (target != null) {
            Vec3 targetPos = player.position().add(player.getLookAngle().normalize().scale(4.0D));
            target.moveTo(targetPos.x(), player.getY(), targetPos.z(), player.getYRot() + 180.0F, 0.0F);
            target.setCustomName(Component.literal("M8 " + label + " target"));
            target.setCustomNameVisible(true);
            player.serverLevel().addFreshEntity(target);
            spawnedTarget = true;
        }
        boolean targetReady = spawnedTarget;
        String registryName = BuiltInRegistries.ITEM.getKey(helmetItem).toString();
        source.sendSuccess(() -> Component.literal(label + " test ready: helmet=" + registryName
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()
                + ", target_ready=" + targetReady
                + ", use R for intangible, shift+R for teleport, or debug press/release commands."), false);
        return 1;
    }

    private static int reportObitoKamuiState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ServerLevel kamuiLevel = KamuiDimension.level(player.server);
        BlockPos platform = KamuiDimension.platformCenter(player.getX(), player.getZ());
        source.sendSuccess(() -> Component.literal("Obito/Eternal Kamui state: kamui_helmet=" + ObitoKamuiHandler.hasKamuiHelmet(player)
                + ", obito=" + ObitoKamuiHandler.hasObitoHelmet(player)
                + ", eternal=" + head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get())
                + ", owner=" + ProcedureUtils.isOriginalOwner(player, head)
                + ", blinded=" + ObitoMangekyoHelmetItem.isBlinded(head)
                + ", intangible=" + ObitoKamuiHandler.isIntangible(player)
                + ", teleport=" + ObitoKamuiHandler.isTeleporting(player)
                + ", grab=" + ObitoKamuiHandler.isGrabbing(player)
                + ", grabbed=" + ObitoKamuiHandler.grabbedDescription(player)
                + ", timer=" + ObitoKamuiHandler.timer(player)
                + ", no_physics=" + player.noPhysics
                + ", no_clip_flag=" + NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.NO_CLIP_FLAG)
                + ", mayfly=" + player.getAbilities().mayfly
                + ", head_damage=" + head.getDamageValue() + "/" + head.getMaxDamage()
                + ", intangible_cost=" + ObitoMangekyoHelmetItem.getIntangibleChakraUsage(player)
                + ", teleport_cost=" + ObitoMangekyoHelmetItem.getTeleportChakraUsage(player)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()
                + ", kamui_nearby_items=" + (kamuiLevel != null ? countKamuiTransferItems(kamuiLevel, platform) : -1)
                + ", dimension=" + player.level().dimension().location()
                + ", return=" + KamuiDimension.describeReturn(player)), false);
        return 1;
    }

    private static int debugObitoKamuiPress(CommandSourceStack source, boolean teleport) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean handled = ObitoKamuiHandler.pressForDebug(player, teleport);
        source.sendSuccess(() -> Component.literal("Obito Kamui key1 press: handled=" + handled
                + ", mode=" + (teleport ? "teleport" : "intangible")
                + ", intangible=" + ObitoKamuiHandler.isIntangible(player)
                + ", teleport=" + ObitoKamuiHandler.isTeleporting(player)
                + ", timer=" + ObitoKamuiHandler.timer(player)), false);
        return handled ? 1 : 0;
    }

    private static int debugObitoKamuiGrabPress(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean handled = ObitoKamuiHandler.grabForDebug(player, true);
        source.sendSuccess(() -> Component.literal("Obito Kamui grab press: handled=" + handled
                + ", grab=" + ObitoKamuiHandler.isGrabbing(player)
                + ", grabbed=" + ObitoKamuiHandler.grabbedDescription(player)
                + ", dimension=" + player.level().dimension().location()), false);
        return handled ? 1 : 0;
    }

    private static int debugObitoKamuiRelease(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean handled = ObitoKamuiHandler.releaseForDebug(player);
        source.sendSuccess(() -> Component.literal("Obito Kamui key1 release: handled=" + handled
                + ", intangible=" + ObitoKamuiHandler.isIntangible(player)
                + ", teleport=" + ObitoKamuiHandler.isTeleporting(player)
                + ", grab=" + ObitoKamuiHandler.isGrabbing(player)
                + ", grabbed=" + ObitoKamuiHandler.grabbedDescription(player)
                + ", timer=" + ObitoKamuiHandler.timer(player)
                + ", dimension=" + player.level().dimension().location()
                + ", return=" + KamuiDimension.describeReturn(player)), false);
        return handled ? 1 : 0;
    }

    private static int setObitoKamuiBlindness(CommandSourceStack source, boolean blinded) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!ObitoKamuiHandler.hasKamuiHelmet(player)) {
            source.sendFailure(Component.literal("Wear an Obito or Eternal Mangekyo Kamui helmet first. Use /narutoport m8_world obito_ready or eternal_kamui_ready."));
            return 0;
        }
        head.getOrCreateTag().putBoolean(ObitoMangekyoHelmetItem.SHARINGAN_BLINDED_TAG, blinded);
        if (blinded) {
            ObitoKamuiHandler.releaseForDebug(player);
        }
        source.sendSuccess(() -> Component.literal("Obito Kamui blindness set: blinded=" + blinded
                + ", intangible=" + ObitoKamuiHandler.isIntangible(player)
                + ", teleport=" + ObitoKamuiHandler.isTeleporting(player)), false);
        return 1;
    }

    private static String describeBlockPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String describeNullableBlockPos(BlockPos pos) {
        return pos == null ? "<none>" : describeBlockPos(pos);
    }

    private static int spawnDummy(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return spawnEntityOfType(source, ModRegistries.PORTING_DUMMY.get(), "narutomod:porting_dummy");
    }

    private static int spawnEntity(CommandSourceStack source, String typeName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (isRasenganName(typeName)) {
            return spawnAttachedRasengan(source, 1.0F);
        }
        RegistryObject<EntityType<PortingDummyEntity>> entityType = ModEntityTypes.dummyTypes().stream()
                .filter(candidate -> candidate.getId().getPath().equals(typeName) || candidate.getId().toString().equals(typeName))
                .findFirst()
                .orElse(null);
        if (entityType == null) {
            source.sendFailure(Component.literal("Unknown narutomod porting entity type: " + typeName));
            return 0;
        }
        return spawnEntityOfType(source, entityType.get(), entityType.getId().toString());
    }

    private static boolean isRasenganName(String typeName) {
        return ModEntityTypes.RASENGAN.getId().getPath().equals(typeName) || ModEntityTypes.RASENGAN.getId().toString().equals(typeName);
    }

    private static int spawnEntityOfType(CommandSourceStack source, EntityType<PortingDummyEntity> entityType, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        PortingDummyEntity entity = entityType.create(level);
        if (entity == null) {
            source.sendFailure(Component.literal("Could not create porting entity: " + name));
            return 0;
        }

        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        level.addFreshEntity(entity);
        source.sendSuccess(() -> Component.literal("Spawned " + name), false);
        return 1;
    }

    private static int spawnAttachedRasengan(CommandSourceStack source, float fullScale) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RasenganEntity entity = ModEntityTypes.RASENGAN.get().create(player.serverLevel());
        if (entity == null) {
            source.sendFailure(Component.literal("Could not create porting entity: " + ModEntityTypes.RASENGAN.getId()));
            return 0;
        }

        entity.configureAttached(player, fullScale);
        player.serverLevel().addFreshEntity(entity);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_RASENGAN_START.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        source.sendSuccess(() -> Component.literal("Spawned " + ModEntityTypes.RASENGAN.getId() + " attached to player"), false);
        return 1;
    }

    private static int prepareCrowEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int spawned = 0;
        for (int i = 0; i < 5; i++) {
            CrowEntity crow = ModEntityTypes.CROW.get().create(level);
            if (crow == null) {
                continue;
            }
            double angle = Math.toRadians(player.getYRot()) + i * Math.PI * 0.4D;
            double radius = 2.0D + i * 0.35D;
            crow.moveTo(
                    player.getX() + Math.sin(angle) * radius,
                    player.getY() + 2.0D + i * 0.15D,
                    player.getZ() + Math.cos(angle) * radius,
                    player.getYRot(),
                    0.0F);
            level.addFreshEntity(crow);
            spawned++;
        }
        int count = countNearbyCrows(player);
        int spawnedCount = spawned;
        source.sendSuccess(() -> Component.literal("M6 crow test spawned " + spawnedCount + " crows; nearby_crows=" + count), false);
        return spawned;
    }

    private static int prepareAltCamViewEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AltCamViewEntity entity = AltCamViewEntity.spawnDebug(player);
        int count = countNearbyAltCamViews(player);
        source.sendSuccess(() -> Component.literal("M6 alt camera view test spawned=" + (entity != null)
                + "; nearby_alt_cam_views=" + count), false);
        return entity != null ? 1 : 0;
    }

    private static int prepareBuddhaArmEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean spawned = BuddhaArmEntity.spawnFrom(player, 100.0F, true);
        int count = countNearbyBuddhaArms(player);
        source.sendSuccess(() -> Component.literal("M6 buddha arm test spawned=" + spawned
                + "; nearby_buddha_arms=" + count
                + ", grow=true"), false);
        return spawned ? 1 : 0;
    }

    private static int prepareGroundShockEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean spawned = GroundShockEntity.spawnFrom(player.serverLevel(), BlockPos.containing(player.getX(), player.getY(), player.getZ()), 10);
        int count = countNearbyGroundShocks(player);
        source.sendSuccess(() -> Component.literal("M6 ground shock test spawned=" + spawned + "; nearby_ground_shock=" + count), false);
        return spawned ? 1 : 0;
    }

    private static int prepareEarthBlocksEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EarthBlocksEntity entity = EarthBlocksEntity.spawnDebug(player);
        int count = countNearbyEarthBlocks(player);
        source.sendSuccess(() -> Component.literal("M6 earth blocks test spawned=" + (entity != null)
                + "; nearby_earth_blocks=" + count
                + ", block_count=" + (entity != null ? entity.getBlockCount() : 0)), false);
        return entity != null ? 1 : 0;
    }

    private static int prepareSpikeEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean spawned = SpikeEntity.spawnFrom(player, 0xC0A0E1FF, 1.0F, 1.2F);
        int count = countNearbySpikes(player);
        source.sendSuccess(() -> Component.literal("M6 spike test spawned=" + spawned + "; nearby_spikes=" + count), false);
        return spawned ? 1 : 0;
    }

    private static int prepareEightTrigramsEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 1600.0D);
        if (player.experienceLevel < 20) {
            player.giveExperienceLevels(20 - player.experienceLevel);
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_HAKKEROKUJUUYONSHOU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        boolean spawned = EightTrigramsEntity.spawnFrom(player);
        int count = countNearbyEightTrigrams(player);
        source.sendSuccess(() -> Component.literal("M6 eight trigrams test spawned=" + spawned
                + "; nearby_eight_trigrams=" + count
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)), false);
        return spawned ? 1 : 0;
    }

    private static int prepareHakkeshoKeitenEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 1600.0D);
        if (player.experienceLevel < 30) {
            player.giveExperienceLevels(30 - player.experienceLevel);
        }
        ItemStack byakugan = ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID());
        player.setItemSlot(EquipmentSlot.HEAD, byakugan);
        boolean spawned = HakkeshoKeitenEntity.spawnFrom(player);
        int count = countNearbyHakkeshoKeiten(player);
        source.sendSuccess(() -> Component.literal("M6 hakkesho keiten test spawned=" + spawned
                + "; nearby_hakkesho_keiten=" + count
                + ", scale=" + Math.max(Math.min(player.experienceLevel / 30.0F, 10.0F), 1.0F)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)), false);
        return spawned ? 1 : 0;
    }

    private static int prepareSpecialEffectLinesTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SpecialEffectEntity entity = SpecialEffectEntity.spawn(player.serverLevel(),
                SpecialEffectEntity.EffectType.ROTATING_LINES_COLOR_END,
                0xFFFF00,
                30.0F,
                120,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ());
        int count = countNearbySpecialEffects(player);
        source.sendSuccess(() -> Component.literal("M6 special effect lines spawned=" + (entity != null)
                + "; nearby_special_effects=" + count), false);
        return entity != null ? 1 : 0;
    }

    private static int prepareSpecialEffectSphereTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SpecialEffectEntity entity = SpecialEffectEntity.spawn(player.serverLevel(),
                SpecialEffectEntity.EffectType.EXPANDING_SPHERES_FADE_TO_BLACK,
                0xFFFFFF,
                32.0F,
                80,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ());
        int count = countNearbySpecialEffects(player);
        source.sendSuccess(() -> Component.literal("M6 special effect sphere spawned=" + (entity != null)
                + "; nearby_special_effects=" + count), false);
        return entity != null ? 1 : 0;
    }

    private static int prepareTailBeastBallEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean spawned = TailBeastBallEntity.spawnFrom(player, 2.5F, 140.0F);
        int count = countNearbyTailBeastBalls(player);
        source.sendSuccess(() -> Component.literal("M6 tail beast ball test spawned=" + spawned
                + "; nearby_tail_beast_balls=" + count
                + ", buildup_ticks=100"), false);
        return spawned ? 1 : 0;
    }

    private static int prepareTailedBeastsEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        DirectionBasis basis = DirectionBasis.from(player);
        List<TailedBeastEntity> beasts = new ArrayList<>();
        int index = 0;
        for (TailedBeastEntity.Variant variant : TailedBeastEntity.Variant.values()) {
            int row = index / 3;
            int col = index % 3;
            Vec3 point = basis.point(34.0D + row * 38.0D, (col - 1) * 36.0D, 0.0D);
            TailedBeastEntity beast = TailedBeastEntity.spawnAt(level, variant, point, player.getYRot() + 180.0F, player);
            if (beast != null) {
                beast.setAngerLevel(2);
                beasts.add(beast);
            }
            index++;
        }
        Zombie target = EntityType.ZOMBIE.create(level);
        if (target != null) {
            Vec3 targetPoint = basis.point(134.0D, 0.0D, 0.0D);
            target.moveTo(targetPoint.x(), player.getY(), targetPoint.z(), player.getYRot() + 180.0F, 0.0F);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("M6 Tailed Beast ranged target"));
            target.setCustomNameVisible(true);
            level.addFreshEntity(target);
            if (!beasts.isEmpty()) {
                beasts.get(beasts.size() - 1).performRangedAttack(target, 0.0F);
            }
        }
        int beastCount = countNearbyTailedBeasts(player);
        int ballCount = countNearbyTailBeastBalls(player);
        int spawnedCount = beasts.size();
        source.sendSuccess(() -> Component.literal("M6 Tailed Beasts test spawned=" + spawnedCount
                + ", nearby_tailed_beasts=" + beastCount
                + ", nearby_tail_beast_balls=" + ballCount
                + ", variants=one_tail..nine_tails"
                + ", ranged_smoke=" + (!beasts.isEmpty())), false);
        return spawnedCount;
    }

    private static int prepareYasakaMagatamaEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean spawned = YasakaMagatamaEntity.spawnFrom(player, 0xD88A32FF, 1.25F, true);
        int count = countNearbyYasakaMagatama(player);
        source.sendSuccess(() -> Component.literal("M6 yasaka magatama test spawned=" + spawned
                + "; nearby_yasaka_magatama=" + count
                + ", flight_timeout_ticks=100"), false);
        return spawned ? 1 : 0;
    }

    private static int preparePretaShieldEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 1600.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.RINNEGANHELMET.get()));
        boolean spawned = PretaShieldEntity.spawnFrom(player);
        int count = countNearbyPretaShields(player);
        source.sendSuccess(() -> Component.literal("M6 preta shield test spawned=" + spawned
                + "; nearby_preta_shields=" + count
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return spawned ? 1 : 0;
    }

    private static int prepareNinjaMobsEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<RegistryObject<EntityType<NinjaMobEntity>>> types = List.of(
                ModEntityTypes.ANBU,
                ModEntityTypes.NINJA_IWA,
                ModEntityTypes.NINJA_KIRI,
                ModEntityTypes.NINJA_KONOHA,
                ModEntityTypes.NINJA_KUMO,
                ModEntityTypes.NINJA_SUNA);
        int spawned = 0;
        for (int i = 0; i < types.size(); i++) {
            NinjaMobEntity mob = types.get(i).get().create(level);
            if (mob == null) {
                continue;
            }
            double angle = Math.toRadians(player.getYRot()) + i * Math.PI * 2.0D / types.size();
            double radius = 4.0D;
            mob.moveTo(
                    player.getX() + Math.sin(angle) * radius,
                    player.getY(),
                    player.getZ() + Math.cos(angle) * radius,
                    player.getYRot() + 180.0F,
                    0.0F);
            level.addFreshEntity(mob);
            spawned++;
        }
        int count = countNearbyNinjaMobs(player);
        int spawnedCount = spawned;
        source.sendSuccess(() -> Component.literal("M6 ninja mobs test spawned=" + spawnedCount
                + "; nearby_ninja_mobs=" + count
                + ", variants=anbu/iwa/kiri/konoha/kumo/suna"), false);
        return spawned;
    }

    private static int prepareNamedNinjaEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<EntityType<? extends NinjaMobEntity>> types = List.of(
                ModEntityTypes.GAARA.get(),
                ModEntityTypes.HAKU.get(),
                ModEntityTypes.ITACHI.get(),
                ModEntityTypes.KAKASHI.get(),
                ModEntityTypes.KISAME_HOSHIGAKI.get(),
                ModEntityTypes.KUROTSUCHI.get(),
                ModEntityTypes.TEMARI.get(),
                ModEntityTypes.WHITEZETSU.get(),
                ModEntityTypes.ZABUZA_MOMOCHI.get());
        int spawned = 0;
        for (int i = 0; i < types.size(); i++) {
            NinjaMobEntity mob = types.get(i).create(level);
            if (mob == null) {
                continue;
            }
            double angle = Math.toRadians(player.getYRot()) + i * Math.PI * 2.0D / types.size();
            double radius = 6.0D;
            mob.moveTo(
                    player.getX() + Math.sin(angle) * radius,
                    player.getY(),
                    player.getZ() + Math.cos(angle) * radius,
                    player.getYRot() + 180.0F,
                    0.0F);
            level.addFreshEntity(mob);
            spawned++;
        }
        int count = countNearbyNinjaMobs(player);
        int spawnedCount = spawned;
        source.sendSuccess(() -> Component.literal("M6 named ninja test spawned=" + spawnedCount
                + "; nearby_ninja_mobs=" + count
                + ", variants=gaara/haku/itachi/kakashi/kisame/kurotsuchi/temari/whitezetsu/zabuza"), false);
        return spawned;
    }

    private static int prepareKankuroEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        NinjaMobEntity mob = ModEntityTypes.KANKURO.get().create(level);
        if (mob == null) {
            source.sendFailure(Component.literal("Could not create Kankuro test entity"));
            return 0;
        }
        mob.moveTo(
                player.getX() + Math.sin(Math.toRadians(player.getYRot())) * 3.0D,
                player.getY(),
                player.getZ() + Math.cos(Math.toRadians(player.getYRot())) * 3.0D,
                player.getYRot() + 180.0F,
                0.0F);
        level.addFreshEntity(mob);
        int count = countNearbyNinjaMobs(player);
        source.sendSuccess(() -> Component.literal("M6 Kankuro test spawned=true; nearby_ninja_mobs=" + count
                + ", texture=kankuro.png"), false);
        return 1;
    }

    private static int prepareTentenEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        NinjaMobEntity mob = ModEntityTypes.TENTEN.get().create(level);
        if (mob == null) {
            source.sendFailure(Component.literal("Could not create Tenten test entity"));
            return 0;
        }
        mob.moveTo(
                player.getX() + Math.sin(Math.toRadians(player.getYRot())) * 4.0D,
                player.getY(),
                player.getZ() + Math.cos(Math.toRadians(player.getYRot())) * 4.0D,
                player.getYRot() + 180.0F,
                0.0F);
        level.addFreshEntity(mob);
        int count = countNearbyNinjaMobs(player);
        source.sendSuccess(() -> Component.literal("M6 Tenten test spawned=true; nearby_ninja_mobs=" + count
                + ", ranged_projectile=entitybulletshuriken"), false);
        return 1;
    }

    private static int prepareIrukaEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        NinjaMobEntity mob = ModEntityTypes.IRUKA_SENSEI.get().create(level);
        if (mob == null) {
            source.sendFailure(Component.literal("Could not create Iruka Sensei test entity"));
            return 0;
        }
        mob.moveTo(
                player.getX() + Math.sin(Math.toRadians(player.getYRot())) * 4.0D,
                player.getY(),
                player.getZ() + Math.cos(Math.toRadians(player.getYRot())) * 4.0D,
                player.getYRot() + 180.0F,
                0.0F);
        level.addFreshEntity(mob);
        int count = countNearbyNinjaMobs(player);
        source.sendSuccess(() -> Component.literal("M6 Iruka test spawned=true; nearby_ninja_mobs=" + count
                + ", trades=golden_apple/body_replacement/kage_bunshin/enchanted_golden_apple"), false);
        return 1;
    }

    private static int prepareSakuraEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        NinjaMobEntity sakura = ModEntityTypes.SAKURA_HARUNO.get().create(level);
        NinjaMobEntity healTarget = ModEntityTypes.TENTEN.get().create(level);
        if (sakura == null || healTarget == null) {
            source.sendFailure(Component.literal("Could not create Sakura Haruno test entities"));
            return 0;
        }
        double yaw = Math.toRadians(player.getYRot());
        sakura.moveTo(
                player.getX() + Math.sin(yaw) * 4.0D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 4.0D,
                player.getYRot() + 180.0F,
                0.0F);
        healTarget.moveTo(
                player.getX() + Math.sin(yaw) * 5.5D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 5.5D,
                player.getYRot() + 180.0F,
                0.0F);
        healTarget.setHealth(Math.max(1.0F, healTarget.getMaxHealth() * 0.2F));
        level.addFreshEntity(sakura);
        level.addFreshEntity(healTarget);
        int count = countNearbyNinjaMobs(player);
        source.sendSuccess(() -> Component.literal("M6 Sakura test spawned=true; nearby_ninja_mobs=" + count
                + ", trades=baked_potato/military_rations_pill/scroll_healing/military_rations_pill_gold"
                + ", heal_target=tenten_low_health"), false);
        return 1;
    }

    private static int prepareMightGuyEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        NinjaMobEntity guy = ModEntityTypes.MIGHTGUY.get().create(level);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (guy == null || target == null) {
            source.sendFailure(Component.literal("Could not create Might Guy test entities"));
            return 0;
        }
        double yaw = Math.toRadians(player.getYRot());
        guy.moveTo(
                player.getX() + Math.sin(yaw) * 4.0D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 4.0D,
                player.getYRot() + 180.0F,
                0.0F);
        target.moveTo(
                player.getX() + Math.sin(yaw) * 10.0D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 10.0D,
                player.getYRot() + 180.0F,
                0.0F);
        target.setPersistenceRequired();
        target.setCustomName(Component.literal("M6 Might Guy target"));
        target.setCustomNameVisible(true);
        level.addFreshEntity(guy);
        level.addFreshEntity(target);
        guy.setTarget(target);
        int count = countNearbyNinjaMobs(player);
        source.sendSuccess(() -> Component.literal("M6 Might Guy test spawned=true; nearby_ninja_mobs=" + count
                + ", eight_gates=held/full_xp, target=zombie"), false);
        return 1;
    }

    private static int prepareGiantDog2hEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        GiantDog2hEntity dog = GiantDog2hEntity.spawnFor(player, GiantDog2hEntity.DEFAULT_MAX_HEALTH);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (dog == null || target == null) {
            source.sendFailure(Component.literal("Could not create Giant Dog 2H test entities"));
            return 0;
        }
        double yaw = Math.toRadians(player.getYRot());
        target.moveTo(
                player.getX() + Math.sin(yaw) * 12.0D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 12.0D,
                player.getYRot() + 180.0F,
                0.0F);
        target.setPersistenceRequired();
        target.setCustomName(Component.literal("M6 Giant Dog target"));
        target.setCustomNameVisible(true);
        level.addFreshEntity(target);
        dog.setTarget(target);
        player.startRiding(dog, true);
        int count = countNearbyGiantDog2h(player);
        source.sendSuccess(() -> Component.literal("M6 giant dog test spawned=true; nearby_giant_dog_2h=" + count
                + ", owner=player, rider=player, target=zombie, split_health=500"), false);
        return 1;
    }

    private static int prepareJinchurikiCloneEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int previousTails = NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS);
        NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, 4);
        NarutomodModVariables.sync(player);
        JinchurikiCloneEntity clone = JinchurikiCloneEntity.spawnFrom(player);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (clone == null || target == null) {
            NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, previousTails);
            NarutomodModVariables.sync(player);
            source.sendFailure(Component.literal("Could not create Jinchuriki Clone test entities"));
            return 0;
        }
        clone.rememberOriginalJinchurikiTails(previousTails);
        double yaw = Math.toRadians(player.getYRot());
        target.moveTo(
                player.getX() + Math.sin(yaw) * 32.0D,
                player.getY(),
                player.getZ() + Math.cos(yaw) * 32.0D,
                player.getYRot() + 180.0F,
                0.0F);
        target.setPersistenceRequired();
        target.setCustomName(Component.literal("M6 Jinchuriki Clone target"));
        target.setCustomNameVisible(true);
        level.addFreshEntity(target);
        clone.setTarget(target);
        int count = countNearbyJinchurikiClones(player);
        source.sendSuccess(() -> Component.literal("M6 Jinchuriki Clone test spawned=true; nearby_jinchuriki_clones=" + count
                + ", player_camera=clone, clone_level=2, target=zombie, previous_tails=" + previousTails), false);
        return 1;
    }

    private static int preparePurpleDragonEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<LivingEntity> targets = new ArrayList<>();
        double yaw = Math.toRadians(player.getYRot());
        for (int i = 0; i < 5; i++) {
            Zombie target = EntityType.ZOMBIE.create(level);
            if (target == null) {
                continue;
            }
            double side = (i - 2) * 2.0D;
            double forward = 18.0D + i * 1.25D;
            target.moveTo(
                    player.getX() + Math.sin(yaw) * forward + Math.cos(yaw) * side,
                    player.getY(),
                    player.getZ() + Math.cos(yaw) * forward - Math.sin(yaw) * side,
                    player.getYRot() + 180.0F,
                    0.0F);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("M6 Purple Dragon target " + (i + 1)));
            target.setCustomNameVisible(true);
            level.addFreshEntity(target);
            targets.add(target);
        }
        PurpleDragonEntity dragon = PurpleDragonEntity.spawnFrom(player, targets);
        if (dragon == null) {
            source.sendFailure(Component.literal("Could not create Purple Dragon test entity"));
            return 0;
        }
        int count = countNearbyPurpleDragons(player);
        int targetCount = targets.size();
        source.sendSuccess(() -> Component.literal("M6 Purple Dragon test spawned=true; nearby_purple_dragons=" + count
                + ", targets=" + targetCount + ", wait_ticks=40"), false);
        return 1;
    }

    private static int prepareGedoStatueEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack helmet = getOrGiveItem(player, ModItems.RINNEGANHELMET.get());
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        GedoStatueEntity gedo = GedoStatueEntity.spawnFrom(player);
        if (gedo == null) {
            source.sendFailure(Component.literal("Could not create Gedo Statue test entity"));
            return 0;
        }
        int targets = 0;
        for (int i = 0; i < 5; i++) {
            targets += spawnM6TargetZombie(player, 12.0D + i * 2.0D, (i - 2) * 2.0D, "M6 Gedo Statue target " + (i + 1));
        }
        boolean dragonSpawned = gedo.trySpawnDragonNow();
        int gedoCount = countNearbyGedoStatues(player);
        int dragonCount = countNearbyPurpleDragons(player);
        int targetCount = targets;
        source.sendSuccess(() -> Component.literal("M6 Gedo Statue test spawned=true; nearby_gedo_statue=" + gedoCount
                + ", nearby_purple_dragons=" + dragonCount
                + ", targets=" + targetCount
                + ", dragon_spawned=" + dragonSpawned
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", no_ai=" + gedo.isNoAi()
                + ", health=" + gedo.getHealth() + "/" + gedo.getMaxHealth()
                + ", attack_damage=" + gedo.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareGedoTenTailsActivationTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        prepareJutsuResourcePool(player, 90000.0D);
        BijuManager.revokePlayer(player);
        BijuManager.clearGedoSealedTails(source.getServer());
        for (int tails = BijuManager.MIN_TAILS; tails <= 8; tails++) {
            BijuManager.setGedoSealedTail(source.getServer(), tails, true);
        }
        BijuManager.clearSavedSpawnedTailedBeast(source.getServer(), BijuManager.MAX_TAILS);
        GedoStatueEntity existing = GedoStatueEntity.findLoaded(source.getServer());
        if (existing != null) {
            existing.discard();
        }

        Vec3 forward = player.getLookAngle();
        Vec3 gedoPos = player.position().add(forward.x() * 8.0D, 0.0D, forward.z() * 8.0D);
        GedoStatueEntity gedo = GedoStatueEntity.spawnAt(level, gedoPos, player.getYRot(), player);
        if (gedo == null) {
            source.sendFailure(Component.literal("Could not create Gedo Statue activation test entity"));
            return 0;
        }
        TailedBeastEntity nineTails = TailedBeastEntity.spawnAt(
                level,
                TailedBeastEntity.Variant.byTailCount(9),
                gedo.position().add(6.0D, 0.0D, 0.0D),
                player.getYRot() + 180.0F,
                player);
        if (nineTails != null) {
            nineTails.setAngerLevel(0);
        }
        boolean absorbed = gedo.tryAbsorbNearbyTailedBeastNow();
        int sealedCount = BijuManager.countGedoSealedTails(source.getServer());
        int tenTailsCount = countNearbyTenTails(player);
        int loadedTenTails = BijuManager.countLoadedTenTails(source.getServer());
        int gedoCount = countNearbyGedoStatues(player);
        source.sendSuccess(() -> Component.literal("Gedo Ten Tails activation ready: gedo_spawned=true"
                + ", ninth_tail_spawned=" + (nineTails != null)
                + ", absorbed=" + absorbed
                + ", gedo_sealed_tails=" + sealedCount
                + ", gedo_sealed_list=" + BijuManager.listGedoSealedTails(source.getServer())
                + ", nearby_gedo_statue=" + gedoCount
                + ", nearby_ten_tails=" + tenTailsCount
                + ", loaded_ten_tails=" + loadedTenTails
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())), false);
        return absorbed && tenTailsCount > 0 ? 1 : 0;
    }

    private static int runTenTailsServerChainSelfTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        MinecraftServer server = source.getServer();
        prepareJutsuResourcePool(player, 90000.0D);
        player.stopRiding();
        BijuManager.revokePlayer(player);
        BijuManager.revokeByTail(server, BijuManager.MAX_TAILS);
        BijuManager.clearGedoSealedTails(server);
        BijuManager.clearSavedSpawnedTailedBeast(server, BijuManager.MAX_TAILS);
        int discardedTenTailsBefore = BijuManager.discardLoadedTenTailsExcept(server, null);
        int discardedGedoBefore = discardLoadedGedoStatues(server);
        for (int tails = BijuManager.MIN_TAILS; tails <= 8; tails++) {
            BijuManager.setGedoSealedTail(server, tails, true);
        }

        Vec3 forward = player.getLookAngle();
        Vec3 gedoPos = player.position().add(forward.x() * 8.0D, 0.0D, forward.z() * 8.0D);
        GedoStatueEntity gedo = GedoStatueEntity.spawnAt(level, gedoPos, player.getYRot(), player);
        if (gedo == null) {
            source.sendFailure(Component.literal("Ten Tails chain selftest failed: could not create Gedo Statue"));
            return 0;
        }
        TailedBeastEntity nineTails = TailedBeastEntity.spawnAt(
                level,
                TailedBeastEntity.Variant.byTailCount(9),
                gedo.position().add(6.0D, 0.0D, 0.0D),
                player.getYRot() + 180.0F,
                player);
        if (nineTails != null) {
            nineTails.setAngerLevel(0);
        }

        boolean absorbed = gedo.tryAbsorbNearbyTailedBeastNow();
        int sealedAfterAbsorb = BijuManager.countGedoSealedTails(server);
        int loadedAfterActivation = BijuManager.countLoadedTenTails(server);
        int savedAfterActivation = BijuManager.savedSpawnedTailedBeastCount(server);
        int restored = BijuManager.restoreSpawnedTailedBeasts(server);
        int loadedAfterRestore = BijuManager.countLoadedTenTails(server);
        TenTailsEntity tenTails = findFirstLoadedTenTails(server);
        boolean sealedIntoPlayer = tenTails != null && BijuManager.sealTenTailsIntoPlayer(tenTails, player);
        int assignedTail = BijuManager.getAssignedTail(player);
        int syncedTail = NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS);
        int loadedAfterSeal = BijuManager.countLoadedTenTails(server);
        int savedAfterSeal = BijuManager.savedSpawnedTailedBeastCount(server);
        boolean passed = nineTails != null
                && absorbed
                && sealedAfterAbsorb == 9
                && loadedAfterActivation == 1
                && savedAfterActivation == 1
                && restored == 0
                && loadedAfterRestore == 1
                && sealedIntoPlayer
                && assignedTail == BijuManager.MAX_TAILS
                && syncedTail == BijuManager.MAX_TAILS
                && loadedAfterSeal == 0
                && savedAfterSeal == 0;
        Component result = Component.literal("Ten Tails chain selftest passed=" + passed
                + ", discarded_gedo_before=" + discardedGedoBefore
                + ", discarded_ten_tails_before=" + discardedTenTailsBefore
                + ", ninth_tail_spawned=" + (nineTails != null)
                + ", absorbed=" + absorbed
                + ", gedo_sealed_tails=" + sealedAfterAbsorb
                + ", loaded_after_activation=" + loadedAfterActivation
                + ", saved_after_activation=" + savedAfterActivation
                + ", restore_return=" + restored
                + ", loaded_after_restore=" + loadedAfterRestore
                + ", sealed_into_player=" + sealedIntoPlayer
                + ", assigned_tail=" + assignedTail
                + ", synced_tail=" + syncedTail
                + ", loaded_after_seal=" + loadedAfterSeal
                + ", saved_after_seal=" + savedAfterSeal);
        if (passed) {
            source.sendSuccess(() -> result, false);
        } else {
            source.sendFailure(result);
        }
        return passed ? 1 : 0;
    }

    private static int prepareSnake8HeadsEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack senjutsuStack = prepareSenjutsuSnake8HeadsStack(player);
        ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_MODE_ACTIVATED_TAG, true);
        ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_TYPE_TAG, SenjutsuItem.SageType.SNAKE.id());
        player.stopRiding();
        Snake8HeadsEntity snake = Snake8HeadsEntity.spawnFrom(player, SenjutsuItem.SNAKE_8_HEADS.chakraUsage() * 0.02D);
        if (snake == null) {
            source.sendFailure(Component.literal("Could not create Snake 8 Heads test entity"));
            return 0;
        }

        DirectionBasis basis = DirectionBasis.from(player);
        int targets = 0;
        int heads = 0;
        for (int i = 0; i < 3; i++) {
            Zombie target = EntityType.ZOMBIE.create(level);
            if (target == null) {
                continue;
            }
            Vec3 point = basis.point(38.0D + i * 7.0D, (i - 1) * 5.0D, 0.0D);
            target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("M6 Snake 8 Heads target " + (i + 1)));
            target.setCustomNameVisible(true);
            level.addFreshEntity(target);
            targets++;
            if (snake.shootHeadAt(target) != null) {
                heads++;
            }
        }

        int snakeCount = countNearbySnake8Heads(player);
        int headCount = countNearbySnake8Head1(player);
        int targetCount = targets;
        int spawnedHeads = heads;
        source.sendSuccess(() -> Component.literal("M6 Snake 8 Heads test spawned=true; nearby_snake_8_heads=" + snakeCount
                + ", nearby_snake_8_head1=" + headCount
                + ", spawned_heads=" + spawnedHeads
                + ", targets=" + targetCount
                + ", riding=" + (player.getVehicle() == snake)
                + ", sage_type=" + SenjutsuItem.getSageType(senjutsuStack)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareTenTailsEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        head.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        player.stopRiding();

        TenTailsEntity tenTails = TenTailsEntity.spawnFrom(player, true);
        if (tenTails == null) {
            source.sendFailure(Component.literal("Could not create Ten Tails test entity"));
            return 0;
        }
        tenTails.setAngerLevel(2);
        tenTails.readyTailBeastBall();

        DirectionBasis basis = DirectionBasis.from(player);
        Zombie target = EntityType.ZOMBIE.create(level);
        boolean targetSpawned = false;
        if (target != null) {
            Vec3 point = basis.point(78.0D, 0.0D, 0.0D);
            target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("M6 Ten Tails ranged target"));
            target.setCustomNameVisible(true);
            level.addFreshEntity(target);
            targetSpawned = true;
            tenTails.performRangedAttack(target, 0.0F);
        }

        int tenTailsCount = countNearbyTenTails(player);
        int loadedTenTails = BijuManager.countLoadedTenTails(source.getServer());
        int ballCount = countNearbyTailBeastBalls(player);
        boolean spawnedTarget = targetSpawned;
        source.sendSuccess(() -> Component.literal("M6 Ten Tails test spawned=true; nearby_ten_tails=" + tenTailsCount
                + ", loaded_ten_tails=" + loadedTenTails
                + ", nearby_tail_beast_balls=" + ballCount
                + ", target_spawned=" + spawnedTarget
                + ", riding=" + (player.getVehicle() == tenTails)
                + ", active_rinnesharingan=" + SixPathSenjutsuItem.hasRinneSharingan(player)
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())
                + ", health=" + tenTails.getHealth() + "/" + tenTails.getMaxHealth()
                + ", attack_damage=" + tenTails.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)), false);
        return 1;
    }

    private static int prepareChibakuTenseiEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Vec3 captureCenter = player.position().add(0.0D, player.getBbHeight() + 84.0D, 0.0D);
        int blockCount = placeChibakuCaptureBlocks(level, captureCenter);
        int targetCount = spawnChibakuTargets(player, captureCenter);
        ChibakuTenseiBallEntity ball = ChibakuTenseiBallEntity.spawnFrom(player);
        if (ball == null) {
            source.sendFailure(Component.literal("Could not create Chibaku Tensei test entity"));
            return 0;
        }
        int ballCount = countNearbyChibakuTenseiBalls(player);
        int satelliteCount = countNearbyChibakuSatellites(player);
        source.sendSuccess(() -> Component.literal("M6 Chibaku Tensei test spawned=true; nearby_chibaku_balls="
                + ballCount + ", nearby_chibaku_satellites=" + satelliteCount
                + ", capture_blocks=" + blockCount + ", airborne_targets=" + targetCount
                + ", launch_ticks=100"), false);
        return 1;
    }

    private static int placeChibakuCaptureBlocks(ServerLevel level, Vec3 center) {
        int count = 0;
        BlockPos origin = BlockPos.containing(center);
        int radius = 5;
        int radiusSqr = radius * radius;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (pos.distSqr(origin) <= radiusSqr && level.isEmptyBlock(pos)) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                count++;
            }
        }
        return count;
    }

    private static int prepareKingOfHellEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        player.setHealth(Math.max(1.0F, Math.min(2.0F, player.getMaxHealth())));
        KingOfHellEntity entity = KingOfHellEntity.spawnFrom(player);
        if (entity == null) {
            source.sendFailure(Component.literal("Could not create King of Hell test entity"));
            return 0;
        }
        int count = countNearbyKingOfHell(player);
        source.sendSuccess(() -> Component.literal("M6 King of Hell test spawned=true; nearby_king_of_hell=" + count
                + ", player_health=" + player.getHealth() + "/" + player.getMaxHealth()
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()
                + ", auto_heal_threshold=<4"), false);
        return 1;
    }

    private static int prepareSusanooSkeletonEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 12000.0D);
        ItemStack helmet = getOrGiveItem(player, ModItems.MANGEKYOSHARINGANHELMET.get());
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        SusanooSkeletonEntity susanoo = SusanooSkeletonEntity.spawnFrom(player);
        if (susanoo == null) {
            source.sendFailure(Component.literal("Could not create Susanoo Skeleton test entity"));
            return 0;
        }
        spawnM6TargetZombie(player, 9.0D, 0.0D, "M6 Susanoo Skeleton target");
        int count = countNearbySusanooSkeletons(player);
        source.sendSuccess(() -> Component.literal("M6 Susanoo Skeleton test spawned=true; nearby_susanoo_skeletons=" + count
                + ", riding=" + (player.getVehicle() == susanoo)
                + ", attack_damage=" + susanoo.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSusanooClothedEntityTest(CommandSourceStack source, boolean fullBody) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, fullBody ? 30000.0D : 18000.0D);
        ItemStack helmet = getOrGiveItem(player, ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        SusanooClothedEntity susanoo = SusanooClothedEntity.spawnFrom(player, fullBody);
        if (susanoo == null) {
            source.sendFailure(Component.literal("Could not create Susanoo Clothed test entity"));
            return 0;
        }
        susanoo.setShowSword(true);
        susanoo.createMagatama(SusanooClothedEntity.MODEL_SCALE * 0.5F);
        spawnM6TargetZombie(player, fullBody ? 14.0D : 10.0D, 0.0D, fullBody ? "M6 Susanoo Clothed full target" : "M6 Susanoo Clothed target");
        int count = countNearbySusanooClothed(player);
        source.sendSuccess(() -> Component.literal("M6 Susanoo Clothed test spawned=true; full_body=" + fullBody
                + ", nearby_susanoo_clothed=" + count
                + ", riding=" + (player.getVehicle() == susanoo)
                + ", show_sword=" + susanoo.shouldShowSword()
                + ", attack_damage=" + susanoo.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSusanooWingedEntityTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack helmet = getOrGiveItem(player, ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
        player.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
        ItemStack kagutsuchi = getOrGiveItem(player, ModItems.KAGUTSUCHISWORDRANGED.get());
        kagutsuchi.setCount(1);
        getOrGiveItem(player, ModItems.KAMUISHURIKEN.get()).setCount(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, kagutsuchi.copy());
        SusanooWingedEntity susanoo = SusanooWingedEntity.spawnFrom(player);
        if (susanoo == null) {
            source.sendFailure(Component.literal("Could not create Susanoo Winged test entity"));
            return 0;
        }
        susanoo.setShowSword(true);
        susanoo.createMagatama(SusanooWingedEntity.MODEL_SCALE * 0.5F);
        spawnM6TargetZombie(player, 18.0D, 0.0D, "M6 Susanoo Winged target");
        int count = countNearbySusanooWinged(player);
        source.sendSuccess(() -> Component.literal("M6 Susanoo Winged test spawned=true; nearby_susanoo_winged=" + count
                + ", riding=" + (player.getVehicle() == susanoo)
                + ", show_sword=" + susanoo.shouldShowSword()
                + ", wing_progress=" + susanoo.getWingSwingProgress()
                + ", attack_damage=" + susanoo.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                + ", kagutsuchi_stack=" + findItem(player, ModItems.KAGUTSUCHISWORDRANGED.get()).getCount()
                + ", kamui_shuriken_stack=" + findItem(player, ModItems.KAMUISHURIKEN.get()).getCount()
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int spawnChibakuTargets(ServerPlayer player, Vec3 center) {
        ServerLevel level = player.serverLevel();
        int count = 0;
        for (int i = 0; i < 4; i++) {
            Zombie target = EntityType.ZOMBIE.create(level);
            if (target == null) {
                continue;
            }
            double angle = i * Math.PI * 0.5D;
            target.moveTo(
                    center.x() + Math.cos(angle) * 18.0D,
                    center.y() + ((i & 1) == 0 ? 3.0D : -3.0D),
                    center.z() + Math.sin(angle) * 18.0D,
                    player.getYRot() + 180.0F,
                    0.0F);
            target.setNoGravity(true);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("M6 Chibaku Tensei target " + (i + 1)));
            target.setCustomNameVisible(true);
            level.addFreshEntity(target);
            count++;
        }
        return count;
    }

    private static int spawnM6TargetZombie(ServerPlayer player, double forward, double side, String name) {
        Zombie target = EntityType.ZOMBIE.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        DirectionBasis basis = DirectionBasis.from(player);
        Vec3 point = basis.point(forward, side, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setPersistenceRequired();
        target.setCustomName(Component.literal(name));
        target.setCustomNameVisible(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int reportM6EntitiesState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M6 entities: nearby_crows=" + countNearbyCrows(player)
                + ", nearby_alt_cam_views=" + countNearbyAltCamViews(player)
                + ", nearby_buddha_arms=" + countNearbyBuddhaArms(player)
                + ", nearby_ground_shock=" + countNearbyGroundShocks(player)
                + ", nearby_earth_blocks=" + countNearbyEarthBlocks(player)
                + ", nearby_spikes=" + countNearbySpikes(player)
                + ", nearby_eight_trigrams=" + countNearbyEightTrigrams(player)
                + ", nearby_hakkesho_keiten=" + countNearbyHakkeshoKeiten(player)
                + ", nearby_special_effects=" + countNearbySpecialEffects(player)
                + ", nearby_tail_beast_balls=" + countNearbyTailBeastBalls(player)
                + ", nearby_tailed_beasts=" + countNearbyTailedBeasts(player)
                + ", nearby_yasaka_magatama=" + countNearbyYasakaMagatama(player)
                + ", nearby_preta_shields=" + countNearbyPretaShields(player)
                + ", nearby_ninja_mobs=" + countNearbyNinjaMobs(player)
                + ", nearby_giant_dog_2h=" + countNearbyGiantDog2h(player)
                + ", nearby_jinchuriki_clones=" + countNearbyJinchurikiClones(player)
                + ", nearby_purple_dragons=" + countNearbyPurpleDragons(player)
                + ", nearby_chibaku_balls=" + countNearbyChibakuTenseiBalls(player)
                + ", nearby_chibaku_satellites=" + countNearbyChibakuSatellites(player)
                + ", nearby_king_of_hell=" + countNearbyKingOfHell(player)
                + ", nearby_susanoo_skeletons=" + countNearbySusanooSkeletons(player)
                + ", nearby_susanoo_clothed=" + countNearbySusanooClothed(player)
                + ", nearby_susanoo_winged=" + countNearbySusanooWinged(player)
                + ", nearby_gedo_statue=" + countNearbyGedoStatues(player)
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", nearby_snake_8_heads=" + countNearbySnake8Heads(player)
                + ", nearby_snake_8_head1=" + countNearbySnake8Head1(player)
                + ", nearby_ten_tails=" + countNearbyTenTails(player)
                + ", loaded_ten_tails=" + BijuManager.countLoadedTenTails(source.getServer())), false);
        return 1;
    }

    private static int countNearbyCrows(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(CrowEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyAltCamViews(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(AltCamViewEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyBuddhaArms(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(BuddhaArmEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyBuddha1000(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(Buddha1000Entity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyGroundShocks(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(GroundShockEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyEarthBlocks(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(EarthBlocksEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySpikes(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SpikeEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyEightTrigrams(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(EightTrigramsEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyHakkeshoKeiten(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(HakkeshoKeitenEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyPortalBlocks(ServerPlayer player, int radius) {
        BlockPos center = player.blockPosition();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (player.serverLevel().getBlockState(pos).is(ModBlocks.PORTALBLOCK.get())) {
                count++;
            }
        }
        return count;
    }

    private static int countNearbySpecialEffects(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SpecialEffectEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyTailBeastBalls(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TailBeastBallEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyTailedBeasts(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TailedBeastEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyYasakaMagatama(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(YasakaMagatamaEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyPretaShields(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(PretaShieldEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyNinjaMobs(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(NinjaMobEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyGiantDog2h(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(GiantDog2hEntity.class, player.getBoundingBox().inflate(128.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyJinchurikiClones(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(JinchurikiCloneEntity.class, player.getBoundingBox().inflate(128.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyPurpleDragons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(PurpleDragonEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyGedoStatues(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(GedoStatueEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countLoadedGedoStatues(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof GedoStatueEntity gedo && gedo.isAlive()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countNearbySnake8Heads(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(Snake8HeadsEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySnake8Head1(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(Snake8HeadEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyTenTails(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TenTailsEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyAmaterasuTargets(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0D),
                        entity -> entity.isAlive() && entity.hasEffect(ModEffects.AMATERASUFLAME.get()))
                .size();
    }

    private static TenTailsEntity findFirstLoadedTenTails(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof TenTailsEntity beast && beast.isAlive()) {
                    return beast;
                }
            }
        }
        return null;
    }

    private static int discardLoadedGedoStatues(MinecraftServer server) {
        int discarded = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof GedoStatueEntity gedo && gedo.isAlive()) {
                    gedo.discard();
                    discarded++;
                }
            }
        }
        return discarded;
    }

    private static int countNearbyChibakuTenseiBalls(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ChibakuTenseiBallEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyChibakuSatellites(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ChibakuSatelliteEntity.class, player.getBoundingBox().inflate(256.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyKingOfHell(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(KingOfHellEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySusanooSkeletons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SusanooSkeletonEntity.class, player.getBoundingBox().inflate(128.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySusanooClothed(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SusanooClothedEntity.class, player.getBoundingBox().inflate(128.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySusanooWinged(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SusanooWingedEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySenjutsuPlatforms(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SenjutsuSitPlatformEntity.class, player.getBoundingBox().inflate(32.0D), entity -> entity.isAlive())
                .size();
    }

    private static int runM3SmokeAll(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int modelCount = spawnM3ModelGrid(source.getPlayerOrException());
        int particleCount = spawnM3ParticleGrid(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("M3 smoke spawned " + modelCount + " model samples and "
                + particleCount + " particle sample points with feedback targets"), false);
        return modelCount + particleCount;
    }

    private static int runM3SmokeModels(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int modelCount = spawnM3ModelGrid(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("M3 smoke spawned " + modelCount + " model samples"), false);
        return modelCount;
    }

    private static int runM3SmokeParticles(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int particleCount = spawnM3ParticleGrid(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("M3 smoke spawned " + particleCount + " particle sample points with feedback targets"), false);
        return particleCount;
    }

    private static int runM3MobAppearanceItachiSmoke(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 point = DirectionBasis.from(player).point(8.0D, 0.0D, 1.5D);
        player.serverLevel().sendParticles(
                ModParticleTypes.options(NarutoParticleKind.MOB_APPEARANCE, LEGACY_ITACHI_ENTITY_ID),
                point.x(),
                point.y(),
                point.z(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        source.sendSuccess(() -> Component.literal("M3 mob_appearance Itachi smoke spawned with legacy entity id "
                + LEGACY_ITACHI_ENTITY_ID), false);
        return 1;
    }

    private static int prepareLegacyEffectsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        prepareJutsuResourcePool(player, 1000.0D);
        double maxChakra = Chakra.pathway(player).getMax();
        NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, maxChakra * 0.25D);
        NarutomodModVariables.sync(player);
        player.addEffect(new MobEffectInstance(ModEffects.CHAKRA_REGENERATION.get(), 200, 0, false, false));
        player.addEffect(new MobEffectInstance(ModEffects.FEATHER_FALLING.get(), 200, 1, false, false));

        Zombie target = EntityType.ZOMBIE.create(level);
        boolean targetSpawned = false;
        if (target != null) {
            DirectionBasis basis = DirectionBasis.from(player);
            Vec3 point = basis.point(7.0D, 0.0D, 0.0D);
            target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
            target.setPersistenceRequired();
            target.setCustomName(Component.literal("Legacy Amaterasu effect target"));
            target.setCustomNameVisible(true);
            target.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 100, 1, false, false));
            level.addFreshEntity(target);
            targetSpawned = true;
        }

        boolean spawned = targetSpawned;
        source.sendSuccess(() -> Component.literal("Legacy effects test ready: player_chakra_regen="
                + describeEffectInstance(player.getEffect(ModEffects.CHAKRA_REGENERATION.get()))
                + ", player_feather_falling=" + describeEffectInstance(player.getEffect(ModEffects.FEATHER_FALLING.get()))
                + ", amaterasu_target_spawned=" + spawned
                + ", amaterasu_targets=" + countNearbyAmaterasuTargets(player)
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return spawned ? 1 : 0;
    }

    private static int reportLegacyEffectsState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("Legacy effects: player_chakra_regen="
                + describeEffectInstance(player.getEffect(ModEffects.CHAKRA_REGENERATION.get()))
                + ", player_feather_falling=" + describeEffectInstance(player.getEffect(ModEffects.FEATHER_FALLING.get()))
                + ", player_amaterasu=" + describeEffectInstance(player.getEffect(ModEffects.AMATERASUFLAME.get()))
                + ", amaterasu_targets=" + countNearbyAmaterasuTargets(player)
                + ", fall_distance=" + player.fallDistance
                + ", chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareRasenganItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 1000.0D);
        prepareOwnedLearnedRasenganStack(player);
        source.sendSuccess(() -> Component.literal("Rasengan item test ready: ninjutsu item, battle_xp="
                + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSenjutsuRasenganItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        prepareOwnedLearnedRasenganStack(player);
        prepareOwnedInactiveSenjutsuStack(player, false);
        source.sendSuccess(() -> Component.literal("Sage Rasengan item test ready: crouch-right-click senjutsu to charge Sage Mode, then right-click for Sage Rasengan; battle_xp="
                + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", nearby_senjutsu_platforms=" + countNearbySenjutsuPlatforms(player)), false);
        return 1;
    }

    private static int prepareM4RasenganScene(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_RASENGAN.get());
        prepareOwnedLearnedRasenganStack(player);
        ItemStack senjutsuStack = prepareOwnedInactiveSenjutsuStack(player, true);
        ProcedureSync.EntityNBTTag.removeAndSync(player, SenjutsuItem.SAGE_MODE_ACTIVATED_TAG);
        ProcedureSync.EntityNBTTag.removeAndSync(player, SenjutsuItem.SAGE_TYPE_TAG);
        ProcedureSync.EntityNBTTag.removeAndSync(player, NarutomodModVariables.FORCE_BOW_POSE);

        int targetCount = spawnM4RasenganTargets(player);
        source.sendSuccess(() -> Component.literal("M4 Rasengan scene ready: scroll+ninjutsu+senjutsu prepared, sage_type="
                + SenjutsuItem.getSageType(senjutsuStack)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", targets=" + targetCount
                + ". Check with /narutoport m4_rasengan state."), false);
        return targetCount;
    }

    private static int reportM4RasenganState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack scrollStack = findItem(player, ModItems.SCROLL_RASENGAN.get());
        ItemStack ninjutsuStack = findItem(player, ModItems.NINJUTSU.get());
        ItemStack senjutsuStack = findItem(player, ModItems.SENJUTSU.get());
        int nearbyRasengan = player.serverLevel()
                .getEntitiesOfClass(RasenganEntity.class, player.getBoundingBox().inflate(16.0D))
                .size();
        int nearbySenjutsuPlatforms = countNearbySenjutsuPlatforms(player);
        boolean syncedSageActive = NarutomodModVariables.get(player).getBoolean(SenjutsuItem.SAGE_MODE_ACTIVATED_TAG);
        int syncedSageType = NarutomodModVariables.get(player).getInt(SenjutsuItem.SAGE_TYPE_TAG);
        boolean forceBowPose = NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.FORCE_BOW_POSE);

        source.sendSuccess(() -> Component.literal("M4 Rasengan state: battle_xp="
                + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", scroll=" + !scrollStack.isEmpty()
                + ", ninjutsu=" + describeNinjutsuState(player, ninjutsuStack)
                + ", senjutsu=" + describeSenjutsuState(player, senjutsuStack)
                + ", sync_sage_active=" + syncedSageActive
                + ", sync_sage_type=" + syncedSageType
                + ", force_bow_pose=" + forceBowPose
                + ", nearby_rasengan=" + nearbyRasengan
                + ", nearby_senjutsu_platforms=" + nearbySenjutsuPlatforms), false);
        return nearbyRasengan;
    }

    private static int reportM5JutsuState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack ninjutsuStack = findItem(player, ModItems.NINJUTSU.get());
        ItemStack senjutsuStack = findItem(player, ModItems.SENJUTSU.get());
        ItemStack raitonStack = findItem(player, ModItems.RAITON.get());
        ItemStack katonStack = findItem(player, ModItems.KATON.get());
        ItemStack dotonStack = findItem(player, ModItems.DOTON.get());
        ItemStack futonStack = findItem(player, ModItems.FUTON.get());
        ItemStack suitonStack = findItem(player, ModItems.SUITON.get());
        ItemStack iryoStack = findItem(player, ModItems.IRYO_JUTSU.get());
        ItemStack intonStack = findItem(player, ModItems.INTON.get());
        String powerIncreaseMainhand = describePowerIncreaseJutsuStack(player.getMainHandItem());
        String powerIncreaseOffhand = describePowerIncreaseJutsuStack(player.getOffhandItem());
        int nearbyReplacementClone = player.serverLevel()
                .getEntitiesOfClass(ReplacementCloneEntity.class, player.getBoundingBox().inflate(32.0D))
                .size();
        int nearbySenjutsuPlatforms = countNearbySenjutsuPlatforms(player);
        int nearbyKageBunshin = player.serverLevel()
                .getEntitiesOfClass(KageBunshinEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyLimboClone = player.serverLevel()
                .getEntitiesOfClass(LimboCloneEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean powerIncreaseByakuganHead = ByakuganHandler.isByakuganHead(headStack);
        boolean powerIncreaseByakuganActive = ByakuganHandler.isActive(player);
        float powerIncreaseByakuganFov = ByakuganHandler.getFov(player);
        float byakuganNinjaLevel = (float)NarutomodModVariables.getNinjaLevel(player);
        double byakuganProjectedCameraDistance = ByakuganHandler.projectedCameraDistance(powerIncreaseByakuganFov, byakuganNinjaLevel);
        int byakuganTargetRenderDistance = ByakuganHandler.targetRenderDistance(byakuganNinjaLevel);
        boolean byakuganRinneSharingan = ByakuganHandler.isRinnesharinganActivated(headStack);
        double byakurinShockwavePressTime = ByakuganHandler.getRinnesharinganPressTime(player);
        long byakugan64CooldownTicks = ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG);
        long byakuganKaitenCooldownTicks = ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG);
        int nearbyByakuganEightTrigrams = countNearbyEightTrigrams(player);
        int nearbyByakuganKaiten = countNearbyHakkeshoKeiten(player);
        int nearbyByakurinPortalBlocks = countNearbyPortalBlocks(player, 32);
        boolean byakuganKaitenVehicle = player.getVehicle() instanceof HakkeshoKeitenEntity;
        boolean powerIncreaseSharinganHead = SusanooPowerIncreaseHandler.isSharinganHead(headStack);
        String powerIncreaseSusanooVehicle = SusanooPowerIncreaseHandler.describeVehicle(player.getVehicle());
        int nearbyPowerIncreaseSusanooSkeletons = countNearbySusanooSkeletons(player);
        int nearbyPowerIncreaseSusanooClothed = countNearbySusanooClothed(player);
        int nearbyPowerIncreaseSusanooWinged = countNearbySusanooWinged(player);
        int rinneganPath = RinneganSpecialJutsuHandler.isRinneganLikeHead(headStack)
                ? RinneganSpecialJutsuHandler.getRinneganPath(headStack)
                : -1;
        long chibakuCooldownTicks = getChibakuCooldownTicks(player);
        int nearbyChibakuBalls = countNearbyChibakuTenseiBalls(player);
        int nearbyChibakuSatellites = countNearbyChibakuSatellites(player);
        int nearbyRinneganPretaShields = countNearbyPretaShields(player);
        int nearbyRinneganAnimalDogs = countNearbyGiantDog2h(player);
        int nearbyRinneganKingOfHell = countNearbyKingOfHell(player);
        int nearbyRinneganGedoStatues = countNearbyGedoStatues(player);
        int loadedGedoStatues = countLoadedGedoStatues(source.getServer());
        int loadedTenTails = BijuManager.countLoadedTenTails(source.getServer());
        int gedoSealedTails = BijuManager.countGedoSealedTails(source.getServer());
        ItemStack asuraBodyStack = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack asuraCannonOffhand = player.getOffhandItem();
        boolean asuraBodyEquipped = RinneganSpecialJutsuHandler.hasAsuraBodyEquipped(player);
        boolean asuraCannonEquipped = RinneganSpecialJutsuHandler.hasAsuraCannonOffhand(player);
        double asuraTicksUsed = asuraBodyStack.getTag() != null
                ? asuraBodyStack.getTag().getDouble(RinneganSpecialJutsuHandler.ASURA_TICKS_USED_TAG)
                : -1.0D;
        int summonedAnimalId = headStack.getTag() != null ? headStack.getTag().getInt(RinneganSpecialJutsuHandler.SUMMONED_ANIMAL_ID_TAG) : 0;
        boolean kingOfHellIdPresent = headStack.getTag() != null && headStack.getTag().hasUUID(RinneganSpecialJutsuHandler.KING_OF_HELL_ID_TAG);
        int nearbySealingChains = player.serverLevel()
                .getEntitiesOfClass(SealingChainsEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyPuppetKarasu = player.serverLevel()
                .getEntitiesOfClass(PuppetKarasuEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyPuppetSanshouo = player.serverLevel()
                .getEntitiesOfClass(PuppetSanshouoEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyKarasuOpeningScrolls = player.serverLevel()
                .getEntitiesOfClass(KarasuScrollProjectileEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbySanshouoOpeningScrolls = player.serverLevel()
                .getEntitiesOfClass(SanshouoScrollProjectileEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyPuppetHiruko = player.serverLevel()
                .getEntitiesOfClass(PuppetHirukoEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyBugSwarm = player.serverLevel()
                .getEntitiesOfClass(BugSwarmEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyTransformationJutsu = player.serverLevel()
                .getEntitiesOfClass(TransformationJutsuEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyChidori = player.serverLevel()
                .getEntitiesOfClass(ChidoriEntity.class, player.getBoundingBox().inflate(32.0D))
                .size();
        int nearbyChidoriSpear = player.serverLevel()
                .getEntitiesOfClass(ChidoriSpearEntity.class, player.getBoundingBox().inflate(32.0D))
                .size();
        int nearbyLightningArc = player.serverLevel()
                .getEntitiesOfClass(LightningArcEntity.class, player.getBoundingBox().inflate(32.0D))
                .size();
        int nearbyChakraMode = player.serverLevel()
                .getEntitiesOfClass(RaitonChakraModeEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyLightningBeast = player.serverLevel()
                .getEntitiesOfClass(LightningBeastEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyFalseDarkness = player.serverLevel()
                .getEntitiesOfClass(FalseDarknessEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyKirin = player.serverLevel()
                .getEntitiesOfClass(KirinEntity.class, player.getBoundingBox().inflate(160.0D))
                .size();
        int nearbyKatonFireball = player.serverLevel()
                .getEntitiesOfClass(KatonFireballEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyKatonFireStream = player.serverLevel()
                .getEntitiesOfClass(KatonFireStreamEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyHidingInAsh = player.serverLevel()
                .getEntitiesOfClass(HidingInAshEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyHidingInRock = player.serverLevel()
                .getEntitiesOfClass(HidingInRockEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyEarthSpears = player.serverLevel()
                .getEntitiesOfClass(EarthSpearsEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyEarthWall = player.serverLevel()
                .getEntitiesOfClass(EarthWallEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbySwampPit = player.serverLevel()
                .getEntitiesOfClass(SwampPitEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyEarthSandwich = player.serverLevel()
                .getEntitiesOfClass(EarthSandwichEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyFutonVacuum = player.serverLevel()
                .getEntitiesOfClass(FutonVacuumEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyBigBlow = player.serverLevel()
                .getEntitiesOfClass(FutonGreatBreakthroughEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyFutonChakraFlow = player.serverLevel()
                .getEntitiesOfClass(FutonChakraFlowEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyRasenshuriken = player.serverLevel()
                .getEntitiesOfClass(RasenshurikenEntity.class, player.getBoundingBox().inflate(128.0D))
                .size();
        int specialEventsPending = SpecialEvent.pendingCount(source.getServer());
        int sphericalExplosionEvents = SpecialEvent.pendingCount(source.getServer(), SpecialEvent.SPHERICAL_EXPLOSION_TYPE);
        int nearbyBuddha1000 = countNearbyBuddha1000(player);
        String woodBuddhaVehicle = describeWoodBuddhaVehicle(player);
        String snake8HeadsVehicle = describeSnake8HeadsVehicle(player);
        int nearbySuitonMist = player.serverLevel()
                .getEntitiesOfClass(SuitonMistEntity.class, player.getBoundingBox().inflate(128.0D))
                .size();
        int nearbySuitonStream = player.serverLevel()
                .getEntitiesOfClass(SuitonStreamEntity.class, player.getBoundingBox().inflate(80.0D))
                .size();
        int nearbyWaterDragon = player.serverLevel()
                .getEntitiesOfClass(WaterDragonEntity.class, player.getBoundingBox().inflate(96.0D))
                .size();
        int nearbyWaterPrison = player.serverLevel()
                .getEntitiesOfClass(WaterPrisonEntity.class, player.getBoundingBox().inflate(32.0D))
                .size();
        int nearbyWaterShark = player.serverLevel()
                .getEntitiesOfClass(WaterSharkEntity.class, player.getBoundingBox().inflate(96.0D))
                .size();
        int nearbyWaterShockwave = player.serverLevel()
                .getEntitiesOfClass(WaterShockwaveEntity.class, player.getBoundingBox().inflate(128.0D))
                .size();
        int nearbyPoisonMist = player.serverLevel()
                .getEntitiesOfClass(PoisonMistEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyCellularActivation = player.serverLevel()
                .getEntitiesOfClass(CellularActivationEntity.class, player.getBoundingBox().inflate(64.0D))
                .size();
        int nearbyMindTransfer = player.serverLevel()
                .getEntitiesOfClass(MindTransferEntity.class, player.getBoundingBox().inflate(96.0D))
                .size();
        int nearbyMindTransferSelf = player.serverLevel()
                .getEntitiesOfClass(MindTransferSelfEntity.class, player.getBoundingBox().inflate(96.0D))
                .size();
        int nearbyShadowImitation = player.serverLevel()
                .getEntitiesOfClass(ShadowImitationEntity.class, player.getBoundingBox().inflate(96.0D))
                .size();
        boolean replacementActive = NinjutsuItem.isReplacementActive(ninjutsuStack);
        boolean hidingCamouflageActive = NinjutsuItem.isHidingInCamouflageActive(ninjutsuStack);
        boolean enhancedStrengthActive = IryoJutsuItem.isEnhancedStrengthActive(iryoStack);
        boolean cellularActivationActive = CellularActivationEntity.hasActiveFor(player);
        boolean mindTransferActive = MindTransferEntity.hasActiveFor(player);
        boolean shadowImitationActive = ShadowImitationEntity.hasAnyActiveFor(player);
        boolean noClipFlag = NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.NO_CLIP_FLAG);
        source.sendSuccess(() -> Component.literal("M5 jutsu state: ninjutsu="
                + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.RASENGAN)
                + ", rasengan_learned=" + NinjutsuItem.hasLearnedRasengan(ninjutsuStack)
                + ", powerincrease_mainhand=" + powerIncreaseMainhand
                + ", powerincrease_offhand=" + powerIncreaseOffhand
                + ", powerincrease_byakugan_head=" + powerIncreaseByakuganHead
                + ", powerincrease_byakugan_active=" + powerIncreaseByakuganActive
                + ", powerincrease_byakugan_fov=" + powerIncreaseByakuganFov
                + ", byakugan_projected_camera_distance=" + String.format("%.2f", byakuganProjectedCameraDistance)
                + ", byakugan_target_render_distance=" + byakuganTargetRenderDistance
                + ", byakugan_rinnesharingan=" + byakuganRinneSharingan
                + ", byakurin_shockwave_press_time=" + byakurinShockwavePressTime
                + ", byakugan_64_cooldown_ticks=" + byakugan64CooldownTicks
                + ", byakugan_kaiten_cooldown_ticks=" + byakuganKaitenCooldownTicks
                + ", byakugan_kaiten_vehicle=" + byakuganKaitenVehicle
                + ", powerincrease_sharingan_head=" + powerIncreaseSharinganHead
                + ", powerincrease_susanoo_vehicle=" + powerIncreaseSusanooVehicle
                + ", nearby_susanoo_skeletons=" + nearbyPowerIncreaseSusanooSkeletons
                + ", nearby_susanoo_clothed=" + nearbyPowerIncreaseSusanooClothed
                + ", nearby_susanoo_winged=" + nearbyPowerIncreaseSusanooWinged
                + ", ninjutsu_replacement=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.REPLACEMENT)
                + ", replacement_active=" + replacementActive
                + ", ninjutsu_kage_bunshin=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.KAGE_BUNSHIN)
                + ", ninjutsu_limbo_clone=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.LIMBO_CLONE)
                + ", rinnegan_head=" + (RinneganSpecialJutsuHandler.isRinneganLikeHead(headStack)
                        ? describeArmorSlot(player, EquipmentSlot.HEAD)
                        : "none")
                + ", rinnegan_path=" + rinneganPath
                + ", rinnegan_asura_body_equipped=" + asuraBodyEquipped
                + ", rinnegan_asura_cannon_offhand=" + asuraCannonEquipped
                + ", rinnegan_asura_chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", rinnegan_asura_offhand=" + describeWeaponStack(asuraCannonOffhand)
                + ", rinnegan_asura_ticks_used=" + asuraTicksUsed
                + ", rinnegan_asura_strength=" + describeEffectInstance(player.getEffect(MobEffects.DAMAGE_BOOST))
                + ", chibaku_tensei_cooldown_ticks=" + chibakuCooldownTicks
                + ", rinnegan_summoned_animal_id=" + summonedAnimalId
                + ", rinnegan_koh_id_present=" + kingOfHellIdPresent
                + ", nearby_rinnegan_gedo_statue=" + nearbyRinneganGedoStatues
                + ", loaded_gedo_statues=" + loadedGedoStatues
                + ", loaded_ten_tails=" + loadedTenTails
                + ", gedo_sealed_tails=" + gedoSealedTails
                + ", ninjutsu_sealing_chain=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.SEALING_CHAIN)
                + ", ninjutsu_puppet=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.PUPPET)
                + ", scroll_karasu=" + describeInventoryPuppetScroll(player, ModItems.SCROLL_KARASU.get())
                + ", scroll_sanshouo=" + describeInventoryPuppetScroll(player, ModItems.SCROLL_SANSHOUO.get())
                + ", ninjutsu_bug_swarm=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.BUG_SWARM)
                + ", ninjutsu_hiding_camouflage=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.INVISIBILITY)
                + ", ninjutsu_transformation=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.TRANSFORM)
                + ", hiding_camouflage_active=" + hidingCamouflageActive
                + ", senjutsu_sage_rasengan=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.SAGE_RASENGAN)
                + ", senjutsu_sage_rasenshuriken=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.SAGE_RASENSHURIKEN)
                + ", senjutsu_wood_buddha=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.WOOD_BUDDHA)
                + ", wood_buddha_vehicle=" + woodBuddhaVehicle
                + ", senjutsu_snake_8_heads=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.SNAKE_8_HEADS)
                + ", snake_8_heads_vehicle=" + snake8HeadsVehicle
                + ", raiton_chidori=" + describeJutsuDefinitionState(raitonStack, RaitonItem.CHIDORI)
                + ", raiton_chakra_mode=" + describeJutsuDefinitionState(raitonStack, RaitonItem.CHAKRA_MODE)
                + ", raiton_lightning_beast=" + describeJutsuDefinitionState(raitonStack, RaitonItem.CHASING_DOG)
                + ", raiton_false_darkness=" + describeJutsuDefinitionState(raitonStack, RaitonItem.FALSE_DARKNESS)
                + ", raiton_kirin=" + describeJutsuDefinitionState(raitonStack, RaitonItem.KIRIN)
                + ", katon_great_fireball=" + describeJutsuDefinitionState(katonStack, KatonItem.GREAT_FIREBALL)
                + ", katon_fire_annihilation=" + describeJutsuDefinitionState(katonStack, KatonItem.FIRE_ANNIHILATION)
                + ", katon_hiding_in_ash=" + describeJutsuDefinitionState(katonStack, KatonItem.HIDING_IN_ASH)
                + ", katon_great_flame=" + describeJutsuDefinitionState(katonStack, KatonItem.GREAT_FLAME)
                + ", doton_hiding_in_rock=" + describeJutsuDefinitionState(dotonStack, DotonItem.HIDING_IN_ROCK)
                + ", doton_earth_wall=" + describeJutsuDefinitionState(dotonStack, DotonItem.EARTH_WALL)
                + ", doton_earth_sandwich=" + describeJutsuDefinitionState(dotonStack, DotonItem.EARTH_SANDWICH)
                + ", doton_swamp_pit=" + describeJutsuDefinitionState(dotonStack, DotonItem.SWAMP_PIT)
                + ", doton_earth_spears=" + describeJutsuDefinitionState(dotonStack, DotonItem.EARTH_SPEARS)
                + ", futon_chakra_flow=" + describeJutsuDefinitionState(futonStack, FutonItem.CHAKRA_FLOW)
                + ", futon_rasenshuriken=" + describeJutsuDefinitionState(futonStack, FutonItem.RASENSHURIKEN)
                + ", futon_vacuum=" + describeJutsuDefinitionState(futonStack, FutonItem.VACUUM)
                + ", futon_big_blow=" + describeJutsuDefinitionState(futonStack, FutonItem.BIG_BLOW)
                + ", suiton_hiding_in_mist=" + describeJutsuDefinitionState(suitonStack, SuitonItem.HIDING_IN_MIST)
                + ", suiton_water_bullet=" + describeJutsuDefinitionState(suitonStack, SuitonItem.WATER_BULLET)
                + ", suiton_water_dragon=" + describeJutsuDefinitionState(suitonStack, SuitonItem.WATER_DRAGON)
                + ", suiton_water_prison=" + describeJutsuDefinitionState(suitonStack, SuitonItem.WATER_PRISON)
                + ", suiton_water_shark=" + describeJutsuDefinitionState(suitonStack, SuitonItem.WATER_SHARK)
                + ", suiton_water_shockwave=" + describeJutsuDefinitionState(suitonStack, SuitonItem.WATER_SHOCKWAVE)
                + ", iryo_healing=" + describeJutsuDefinitionState(iryoStack, IryoJutsuItem.HEALING)
                + ", iryo_poison_mist=" + describeJutsuDefinitionState(iryoStack, IryoJutsuItem.POISON_MIST)
                + ", iryo_cellular_activation=" + describeJutsuDefinitionState(iryoStack, IryoJutsuItem.CELLULAR_ACTIVATION)
                + ", iryo_enhanced_strength=" + describeJutsuDefinitionState(iryoStack, IryoJutsuItem.ENHANCED_STRENGTH)
                + ", cellular_activation_active=" + cellularActivationActive
                + ", enhanced_strength_active=" + enhancedStrengthActive
                + ", inton_genjutsu=" + describeJutsuDefinitionState(intonStack, IntonItem.GENJUTSU)
                + ", inton_mind_transfer=" + describeJutsuDefinitionState(intonStack, IntonItem.MIND_TRANSFER)
                + ", inton_shadow_imitation=" + describeJutsuDefinitionState(intonStack, IntonItem.SHADOW_IMITATION)
                + ", mind_transfer_active=" + mindTransferActive
                + ", shadow_imitation_active=" + shadowImitationActive
                + ", nearby_replacementclone=" + nearbyReplacementClone
                + ", nearby_senjutsu_platforms=" + nearbySenjutsuPlatforms
                + ", nearby_kage_bunshin=" + nearbyKageBunshin
                + ", nearby_limbo_clone=" + nearbyLimboClone
                + ", nearby_chibaku_balls=" + nearbyChibakuBalls
                + ", nearby_chibaku_satellites=" + nearbyChibakuSatellites
                + ", nearby_byakugan_eight_trigrams=" + nearbyByakuganEightTrigrams
                + ", nearby_byakugan_kaiten=" + nearbyByakuganKaiten
                + ", nearby_byakurin_portal_blocks=" + nearbyByakurinPortalBlocks
                + ", nearby_rinnegan_preta_shields=" + nearbyRinneganPretaShields
                + ", nearby_rinnegan_animal_dogs=" + nearbyRinneganAnimalDogs
                + ", nearby_rinnegan_king_of_hell=" + nearbyRinneganKingOfHell
                + ", nearby_rinnegan_gedo_statues=" + nearbyRinneganGedoStatues
                + ", nearby_sealing_chains=" + nearbySealingChains
                + ", nearby_puppet_karasu=" + nearbyPuppetKarasu
                + ", nearby_puppet_sanshouo=" + nearbyPuppetSanshouo
                + ", nearby_karasu_opening_scrolls=" + nearbyKarasuOpeningScrolls
                + ", nearby_sanshouo_opening_scrolls=" + nearbySanshouoOpeningScrolls
                + ", nearby_puppet_hiruko=" + nearbyPuppetHiruko
                + ", nearby_bug_swarm=" + nearbyBugSwarm
                + ", nearby_transformation_jutsu=" + nearbyTransformationJutsu
                + ", nearby_chidori=" + nearbyChidori
                + ", nearby_chidori_spear=" + nearbyChidoriSpear
                + ", nearby_lightning_arc=" + nearbyLightningArc
                + ", nearby_raiton_chakra_mode=" + nearbyChakraMode
                + ", nearby_lightning_beast=" + nearbyLightningBeast
                + ", nearby_false_darkness=" + nearbyFalseDarkness
                + ", nearby_kirin=" + nearbyKirin
                + ", nearby_katon_fireball=" + nearbyKatonFireball
                + ", nearby_katon_firestream=" + nearbyKatonFireStream
                + ", nearby_hiding_in_ash=" + nearbyHidingInAsh
                + ", nearby_hiding_in_rock=" + nearbyHidingInRock
                + ", nearby_earth_wall=" + nearbyEarthWall
                + ", nearby_earth_sandwich=" + nearbyEarthSandwich
                + ", nearby_swamp_pit=" + nearbySwampPit
                + ", nearby_earth_spears=" + nearbyEarthSpears
                + ", nearby_futon_chakra_flow=" + nearbyFutonChakraFlow
                + ", nearby_rasenshuriken=" + nearbyRasenshuriken
                + ", special_events_pending=" + specialEventsPending
                + ", spherical_explosion_events=" + sphericalExplosionEvents
                + ", nearby_buddha_1000=" + nearbyBuddha1000
                + ", nearby_buddha_arms=" + countNearbyBuddhaArms(player)
                + ", nearby_futon_vacuum=" + nearbyFutonVacuum
                + ", nearby_big_blow=" + nearbyBigBlow
                + ", nearby_suitonmist=" + nearbySuitonMist
                + ", nearby_suitonstream=" + nearbySuitonStream
                + ", nearby_water_dragon=" + nearbyWaterDragon
                + ", nearby_water_prison=" + nearbyWaterPrison
                + ", nearby_water_shark=" + nearbyWaterShark
                + ", nearby_water_shockwave=" + nearbyWaterShockwave
                + ", nearby_poison_mist=" + nearbyPoisonMist
                + ", nearby_cellular_activation=" + nearbyCellularActivation
                + ", nearby_mind_transfer=" + nearbyMindTransfer
                + ", nearby_mind_transfer_self=" + nearbyMindTransferSelf
                + ", nearby_shadow_imitation=" + nearbyShadowImitation
                + ", no_clip_flag=" + noClipFlag), false);
        return 1;
    }

    private static int prepareNinjaToolsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack kunai = getOrGiveItem(player, ModItems.KUNAI.get());
        ItemStack explosiveKunai = getOrGiveItem(player, ModItems.KUNAI_EXPLOSIVE.get());
        ItemStack shuriken = getOrGiveItem(player, ModItems.SHURIKEN.get());
        ItemStack smokeBomb = getOrGiveItem(player, ModItems.SMOKE_BOMB.get());
        kunai.setCount(kunai.getMaxStackSize());
        explosiveKunai.setCount(explosiveKunai.getMaxStackSize());
        shuriken.setCount(shuriken.getMaxStackSize());
        smokeBomb.setCount(smokeBomb.getMaxStackSize());
        source.sendSuccess(() -> Component.literal("Ninja tools test ready: kunai=" + kunai.getCount()
                + ", kunai_explosive=" + explosiveKunai.getCount()
                + ", shuriken=" + shuriken.getCount()
                + ", smoke_bomb=" + smokeBomb.getCount()
                + ". Hold right-click and release to throw; check with /narutoport m5_tools state."), false);
        return 1;
    }

    private static int prepareSpecialWeaponsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.RINNEGANHELMET.get());
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isAshBonesAllowedHead(head)) {
            if (!head.isEmpty()) {
                ItemStack displaced = head.copy();
                if (!player.getInventory().add(displaced)) {
                    player.drop(displaced, false);
                }
            }
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.BYAKURINNESHARINGANHELMET.get()));
        }
        ItemStack ashBones = getOrGiveItem(player, ModItems.ASHBONES.get());
        ItemStack blackReceiver = getOrGiveItem(player, ModItems.BLACK_RECEIVER.get());
        ashBones.setCount(1);
        blackReceiver.setDamageValue(0);
        source.sendSuccess(() -> Component.literal("Special weapons test ready: ashbones=" + ashBones.getCount()
                + ", black_receiver=" + blackReceiver.getCount()
                + ", black_receiver_damage=" + blackReceiver.getDamageValue()
                + "/" + blackReceiver.getMaxDamage()
                + ", ashbones_head_allowed=" + isAshBonesAllowedHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ". Hold right-click and release to throw; check with /narutoport m5_tools state."), false);
        return 1;
    }

    private static int prepareFoldingFanTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack foldingFan = getOrGiveItem(player, ModItems.FOLDING_FAN.get());
        foldingFan.setDamageValue(0);
        int targets = spawnToolTargetArmorStand(player, "M5 Folding Fan target");
        source.sendSuccess(() -> Component.literal("Folding Fan test ready: folding_fan=" + foldingFan.getCount()
                + ", damage=" + foldingFan.getDamageValue()
                + "/" + foldingFan.getMaxDamage()
                + ", target_armor_stands=" + targets
                + ". Hold right-click and release to throw and trigger Great Breakthrough; check with /narutoport m5_tools state."), false);
        return 1;
    }

    private static int prepareKibaBladesTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack kibaBlades = getOrGiveItem(player, ModItems.KIBA_BLADES.get());
        int targets = spawnToolTargetArmorStand(player, "M5 Kiba Blades target");
        source.sendSuccess(() -> Component.literal("Kiba Blades test ready: kiba_blades=" + kibaBlades.getCount()
                + ", target_armor_stands=" + targets
                + ". Select Kiba Blades, hold right-click for at least 20 ticks, and release while looking at the target; melee hits can trigger lightning."), false);
        return 1;
    }

    private static int prepareKusanagiTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack kusanagi = getOrGiveItem(player, ModItems.KUSANAGI_SWORD.get());
        int targets = spawnToolTargetArmorStand(player, "M5 Kusanagi target");
        source.sendSuccess(() -> Component.literal("Kusanagi test ready: kusanagi_sword=" + kusanagi.getCount()
                + ", target_armor_stands=" + targets
                + ". Select Kusanagi, hold right-click, release to throw, then move near the returning sword to recover it."), false);
        return 1;
    }

    private static int prepareHiramekareiTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        ItemStack hiramekarei = getOrGiveItem(player, ModItems.HIRAMEKAREI_SWORD.get());
        HiramekareiEffectEntity.clearActiveTag(hiramekarei);
        source.sendSuccess(() -> Component.literal("Hiramekarei test ready: hiramekarei_sword=" + hiramekarei.getCount()
                + ", expected_active_strength=" + (Chakra.getLevel(player) * 0.5D)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ". Select Hiramekarei, hold right-click, then release to activate the chakra blade window."), false);
        return 1;
    }

    private static int prepareAsuraCannonTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack asuraCannon = getOrGiveItem(player, ModItems.ASURACANON.get());
        asuraCannon.setDamageValue(0);
        source.sendSuccess(() -> Component.literal("Asura Cannon test ready: asuracanon=" + asuraCannon.getCount()
                + ", damage=" + asuraCannon.getDamageValue()
                + "/" + asuraCannon.getMaxDamage()
                + ". Select Asura Cannon, hold right-click, release into a safe area, and check /narutoport m5_tools state."), false);
        return 1;
    }

    private static int prepareKamuiShurikenTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 10000.0D);
        ItemStack kamuiShuriken = getOrGiveItem(player, ModItems.KAMUISHURIKEN.get());
        int targets = spawnToolTargetArmorStand(player, "M5 Kamui Shuriken target");
        source.sendSuccess(() -> Component.literal("Kamui Shuriken test ready: kamuishuriken=" + kamuiShuriken.getCount()
                + ", target_armor_stands=" + targets
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ". Select Kamui Shuriken, hold right-click, release toward the target, and check /narutoport m5_tools state."), false);
        return 1;
    }

    private static int prepareNuibariTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack nuibari = getOrGiveItem(player, ModItems.NUIBARI_SWORD.get());
        int targets = spawnToolTargetArmorStand(player, "M5 Nuibari target");
        source.sendSuccess(() -> Component.literal("Nuibari test ready: nuibari_sword=" + nuibari.getCount()
                + ", target_armor_stands=" + targets
                + ". Select Nuibari, hold right-click and release to throw; use the Nuibari Thrown item to retrieve it."), false);
        return 1;
    }

    private static int reportM5ToolsState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack hiramekarei = findItem(player, ModItems.HIRAMEKAREI_SWORD.get());
        ItemStack asuraCannon = findItem(player, ModItems.ASURACANON.get());
        source.sendSuccess(() -> Component.literal("M5 tools state: kunai_stack=" + findItem(player, ModItems.KUNAI.get()).getCount()
                + ", kunai_explosive_stack=" + findItem(player, ModItems.KUNAI_EXPLOSIVE.get()).getCount()
                + ", shuriken_stack=" + findItem(player, ModItems.SHURIKEN.get()).getCount()
                + ", smoke_bomb_stack=" + findItem(player, ModItems.SMOKE_BOMB.get()).getCount()
                + ", ashbones_stack=" + findItem(player, ModItems.ASHBONES.get()).getCount()
                + ", black_receiver_stack=" + findItem(player, ModItems.BLACK_RECEIVER.get()).getCount()
                + ", folding_fan_stack=" + findItem(player, ModItems.FOLDING_FAN.get()).getCount()
                + ", kiba_blades_stack=" + findItem(player, ModItems.KIBA_BLADES.get()).getCount()
                + ", kusanagi_stack=" + findItem(player, ModItems.KUSANAGI_SWORD.get()).getCount()
                + ", hiramekarei_stack=" + hiramekarei.getCount()
                + ", hiramekarei_active=" + HiramekareiEffectEntity.isActive(player.serverLevel(), hiramekarei)
                + ", hiramekarei_strength=" + HiramekareiEffectEntity.getActiveStrength(hiramekarei)
                + ", asura_cannon_stack=" + asuraCannon.getCount()
                + ", asura_cannon_damage=" + asuraCannon.getDamageValue()
                + "/" + asuraCannon.getMaxDamage()
                + ", kamui_shuriken_stack=" + findItem(player, ModItems.KAMUISHURIKEN.get()).getCount()
                + ", nuibari_sword_stack=" + findItem(player, ModItems.NUIBARI_SWORD.get()).getCount()
                + ", nuibari_thrown_stack=" + findItem(player, ModItems.NUIBARI_THROWN.get()).getCount()
                + ", ashbones_head_allowed=" + isAshBonesAllowedHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", nearby_kunai_projectiles=" + countThrownTool(player, ModEntityTypes.ENTITYBULLETKUNAI.get())
                + ", nearby_kunai_explosive_projectiles=" + countThrownTool(player, ModEntityTypes.ENTITYBULLETKUNAI_EXPLOSIVE.get())
                + ", nearby_shuriken_projectiles=" + countThrownTool(player, ModEntityTypes.ENTITYBULLETSHURIKEN.get())
                + ", nearby_smoke_bomb_projectiles=" + countThrownTool(player, ModEntityTypes.ENTITYBULLETSMOKE_BOMB.get())
                + ", nearby_ashbones_projectiles=" + countThrownSpecialWeapon(player, ModEntityTypes.ENTITYBULLETASHBONES.get())
                + ", nearby_black_receiver_projectiles=" + countThrownSpecialWeapon(player, ModEntityTypes.ENTITYBULLETBLACK_RECEIVER.get())
                + ", nearby_folding_fan_projectiles=" + countFoldingFanProjectile(player)
                + ", nearby_great_breakthrough=" + player.serverLevel()
                        .getEntitiesOfClass(FutonGreatBreakthroughEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_kiba_blade_aura=" + player.serverLevel()
                        .getEntitiesOfClass(KibaBladeAuraEntity.class, player.getBoundingBox().inflate(32.0D)).size()
                + ", nearby_false_darkness=" + player.serverLevel()
                        .getEntitiesOfClass(FalseDarknessEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_lightning_arc=" + player.serverLevel()
                        .getEntitiesOfClass(LightningArcEntity.class, player.getBoundingBox().inflate(32.0D)).size()
                + ", nearby_kusanagi_sword=" + player.serverLevel()
                        .getEntitiesOfClass(KusanagiSwordEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_hiramekarei_effect=" + player.serverLevel()
                        .getEntitiesOfClass(HiramekareiEffectEntity.class, player.getBoundingBox().inflate(32.0D)).size()
                + ", nearby_asura_cannonballs=" + player.serverLevel()
                        .getEntitiesOfClass(AsuraCannonballEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_kamui_shuriken=" + player.serverLevel()
                        .getEntitiesOfClass(KamuiShurikenEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_nuibari_sword=" + player.serverLevel()
                        .getEntitiesOfClass(NuibariSwordEntity.class, player.getBoundingBox().inflate(96.0D)).size()), false);
        return 1;
    }

    private static int prepareBasicArmorTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.NINJA_ARMOR_KONOHAHELMET.get()));
        equipDebugArmor(player, EquipmentSlot.CHEST, new ItemStack(ModItems.NINJA_ARMOR_KONOHABODY.get()));
        equipDebugArmor(player, EquipmentSlot.LEGS, new ItemStack(ModItems.NINJA_ARMOR_KONOHALEGS.get()));
        equipDebugArmor(player, EquipmentSlot.FEET, new ItemStack(ModItems.UCHIHABOOTS.get()));
        getOrGiveItem(player, ModItems.AKATSUKI_ROBEHELMET.get());
        getOrGiveItem(player, ModItems.AKATSUKI_ROBEBODY.get());
        getOrGiveItem(player, ModItems.SAMURAI_ARMORHELMET.get());
        getOrGiveItem(player, ModItems.SAMURAI_ARMORBODY.get());
        getOrGiveItem(player, ModItems.SAMURAI_ARMORLEGS.get());
        getOrGiveItem(player, ModItems.RINNEGANHELMET.get());
        source.sendSuccess(() -> Component.literal("Basic armor test ready: equipped ninja_armor_konoha head/body/legs "
                + "and uchihaboots; extra akatsuki, samurai, and rinnegan samples are in inventory. "
                + "Check with /narutoport m5_armor state."), false);
        return 1;
    }

    private static int reportM5ArmorState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 armor state: head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", legs=" + describeArmorSlot(player, EquipmentSlot.LEGS)
                + ", feet=" + describeArmorSlot(player, EquipmentSlot.FEET)
                + ", akatsuki_robehelmet_stack=" + findItem(player, ModItems.AKATSUKI_ROBEHELMET.get()).getCount()
                + ", akatsuki_robebody_stack=" + findItem(player, ModItems.AKATSUKI_ROBEBODY.get()).getCount()
                + ", samurai_armorhelmet_stack=" + findItem(player, ModItems.SAMURAI_ARMORHELMET.get()).getCount()
                + ", samurai_armorbody_stack=" + findItem(player, ModItems.SAMURAI_ARMORBODY.get()).getCount()
                + ", samurai_armorlegs_stack=" + findItem(player, ModItems.SAMURAI_ARMORLEGS.get()).getCount()
                + ", rinneganhelmet_stack=" + findItem(player, ModItems.RINNEGANHELMET.get()).getCount()), false);
        return 1;
    }

    private static void equipDebugArmor(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty()) {
            ItemStack displaced = current.copy();
            if (!player.getInventory().add(displaced)) {
                player.drop(displaced, false);
            }
        }
        player.setItemSlot(slot, stack);
    }

    private static String describeArmorSlot(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return "empty";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return itemId + "[" + armorItem.getType().getName() + "]";
        }
        return itemId + "[not_armor]";
    }

    private static int prepareBasicMeleeWeaponsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.ANBU_SWORD.get());
        getOrGiveItem(player, ModItems.BONE_SWORD.get());
        getOrGiveItem(player, ModItems.BONE_DRILL.get());
        getOrGiveItem(player, ModItems.CHOKUTO.get());
        getOrGiveItem(player, ModItems.KABUTOWARI.get());
        getOrGiveItem(player, ModItems.SAMEHADA.get());
        getOrGiveItem(player, ModItems.SHIBUKI_SWORD.get());
        getOrGiveItem(player, ModItems.TOTSUKA_SWORD.get());
        getOrGiveItem(player, ModItems.ZABUZA_SWORD.get());
        int targets = spawnToolTargetArmorStand(player, "M5 Basic Melee target");
        source.sendSuccess(() -> Component.literal("Basic melee weapons test ready: anbu_sword, bone_sword, bone_drill, "
                + "chokuto, kabutowari, samehada, shibuki_sword, totsuka_sword, and zabuza_sword are in inventory; "
                + "target_armor_stands=" + targets + ". Check with /narutoport m5_weapons state."), false);
        return 1;
    }

    private static int prepareIceSenbonTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.ICE_SENBON.get());
        stack.setDamageValue(0);
        int targets = spawnToolTargetArmorStand(player, "M5 Ice Senbon target");
        source.sendSuccess(() -> Component.literal("Ice Senbon test ready: ice_senbon is in inventory, damage reset, "
                + "target_armor_stands=" + targets + ". Check with /narutoport m5_weapons state."), false);
        return 1;
    }

    private static int reportM5WeaponsState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 weapons state: mainhand=" + describeWeaponStack(player.getMainHandItem())
                + ", offhand=" + describeWeaponStack(player.getOffhandItem())
                + ", anbu_sword=" + describeInventoryWeapon(player, ModItems.ANBU_SWORD.get())
                + ", bone_sword=" + describeInventoryWeapon(player, ModItems.BONE_SWORD.get())
                + ", bone_drill=" + describeInventoryWeapon(player, ModItems.BONE_DRILL.get())
                + ", chokuto=" + describeInventoryWeapon(player, ModItems.CHOKUTO.get())
                + ", kabutowari=" + describeInventoryWeapon(player, ModItems.KABUTOWARI.get())
                + ", kabutowari_axe=" + describeInventoryWeapon(player, ModItems.KABUTOWARI_AXE.get())
                + ", kabutowari_hammer=" + describeInventoryWeapon(player, ModItems.KABUTOWARI_HAMMER.get())
                + ", samehada=" + describeInventoryWeapon(player, ModItems.SAMEHADA.get())
                + ", shibuki_sword=" + describeInventoryWeapon(player, ModItems.SHIBUKI_SWORD.get())
                + ", totsuka_sword=" + describeInventoryWeapon(player, ModItems.TOTSUKA_SWORD.get())
                + ", zabuza_sword=" + describeInventoryWeapon(player, ModItems.ZABUZA_SWORD.get())
                + ", ice_senbon=" + describeInventoryWeapon(player, ModItems.ICE_SENBON.get())
                + ", kagutsuchi_sword=" + describeKagutsuchiStack(findItem(player, ModItems.KAGUTSUCHISWORDRANGED.get()))
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareKagutsuchiSwordTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.KAGUTSUCHISWORDRANGED.get());
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.MANGEKYOSHARINGANHELMET.get()));
        prepareJutsuResourcePool(player, 90000.0D);
        int targets = spawnToolTargetArmorStand(player, "M5 Kagutsuchi target");
        source.sendSuccess(() -> Component.literal("Kagutsuchi Sword test ready: " + describeKagutsuchiStack(stack)
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", target_armor_stands=" + targets
                + ". Select it, hold right-click and release to fire; check /narutoport m5_kagutsuchi state."), false);
        return 1;
    }

    private static int reportM5KagutsuchiState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.KAGUTSUCHISWORDRANGED.get());
        MobEffectInstance amaterasu = player.getEffect(ModEffects.AMATERASUFLAME.get());
        source.sendSuccess(() -> Component.literal("M5 kagutsuchi: sword=" + describeKagutsuchiStack(stack)
                + ", mainhand=" + describeKagutsuchiStack(player.getMainHandItem())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", riding_winged_susanoo=" + KagutsuchiSwordItem.isHolderRidingWingedSusanoo(player)
                + ", susanoo_scale=" + KagutsuchiSwordItem.susanooScale(player)
                + ", cooldown=" + player.getCooldowns().isOnCooldown(ModItems.KAGUTSUCHISWORDRANGED.get())
                + ", amaterasu_effect=" + (amaterasu != null ? amaterasu.getDuration() + "t/a" + amaterasu.getAmplifier() : "none")
                + ", nearby_small_fireballs=" + countNearbyKagutsuchiFireballs(player, false)
                + ", nearby_big_fireballs=" + countNearbyKagutsuchiFireballs(player, true)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static String describeKagutsuchiStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId + "[kagutsuchi=" + (stack.getItem() instanceof KagutsuchiSwordItem)
                + ",count=" + stack.getCount()
                + ",max=" + stack.getMaxStackSize()
                + "]";
    }

    private static String describeEffectInstance(MobEffectInstance effect) {
        return effect == null ? "none" : effect.getDuration() + "t/a" + effect.getAmplifier();
    }

    private static int countNearbyKagutsuchiFireballs(ServerPlayer player, boolean big) {
        EntityType<?> expectedType = big
                ? ModEntityTypes.ENTITYKAGUTSUCHISWORDBIGFIREBALL.get()
                : ModEntityTypes.ENTITYKAGUTSUCHISWORDFIREBALL.get();
        return player.serverLevel()
                .getEntitiesOfClass(KagutsuchiFireballEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == expectedType)
                .size();
    }

    private static int prepareMedicalScrollTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack scroll = getOrGiveItem(player, ModItems.MEDICAL_SCROLL.get());
        ProcedureUtils.grantAdvancement(player, "narutomod:achievementmedicalgenin", false);

        ItemStack mangekyoPrimary = ownedEye(ModItems.MANGEKYOSHARINGANHELMET.get(), player.getUUID());
        mangekyoPrimary.getOrCreateTag().putInt(MedicalScrollItem.SHARINGAN_COLOR_TAG, 0xFF0000);
        ItemStack mangekyoSecondary = ownedEye(ModItems.MANGEKYOSHARINGANOBITOHELMET.get(), UUID.randomUUID());
        ItemStack byakuganPrimary = ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID());
        ItemStack byakuganSecondary = ownedEye(ModItems.BYAKUGANHELMET.get(), UUID.randomUUID());
        giveDebugStack(player, mangekyoPrimary);
        giveDebugStack(player, mangekyoSecondary);
        giveDebugStack(player, byakuganPrimary);
        giveDebugStack(player, byakuganSecondary);

        source.sendSuccess(() -> Component.literal("Medical Scroll test ready: " + describeMedicalScrollStack(scroll)
                + ", medical_genin=" + ProcedureUtils.advancementAchieved(player, "narutomod:achievementmedicalgenin")
                + ". Put the player-owned Mangekyo/Byakugan in the left slot and a random-owned eye in the right slot, then press Activate."
                + " Check with /narutoport m5_medical_scroll state."), false);
        return 1;
    }

    private static int prepareTenseiganEvolutionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack byakugan = ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID());
        CompoundTag tag = byakugan.getOrCreateTag();
        tag.putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, 5.0D);
        tag.putDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME, 20.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, byakugan);

        source.sendSuccess(() -> Component.literal("Tenseigan evolution test ready: head="
                + describeMedicalEyeStack(player.getItemBySlot(EquipmentSlot.HEAD))
                + ". Wait one second or run /narutoport m5_medical_scroll tenseigan_evolution_finish."), false);
        return 1;
    }

    private static int finishTenseiganEvolutionTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.BYAKUGANHELMET.get())) {
            source.sendFailure(Component.literal("Head slot must contain an owned Byakugan."));
            return 0;
        }
        boolean evolved = ByakuganHelmetItem.finishTenseiganEvolution(player, head);
        source.sendSuccess(() -> Component.literal("Tenseigan evolution finish: evolved=" + evolved
                + ", head=" + describeMedicalEyeStack(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", returned_byakugan=" + describeMedicalEyeStack(findItem(player, ModItems.BYAKUGANHELMET.get()))
                + ", tenseigan_achieved=" + ProcedureUtils.advancementAchieved(player, "narutomod:tenseigan_achieved")), false);
        return evolved ? 1 : 0;
    }

    private static int reportM5MedicalScrollState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 medical scroll: scroll=" + describeMedicalScrollStack(findItem(player, ModItems.MEDICAL_SCROLL.get()))
                + ", medical_genin=" + ProcedureUtils.advancementAchieved(player, "narutomod:achievementmedicalgenin")
                + ", tenseigan_achieved=" + ProcedureUtils.advancementAchieved(player, "narutomod:tenseigan_achieved")
                + ", head=" + describeMedicalEyeStack(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", mangekyo=" + describeMedicalEyeStack(findItem(player, ModItems.MANGEKYOSHARINGANHELMET.get()))
                + ", obito_mangekyo=" + describeMedicalEyeStack(findItem(player, ModItems.MANGEKYOSHARINGANOBITOHELMET.get()))
                + ", eternal_mangekyo=" + describeMedicalEyeStack(findItem(player, ModItems.MANGEKYOSHARINGANETERNALHELMET.get()))
                + ", byakugan=" + describeMedicalEyeStack(findItem(player, ModItems.BYAKUGANHELMET.get()))
                + ", tenseigan=" + describeMedicalEyeStack(findItem(player, ModItems.TENSEIGANHELMET.get()))), false);
        return 1;
    }

    private static ItemStack ownedEye(Item item, UUID ownerId) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putUUID("player_id", ownerId);
        ByakuganHelmetItem.ensureImplicitRinnesharinganTag(stack);
        if (ByakuganHelmetItem.isByakuganStack(stack)) {
            ByakuganHelmetItem.ensureByakuganCount(stack);
        }
        return stack;
    }

    private static void giveDebugStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static String describeMedicalScrollStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId + "[medical_scroll=" + (stack.getItem() instanceof MedicalScrollItem)
                + ",count=" + stack.getCount()
                + ",max=" + stack.getMaxStackSize()
                + "]";
    }

    private static String describeMedicalEyeStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        CompoundTag tag = stack.getTag();
        UUID ownerId = ProcedureUtils.getOwnerId(stack);
        String owner = ownerId != null ? ownerId.toString().substring(0, 8) : "none";
        String byakuganCount = tag != null && tag.contains(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG)
                ? String.valueOf(tag.getDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG))
                : "none";
        String evolveTime = tag != null && tag.contains(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME)
                ? String.valueOf(tag.getDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME))
                : "none";
        String color = tag != null && tag.contains(MedicalScrollItem.SHARINGAN_COLOR_TAG)
                ? Integer.toHexString(tag.getInt(MedicalScrollItem.SHARINGAN_COLOR_TAG))
                : "none";
        return itemId + "[owner=" + owner
                + ",byakugan_count=" + byakuganCount
                + ",tenseigan_evolve=" + evolveTime
                + ",sharingan_color=" + color
                + "]";
    }

    private static String describeInventoryWeapon(ServerPlayer player, Item item) {
        return describeWeaponStack(findItem(player, item));
    }

    private static String describeWeaponStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean basicWeapon = stack.getItem() instanceof BasicMeleeWeaponItem;
        boolean iceSenbon = stack.getItem() instanceof IceSenbonItem;
        String durability = stack.isDamageableItem()
                ? ",damage=" + stack.getDamageValue() + "/" + stack.getMaxDamage()
                : "";
        return itemId + "[basic_melee=" + basicWeapon + ",ice_senbon=" + iceSenbon + durability + "]";
    }

    private static int prepareConsumablesAndCurrencyTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ensureInventoryCount(player, ModItems.MILITARY_RATIONS_PILL.get(), 3);
        ensureInventoryCount(player, ModItems.MILITARY_RATIONS_PILL_GOLD.get(), 3);
        ensureInventoryCount(player, ModItems.CHAKRAFRUIT.get(), 1);
        ensureInventoryCount(player, ModItems.WHITEZETSUFLESH.get(), 16);
        ensureInventoryCount(player, ModItems.RYO_100.get(), 4);
        ensureInventoryCount(player, ModItems.RYO_1000.get(), 4);
        ensureInventoryCount(player, ModItems.RYO_10000.get(), 4);
        ensureInventoryCount(player, ModItems.RYO_1_M.get(), 1);
        source.sendSuccess(() -> Component.literal("Consumables and currency test ready: military rations pills, chakrafruit, "
                + "whitezetsuflesh, and ryo samples are in inventory. Check with /narutoport m5_consumables state."), false);
        return 1;
    }

    private static int reportM5ConsumablesState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 consumables state: military_rations_pill="
                + describeInventoryConsumable(player, ModItems.MILITARY_RATIONS_PILL.get())
                + ", military_rations_pill_gold=" + describeInventoryConsumable(player, ModItems.MILITARY_RATIONS_PILL_GOLD.get())
                + ", chakrafruit=" + describeInventoryConsumable(player, ModItems.CHAKRAFRUIT.get())
                + ", whitezetsuflesh=" + describeInventoryConsumable(player, ModItems.WHITEZETSUFLESH.get())
                + ", ryo_100=" + describeInventoryCurrency(player, ModItems.RYO_100.get())
                + ", ryo_1000=" + describeInventoryCurrency(player, ModItems.RYO_1000.get())
                + ", ryo_10000=" + describeInventoryCurrency(player, ModItems.RYO_10000.get())
                + ", ryo_1_m=" + describeInventoryCurrency(player, ModItems.RYO_1_M.get())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static String describeInventoryConsumable(ServerPlayer player, Item item) {
        return describeConsumableStack(findItem(player, item));
    }

    private static String describeConsumableStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId + "[count=" + stack.getCount()
                + ",max=" + stack.getMaxStackSize()
                + ",runtime=" + (stack.getItem() instanceof NarutoConsumableItem)
                + ",foil=" + stack.hasFoil()
                + "]";
    }

    private static String describeInventoryCurrency(ServerPlayer player, Item item) {
        ItemStack stack = findItem(player, item);
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId + "[count=" + stack.getCount()
                + ",max=" + stack.getMaxStackSize()
                + ",ryo=" + (stack.getItem() instanceof RyoItem)
                + "]";
    }

    private static int prepareEightGatesTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.EIGHTGATES.get());
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, player);
            eightGatesItem.setBattleXP(stack, 2760);
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 0.0F);
            stack.getOrCreateTag().putInt(EightGatesItem.SEKIZO_PUNCH_COUNT_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        source.sendSuccess(() -> Component.literal("Eight Gates test ready: " + describeEightGatesStack(player, stack)
                + ". Select it, hold shift-right-click to open gates, and check /narutoport m5_eight_gates state."), false);
        return 1;
    }

    private static int prepareEightGatesHirudoraTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.EIGHTGATES.get());
        if (player.getMainHandItem() != stack) {
            ItemStack prepared = stack.copy();
            stack.setCount(0);
            ItemStack displaced = player.getMainHandItem();
            if (!displaced.isEmpty() && !player.getInventory().add(displaced.copy())) {
                player.drop(displaced.copy(), false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, prepared);
            stack = player.getMainHandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, player);
            eightGatesItem.setBattleXP(stack, 1480);
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 7.0F);
            stack.getOrCreateTag().putInt(EightGatesItem.SEKIZO_PUNCH_COUNT_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack preparedStack = stack;
        source.sendSuccess(() -> Component.literal("Hirudora test ready: " + describeEightGatesStack(player, preparedStack)
                + ". Main-hand release right-click without shift to fire Hirudora."), false);
        return 1;
    }

    private static int prepareEightGatesAsakujakuTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.EIGHTGATES.get());
        if (player.getMainHandItem() != stack) {
            ItemStack prepared = stack.copy();
            stack.setCount(0);
            ItemStack displaced = player.getMainHandItem();
            if (!displaced.isEmpty() && !player.getInventory().add(displaced.copy())) {
                player.drop(displaced.copy(), false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, prepared);
            stack = player.getMainHandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, player);
            eightGatesItem.setBattleXP(stack, 840);
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 6.0F);
            stack.getOrCreateTag().putInt(EightGatesItem.SEKIZO_PUNCH_COUNT_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack preparedStack = stack;
        source.sendSuccess(() -> Component.literal("Asakujaku test ready: " + describeEightGatesStack(player, preparedStack)
                + ". Main-hand left-click a target to fire Asakujaku."), false);
        return 1;
    }

    private static int prepareEightGatesSekizoTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.EIGHTGATES.get());
        if (player.getMainHandItem() != stack) {
            ItemStack prepared = stack.copy();
            stack.setCount(0);
            ItemStack displaced = player.getMainHandItem();
            if (!displaced.isEmpty() && !player.getInventory().add(displaced.copy())) {
                player.drop(displaced.copy(), false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, prepared);
            stack = player.getMainHandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, player);
            eightGatesItem.setBattleXP(stack, 2760);
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 8.0F);
            stack.getOrCreateTag().putInt(EightGatesItem.SEKIZO_PUNCH_COUNT_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack preparedStack = stack;
        source.sendSuccess(() -> Component.literal("Sekizo test ready: " + describeEightGatesStack(player, preparedStack)
                + ". Main-hand left-click a target up to five times to fire Sekizo."), false);
        return 1;
    }

    private static int prepareEightGatesNightGuyTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.EIGHTGATES.get());
        if (player.getMainHandItem() != stack) {
            ItemStack prepared = stack.copy();
            stack.setCount(0);
            ItemStack displaced = player.getMainHandItem();
            if (!displaced.isEmpty() && !player.getInventory().add(displaced.copy())) {
                player.drop(displaced.copy(), false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, prepared);
            stack = player.getMainHandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, player);
            eightGatesItem.setBattleXP(stack, 2760);
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 8.0F);
            stack.getOrCreateTag().putInt(EightGatesItem.SEKIZO_PUNCH_COUNT_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack preparedStack = stack;
        source.sendSuccess(() -> Component.literal("Night Guy test ready: " + describeEightGatesStack(player, preparedStack)
                + ". Main-hand release right-click without shift to fire Night Guy."), false);
        return 1;
    }

    private static int reportM5EightGatesState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.EIGHTGATES.get());
        source.sendSuccess(() -> Component.literal("M5 eight gates: eightgates=" + describeEightGatesStack(player, stack)
                + ", held_main=" + player.getMainHandItem().is(ModItems.EIGHTGATES.get())
                + ", held_offhand=" + player.getOffhandItem().is(ModItems.EIGHTGATES.get())
                + ", cooldown=" + player.getCooldowns().isOnCooldown(ModItems.EIGHTGATES.get())
                + ", mayfly=" + player.getAbilities().mayfly
                + ", flight_effect=" + player.hasEffect(ModEffects.FLIGHT.get())
                + ", health=" + player.getHealth()
                + "/" + player.getMaxHealth()
                + ", nearby_asakujaku_fireballs=" + countNearbyAsakujakuFireballs(player)
                + ", nearby_hirudora=" + countNearbyHirudora(player)
                + ", nearby_sekizo=" + countNearbySekizo(player)
                + ", nearby_night_guy=" + countNearbyNightGuy(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)), false);
        return 1;
    }

    private static int countNearbyAsakujakuFireballs(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(AsakujakuFireballEntity.class, player.getBoundingBox().inflate(96.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyHirudora(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(HirudoraEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbySekizo(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SekizoEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static int countNearbyNightGuy(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(NightGuyDragonEntity.class, player.getBoundingBox().inflate(160.0D), entity -> entity.isAlive())
                .size();
    }

    private static String describeEightGatesStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            return itemId + "[eight_gates=true," + eightGatesItem.describeState(stack, player) + "]";
        }
        return itemId + "[eight_gates=false]";
    }

    private static String describeInventoryPuppetScroll(ServerPlayer player, Item item) {
        return describePuppetScrollStack(findItem(player, item));
    }

    private static String describePuppetScrollStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof PuppetScrollItem puppetScrollItem) {
            return itemId + "[kind=" + puppetScrollItem.kind()
                    + ",sealed=" + PuppetScrollItem.isSealed(stack)
                    + ",damage=" + stack.getDamageValue()
                    + "/" + stack.getMaxDamage()
                    + "]";
        }
        return itemId + "[puppet_scroll=false]";
    }

    private static int prepareAdvancedNatureJutsuTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.FUTON.get());
        getOrGiveItem(player, ModItems.KATON.get());
        getOrGiveItem(player, ModItems.RAITON.get());
        getOrGiveItem(player, ModItems.SUITON.get());
        equipDebugArmor(player, EquipmentSlot.CHEST, new ItemStack(ModItems.GOURDBODY.get()));
        getOrGiveItem(player, ModItems.BAKUTON.get());
        getOrGiveItem(player, ModItems.FUTTON.get());
        getOrGiveItem(player, ModItems.HYOTON.get());
        getOrGiveItem(player, ModItems.JINTON.get());
        getOrGiveItem(player, ModItems.JITON.get());
        getOrGiveItem(player, ModItems.KEKKEI_MORA.get());
        getOrGiveItem(player, ModItems.MOKUTON.get());
        getOrGiveItem(player, ModItems.RANTON.get());
        getOrGiveItem(player, ModItems.SHAKUTON.get());
        getOrGiveItem(player, ModItems.SHIKOTSUMYAKU.get());
        ItemStack yooton = getOrGiveItem(player, ModItems.YOOTON.get());
        prepareYootonMagmaBallStack(player, yooton);
        getOrGiveItem(player, ModItems.YOTON.get());
        getOrGiveItem(player, ModItems.SCROLL_MULTI_SIZE.get());
        spawnToolTargetArmorStand(player, "M5 Magma Ball target");
        source.sendSuccess(() -> Component.literal("Advanced nature jutsu test ready: base nature items, gourd body, "
                + "12 advanced jutsu items, multi-size scroll, and Yooton Magma Ball XP are prepared. "
                + "Select yooton, hold right-click, release toward the target, then check /narutoport m5_advanced_jutsu state."), false);
        return 1;
    }

    private static int prepareYootonMagmaBallTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareYootonJutsuTest(source, 0, "M5 Magma Ball target",
                "Yooton Magma Ball test ready: doton+katon+yooton prepared. "
                        + "Select yooton, hold right-click, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareYootonMeltingJutsuTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareYootonJutsuTest(source, 1, "M5 Melting Jutsu target",
                "Yooton Melting Jutsu test ready: doton+katon+yooton prepared. "
                        + "Select yooton, hold right-click, release toward the target, then watch the lava drip solidify.");
    }

    private static int prepareYootonLavaChakraModeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BijuManager.setPlayerAsJinchuriki(player, 4);
        return prepareYootonJutsuTest(source, 2, "M5 Lava Chakra Mode aura target",
                "Yooton Lava Chakra Mode test ready: doton+katon+yooton prepared and four_tails jinchuriki state is saved. "
                        + "Select yooton, hold right-click briefly, release to toggle the mode, then release again to stop it.");
    }

    private static int prepareYotonSealingTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack yoton = getOrGiveItem(player, ModItems.YOTON.get());
        prepareAdvancedNatureJutsuStack(player, yoton, 1);
        BlockPos target = placeSealingTargetPlatform(player);
        source.sendSuccess(() -> Component.literal("Yoton Sealing test ready: yoton index 1 prepared and a clear seal platform placed at "
                + target.getX() + "," + target.getY() + "," + target.getZ()
                + ". Select Sealing, look at the platform top face within 10 blocks, hold right-click briefly, then release. "
                + describeInventoryAdvancedNature(player, ModItems.YOTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareYotonBiggerMeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack yoton = getOrGiveItem(player, ModItems.YOTON.get());
        prepareAdvancedNatureJutsuStack(player, yoton, 0);
        getOrGiveItem(player, ModItems.SCROLL_MULTI_SIZE.get());
        spawnToolTargetArmorStand(player, "M5 Bigger Me target");
        source.sendSuccess(() -> Component.literal("Yoton Bigger Me test ready: yoton index 0 and multi-size scroll prepared. "
                + "Select Bigger Me, hold right-click to charge, release, then attack the target while riding. "
                + describeInventoryAdvancedNature(player, ModItems.YOTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareBakutonJiraikenTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.RAITON.get());
        ItemStack bakuton = getOrGiveItem(player, ModItems.BAKUTON.get());
        prepareAdvancedNatureJutsuStack(player, bakuton, 0);
        AdvancedNatureJutsuItem.setJiraikenActive(bakuton, false);
        player.removeEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
        spawnToolTargetArmorStand(player, "M5 Jiraiken target");
        source.sendSuccess(() -> Component.literal("Bakuton Jiraiken test ready: doton+raiton+bakuton prepared. "
                + "Select bakuton, hold right-click to charge, release to activate, then punch the target. "
                + describeInventoryAdvancedNature(player, ModItems.BAKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareBakutonExplosiveClayTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.RAITON.get());
        ItemStack bakuton = getOrGiveItem(player, ModItems.BAKUTON.get());
        prepareAdvancedNatureJutsuStack(player, bakuton, 1);
        spawnToolTargetArmorStand(player, "M5 Explosive Clay target");
        source.sendSuccess(() -> Component.literal("Bakuton Explosive Clay test ready: doton+raiton+bakuton index 1 prepared. "
                + "Select c_1, hold to charge C-1/C-2/C-3, release to spawn clay, then left-click the target to direct C-1/C-2. "
                + describeInventoryAdvancedNature(player, ModItems.BAKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareBakutonExplosiveCloneTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.RAITON.get());
        ItemStack bakuton = getOrGiveItem(player, ModItems.BAKUTON.get());
        prepareAdvancedNatureJutsuStack(player, bakuton, 2);
        spawnToolTargetArmorStand(player, "M5 Explosive Clone target");
        source.sendSuccess(() -> Component.literal("Bakuton Explosive Clone test ready: doton+raiton+bakuton index 2 prepared. "
                + "Select explosive_clone, release to spawn a clone, attack a target to arm it, or sneak-release to dismiss active clones. "
                + describeInventoryAdvancedNature(player, ModItems.BAKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFuttonMistTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareFuttonJutsuTest(source, 0, "M5 Futton Mist target",
                "Futton Mist test ready: katon+suiton+futton prepared. "
                        + "Select futton, hold right-click to charge, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareFuttonUnrivaledStrengthTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareFuttonJutsuTest(source, 1, "M5 Unrivaled Strength target",
                "Futton Unrivaled Strength test ready: katon+suiton+futton prepared. "
                        + "Select futton, hold right-click to charge, release, then punch the target and check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareHyotonIceSpearTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareHyotonJutsuTest(source, 1, "M5 Ice Spear target",
                "Hyoton Ice Spear test ready: futon+suiton+hyoton prepared. "
                        + "Select hyoton, hold right-click to charge, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareHyotonIceSpikeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareHyotonJutsuTest(source, 0, "M5 Ice Spike target",
                "Hyoton Ice Spike test ready: futon+suiton+hyoton prepared. "
                        + "Select hyoton, aim at the top of a ground block near the target, hold right-click to charge, then release.");
    }

    private static int prepareHyotonIceDomeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareHyotonJutsuTest(source, 2, "M5 Ice Dome target",
                "Hyoton Ice Dome test ready: futon+suiton+hyoton prepared. "
                        + "Select hyoton, hold right-click to charge, release to form the dome, then cast again inside it to fire spears.");
    }

    private static int prepareHyotonIcePrisonTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareHyotonJutsuTest(source, 3, "M5 Ice Prison target",
                "Hyoton Ice Prison test ready: futon+suiton+hyoton prepared. "
                        + "Select hyoton, aim at the target within 10 blocks, hold right-click to charge, then release.");
    }

    private static int prepareMokutonWoodBurialTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack mokuton = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokuton, 0);
        spawnToolTargetArmorStand(player, "M5 Wood Burial target");
        source.sendSuccess(() -> Component.literal("Mokuton Wood Burial test ready: mokuton index 0 prepared and a target spawned. "
                + "Select Wood Burial, look at the target within 20 blocks, hold right-click briefly, then release. "
                + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareMokutonWoodPrisonTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack mokuton = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokuton, 1);
        BlockPos target = placeWoodPrisonTargetBlock(player);
        source.sendSuccess(() -> Component.literal("Mokuton Wood Prison test ready: mokuton index 1 prepared and a stone target block placed at "
                + target.getX() + "," + target.getY() + "," + target.getZ()
                + ". Select wood_prison, look at the stone block within 20 blocks, hold right-click to charge, then release. "
                + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareMokutonWoodHouseTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack mokuton = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokuton, 2);
        BlockPos target = placeWoodHouseTargetPlatform(player);
        source.sendSuccess(() -> Component.literal("Mokuton Four-Pillar House test ready: mokuton index 2 prepared and a stone top-face target placed at "
                + target.getX() + "," + target.getY() + "," + target.getZ()
                + ". Select Four-Pillar House, look at the top face within 30 blocks, hold right-click briefly, then release. "
                + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareMokutonWoodArmTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack mokuton = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokuton, 4);
        spawnToolTargetArmorStand(player, "M5 Wood Arm target");
        source.sendSuccess(() -> Component.literal("Mokuton Wood Arm test ready: mokuton index 4 prepared and a target spawned. "
                + "Select Wood Arm, look at the target within 30 blocks, hold right-click briefly, then release. "
                + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareMokutonWoodGolemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack mokuton = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokuton, 3);
        spawnToolTargetArmorStand(player, "M5 Wood Golem target");
        source.sendSuccess(() -> Component.literal("Mokuton Wood Golem test ready: mokuton index 3 prepared and a target spawned. "
                + "Select Wood Golem, hold right-click briefly, release to summon and mount it, then swing while looking at the target. "
                + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareJintonBeamTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJintonJutsuTest(source, 0, "M5 Jinton Beam target",
                "Jinton Beam test ready: doton+futon+katon+jinton prepared. "
                        + "Select jinton, hold right-click to charge, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareJintonCubeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJintonJutsuTest(source, 1, "M5 Jinton Cube target",
                "Jinton Cube test ready: doton+futon+katon+jinton prepared. "
                        + "Select jinton, hold right-click to charge, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareRantonCloudTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareRantonJutsuTest(source, 0, "M5 Ranton Cloud target",
                "Ranton Cloud test ready: raiton+suiton+ranton prepared. "
                        + "Select ranton, hold right-click briefly, release to toggle the cloud, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareRantonLaserCircusTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareRantonJutsuTest(source, 1, "M5 Laser Circus target",
                "Ranton Laser Circus test ready: raiton+suiton+ranton prepared. "
                        + "Select ranton, hold right-click to charge, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareShakutonScorchOrbTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShakutonJutsuTest(source, 0, "M5 Scorch Orb target",
                "Shakuton Scorch Orb test ready: futon+katon+shakuton prepared. "
                        + "Select shakuton, hold right-click briefly, release to spawn an orb, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareShakutonScorchKillTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShakutonJutsuTest(source, 1, "M5 Scorch Kill target",
                "Shakuton Scorch Kill test ready: futon+katon+shakuton prepared. "
                        + "Spawn at least one Scorch Orb first, switch to Scorch Kill, aim at the target, and release.");
    }

    private static int prepareShakutonScorchBlastTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShakutonJutsuTest(source, 2, "M5 Scorch Blast target",
                "Shakuton Scorch Blast test ready: futon+katon+shakuton prepared. "
                        + "Spawn one or more Scorch Orbs first, switch to Scorch Blast, then release to combine and fire them.");
    }

    private static int prepareJitonSandBulletTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJitonJutsuTest(source, 1, "M5 Sand Bullet target",
                "Jiton Sand Bullet test ready: futon+doton+jiton prepared with gourd body equipped. "
                        + "Select jiton, hold right-click briefly, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareJitonSandShieldTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJitonJutsuTest(source, 0, "M5 Sand Shield attacker",
                "Jiton Sand Shield test ready: futon+doton+jiton prepared with gourd body equipped. "
                        + "Select entityjitonshield, release to summon the shield, then let the attacker hit you or strike the shield.");
    }

    private static int prepareJitonSandBindTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJitonJutsuTest(source, 2, "M5 Sand Bind target",
                "Jiton Sand Bind test ready: futon+doton+jiton prepared with gourd body equipped. "
                        + "Select sand_bind, release while looking at the target to capture it; release again while looking at the bind to trigger Sand Funeral.");
    }

    private static int prepareJitonSandLevitationTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareJitonJutsuTest(source, 3, "M5 Sand Levitation marker",
                "Jiton Sand Levitation test ready: futon+doton+jiton prepared with gourd body equipped. "
                        + "Select sand_levitation, release to summon the cloud, wait for auto-mount, then steer with movement keys.");
    }

    private static int prepareShikotsumyakuFingerBoneTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 3, "M5 Finger Bone target",
                "Shikotsumyaku Finger Bone test ready: shikotsumyaku index 3 prepared. "
                        + "Select finger_bone, release toward the target, then check /narutoport m5_advanced_jutsu state.");
    }

    private static int prepareShikotsumyakuLarchDanceTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 0, "M5 Larch Dance attacker",
                "Shikotsumyaku Larch Dance test ready: shikotsumyaku index 0 prepared. "
                        + "Select Larch Dance and release to equip/activate bone armor; let a nearby mob hit you to check thorns.");
    }

    private static int prepareShikotsumyakuWillowDanceTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 1, "M5 Willow Dance target",
                "Shikotsumyaku Willow Dance test ready: shikotsumyaku index 1 prepared. "
                        + "Select Willow Dance and release to equip/activate bone armor, then check strength and melee damage.");
    }

    private static int prepareShikotsumyakuCamelliaDanceTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 2, "M5 Camellia Dance target",
                "Shikotsumyaku Camellia Dance test ready: shikotsumyaku index 2 prepared. "
                        + "Select Camellia Dance and release to replace the main hand with a bone sword.");
    }

    private static int prepareShikotsumyakuClematisFlowerTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 4, "M5 Clematis Flower target",
                "Shikotsumyaku Clematis Flower test ready: shikotsumyaku index 4 prepared. "
                        + "Select Clematis Flower and release to receive a bone drill and start its 60s cooldown.");
    }

    private static int prepareShikotsumyakuBrackenDanceTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareShikotsumyakuJutsuTest(source, 5, "M5 Bracken Dance target",
                "Shikotsumyaku Bracken Dance test ready: shikotsumyaku index 5 prepared. "
                        + "Select entitybrackendance, charge briefly, look at an exposed top face within 30 blocks, then release.");
    }

    private static int prepareKekkeiMoraAshBonesTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.BYAKURINNESHARINGANHELMET.get()));
        ItemStack kekkeiMora = getOrGiveItem(player, ModItems.KEKKEI_MORA.get());
        prepareAdvancedNatureJutsuStack(player, kekkeiMora, 3);
        spawnToolTargetArmorStand(player, "M5 Ash Bones target");
        source.sendSuccess(() -> Component.literal("Kekkei Mora Ash Bones test ready: kekkei_mora index 3 prepared and Byakurin Rinne Sharingan helmet equipped. "
                + "Select Ash Bones and release to replace the main hand, then use the weapon toward the target. "
                + describeInventoryAdvancedNature(player, ModItems.KEKKEI_MORA.get())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", ashbones=" + findItem(player, ModItems.ASHBONES.get()).getCount()
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareKekkeiMoraEightyGodsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.BYAKURINNESHARINGANHELMET.get()));
        ItemStack kekkeiMora = getOrGiveItem(player, ModItems.KEKKEI_MORA.get());
        prepareAdvancedNatureJutsuStack(player, kekkeiMora, 0);
        spawnToolTargetArmorStand(player, "M5 Eighty Gods target");
        source.sendSuccess(() -> Component.literal("Kekkei Mora Eighty Gods test ready: kekkei_mora index 0 prepared and Byakurin Rinne Sharingan helmet equipped. "
                + "Select Eighty Gods, hold right-click for repeated fists, release for one final fist, then check /narutoport m5_advanced_jutsu state. "
                + describeInventoryAdvancedNature(player, ModItems.KEKKEI_MORA.get())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareKekkeiMoraYomotsuHirasakaTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.BYAKURINNESHARINGANHELMET.get()));
        ItemStack kekkeiMora = getOrGiveItem(player, ModItems.KEKKEI_MORA.get());
        prepareAdvancedNatureJutsuStack(player, kekkeiMora, 1);
        spawnToolTargetArmorStand(player, "M5 Yomotsu target");
        source.sendSuccess(() -> Component.literal("Kekkei Mora Yomotsu Hirasaka test ready: kekkei_mora index 1 prepared and Byakurin Rinne Sharingan helmet equipped. "
                + "Select Yomotsu Hirasaka, release while looking at the target or a distant block, then check /narutoport m5_advanced_jutsu state. "
                + describeInventoryAdvancedNature(player, ModItems.KEKKEI_MORA.get())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareKekkeiMoraExpansiveTruthSeekingBallTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        equipDebugArmor(player, EquipmentSlot.HEAD, new ItemStack(ModItems.BYAKURINNESHARINGANHELMET.get()));
        ItemStack kekkeiMora = getOrGiveItem(player, ModItems.KEKKEI_MORA.get());
        prepareAdvancedNatureJutsuStack(player, kekkeiMora, 2);
        spawnToolTargetArmorStand(player, "M5 Expansive TSB target");
        source.sendSuccess(() -> Component.literal("Kekkei Mora Expansive Truth-Seeking Ball test ready: kekkei_mora index 2 prepared and Byakurin Rinne Sharingan helmet equipped. "
                + "Select Expansive Truth-Seeking Ball, release toward open space or the target, then check /narutoport m5_advanced_jutsu state. "
                + describeInventoryAdvancedNature(player, ModItems.KEKKEI_MORA.get())
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHyotonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.FUTON.get());
        getOrGiveItem(player, ModItems.SUITON.get());
        ItemStack hyoton = getOrGiveItem(player, ModItems.HYOTON.get());
        prepareAdvancedNatureJutsuStack(player, hyoton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.HYOTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFuttonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.KATON.get());
        getOrGiveItem(player, ModItems.SUITON.get());
        ItemStack futton = getOrGiveItem(player, ModItems.FUTTON.get());
        prepareAdvancedNatureJutsuStack(player, futton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.FUTTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareJintonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.FUTON.get());
        getOrGiveItem(player, ModItems.KATON.get());
        ItemStack jinton = getOrGiveItem(player, ModItems.JINTON.get());
        prepareAdvancedNatureJutsuStack(player, jinton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.JINTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareRantonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.RAITON.get());
        getOrGiveItem(player, ModItems.SUITON.get());
        ItemStack ranton = getOrGiveItem(player, ModItems.RANTON.get());
        prepareAdvancedNatureJutsuStack(player, ranton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.RANTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareShakutonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.FUTON.get());
        getOrGiveItem(player, ModItems.KATON.get());
        ItemStack shakuton = getOrGiveItem(player, ModItems.SHAKUTON.get());
        prepareAdvancedNatureJutsuStack(player, shakuton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.SHAKUTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareJitonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.FUTON.get());
        getOrGiveItem(player, ModItems.DOTON.get());
        equipDebugArmor(player, EquipmentSlot.CHEST, new ItemStack(ModItems.GOURDBODY.get()));
        ItemStack jiton = getOrGiveItem(player, ModItems.JITON.get());
        prepareAdvancedNatureJutsuStack(player, jiton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.JITON.get())
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareShikotsumyakuJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack shikotsumyaku = getOrGiveItem(player, ModItems.SHIKOTSUMYAKU.get());
        prepareAdvancedNatureJutsuStack(player, shikotsumyaku, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " "
                + describeInventoryAdvancedNature(player, ModItems.SHIKOTSUMYAKU.get())
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareYootonJutsuTest(CommandSourceStack source, int jutsuIndex, String targetName, String message)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        getOrGiveItem(player, ModItems.DOTON.get());
        getOrGiveItem(player, ModItems.KATON.get());
        ItemStack yooton = getOrGiveItem(player, ModItems.YOOTON.get());
        prepareAdvancedNatureJutsuStack(player, yooton, jutsuIndex);
        spawnToolTargetArmorStand(player, targetName);
        source.sendSuccess(() -> Component.literal(message + " " + describeInventoryAdvancedNature(player, ModItems.YOOTON.get())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int reportM5AdvancedNatureState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 advanced jutsu prerequisites: doton=" + findItem(player, ModItems.DOTON.get()).getCount()
                + ", futon=" + findItem(player, ModItems.FUTON.get()).getCount()
                + ", katon=" + findItem(player, ModItems.KATON.get()).getCount()
                + ", raiton=" + findItem(player, ModItems.RAITON.get()).getCount()
                + ", suiton=" + findItem(player, ModItems.SUITON.get()).getCount()
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", larch_active=" + BoneArmorItem.isLarchActive(player.getItemBySlot(EquipmentSlot.CHEST))
                + ", willow_active=" + BoneArmorItem.isWillowActive(player.getItemBySlot(EquipmentSlot.CHEST))
                + ", bone_sword=" + findItem(player, ModItems.BONE_SWORD.get()).getCount()
                + ", bone_drill=" + findItem(player, ModItems.BONE_DRILL.get()).getCount()
                + ", ashbones=" + findItem(player, ModItems.ASHBONES.get()).getCount()
                + ", ashbones_head_allowed=" + isAshBonesAllowedHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", bakuton_jiraiken_active=" + AdvancedNatureJutsuItem.isJiraikenActive(findItem(player, ModItems.BAKUTON.get()))
                + ", bakuton_jiraiken_power=" + AdvancedNatureJutsuItem.getJiraikenPower(findItem(player, ModItems.BAKUTON.get()))
                + ", nearby_c1=" + countNearbyExplosiveClay(player, ModEntityTypes.C_1.get())
                + ", nearby_c2=" + countNearbyExplosiveClay(player, ModEntityTypes.C_2.get())
                + ", nearby_c3=" + countNearbyExplosiveClay(player, ModEntityTypes.C_3.get())
                + ", nearby_explosive_clones=" + countNearbyExplosiveClones(player)
                + ", chakra_enhanced_strength=" + player.hasEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get())
                + ", jinchuriki_tails=" + NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", nearby_magma_balls=" + countNearbyMagmaBalls(player)
                + ", nearby_melting_jutsu=" + countNearbyMeltingJutsu(player)
                + ", nearby_lava_chakra_mode=" + countNearbyLavaChakraModes(player)
                + ", nearby_futton_mist=" + countNearbyFuttonMist(player)
                + ", nearby_unrivaled_strength=" + countNearbyUnrivaledStrength(player)
                + ", nearby_ice_spikes=" + countNearbyIceSpikes(player)
                + ", nearby_ice_spears=" + countNearbyIceSpears(player)
                + ", nearby_ice_domes=" + countNearbyIceDomes(player)
                + ", nearby_ice_prisons=" + countNearbyIcePrisons(player)
                + ", nearby_wood_arms=" + countNearbyWoodArms(player)
                + ", nearby_wood_burials=" + countNearbyWoodBurials(player)
                + ", nearby_wood_golems=" + countNearbyWoodGolems(player)
                + ", nearby_wood_prisons=" + countNearbyWoodPrisons(player)
                + ", nearby_jinton_beams=" + countNearbyJintonBeams(player)
                + ", nearby_jinton_cubes=" + countNearbyJintonCubes(player)
                + ", nearby_ranton_clouds=" + countNearbyRantonClouds(player)
                + ", nearby_laser_circus=" + countNearbyLaserCircus(player)
                + ", nearby_laser_rings=" + countNearbyLaserRings(player)
                + ", nearby_scorch_orbs=" + countNearbyScorchOrbs(player)
                + ", nearby_sand_shields=" + countNearbySandShields(player)
                + ", nearby_sand_bullets=" + countNearbySandBullets(player)
                + ", nearby_sand_binds=" + countNearbySandBinds(player)
                + ", nearby_sand_levitation=" + countNearbySandLevitation(player)
                + ", nearby_finger_bones=" + countNearbyFingerBones(player)
                + ", nearby_bracken_dance=" + countNearbyBrackenDance(player)
                + ", nearby_eighty_gods=" + countNearbyEightyGods(player)
                + ", nearby_portalblocks=" + countNearbyPortalBlocks(player)
                + ", nearby_truth_seeker_balls=" + countNearbyTruthSeekerBalls(player)
                + ", nearby_yoton_biggerme=" + countNearbyBiggerMe(player)
                + ", nearby_yoton_sealing=" + countNearbySealing(player)), false);
        source.sendSuccess(() -> Component.literal("M5 advanced jutsu items: bakuton=" + describeInventoryAdvancedNature(player, ModItems.BAKUTON.get())
                + ", futton=" + describeInventoryAdvancedNature(player, ModItems.FUTTON.get())
                + ", hyoton=" + describeInventoryAdvancedNature(player, ModItems.HYOTON.get())
                + ", jinton=" + describeInventoryAdvancedNature(player, ModItems.JINTON.get())
                + ", jiton=" + describeInventoryAdvancedNature(player, ModItems.JITON.get())
                + ", kekkei_mora=" + describeInventoryAdvancedNature(player, ModItems.KEKKEI_MORA.get())
                + ", mokuton=" + describeInventoryAdvancedNature(player, ModItems.MOKUTON.get())
                + ", ranton=" + describeInventoryAdvancedNature(player, ModItems.RANTON.get())
                + ", shakuton=" + describeInventoryAdvancedNature(player, ModItems.SHAKUTON.get())
                + ", shikotsumyaku=" + describeInventoryAdvancedNature(player, ModItems.SHIKOTSUMYAKU.get())
                + ", yooton=" + describeInventoryAdvancedNature(player, ModItems.YOOTON.get())
                + ", yoton=" + describeInventoryAdvancedNature(player, ModItems.YOTON.get())
                + ", scroll_multi_size=" + describeInventoryJutsuScroll(player, ModItems.SCROLL_MULTI_SIZE.get())), false);
        return 1;
    }

    private static void prepareYootonMagmaBallStack(ServerPlayer player, ItemStack stack) {
        prepareAdvancedNatureJutsuStack(player, stack, 0);
    }

    private static BlockPos placeWoodPrisonTargetBlock(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        BlockPos target = BlockPos.containing(basis.point(7.0D, 0.0D, 1.0D));
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        return target;
    }

    private static BlockPos placeWoodHouseTargetPlatform(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(basis.point(7.0D, 0.0D, 0.0D)).atY(player.blockPosition().getY());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        return center;
    }

    private static BlockPos placeSealingTargetPlatform(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        BlockPos center = BlockPos.containing(basis.point(7.0D, 0.0D, 0.0D)).atY(player.blockPosition().getY());
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x * x + z * z < 49) {
                    BlockPos ground = center.offset(x, 0, z);
                    level.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(ground.above(2), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        int[][] torchOffsets = {
                {-2, 1}, {-1, 2}, {1, 2}, {2, 1},
                {2, -1}, {1, -2}, {-1, -2}, {-2, -1}
        };
        for (int[] offset : torchOffsets) {
            level.setBlock(center.offset(offset[0], 1, offset[1]), Blocks.TORCH.defaultBlockState(), 3);
        }
        return center;
    }

    private static void prepareAdvancedNatureJutsuStack(ServerPlayer player, ItemStack stack, int jutsuIndex) {
        if (stack.getItem() instanceof AdvancedNatureJutsuItem advancedNatureJutsuItem) {
            JutsuItem.JutsuDefinition definition = advancedNatureJutsuItem.kind().definitionByIndex(jutsuIndex);
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            advancedNatureJutsuItem.enableJutsu(stack, definition, true);
            advancedNatureJutsuItem.setJutsuXp(stack, definition, advancedNatureJutsuItem.getRequiredXp(stack, definition));
        }
        prepareJutsuResourcePool(player, 50000.0D);
        if (player.experienceLevel < 30) {
            player.giveExperienceLevels(30 - player.experienceLevel);
        }
    }

    private static int countNearbyMagmaBalls(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(MagmaBallEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.MAGMABALL.get())
                .size();
    }

    private static int countNearbyExplosiveClay(ServerPlayer player, EntityType<?> type) {
        return player.serverLevel()
                .getEntitiesOfClass(ExplosiveClayEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == type)
                .size();
    }

    private static int countNearbyExplosiveClones(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ExplosiveCloneEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.EXPLOSIVE_CLONE.get())
                .size();
    }

    private static int countNearbyMeltingJutsu(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(MeltingJutsuEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.MELTING_JUTSU.get())
                .size();
    }

    private static int countNearbyLavaChakraModes(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(LavaChakraModeEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.LAVA_CHAKRA_MODE.get())
                .size();
    }

    private static int countNearbyFuttonMist(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(FuttonMistEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.FUTTON_MIST.get())
                .size();
    }

    private static int countNearbyUnrivaledStrength(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(UnrivaledStrengthEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.UNRIVALED_STRENGTH.get())
                .size();
    }

    private static int countNearbyIceSpikes(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(IceSpikeEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.ICE_SPIKE.get())
                .size();
    }

    private static int countNearbyIceSpears(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(IceSpearEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.ICE_SPEAR.get())
                .size();
    }

    private static int countNearbyIceDomes(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(IceDomeEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.ICE_DOME.get())
                .size();
    }

    private static int countNearbyIcePrisons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(IcePrisonEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.ICE_PRISON.get())
                .size();
    }

    private static int countNearbyWoodArms(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(WoodArmEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.WOOD_ARM.get())
                .size();
    }

    private static int countNearbyWoodBurials(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(WoodBurialEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.WOOD_BURIAL.get())
                .size();
    }

    private static int countNearbyWoodGolems(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(WoodGolemEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.WOOD_GOLEM.get())
                .size();
    }

    private static int countNearbyWoodPrisons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(WoodPrisonEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.WOOD_PRISON.get())
                .size();
    }

    private static int countNearbyJintonBeams(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(JintonBeamEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.JINTONBEAM.get())
                .size();
    }

    private static int countNearbyJintonCubes(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(JintonCubeEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.JINTONCUBE.get())
                .size();
    }

    private static int countNearbyRantonClouds(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(RantonCloudEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.RANTONCLOUD.get())
                .size();
    }

    private static int countNearbyLaserCircus(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(LaserCircusEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.LASER_CIRCUS.get())
                .size();
    }

    private static int countNearbyLaserRings(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(LaserRingEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.LASER_RING.get())
                .size();
    }

    private static int countNearbyScorchOrbs(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ScorchOrbEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SCORCHORB.get())
                .size();
    }

    private static int countNearbySandBullets(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SandBulletEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SAND_BULLET.get())
                .size();
    }

    private static int countNearbySandShields(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SandShieldEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.ENTITYJITONSHIELD.get())
                .size();
    }

    private static int countNearbySandBinds(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SandBindEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SAND_BIND.get())
                .size();
    }

    private static int countNearbySandLevitation(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SandLevitationEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SAND_LEVITATION.get())
                .size();
    }

    private static int countNearbyFingerBones(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(FingerBoneEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.FINGER_BONE.get())
                .size();
    }

    private static int countNearbyBrackenDance(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(BrackenDanceEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.ENTITYBRACKENDANCE.get())
                .size();
    }

    private static int countNearbySealing(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SealingEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SEALING.get())
                .size();
    }

    private static int countNearbyBiggerMe(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(BiggerMeEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.BIGGERME.get())
                .size();
    }

    private static int countNearbyEightyGods(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(EightyGodsEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.ENTITY80GODS.get())
                .size();
    }

    private static int countNearbyTruthSeekerBalls(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TruthSeekerBallEntity.class, player.getBoundingBox().inflate(256.0D),
                        entity -> entity.getType() == ModEntityTypes.TRUTHSEEKERBALL.get())
                .size();
    }

    private static int countNearbyPortalBlocks(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-32, -8, -32), center.offset(32, 8, 32))) {
            if (player.serverLevel().getBlockState(pos).is(ModBlocks.PORTALBLOCK.get())) {
                count++;
            }
        }
        return count;
    }

    private static String describeInventoryJutsuScroll(ServerPlayer player, Item item) {
        ItemStack stack = findItem(player, item);
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId + "[jutsu_scroll=" + (stack.getItem() instanceof JutsuScrollItem)
                + ",count=" + stack.getCount()
                + "]";
    }

    private static String describeInventoryAdvancedNature(ServerPlayer player, Item item) {
        ItemStack stack = findItem(player, item);
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof AdvancedNatureJutsuItem advancedNatureJutsuItem) {
            return itemId + "[kind=" + advancedNatureJutsuItem.kind()
                    + ",jutsu_count=" + advancedNatureJutsuItem.jutsuList().size()
                    + ",current=" + advancedNatureJutsuItem.describeCurrentJutsu(stack)
                    + "]";
        }
        return itemId + "[advanced_jutsu=false]";
    }

    private static String describeBijuSideEffectItems(ServerPlayer player) {
        ItemStack doton = findItem(player, ModItems.DOTON.get());
        ItemStack futon = findItem(player, ModItems.FUTON.get());
        ItemStack katon = findItem(player, ModItems.KATON.get());
        ItemStack yooton = findItem(player, ModItems.YOOTON.get());
        return "{jiton=" + describeInventoryAdvancedNature(player, ModItems.JITON.get())
                + ",yooton=" + describeInventoryAdvancedNature(player, ModItems.YOOTON.get())
                + ",futon_vacuum=" + describeJutsuDefinitionState(futon, FutonItem.VACUUM)
                + ",doton_earth_wall=" + describeJutsuDefinitionState(doton, DotonItem.EARTH_WALL)
                + ",katon_great_fireball=" + describeJutsuDefinitionState(katon, KatonItem.GREAT_FIREBALL)
                + ",yooton_lava_chakra_mode="
                + (yooton.isEmpty() ? "missing" : yooton.getItem() instanceof AdvancedNatureJutsuItem advancedNatureJutsuItem
                        ? describeJutsuDefinitionState(
                                yooton,
                                AdvancedNatureJutsuItem.AdvancedNatureKind.YOOTON.definitionByIndex(2))
                        : "not_advanced_jutsu_item")
                + "}";
    }

    private static int prepareSixPathSenjutsuTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.SHOOT.index(), "Shoot Truth-Seeking Ball");
    }

    private static int prepareSixPathShootTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.SHOOT.index(), "Shoot Truth-Seeking Ball");
    }

    private static int prepareSixPathShieldTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.SHIELD.index(), "Truth-Seeking Shield");
    }

    private static int prepareSixPathIntonRaihaTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.THUNDER.index(), "Inton Raiha");
    }

    private static int prepareSixPathRantonKogaTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.LASER.index(), "Ranton Koga");
    }

    private static int prepareSixPathOuterPathTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSixPathSenjutsuTest(source, SixPathSenjutsuItem.OUTER_PATH.index(), "Outer Path");
    }

    private static int prepareSixPathRasenshurikenTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 90000.0D);
        getOrGiveItem(player, ModItems.SCROLL_RASENSHURIKEN.get());
        ItemStack futonStack = prepareFutonJutsuStack(player, FutonItem.RASENSHURIKEN);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        head.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        ItemStack stack = getOrGiveItem(player, ModItems.SIX_PATH_SENJUTSU.get());
        if (stack.getItem() instanceof SixPathSenjutsuItem sixPathSenjutsuItem) {
            JutsuItem.setOwner(stack, player);
            selectSixPathJutsu(stack, sixPathSenjutsuItem, SixPathSenjutsuItem.RASENSHURIKEN.index());
        }
        spawnToolTargetArmorStand(player, "M5 Six Path target");
        source.sendSuccess(() -> Component.literal("Six Path Senjutsu Truth Seeking Rasenshuriken test ready: "
                + describeSixPathStack(player, stack)
                + ", futon_rasenshuriken=" + describeJutsuDefinitionState(futonStack, FutonItem.RASENSHURIKEN)
                + ". Right-click the Six Path item to spawn the variant, then check /narutoport m5_six_path state."), false);
        return 1;
    }

    private static int prepareSixPathSenjutsuTest(CommandSourceStack source, int jutsuIndex, String label)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        head.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        ItemStack stack = getOrGiveItem(player, ModItems.SIX_PATH_SENJUTSU.get());
        if (stack.getItem() instanceof SixPathSenjutsuItem sixPathSenjutsuItem) {
            JutsuItem.setOwner(stack, player);
            selectSixPathJutsu(stack, sixPathSenjutsuItem, jutsuIndex);
        }
        spawnToolTargetArmorStand(player, "M5 Six Path target");
        prepareJutsuResourcePool(player, 90000.0D);
        source.sendSuccess(() -> Component.literal("Six Path Senjutsu " + label + " test ready: " + describeSixPathStack(player, stack)
                + ". Hold the item to spawn Truth-Seeking Balls, right-click to use the selected jutsu, then check /narutoport m5_six_path state."), false);
        return 1;
    }

    private static int reportM5SixPathState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.SIX_PATH_SENJUTSU.get());
        ItemStack futonStack = findItem(player, ModItems.FUTON.get());
        ItemStack ninjutsuStack = findItem(player, ModItems.NINJUTSU.get());
        int specialEventsPending = SpecialEvent.pendingCount(source.getServer());
        int sphericalExplosionEvents = SpecialEvent.pendingCount(source.getServer(), SpecialEvent.SPHERICAL_EXPLOSION_TYPE);
        source.sendSuccess(() -> Component.literal("M5 six path: head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", active_rinnesharingan=" + SixPathSenjutsuItem.hasRinneSharingan(player)
                + ", legacy_tick=" + describeRinneganHelmetLegacyTickState(player)
                + ", six_path_senjutsu=" + describeSixPathStack(player, stack)
                + ", ninjutsu_limbo_clone=" + describeJutsuDefinitionState(ninjutsuStack, NinjutsuItem.LIMBO_CLONE)
                + ", futon_rasenshuriken=" + describeJutsuDefinitionState(futonStack, FutonItem.RASENSHURIKEN)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", nearby_inton_raiha=" + countNearbyIntonRaiha(player)
                + ", nearby_ranton_koga=" + countNearbyRantonKoga(player)
                + ", nearby_rasenshuriken=" + countNearbyRasenshuriken(player)
                + ", special_events_pending=" + specialEventsPending
                + ", spherical_explosion_events=" + sphericalExplosionEvents
                + ", nearby_gedo_statue=" + countNearbyGedoStatues(player)
                + ", active_sentry_targets=" + TruthSeekerBallEntity.countActiveSentryTargets(player, stack)
                + ", nearby_lightning_arcs=" + countNearbyLightningArcs(player)
                + ", nearby_truth_seeker_balls=" + countNearbyTruthSeekerBalls(player)), false);
        return 1;
    }

    private static int prepareRinneganHelmetTickTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        head.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        player.removeEffect(MobEffects.POISON);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, false));
        player.setHealth(Math.max(1.0F, player.getMaxHealth() - 4.0F));
        boolean ran = RinneganHelmetItem.applyLegacyTick(player, player.getItemBySlot(EquipmentSlot.HEAD));
        source.sendSuccess(() -> Component.literal("Rinnegan helmet legacy tick ready: ran=" + ran
                + ", " + describeRinneganHelmetLegacyTickState(player)
                + ". Check again after a game tick with /narutoport m5_six_path state."), false);
        return ran ? 1 : 0;
    }

    private static int prepareTenseiganHelmetTickTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = new ItemStack(ModItems.TENSEIGANHELMET.get());
        head.getOrCreateTag().putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, 5.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        ItemStack ninjutsu = getOrGiveItem(player, ModItems.NINJUTSU.get());
        if (ninjutsu.getItem() instanceof NinjutsuItem ninjutsuItem) {
            ninjutsuItem.enableJutsu(ninjutsu, NinjutsuItem.LIMBO_CLONE, false);
        }
        boolean ran = RinneganHelmetItem.applyLegacyTick(player, player.getItemBySlot(EquipmentSlot.HEAD));
        source.sendSuccess(() -> Component.literal("Tenseigan helmet legacy tick ready: ran=" + ran
                + ", " + describeRinneganHelmetLegacyTickState(player)
                + ". Check again after a game tick with /narutoport m5_six_path state."), false);
        return ran ? 1 : 0;
    }

    private static void selectSixPathJutsu(ItemStack stack, SixPathSenjutsuItem item, int jutsuIndex) {
        for (JutsuItem.JutsuDefinition definition : item.jutsuList()) {
            if (definition.index() == jutsuIndex) {
                stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
                item.enableJutsu(stack, definition, true);
                item.setJutsuXp(stack, definition, item.getRequiredXp(stack, definition));
                return;
            }
        }
    }

    private static int countNearbyIntonRaiha(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(IntonRaihaEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.INTON_RAIHA.get())
                .size();
    }

    private static int countNearbyRantonKoga(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(RantonKogaEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.RANTON_KOGA.get())
                .size();
    }

    private static int countNearbyRasenshuriken(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(RasenshurikenEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.RASENSHURIKEN.get())
                .size();
    }

    private static int countNearbyLightningArcs(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(LightningArcEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.LIGHTNING_ARC.get())
                .size();
    }

    private static String describeSixPathStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof SixPathSenjutsuItem sixPathSenjutsuItem) {
            int[] spawned = stack.getOrCreateTag().getIntArray(SixPathSenjutsuItem.SPAWNED_BALLS_TAG);
            int activeIds = 0;
            for (int id : spawned) {
                if (id > 0) {
                    activeIds++;
                }
            }
            return itemId + "[six_path=true,jutsu_count=" + sixPathSenjutsuItem.jutsuList().size()
                    + ",current=" + sixPathSenjutsuItem.describeCurrentJutsu(stack)
                    + ",owned=" + JutsuItem.isOwnedByOrUnbound(player, stack)
                    + ",spawned_ids=" + activeIds
                    + ",current_ball=" + stack.getOrCreateTag().getInt(SixPathSenjutsuItem.CURRENT_BALL_TAG)
                    + "]";
        }
        return itemId + "[six_path=false]";
    }

    private static String describeRinneganHelmetLegacyTickState(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack ninjutsu = findItem(player, ModItems.NINJUTSU.get());
        return "rinnesharingan_tag=" + RinneganHelmetItem.isRinnesharinganActivated(head)
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", legs=" + describeArmorSlot(player, EquipmentSlot.LEGS)
                + ", six_path_stack=" + findItem(player, ModItems.SIX_PATH_SENJUTSU.get()).getCount()
                + ", black_receiver_stack=" + findItem(player, ModItems.BLACK_RECEIVER.get()).getCount()
                + ", tenseigan_chakra_mode_stack=" + findItem(player, ModItems.TENSEIGAN_CHAKRA_MODE.get()).getCount()
                + ", limbo_enabled=" + describeJutsuDefinitionState(ninjutsu, NinjutsuItem.LIMBO_CLONE)
                + ", flight=" + describeEffectInstance(player.getEffect(ModEffects.FLIGHT.get()))
                + ", resistance=" + describeEffectInstance(player.getEffect(MobEffects.DAMAGE_RESISTANCE))
                + ", strength=" + describeEffectInstance(player.getEffect(MobEffects.DAMAGE_BOOST))
                + ", speed=" + describeEffectInstance(player.getEffect(MobEffects.MOVEMENT_SPEED))
                + ", night_vision=" + describeEffectInstance(player.getEffect(MobEffects.NIGHT_VISION))
                + ", saturation=" + describeEffectInstance(player.getEffect(MobEffects.SATURATION))
                + ", poison=" + describeEffectInstance(player.getEffect(MobEffects.POISON))
                + ", health=" + String.format("%.1f", player.getHealth())
                + "/" + String.format("%.1f", player.getMaxHealth());
    }

    private static int prepareTenseiganChakraModeTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareTenseiganChakraModeTest(source, TenseiganChakraModeItem.CHAKRA_ORBS.index(), "Chakra Mode");
    }

    private static int prepareTenseiganOrbsTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareTenseiganChakraModeTest(source, TenseiganChakraModeItem.CHAKRA_ORBS.index(), "Localised Rebirth Blast");
    }

    private static int prepareTenseiganSilverTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareTenseiganChakraModeTest(source, TenseiganChakraModeItem.SILVER_BLAST.index(), "Silver Wheel Rebirth Blast");
    }

    private static int prepareTenseiganGoldTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareTenseiganChakraModeTest(source, TenseiganChakraModeItem.GOLD_BLAST.index(), "Golden Wheel Rebirth Blast");
    }

    private static int prepareTenseiganChakraModeTest(CommandSourceStack source, int jutsuIndex, String label)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = new ItemStack(ModItems.TENSEIGANHELMET.get());
        head.getOrCreateTag().putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, 5.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        ItemStack stack = getOrGiveItem(player, ModItems.TENSEIGAN_CHAKRA_MODE.get());
        if (stack.getItem() instanceof TenseiganChakraModeItem tenseiganChakraModeItem) {
            JutsuItem.setOwner(stack, player);
            selectTenseiganJutsu(stack, tenseiganChakraModeItem, jutsuIndex);
            stack.getOrCreateTag().putInt(TenseiganChakraModeItem.CHEST_ARMOR_DAMAGE_TAG, 0);
            stack.getOrCreateTag().putInt(TenseiganChakraModeItem.LEG_ARMOR_DAMAGE_TAG, 0);
        }
        prepareJutsuResourcePool(player, 90000.0D);
        source.sendSuccess(() -> Component.literal("Tenseigan Chakra Mode " + label + " test ready: "
                + describeTenseiganChakraModeStack(player, stack)
                + ". Select the item in the main hand, right-click to use the selected jutsu, then check /narutoport m5_tenseigan state."), false);
        return 1;
    }

    private static int reportM5TenseiganState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.TENSEIGAN_CHAKRA_MODE.get());
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        source.sendSuccess(() -> Component.literal("M5 tenseigan: head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", legs=" + describeArmorSlot(player, EquipmentSlot.LEGS)
                + ", can_use_chakra_mode=" + TenseiganChakraModeItem.canUseChakraMode(head)
                + ", flight_effect=" + player.hasEffect(ModEffects.FLIGHT.get())
                + ", mayfly=" + player.getAbilities().mayfly
                + ", tenseigan_chakra_mode=" + describeTenseiganChakraModeStack(player, stack)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", nearby_tenseigan_orbs=" + countNearbyTenseiganOrbs(player)
                + ", nearby_tensei_baku_silver=" + countNearbyTenseiBakuSilver(player)
                + ", nearby_tensei_baku_gold=" + countNearbyTenseiBakuGold(player)), false);
        return 1;
    }

    private static void selectTenseiganJutsu(ItemStack stack, TenseiganChakraModeItem item, int jutsuIndex) {
        for (JutsuItem.JutsuDefinition definition : item.jutsuList()) {
            if (definition.index() == jutsuIndex) {
                stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
                item.enableJutsu(stack, definition, true);
                item.setJutsuXp(stack, definition, item.getRequiredXp(stack, definition));
                return;
            }
        }
    }

    private static int countNearbyTenseiganOrbs(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TenseiganOrbEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.TENSEIGANGUN.get())
                .size();
    }

    private static int countNearbyTenseiBakuSilver(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TenseiBakuSilverEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.TENSEI_BAKU_SILVER.get())
                .size();
    }

    private static int countNearbyTenseiBakuGold(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(TenseiBakuGoldEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.TENSEI_BAKU_GOLD.get())
                .size();
    }

    private static String describeTenseiganChakraModeStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof TenseiganChakraModeItem tenseiganChakraModeItem) {
            return itemId + "[tenseigan_chakra_mode=true,jutsu_count=" + tenseiganChakraModeItem.jutsuList().size()
                    + ",current=" + tenseiganChakraModeItem.describeCurrentJutsu(stack)
                    + ",owned=" + JutsuItem.isOwnedByOrUnbound(player, stack)
                    + ",cooldown=" + tenseiganChakraModeItem.isOnCooldown(player)
                    + ",chest_damage=" + stack.getOrCreateTag().getInt(TenseiganChakraModeItem.CHEST_ARMOR_DAMAGE_TAG)
                    + ",leg_damage=" + stack.getOrCreateTag().getInt(TenseiganChakraModeItem.LEG_ARMOR_DAMAGE_TAG)
                    + "]";
        }
        return itemId + "[tenseigan_chakra_mode=false]";
    }

    private static int prepareSummoningContractTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return prepareSummoningContractTest(source, SummoningContractItem.SUMMON_TOAD);
    }

    private static int prepareSummoningContractTest(CommandSourceStack source, JutsuItem.JutsuDefinition selected)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.SUMMONING_CONTRACT.get());
        if (stack.getItem() instanceof SummoningContractItem summoningContractItem) {
            JutsuItem.setOwner(stack, player);
            enableSummoningContractJutsu(stack, summoningContractItem, SummoningContractItem.SUMMON_TOAD);
            enableSummoningContractJutsu(stack, summoningContractItem, SummoningContractItem.SUMMON_SNAKE);
            enableSummoningContractJutsu(stack, summoningContractItem, SummoningContractItem.SUMMON_SLUG);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, selected.index());
        }
        prepareJutsuResourcePool(player, 90000.0D);
        source.sendSuccess(() -> Component.literal("Summoning Contract test ready: " + describeSummoningContractStack(player, stack)
                + ". Hold right-click to charge, release to summon, or check /narutoport m5_summoning state."), false);
        return 1;
    }

    private static int reportM5SummoningState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.SUMMONING_CONTRACT.get());
        source.sendSuccess(() -> Component.literal("M5 summoning: summoning_contract=" + describeSummoningContractStack(player, stack)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", nearby_toad_summons=" + countNearbyToadSummons(player)
                + ", nearby_snake_summons=" + countNearbySnakeSummons(player)
                + ", nearby_slug_summons=" + countNearbySlugSummons(player)
                + ", nearby_gamabunta=" + countNearbyGamabunta(player)
                + ", nearby_manda=" + countNearbyManda(player)
                + ", special_events_pending=" + SpecialEvent.pendingCount(source.getServer())), false);
        return 1;
    }

    private static int spawnGamabuntaSummon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int before = SpecialEvent.pendingCount(source.getServer());
        boolean spawned = GamabuntaEntity.spawnFrom(player);
        int after = SpecialEvent.pendingCount(source.getServer());
        source.sendSuccess(() -> Component.literal("Gamabunta spawn=" + spawned
                + ", nearby_gamabunta=" + countNearbyGamabunta(player)
                + ", pending_before=" + before
                + ", pending_after=" + after), false);
        return spawned ? 1 : 0;
    }

    private static int spawnMandaSummon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int before = SpecialEvent.pendingCount(source.getServer());
        boolean spawned = MandaEntity.spawnFrom(player);
        int after = SpecialEvent.pendingCount(source.getServer());
        source.sendSuccess(() -> Component.literal("Manda spawn=" + spawned
                + ", nearby_manda=" + countNearbyManda(player)
                + ", pending_before=" + before
                + ", pending_after=" + after), false);
        return spawned ? 1 : 0;
    }

    private static int countNearbyToadSummons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ToadSummonEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.TOAD_SUMMON.get())
                .size();
    }

    private static int countNearbySnakeSummons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SnakeSummonEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SNAKE_SUMMON.get())
                .size();
    }

    private static int countNearbySlugSummons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SlugSummonEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SLUG.get())
                .size();
    }

    private static int countNearbyGamabunta(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(GamabuntaEntity.class, player.getBoundingBox().inflate(160.0D),
                        entity -> entity.getType() == ModEntityTypes.GAMABUNTA.get())
                .size();
    }

    private static int countNearbyManda(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(MandaEntity.class, player.getBoundingBox().inflate(160.0D),
                        entity -> entity.getType() == ModEntityTypes.MANDA.get())
                .size();
    }

    private static void enableSummoningContractJutsu(ItemStack stack, SummoningContractItem summoningContractItem,
            JutsuItem.JutsuDefinition definition) {
        summoningContractItem.enableJutsu(stack, definition, true);
        summoningContractItem.setJutsuXp(stack, definition, summoningContractItem.getRequiredXp(stack, definition));
    }

    private static String describeSummoningContractStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof SummoningContractItem summoningContractItem) {
            return itemId + "[summoning_contract=true,jutsu_count=" + summoningContractItem.jutsuList().size()
                    + ",current=" + summoningContractItem.describeCurrentJutsu(stack)
                    + ",owned=" + JutsuItem.isOwnedByOrUnbound(player, stack)
                    + ",toad=" + describeJutsuDefinitionState(stack, SummoningContractItem.SUMMON_TOAD)
                    + ",snake=" + describeJutsuDefinitionState(stack, SummoningContractItem.SUMMON_SNAKE)
                    + ",slug=" + describeJutsuDefinitionState(stack, SummoningContractItem.SUMMON_SLUG)
                    + "]";
        }
        return itemId + "[summoning_contract=false]";
    }

    private static int prepareTeamScrollTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.TEAM_SCROLL.get());
        if (player.experienceLevel < 15) {
            player.giveExperienceLevels(15 - player.experienceLevel);
        }
        NarutomodModVariables.setBattleExperience(player, Math.max(NarutomodModVariables.getBattleExperience(player), 1.0D));
        source.sendSuccess(() -> Component.literal("Team Scroll test ready: " + describeTeamScrollStack(player, stack)
                + ". Use /narutoport m5_team join, /narutoport m5_team leave, or right-click the scroll."), false);
        return 1;
    }

    private static int joinTeamScroll(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.TEAM_SCROLL.get());
        if (stack.isEmpty()) {
            stack = getOrGiveItem(player, ModItems.TEAM_SCROLL.get());
        }
        boolean joined = stack.getItem() instanceof TeamScrollItem && TeamScrollItem.addTeamMember(stack, player);
        ItemStack resultStack = stack;
        source.sendSuccess(() -> Component.literal("Team Scroll join=" + joined + ": " + describeTeamScrollStack(player, resultStack)), false);
        return joined ? 1 : 0;
    }

    private static int leaveTeamScroll(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.TEAM_SCROLL.get());
        boolean left = !stack.isEmpty() && stack.getItem() instanceof TeamScrollItem && TeamScrollItem.removeTeamMember(stack, player);
        source.sendSuccess(() -> Component.literal("Team Scroll leave=" + left + ": " + describeTeamScrollStack(player, stack)), false);
        return left ? 1 : 0;
    }

    private static int reportM5TeamScrollState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = findItem(player, ModItems.TEAM_SCROLL.get());
        source.sendSuccess(() -> Component.literal("M5 team scroll: " + describeTeamScrollStack(player, stack)
                + ", player_team=" + (player.getTeam() != null ? player.getTeam().getName() : "none")
                + ", xp_level=" + player.experienceLevel
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)), false);
        return 1;
    }

    private static String describeTeamScrollStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof TeamScrollItem) {
            String team = TeamScrollItem.getTeamDisplayName(player.level(), stack);
            String members = String.join("|", TeamScrollItem.getTeamMembers(player.level(), stack));
            return itemId + "[team_scroll=true,team=" + (team.isEmpty() ? "none" : team)
                    + ",members=" + (members.isEmpty() ? "none" : members)
                    + "]";
        }
        return itemId + "[team_scroll=false]";
    }

    private static int prepareZzzTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = getOrGiveItem(player, ModItems.ZZZ.get());
        if (stack.getItem() instanceof ZzzItem) {
            ZzzItem.clearAttacker(stack);
        }
        source.sendSuccess(() -> Component.literal("Zzz test ready: " + describeZzzStack(stack)
                + ". Look at a mob and right-click to store attacker, look at a living target and right-click again, "
                + "or right-click a block near Toad/Gamabunta/Slug summons to send them there."), false);
        return 1;
    }

    private static int reportM5MiscState(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("M5 misc: zzz=" + describeZzzStack(findItem(player, ModItems.ZZZ.get()))
                + ", commanded_toad_summons=" + countCommandedToadSummons(player)
                + ", commanded_gamabunta=" + countCommandedGamabunta(player)
                + ", commanded_slug_summons=" + countCommandedSlugSummons(player)), false);
        return 1;
    }

    private static String describeZzzStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof ZzzItem) {
            return itemId + "[zzz=true,attackerID=" + ZzzItem.getAttackerId(stack) + "]";
        }
        return itemId + "[zzz=false]";
    }

    private static int countCommandedToadSummons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ToadSummonEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.TOAD_SUMMON.get() && entity.hasCommandedNavigationTarget())
                .size();
    }

    private static int countCommandedSlugSummons(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(SlugSummonEntity.class, player.getBoundingBox().inflate(128.0D),
                        entity -> entity.getType() == ModEntityTypes.SLUG.get() && entity.hasCommandedNavigationTarget())
                .size();
    }

    private static int countCommandedGamabunta(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(GamabuntaEntity.class, player.getBoundingBox().inflate(160.0D),
                        entity -> entity.getType() == ModEntityTypes.GAMABUNTA.get() && entity.hasCommandedNavigationTarget())
                .size();
    }

    private static int countThrownTool(ServerPlayer player, EntityType<ThrownNinjaToolEntity> type) {
        return player.serverLevel()
                .getEntitiesOfClass(ThrownNinjaToolEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == type)
                .size();
    }

    private static int countThrownSpecialWeapon(ServerPlayer player, EntityType<ThrownSpecialWeaponEntity> type) {
        return player.serverLevel()
                .getEntitiesOfClass(ThrownSpecialWeaponEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == type)
                .size();
    }

    private static int countFoldingFanProjectile(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(FoldingFanProjectileEntity.class, player.getBoundingBox().inflate(96.0D),
                        entity -> entity.getType() == ModEntityTypes.ENTITYBULLETFOLDING_FAN.get())
                .size();
    }

    private static boolean isAshBonesAllowedHead(ItemStack head) {
        return head.is(ModItems.BYAKURINNESHARINGANHELMET.get())
                || head.is(ModItems.RINNEGANHELMET.get())
                || head.is(ModItems.TENSEIGANHELMET.get());
    }

    private static int spawnToolTargetArmorStand(ServerPlayer player, String name) {
        DirectionBasis basis = DirectionBasis.from(player);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        Vec3 point = basis.point(8.0D, 0.0D, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal(name));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int prepareTransformationItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        TransformationJutsuEntity.stopFor(player);
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.TRANSFORM);
        getOrGiveItem(player, ModItems.SCROLL_TRANSFORMATION.get());
        int targets = spawnTransformationTargetArmorStand(player);
        source.sendSuccess(() -> Component.literal("Transformation item test ready: scroll_transformation+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.TRANSFORM)
                + ", target_armor_stands=" + targets
                + ", nearby_transformation_jutsu=" + player.serverLevel()
                        .getEntitiesOfClass(TransformationJutsuEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int spawnTransformationTargetArmorStand(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        Vec3 point = basis.point(6.0D, 0.0D, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M5 Transformation target"));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int prepareBugSwarmItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        BugSwarmEntity.returnOwnedNear(player, 64.0D);
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.BUG_SWARM);
        getOrGiveItem(player, ModItems.SCROLL_KIKAICHU_SPHERE.get());
        int targets = spawnBugSwarmTargetArmorStand(player);
        source.sendSuccess(() -> Component.literal("Bug Swarm item test ready: scroll_kikaichu_sphere+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.BUG_SWARM)
                + ", target_armor_stands=" + targets
                + ", nearby_bug_swarm=" + player.serverLevel()
                        .getEntitiesOfClass(BugSwarmEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int spawnBugSwarmTargetArmorStand(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        Vec3 point = basis.point(8.0D, 0.0D, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M5 Bug Swarm chakra target"));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int preparePuppetItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.PUPPET);
        ItemStack karasuScroll = getOrGiveItem(player, ModItems.SCROLL_KARASU.get());
        karasuScroll.setDamageValue(0);
        PuppetScrollItem.setSealed(karasuScroll, true);
        ItemStack sanshouoScroll = getOrGiveItem(player, ModItems.SCROLL_SANSHOUO.get());
        sanshouoScroll.setDamageValue(0);
        PuppetScrollItem.setSealed(sanshouoScroll, true);
        if (player.serverLevel().getEntitiesOfClass(PuppetKarasuEntity.class, player.getBoundingBox().inflate(12.0D)).isEmpty()) {
            PuppetKarasuEntity.spawnNear(player);
        }
        if (player.serverLevel().getEntitiesOfClass(PuppetSanshouoEntity.class, player.getBoundingBox().inflate(12.0D)).isEmpty()) {
            PuppetSanshouoEntity.spawnNear(player);
        }
        if (player.serverLevel().getEntitiesOfClass(PuppetHirukoEntity.class, player.getBoundingBox().inflate(12.0D)).isEmpty()) {
            PuppetHirukoEntity.spawnNear(player);
        }
        source.sendSuccess(() -> Component.literal("Puppet item test ready: sealed scroll_karasu+scroll_sanshouo+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.PUPPET)
                + ", scroll_karasu=" + describePuppetScrollStack(karasuScroll)
                + ", scroll_sanshouo=" + describePuppetScrollStack(sanshouoScroll)
                + ", nearby_puppet_karasu=" + player.serverLevel()
                        .getEntitiesOfClass(PuppetKarasuEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", nearby_puppet_sanshouo=" + player.serverLevel()
                        .getEntitiesOfClass(PuppetSanshouoEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", nearby_puppet_hiruko=" + player.serverLevel()
                        .getEntitiesOfClass(PuppetHirukoEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSealingChainsItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        SealingChainsEntity.retractOwnedNear(player, 64.0D);
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.SEALING_CHAIN);
        source.sendSuccess(() -> Component.literal("Sealing Chains item test ready: ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.SEALING_CHAIN)
                + ", nearby_sealing_chains=" + player.serverLevel()
                        .getEntitiesOfClass(SealingChainsEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareLimboCloneItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        LimboCloneEntity.removeAllFor(player);
        getOrGiveItem(player, ModItems.RINNEGANHELMET.get());
        getOrGiveItem(player, ModItems.SIX_PATH_SENJUTSU.get());
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.LIMBO_CLONE);
        source.sendSuccess(() -> Component.literal("Limbo Clone item test ready: ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.LIMBO_CLONE)
                + ", nearby_limbo_clone=" + player.serverLevel()
                        .getEntitiesOfClass(LimboCloneEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareRinneganDevaSpecialJutsu2Test(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        RinneganSpecialJutsuHandler.setRinneganPath(head, 0);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        player.getPersistentData().remove(RinneganSpecialJutsuHandler.CHIBAKU_COOLDOWN_TAG);
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, false);
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Deva special jutsu 2 test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", path=" + RinneganSpecialJutsuHandler.getRinneganPath(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", chibaku_tensei_cooldown_ticks=" + getChibakuCooldownTicks(player)
                + ", nearby_chibaku_balls=" + countNearbyChibakuTenseiBalls(player)
                + ", nearby_chibaku_satellites=" + countNearbyChibakuSatellites(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", use Up or /narutoport m5_jutsu rinnegan_path_cycle to cycle paths."), false);
        return 1;
    }

    private static int prepareRinneganAsuraSpecialJutsu2Test(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        RinneganSpecialJutsuHandler.setRinneganPath(head, RinneganSpecialJutsuHandler.ASURA_PATH);
        head.getOrCreateTag().putInt(RinneganSpecialJutsuHandler.SUMMONED_ANIMAL_ID_TAG, 0);
        ProcedureUtils.removeUniqueIdTag(head, RinneganSpecialJutsuHandler.KING_OF_HELL_ID_TAG);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, false);
        NarutomodModVariables.sync(player);
        boolean active = RinneganSpecialJutsuHandler.maintainAsuraPath(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Asura passive test ready: "
                + describeRinneganAsuraState(player, active)
                + ", use the offhand Asura Cannon with right-click release."), false);
        return active ? 1 : 0;
    }

    private static int maintainRinneganAsuraPath(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RinneganSpecialJutsuHandler.isRinneganLikeHead(head)) {
            source.sendFailure(Component.literal("Rinnegan/Tenseigan head is required. Run /narutoport m5_jutsu rinnegan_asura_ready."));
            return 0;
        }
        boolean active = RinneganSpecialJutsuHandler.maintainAsuraPath(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Asura maintain: "
                + describeRinneganAsuraState(player, active)), false);
        return active ? 1 : 0;
    }

    private static String describeRinneganAsuraState(ServerPlayer player, boolean active) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return "active=" + active
                + ", head=" + (RinneganSpecialJutsuHandler.isRinneganLikeHead(head)
                        ? describeArmorSlot(player, EquipmentSlot.HEAD)
                        : "none")
                + ", path=" + (RinneganSpecialJutsuHandler.isRinneganLikeHead(head)
                        ? RinneganSpecialJutsuHandler.getRinneganPath(head)
                        : -1)
                + ", chest=" + describeArmorSlot(player, EquipmentSlot.CHEST)
                + ", offhand=" + describeWeaponStack(player.getOffhandItem())
                + ", asura_body_equipped=" + RinneganSpecialJutsuHandler.hasAsuraBodyEquipped(player)
                + ", asura_cannon_offhand=" + RinneganSpecialJutsuHandler.hasAsuraCannonOffhand(player)
                + ", asura_body_ticks_used=" + getAsuraBodyTicksUsed(chest)
                + ", strength=" + describeEffectInstance(player.getEffect(MobEffects.DAMAGE_BOOST))
                + ", speed=" + describeEffectInstance(player.getEffect(MobEffects.MOVEMENT_SPEED))
                + ", haste=" + describeEffectInstance(player.getEffect(MobEffects.DIG_SPEED))
                + ", jump=" + describeEffectInstance(player.getEffect(MobEffects.JUMP))
                + ", saturation=" + describeEffectInstance(player.getEffect(MobEffects.SATURATION))
                + ", asurapatharmorbody_stack=" + findItem(player, ModItems.ASURAPATHARMORBODY.get()).getCount()
                + ", asuracanon_stack=" + findItem(player, ModItems.ASURACANON.get()).getCount();
    }

    private static double getAsuraBodyTicksUsed(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getDouble(RinneganSpecialJutsuHandler.ASURA_TICKS_USED_TAG) : -1.0D;
    }

    private static int preparePowerIncreaseJutsuCycleTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        ItemStack stack = getOrGiveItem(player, ModItems.NINJUTSU.get());
        if (stack.getItem() instanceof NinjutsuItem ninjutsuItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, NinjutsuItem.REPLACEMENT.index());
            ninjutsuItem.enableJutsu(stack, NinjutsuItem.REPLACEMENT, true);
            ninjutsuItem.enableJutsu(stack, NinjutsuItem.RASENGAN, true);
            ninjutsuItem.setJutsuXp(stack, NinjutsuItem.REPLACEMENT, ninjutsuItem.getRequiredXp(stack, NinjutsuItem.REPLACEMENT));
            ninjutsuItem.setJutsuXp(stack, NinjutsuItem.RASENGAN, ninjutsuItem.getRequiredXp(stack, NinjutsuItem.RASENGAN));
            NinjutsuItem.setRasenganLearned(stack, true);
        }
        stack = moveStackToMainHand(player, stack);
        ItemStack preparedStack = stack;
        source.sendSuccess(() -> Component.literal("PowerIncrease jutsu cycle test ready: mainhand="
                + describePowerIncreaseJutsuStack(preparedStack)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", use /narutoport m5_jutsu powerincrease_jutsu_cycle or press Up to switch."), false);
        return 1;
    }

    private static int cyclePowerIncreaseJutsuItem(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String beforeMainhand = describePowerIncreaseJutsuStack(player.getMainHandItem());
        String beforeOffhand = describePowerIncreaseJutsuStack(player.getOffhandItem());
        boolean handled = PowerIncreaseKeyHandler.handle(player, false);
        source.sendSuccess(() -> Component.literal("PowerIncrease jutsu cycle: handled=" + handled
                + ", before_mainhand=" + beforeMainhand
                + ", after_mainhand=" + describePowerIncreaseJutsuStack(player.getMainHandItem())
                + ", before_offhand=" + beforeOffhand
                + ", after_offhand=" + describePowerIncreaseJutsuStack(player.getOffhandItem())), false);
        return handled ? 1 : 0;
    }

    private static ItemStack moveStackToMainHand(ServerPlayer player, ItemStack stack) {
        if (player.getMainHandItem() == stack) {
            return stack;
        }
        ItemStack prepared = stack.copy();
        stack.setCount(0);
        ItemStack displaced = player.getMainHandItem();
        if (!displaced.isEmpty() && !player.getInventory().add(displaced.copy())) {
            player.drop(displaced.copy(), false);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, prepared);
        return player.getMainHandItem();
    }

    private static String describePowerIncreaseJutsuStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!(stack.getItem() instanceof JutsuItem jutsuItem)) {
            return itemId + "[not_jutsu_item]";
        }
        return itemId + "[current=" + jutsuItem.describeCurrentJutsuKey(stack) + "]";
    }

    private static int preparePowerIncreaseByakuganFovTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID()));
        moveHeldItemToInventory(player, InteractionHand.MAIN_HAND);
        moveHeldItemToInventory(player, InteractionHand.OFF_HAND);
        ByakuganHandler.handleSpecialJutsuKey(player, 1, false);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 1, true);
        source.sendSuccess(() -> Component.literal("PowerIncrease Byakugan ready: handled=" + handled
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", byakugan_head=" + ByakuganHandler.isByakuganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", active=" + ByakuganHandler.isActive(player)
                + ", fov=" + ByakuganHandler.getFov(player)
                + ", chakra_usage=" + ByakuganHandler.getByakuganChakraUsage(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", mainhand=" + describePowerIncreaseJutsuStack(player.getMainHandItem())
                + ", offhand=" + describePowerIncreaseJutsuStack(player.getOffhandItem())
                + ", use /narutoport m5_jutsu powerincrease_byakugan_press or press Up to reduce FOV."), false);
        return handled ? 1 : 0;
    }

    private static int prepareByakuganVisualTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID()));
        moveHeldItemToInventory(player, InteractionHand.MAIN_HAND);
        moveHeldItemToInventory(player, InteractionHand.OFF_HAND);
        ByakuganHandler.handleSpecialJutsuKey(player, 1, false);
        boolean activated = ByakuganHandler.handleSpecialJutsuKey(player, 1, true);
        int powerPresses = 0;
        for (int index = 0; index < 5; index++) {
            if (PowerIncreaseKeyHandler.handle(player, true)) {
                powerPresses++;
            }
        }
        int targets = spawnToolTargetArmorStand(player, "M5 Byakugan glow target");
        float fov = ByakuganHandler.getFov(player);
        float ninjaLevel = (float)NarutomodModVariables.getNinjaLevel(player);
        int finalPowerPresses = powerPresses;
        source.sendSuccess(() -> Component.literal("Byakugan visual test ready: activated=" + activated
                + ", power_presses=" + finalPowerPresses
                + ", fov=" + fov
                + ", projected_camera_distance=" + String.format("%.2f", ByakuganHandler.projectedCameraDistance(fov, ninjaLevel))
                + ", target_render_distance=" + ByakuganHandler.targetRenderDistance(ninjaLevel)
                + ", target_armor_stands=" + targets
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ". Client should switch to the Byakugan camera and glow nearby living entities."), false);
        return activated ? 1 : 0;
    }

    private static int pressPowerIncreaseByakuganFov(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean activeBefore = ByakuganHandler.isActive(player);
        float fovBefore = ByakuganHandler.getFov(player);
        boolean handled = PowerIncreaseKeyHandler.handle(player, true);
        source.sendSuccess(() -> Component.literal("PowerIncrease Byakugan press: handled=" + handled
                + ", active_before=" + activeBefore
                + ", active_after=" + ByakuganHandler.isActive(player)
                + ", fov_before=" + fovBefore
                + ", fov_after=" + ByakuganHandler.getFov(player)
                + ", byakugan_head=" + ByakuganHandler.isByakuganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", mainhand=" + describePowerIncreaseJutsuStack(player.getMainHandItem())
                + ", offhand=" + describePowerIncreaseJutsuStack(player.getOffhandItem())
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return handled ? 1 : 0;
    }

    private static int releasePowerIncreaseByakugan(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean activeBefore = ByakuganHandler.isActive(player);
        float fovBefore = ByakuganHandler.getFov(player);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 1, false);
        source.sendSuccess(() -> Component.literal("PowerIncrease Byakugan release: handled=" + handled
                + ", active_before=" + activeBefore
                + ", active_after=" + ByakuganHandler.isActive(player)
                + ", fov_before=" + fovBefore
                + ", fov_after=" + ByakuganHandler.getFov(player)), false);
        return handled ? 1 : 0;
    }

    private static int prepareByakugan64PalmsTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        if (player.experienceLevel < 20) {
            player.giveExperienceLevels(20 - player.experienceLevel);
        }
        ItemStack head = ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID());
        ByakuganHandler.clearTechniqueCooldowns(head);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, false);
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Byakugan 64 Palms test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", level=" + player.experienceLevel
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", cooldown_ticks=" + ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG)
                + ", nearby_eight_trigrams=" + countNearbyEightTrigrams(player)
                + ", use /narutoport m5_jutsu byakugan_64_release or press Special Jutsu 2."), false);
        return 1;
    }

    private static int releaseByakugan64Palms(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int before = countNearbyEightTrigrams(player);
        double chakraBefore = Chakra.pathway(player).getAmount();
        long cooldownBefore = ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 2, false);
        source.sendSuccess(() -> Component.literal("Byakugan 64 Palms release: handled=" + handled
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", level=" + player.experienceLevel
                + ", chakra_spent=" + (chakraBefore - Chakra.pathway(player).getAmount())
                + ", cooldown_before=" + cooldownBefore
                + ", cooldown_after=" + ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG)
                + ", nearby_eight_trigrams=" + countNearbyEightTrigrams(player)
                + ", eight_trigrams_delta=" + (countNearbyEightTrigrams(player) - before)
                + ", jutsu_key2_pressed=" + NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED)), false);
        return handled ? 1 : 0;
    }

    private static int prepareByakuganKaitenTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        if (player.experienceLevel < 30) {
            player.giveExperienceLevels(30 - player.experienceLevel);
        }
        ItemStack head = ownedEye(ModItems.BYAKUGANHELMET.get(), player.getUUID());
        ByakuganHandler.clearTechniqueCooldowns(head);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        HakkeshoKeitenEntity.releaseOwnedBy(player);
        source.sendSuccess(() -> Component.literal("Byakugan Kaiten test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", level=" + player.experienceLevel
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", cooldown_ticks=" + ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG)
                + ", nearby_hakkesho_keiten=" + countNearbyHakkeshoKeiten(player)
                + ", use byakugan_kaiten_press then byakugan_kaiten_release, or press/release Special Jutsu 3."), false);
        return 1;
    }

    private static int pressByakuganKaiten(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int before = countNearbyHakkeshoKeiten(player);
        double chakraBefore = Chakra.pathway(player).getAmount();
        long cooldownBefore = ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 3, true);
        source.sendSuccess(() -> Component.literal("Byakugan Kaiten press: handled=" + handled
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", level=" + player.experienceLevel
                + ", vehicle=" + (player.getVehicle() instanceof HakkeshoKeitenEntity)
                + ", chakra_before=" + chakraBefore
                + ", chakra_after=" + Chakra.pathway(player).getAmount()
                + ", cooldown_before=" + cooldownBefore
                + ", cooldown_after=" + ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG)
                + ", nearby_hakkesho_keiten=" + countNearbyHakkeshoKeiten(player)
                + ", hakkesho_keiten_delta=" + (countNearbyHakkeshoKeiten(player) - before)), false);
        return handled ? 1 : 0;
    }

    private static int releaseByakuganKaiten(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int before = countNearbyHakkeshoKeiten(player);
        int foodBefore = player.getFoodData().getFoodLevel();
        double chakraBefore = Chakra.pathway(player).getAmount();
        long cooldownBefore = ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 3, false);
        source.sendSuccess(() -> Component.literal("Byakugan Kaiten release: handled=" + handled
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", vehicle=" + (player.getVehicle() instanceof HakkeshoKeitenEntity)
                + ", food_before=" + foodBefore
                + ", food_after=" + player.getFoodData().getFoodLevel()
                + ", chakra_before=" + chakraBefore
                + ", chakra_after=" + Chakra.pathway(player).getAmount()
                + ", cooldown_before=" + cooldownBefore
                + ", cooldown_after=" + ByakuganHandler.getCooldownTicks(player, ByakuganHandler.HAKKESHOKAITEN_COOLDOWN_TAG)
                + ", nearby_hakkesho_keiten=" + countNearbyHakkeshoKeiten(player)
                + ", hakkesho_keiten_delta=" + (countNearbyHakkeshoKeiten(player) - before)), false);
        return handled ? 1 : 0;
    }

    private static int prepareByakurinYomotsuTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, byakurinByakuganHead(player));
        int targets = spawnToolTargetArmorStand(player, "M5 Byakurin Yomotsu target");
        source.sendSuccess(() -> Component.literal("Byakurin Yomotsu test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", byakugan_head=" + ByakuganHandler.isByakuganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", rinnesharingan=" + ByakuganHandler.isRinnesharinganActivated(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", target_armor_stands=" + targets
                + ", nearby_portal_blocks=" + countNearbyPortalBlocks(player, 32)
                + ", use /narutoport m5_jutsu byakurin_yomotsu_release or press Special Jutsu 2."), false);
        return 1;
    }

    private static int releaseByakurinYomotsu(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int portalBlocksBefore = countNearbyPortalBlocks(player, 32);
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 2, false);
        source.sendSuccess(() -> Component.literal("Byakurin Yomotsu release: handled=" + handled
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", byakugan_head=" + ByakuganHandler.isByakuganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", rinnesharingan=" + ByakuganHandler.isRinnesharinganActivated(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", nearby_portal_blocks=" + countNearbyPortalBlocks(player, 32)
                + ", portal_block_delta=" + (countNearbyPortalBlocks(player, 32) - portalBlocksBefore)
                + ", jutsu_key2_pressed=" + NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED)), false);
        return handled ? 1 : 0;
    }

    private static int prepareByakurinShockwaveTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, byakurinByakuganHead(player));
        player.getPersistentData().putDouble(ByakuganHandler.RINNESHARINGAN_PRESS_TIME_TAG, 0.0D);
        int targets = spawnToolTargetArmorStand(player, "M5 Byakurin Shockwave target");
        source.sendSuccess(() -> Component.literal("Byakurin shockwave test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", rinnesharingan=" + ByakuganHandler.isRinnesharinganActivated(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", press_time=" + ByakuganHandler.getRinnesharinganPressTime(player)
                + ", target_armor_stands=" + targets
                + ", use byakurin_shockwave_charge <ticks> then byakurin_shockwave_release, or hold/release Special Jutsu 3."), false);
        return 1;
    }

    private static int pressByakurinShockwave(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return chargeByakurinShockwave(source, 1);
    }

    private static int chargeByakurinShockwave(CommandSourceStack source, int ticks)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double before = ByakuganHandler.getRinnesharinganPressTime(player);
        boolean handled = false;
        for (int i = 0; i < ticks; i++) {
            handled |= ByakuganHandler.handleSpecialJutsuKey(player, 3, true);
        }
        boolean finalHandled = handled;
        source.sendSuccess(() -> Component.literal("Byakurin shockwave charge: handled=" + finalHandled
                + ", ticks=" + ticks
                + ", press_time_before=" + before
                + ", press_time_after=" + ByakuganHandler.getRinnesharinganPressTime(player)
                + ", projected_radius=" + (ByakuganHandler.getRinnesharinganPressTime(player) / 2.0D)
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)), false);
        return handled ? 1 : 0;
    }

    private static int releaseByakurinShockwave(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double before = ByakuganHandler.getRinnesharinganPressTime(player);
        int nearbyArmorStands = player.serverLevel()
                .getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(120.0D), Entity::isAlive)
                .size();
        boolean handled = ByakuganHandler.handleSpecialJutsuKey(player, 3, false);
        source.sendSuccess(() -> Component.literal("Byakurin shockwave release: handled=" + handled
                + ", press_time_before=" + before
                + ", press_time_after=" + ByakuganHandler.getRinnesharinganPressTime(player)
                + ", released_radius=" + (before / 2.0D)
                + ", nearby_armor_stands=" + nearbyArmorStands
                + ", head=" + describeArmorSlot(player, EquipmentSlot.HEAD)), false);
        return handled ? 1 : 0;
    }

    private static ItemStack byakurinByakuganHead(ServerPlayer player) {
        return ownedEye(ModItems.BYAKURINNESHARINGANHELMET.get(), player.getUUID());
    }

    private static int preparePowerIncreaseSusanooUpgradeTest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 30000.0D);
        equipDebugArmor(player, EquipmentSlot.HEAD, ownedEye(ModItems.MANGEKYOSHARINGANETERNALHELMET.get(), player.getUUID()));
        moveHeldItemToInventory(player, InteractionHand.MAIN_HAND);
        moveHeldItemToInventory(player, InteractionHand.OFF_HAND);
        if (player.getVehicle() instanceof AbstractSusanooEntity current && current.isOwnedBy(player)) {
            player.stopRiding();
            current.discard();
        }
        SusanooSkeletonEntity susanoo = SusanooSkeletonEntity.spawnFrom(player);
        if (susanoo == null) {
            source.sendFailure(Component.literal("Could not create PowerIncrease Susanoo test entity"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("PowerIncrease Susanoo ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", sharingan_head=" + SusanooPowerIncreaseHandler.isSharinganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", vehicle=" + SusanooPowerIncreaseHandler.describeVehicle(player.getVehicle())
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", mainhand=" + describePowerIncreaseJutsuStack(player.getMainHandItem())
                + ", offhand=" + describePowerIncreaseJutsuStack(player.getOffhandItem())
                + ", use /narutoport m5_jutsu powerincrease_susanoo_upgrade or press Up to upgrade."), false);
        return 1;
    }

    private static int upgradePowerIncreaseSusanoo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String beforeVehicle = SusanooPowerIncreaseHandler.describeVehicle(player.getVehicle());
        int skeletonsBefore = countNearbySusanooSkeletons(player);
        int clothedBefore = countNearbySusanooClothed(player);
        int wingedBefore = countNearbySusanooWinged(player);
        double chakraBefore = Chakra.pathway(player).getAmount();
        boolean handled = PowerIncreaseKeyHandler.handle(player, false);
        source.sendSuccess(() -> Component.literal("PowerIncrease Susanoo upgrade: handled=" + handled
                + ", before_vehicle=" + beforeVehicle
                + ", after_vehicle=" + SusanooPowerIncreaseHandler.describeVehicle(player.getVehicle())
                + ", sharingan_head=" + SusanooPowerIncreaseHandler.isSharinganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", mainhand=" + describePowerIncreaseJutsuStack(player.getMainHandItem())
                + ", offhand=" + describePowerIncreaseJutsuStack(player.getOffhandItem())
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", chakra_spent=" + (chakraBefore - Chakra.pathway(player).getAmount())
                + ", nearby_susanoo_skeletons=" + countNearbySusanooSkeletons(player)
                + ", skeleton_delta=" + (countNearbySusanooSkeletons(player) - skeletonsBefore)
                + ", nearby_susanoo_clothed=" + countNearbySusanooClothed(player)
                + ", clothed_delta=" + (countNearbySusanooClothed(player) - clothedBefore)
                + ", nearby_susanoo_winged=" + countNearbySusanooWinged(player)
                + ", winged_delta=" + (countNearbySusanooWinged(player) - wingedBefore)), false);
        return handled ? 1 : 0;
    }

    private static void moveHeldItemToInventory(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return;
        }
        ItemStack displaced = held.copy();
        player.setItemInHand(hand, ItemStack.EMPTY);
        if (!player.getInventory().add(displaced)) {
            player.drop(displaced, false);
        }
    }

    private static int prepareRinneganOuterSpecialJutsu2Test(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 90000.0D);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        RinneganSpecialJutsuHandler.setRinneganPath(head, RinneganSpecialJutsuHandler.OUTER_PATH);
        head.getOrCreateTag().putInt(RinneganSpecialJutsuHandler.SUMMONED_ANIMAL_ID_TAG, 0);
        ProcedureUtils.removeUniqueIdTag(head, RinneganSpecialJutsuHandler.KING_OF_HELL_ID_TAG);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, false);
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Outer special jutsu 2 test ready: "
                + describeRinneganOuterState(source.getServer(), player)
                + ", use /narutoport m5_jutsu rinnegan_path_release to summon or dismiss Gedo Statue."), false);
        return 1;
    }

    private static String describeRinneganOuterState(MinecraftServer server, ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return "head=" + (RinneganSpecialJutsuHandler.isRinneganLikeHead(head)
                ? describeArmorSlot(player, EquipmentSlot.HEAD)
                : "none")
                + ", path=" + (RinneganSpecialJutsuHandler.isRinneganLikeHead(head)
                        ? RinneganSpecialJutsuHandler.getRinneganPath(head)
                        : -1)
                + ", path_name=" + RinneganSpecialJutsuHandler.pathDisplayName(RinneganSpecialJutsuHandler.OUTER_PATH).getString()
                + ", active_rinnesharingan=" + SixPathSenjutsuItem.hasRinneSharingan(player)
                + ", ninja_level=" + NarutomodModVariables.getNinjaLevel(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", nearby_gedo_statue=" + countNearbyGedoStatues(player)
                + ", loaded_gedo_statues=" + countLoadedGedoStatues(server)
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(server)
                + ", loaded_ten_tails=" + BijuManager.countLoadedTenTails(server);
    }

    private static int prepareRinneganPathSpecialJutsu2Test(CommandSourceStack source, int path, String label)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        ItemStack head = new ItemStack(ModItems.RINNEGANHELMET.get());
        RinneganSpecialJutsuHandler.setRinneganPath(head, path);
        head.getOrCreateTag().putInt(RinneganSpecialJutsuHandler.SUMMONED_ANIMAL_ID_TAG, 0);
        ProcedureUtils.removeUniqueIdTag(head, RinneganSpecialJutsuHandler.KING_OF_HELL_ID_TAG);
        equipDebugArmor(player, EquipmentSlot.HEAD, head);
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, false);
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Rinnegan " + label + " special jutsu 2 test ready: head="
                + describeArmorSlot(player, EquipmentSlot.HEAD)
                + ", path=" + RinneganSpecialJutsuHandler.getRinneganPath(player.getItemBySlot(EquipmentSlot.HEAD))
                + ", path_name=" + RinneganSpecialJutsuHandler.pathDisplayName(path).getString()
                + ", nearby_preta_shields=" + countNearbyPretaShields(player)
                + ", nearby_giant_dog_2h=" + countNearbyGiantDog2h(player)
                + ", nearby_king_of_hell=" + countNearbyKingOfHell(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", use /narutoport m5_jutsu rinnegan_path_release to trigger."), false);
        return 1;
    }

    private static int cycleRinneganPowerIncreasePath(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RinneganSpecialJutsuHandler.isRinneganLikeHead(head)) {
            source.sendFailure(Component.literal("Rinnegan/Tenseigan head is required. Run /narutoport m5_jutsu rinnegan_deva_ready."));
            return 0;
        }
        int before = RinneganSpecialJutsuHandler.getRinneganPath(head);
        boolean handled = RinneganSpecialJutsuHandler.handlePowerIncreaseKey(player, false);
        int after = RinneganSpecialJutsuHandler.getRinneganPath(player.getItemBySlot(EquipmentSlot.HEAD));
        source.sendSuccess(() -> Component.literal("Rinnegan powerincrease cycle: handled=" + handled
                + ", before=" + before
                + ", after=" + after
                + ", path=" + RinneganSpecialJutsuHandler.pathDisplayName(after).getString()), false);
        return handled ? 1 : 0;
    }

    private static int triggerRinneganSelectedPathRelease(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RinneganSpecialJutsuHandler.isRinneganLikeHead(head)) {
            source.sendFailure(Component.literal("Rinnegan/Tenseigan head is required. Run a rinnegan_*_ready command first."));
            return 0;
        }
        int path = RinneganSpecialJutsuHandler.getRinneganPath(head);
        int pretaBefore = countNearbyPretaShields(player);
        int dogsBefore = countNearbyGiantDog2h(player);
        int kingsBefore = countNearbyKingOfHell(player);
        int gedoBefore = countNearbyGedoStatues(player);
        int loadedGedoBefore = countLoadedGedoStatues(source.getServer());
        int loadedTenTailsBefore = BijuManager.countLoadedTenTails(source.getServer());
        double chakraBefore = Chakra.pathway(player).getAmount();
        boolean triggered = RinneganSpecialJutsuHandler.triggerPath(player, path, false, true);
        ItemStack currentHead = player.getItemBySlot(EquipmentSlot.HEAD);
        source.sendSuccess(() -> Component.literal("Rinnegan selected path release: triggered=" + triggered
                + ", path=" + path
                + ", path_name=" + RinneganSpecialJutsuHandler.pathDisplayName(path).getString()
                + ", nearby_preta_shields=" + countNearbyPretaShields(player)
                + ", preta_delta=" + (countNearbyPretaShields(player) - pretaBefore)
                + ", nearby_giant_dog_2h=" + countNearbyGiantDog2h(player)
                + ", dog_delta=" + (countNearbyGiantDog2h(player) - dogsBefore)
                + ", summoned_animal_id=" + (currentHead.getTag() != null
                        ? currentHead.getTag().getInt(RinneganSpecialJutsuHandler.SUMMONED_ANIMAL_ID_TAG)
                        : 0)
                + ", nearby_king_of_hell=" + countNearbyKingOfHell(player)
                + ", king_delta=" + (countNearbyKingOfHell(player) - kingsBefore)
                + ", koh_id_present=" + (currentHead.getTag() != null
                        && currentHead.getTag().hasUUID(RinneganSpecialJutsuHandler.KING_OF_HELL_ID_TAG))
                + ", nearby_gedo_statue=" + countNearbyGedoStatues(player)
                + ", gedo_delta=" + (countNearbyGedoStatues(player) - gedoBefore)
                + ", loaded_gedo_statues=" + countLoadedGedoStatues(source.getServer())
                + ", loaded_gedo_delta=" + (countLoadedGedoStatues(source.getServer()) - loadedGedoBefore)
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", loaded_ten_tails=" + BijuManager.countLoadedTenTails(source.getServer())
                + ", loaded_ten_tails_delta=" + (BijuManager.countLoadedTenTails(source.getServer()) - loadedTenTailsBefore)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", chakra_spent=" + (chakraBefore - Chakra.pathway(player).getAmount())), false);
        return triggered ? 1 : 0;
    }

    private static int triggerRinneganDevaChibakuRelease(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireRinneganDevaPath(source, player)) {
            return 0;
        }
        int ballsBefore = countNearbyChibakuTenseiBalls(player);
        double chakraBefore = Chakra.pathway(player).getAmount();
        boolean triggered = RinneganSpecialJutsuHandler.triggerDevaPath(player, false, true);
        int ballsAfter = countNearbyChibakuTenseiBalls(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Deva Chibaku release: triggered=" + triggered
                + ", nearby_chibaku_balls=" + ballsAfter
                + ", delta=" + (ballsAfter - ballsBefore)
                + ", nearby_chibaku_satellites=" + countNearbyChibakuSatellites(player)
                + ", chibaku_tensei_cooldown_ticks=" + getChibakuCooldownTicks(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", chakra_spent=" + (chakraBefore - Chakra.pathway(player).getAmount())), false);
        return triggered ? 1 : 0;
    }

    private static int triggerRinneganDevaMeteorRelease(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireRinneganDevaPath(source, player)) {
            return 0;
        }
        int satellitesBefore = countNearbyChibakuSatellites(player);
        boolean reusableBefore = ProcedureMeteorStrike.hasReusableSatellite(player.serverLevel(), player);
        double chakraBefore = Chakra.pathway(player).getAmount();
        boolean triggered = RinneganSpecialJutsuHandler.triggerDevaPath(player, true, true);
        int satellitesAfter = countNearbyChibakuSatellites(player);
        source.sendSuccess(() -> Component.literal("Rinnegan Deva Meteor release: triggered=" + triggered
                + ", reusable_satellite_before=" + reusableBefore
                + ", nearby_chibaku_satellites=" + satellitesAfter
                + ", delta=" + (satellitesAfter - satellitesBefore)
                + ", nearby_chibaku_balls=" + countNearbyChibakuTenseiBalls(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", chakra_spent=" + (chakraBefore - Chakra.pathway(player).getAmount())), false);
        return triggered ? 1 : 0;
    }

    private static boolean requireRinneganDevaPath(CommandSourceStack source, ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RinneganSpecialJutsuHandler.isRinneganLikeHead(head)) {
            source.sendFailure(Component.literal("Rinnegan/Tenseigan head is required. Run /narutoport m5_jutsu rinnegan_deva_ready."));
            return false;
        }
        int path = RinneganSpecialJutsuHandler.getRinneganPath(head);
        if (path != 0) {
            source.sendFailure(Component.literal("Rinnegan Deva path is not selected: path=" + path
                    + ". Run /narutoport m5_jutsu rinnegan_deva_ready."));
            return false;
        }
        return true;
    }

    private static long getChibakuCooldownTicks(ServerPlayer player) {
        long cooldownUntil = player.getPersistentData().getLong(RinneganSpecialJutsuHandler.CHIBAKU_COOLDOWN_TAG);
        return Math.max(0L, cooldownUntil - player.serverLevel().getGameTime());
    }

    private static int prepareKageBunshinItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        KageBunshinEntity.removeAllFor(player);
        getOrGiveItem(player, ModItems.SCROLL_KAGE_BUNSHIN.get());
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.KAGE_BUNSHIN);
        source.sendSuccess(() -> Component.literal("Shadow Clone item test ready: scroll_kage_bunshin+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.KAGE_BUNSHIN)
                + ", nearby_kage_bunshin=" + player.serverLevel()
                        .getEntitiesOfClass(KageBunshinEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareReplacementItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_BODY_REPLACEMENT.get());
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.REPLACEMENT);
        NinjutsuItem.setReplacementActive(stack, false, player.level().getGameTime());
        source.sendSuccess(() -> Component.literal("Replacement item test ready: scroll_body_replacement+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.REPLACEMENT)
                + ", active=" + NinjutsuItem.isReplacementActive(stack)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHidingCamouflageItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_HIDING_IN_CAMOUFLAGE.get());
        ItemStack stack = prepareNinjutsuJutsuStack(player, NinjutsuItem.INVISIBILITY);
        NinjutsuItem.setHidingInCamouflageActive(stack, false);
        source.sendSuccess(() -> Component.literal("Hiding in Camouflage item test ready: scroll_hiding_in_camouflage+ninjutsu prepared, "
                + describeJutsuDefinitionState(stack, NinjutsuItem.INVISIBILITY)
                + ", active=" + NinjutsuItem.isHidingInCamouflageActive(stack)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHealingItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_HEALING.get());
        ItemStack stack = prepareIryoJutsuStack(player, IryoJutsuItem.HEALING);
        source.sendSuccess(() -> Component.literal("Healing item test ready: scroll_healing+iryo_jutsu prepared, "
                + describeJutsuDefinitionState(stack, IryoJutsuItem.HEALING)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int preparePoisonMistItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_POISON_MIST.get());
        ItemStack stack = prepareIryoJutsuStack(player, IryoJutsuItem.POISON_MIST);
        int targets = spawnPoisonMistTargetArmorStand(player);
        source.sendSuccess(() -> Component.literal("Poison Mist item test ready: scroll_poison_mist+iryo_jutsu prepared, "
                + describeJutsuDefinitionState(stack, IryoJutsuItem.POISON_MIST)
                + ", target_armor_stands=" + targets
                + ", nearby_poison_mist=" + player.serverLevel()
                        .getEntitiesOfClass(PoisonMistEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int spawnPoisonMistTargetArmorStand(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        Vec3 point = basis.point(5.0D, 0.0D, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M5 Poison Mist target"));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int prepareCellularActivationItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        CellularActivationEntity.stopFor(player);
        getOrGiveItem(player, ModItems.SCROLL_CELLULAR_ACTIVATION.get());
        ItemStack stack = prepareIryoJutsuStack(player, IryoJutsuItem.CELLULAR_ACTIVATION);
        source.sendSuccess(() -> Component.literal("Cellular Activation item test ready: scroll_cellular_activation+iryo_jutsu prepared, "
                + describeJutsuDefinitionState(stack, IryoJutsuItem.CELLULAR_ACTIVATION)
                + ", active=" + CellularActivationEntity.hasActiveFor(player)
                + ", nearby_cellular_activation=" + player.serverLevel()
                        .getEntitiesOfClass(CellularActivationEntity.class, player.getBoundingBox().inflate(64.0D)).size()
                + ", ninja_level=" + NarutomodModVariables.getNinjaLevel(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareEnhancedStrengthItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_ENHANCED_STRENGTH.get());
        ItemStack stack = prepareIryoJutsuStack(player, IryoJutsuItem.ENHANCED_STRENGTH);
        IryoJutsuItem.setEnhancedStrengthActive(stack, false);
        player.removeEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
        source.sendSuccess(() -> Component.literal("Chakra Enhanced Strength item test ready: scroll_enhanced_strength+iryo_jutsu prepared, "
                + describeJutsuDefinitionState(stack, IryoJutsuItem.ENHANCED_STRENGTH)
                + ", active=" + IryoJutsuItem.isEnhancedStrengthActive(stack)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareGenjutsuItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_GENJUTSU.get());
        ItemStack stack = prepareIntonJutsuStack(player, IntonItem.GENJUTSU);
        int targets = spawnIntonTargetArmorStand(player, "M5 Genjutsu target");
        source.sendSuccess(() -> Component.literal("Genjutsu item test ready: scroll_genjutsu+inton prepared, "
                + describeJutsuDefinitionState(stack, IntonItem.GENJUTSU)
                + ", target_armor_stands=" + targets
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareMindTransferItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        MindTransferEntity.stopFor(player);
        getOrGiveItem(player, ModItems.SCROLL_MIND_TRANSFER.get());
        ItemStack stack = prepareIntonJutsuStack(player, IntonItem.MIND_TRANSFER);
        int targets = spawnIntonTargetArmorStand(player, "M5 Mind Transfer target");
        source.sendSuccess(() -> Component.literal("Mind Transfer item test ready: scroll_mind_transfer+inton prepared, "
                + describeJutsuDefinitionState(stack, IntonItem.MIND_TRANSFER)
                + ", target_armor_stands=" + targets
                + ", active=" + MindTransferEntity.hasActiveFor(player)
                + ", nearby_mind_transfer=" + player.serverLevel()
                        .getEntitiesOfClass(MindTransferEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", nearby_mind_transfer_self=" + player.serverLevel()
                        .getEntitiesOfClass(MindTransferSelfEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareShadowImitationItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        ShadowImitationEntity.stopOwnedNear(player, 128.0D);
        getOrGiveItem(player, ModItems.SCROLL_SHADOW_IMITATION.get());
        ItemStack stack = prepareIntonJutsuStack(player, IntonItem.SHADOW_IMITATION);
        int targets = spawnIntonTargetArmorStand(player, "M5 Shadow Imitation target");
        source.sendSuccess(() -> Component.literal("Shadow Imitation item test ready: scroll_shadow_imitation+inton prepared, "
                + describeJutsuDefinitionState(stack, IntonItem.SHADOW_IMITATION)
                + ", target_armor_stands=" + targets
                + ", active=" + ShadowImitationEntity.hasAnyActiveFor(player)
                + ", nearby_shadow_imitation=" + player.serverLevel()
                        .getEntitiesOfClass(ShadowImitationEntity.class, player.getBoundingBox().inflate(96.0D)).size()
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int spawnIntonTargetArmorStand(ServerPlayer player, String name) {
        DirectionBasis basis = DirectionBasis.from(player);
        ArmorStand target = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (target == null) {
            return 0;
        }
        Vec3 point = basis.point(6.0D, 0.0D, 0.0D);
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal(name));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        player.serverLevel().addFreshEntity(target);
        return 1;
    }

    private static int prepareChidoriItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_CHIDORI.get());
        ItemStack stack = getOrGiveItem(player, ModItems.RAITON.get());
        if (stack.getItem() instanceof RaitonItem raitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, RaitonItem.CHIDORI.index());
            raitonItem.enableJutsu(stack, RaitonItem.CHIDORI, true);
            raitonItem.setJutsuXp(stack, RaitonItem.CHIDORI, raitonItem.getRequiredXp(stack, RaitonItem.CHIDORI));
        }
        source.sendSuccess(() -> Component.literal("Chidori item test ready: scroll_chidori+raiton prepared, "
                + describeJutsuDefinitionState(stack, RaitonItem.CHIDORI)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareChakraModeItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_LIGHTNING_CHAKRA_MODE.get());
        ItemStack stack = getOrGiveItem(player, ModItems.RAITON.get());
        if (stack.getItem() instanceof RaitonItem raitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, RaitonItem.CHAKRA_MODE.index());
            raitonItem.enableJutsu(stack, RaitonItem.CHAKRA_MODE, true);
            raitonItem.setJutsuXp(stack, RaitonItem.CHAKRA_MODE, raitonItem.getRequiredXp(stack, RaitonItem.CHAKRA_MODE));
        }
        source.sendSuccess(() -> Component.literal("Lightning Chakra Mode item test ready: scroll_lightning_chakra_mode+raiton prepared, "
                + describeJutsuDefinitionState(stack, RaitonItem.CHAKRA_MODE)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareLightningBeastItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_LIGHTNING_BEAST.get());
        ItemStack stack = getOrGiveItem(player, ModItems.RAITON.get());
        if (stack.getItem() instanceof RaitonItem raitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, RaitonItem.CHASING_DOG.index());
            raitonItem.enableJutsu(stack, RaitonItem.CHASING_DOG, true);
            raitonItem.setJutsuXp(stack, RaitonItem.CHASING_DOG, raitonItem.getRequiredXp(stack, RaitonItem.CHASING_DOG));
        }
        source.sendSuccess(() -> Component.literal("Lightning Beast item test ready: scroll_lightning_beast+raiton prepared, "
                + describeJutsuDefinitionState(stack, RaitonItem.CHASING_DOG)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFalseDarknessItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_FALSE_DARKNESS.get());
        ItemStack stack = getOrGiveItem(player, ModItems.RAITON.get());
        if (stack.getItem() instanceof RaitonItem raitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, RaitonItem.FALSE_DARKNESS.index());
            raitonItem.enableJutsu(stack, RaitonItem.FALSE_DARKNESS, true);
            raitonItem.setJutsuXp(stack, RaitonItem.FALSE_DARKNESS, raitonItem.getRequiredXp(stack, RaitonItem.FALSE_DARKNESS));
        }
        source.sendSuccess(() -> Component.literal("False Darkness item test ready: scroll_false_darkness+raiton prepared, "
                + describeJutsuDefinitionState(stack, RaitonItem.FALSE_DARKNESS)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareKirinItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 10000.0D);
        ItemStack stack = getOrGiveItem(player, ModItems.RAITON.get());
        if (stack.getItem() instanceof RaitonItem raitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, RaitonItem.KIRIN.index());
            raitonItem.enableJutsu(stack, RaitonItem.KIRIN, true);
            raitonItem.setJutsuXp(stack, RaitonItem.KIRIN, raitonItem.getRequiredXp(stack, RaitonItem.KIRIN));
        }
        source.sendSuccess(() -> Component.literal("Kirin item test ready: raiton prepared, "
                + describeJutsuDefinitionState(stack, RaitonItem.KIRIN)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareGreatFireballItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_GREAT_FIREBALL.get());
        ItemStack stack = prepareKatonJutsuStack(player, KatonItem.GREAT_FIREBALL);
        source.sendSuccess(() -> Component.literal("Great Fireball item test ready: scroll_great_fireball+katon prepared, "
                + describeJutsuDefinitionState(stack, KatonItem.GREAT_FIREBALL)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFireAnnihilationItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_FIRE_ANNIHILATION.get());
        ItemStack stack = prepareKatonJutsuStack(player, KatonItem.FIRE_ANNIHILATION);
        source.sendSuccess(() -> Component.literal("Fire Annihilation item test ready: scroll_fire_annihilation+katon prepared, "
                + describeJutsuDefinitionState(stack, KatonItem.FIRE_ANNIHILATION)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHidingInAshItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_HIDING_IN_ASH.get());
        ItemStack stack = prepareKatonJutsuStack(player, KatonItem.HIDING_IN_ASH);
        source.sendSuccess(() -> Component.literal("Hiding in Ash item test ready: scroll_hiding_in_ash+katon prepared, "
                + describeJutsuDefinitionState(stack, KatonItem.HIDING_IN_ASH)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareGreatFlameItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_FIRE_STREAM.get());
        ItemStack stack = prepareKatonJutsuStack(player, KatonItem.GREAT_FLAME);
        source.sendSuccess(() -> Component.literal("Great Flame item test ready: scroll_fire_stream+katon prepared, "
                + describeJutsuDefinitionState(stack, KatonItem.GREAT_FLAME)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHidingInRockItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_HIDING_IN_ROCK.get());
        ItemStack stack = prepareDotonJutsuStack(player, DotonItem.HIDING_IN_ROCK);
        source.sendSuccess(() -> Component.literal("Hiding in Rock item test ready: scroll_hiding_in_rock+doton prepared, "
                + describeJutsuDefinitionState(stack, DotonItem.HIDING_IN_ROCK)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareEarthSpearsItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_EARTH_SPEARS.get());
        ItemStack stack = prepareDotonJutsuStack(player, DotonItem.EARTH_SPEARS);
        source.sendSuccess(() -> Component.literal("Earth Spears item test ready: scroll_earth_spears+doton prepared, "
                + describeJutsuDefinitionState(stack, DotonItem.EARTH_SPEARS)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareEarthWallItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_EARTH_WALL.get());
        ItemStack stack = prepareDotonJutsuStack(player, DotonItem.EARTH_WALL);
        source.sendSuccess(() -> Component.literal("Earth Wall item test ready: scroll_earth_wall+doton prepared, "
                + describeJutsuDefinitionState(stack, DotonItem.EARTH_WALL)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSwampPitItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_SWAMP_PIT.get());
        ItemStack stack = prepareDotonJutsuStack(player, DotonItem.SWAMP_PIT);
        source.sendSuccess(() -> Component.literal("Swamp Pit item test ready: scroll_swamp_pit+doton prepared, "
                + describeJutsuDefinitionState(stack, DotonItem.SWAMP_PIT)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareEarthSandwichItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_EARTH_SANDWICH.get());
        ItemStack stack = prepareDotonJutsuStack(player, DotonItem.EARTH_SANDWICH);
        source.sendSuccess(() -> Component.literal("Earth Sandwich item test ready: scroll_earth_sandwich+doton prepared, "
                + describeJutsuDefinitionState(stack, DotonItem.EARTH_SANDWICH)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareHidingInMistItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        if (player.experienceLevel < 20) {
            player.giveExperienceLevels(20 - player.experienceLevel);
        }
        getOrGiveItem(player, ModItems.SCROLL_HIDING_IN_MIST.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.HIDING_IN_MIST);
        source.sendSuccess(() -> Component.literal("Hiding in Mist item test ready: scroll_hiding_in_mist+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.HIDING_IN_MIST)
                + ", xp_level=" + player.experienceLevel
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareWaterStreamItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_WATER_STREAM.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.WATER_BULLET);
        source.sendSuccess(() -> Component.literal("Water Bullet item test ready: scroll_water_stream+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.WATER_BULLET)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareWaterDragonItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_WATER_DRAGON.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.WATER_DRAGON);
        source.sendSuccess(() -> Component.literal("Water Dragon item test ready: scroll_water_dragon+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.WATER_DRAGON)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareWaterPrisonItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_WATER_PRISON.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.WATER_PRISON);
        source.sendSuccess(() -> Component.literal("Water Prison item test ready: scroll_water_prison+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.WATER_PRISON)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareWaterSharkItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_WATER_SHARK.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.WATER_SHARK);
        source.sendSuccess(() -> Component.literal("Water Shark item test ready: scroll_water_shark+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.WATER_SHARK)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareWaterShockwaveItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_WATER_SHOCKWAVE.get());
        ItemStack stack = prepareSuitonJutsuStack(player, SuitonItem.WATER_SHOCKWAVE);
        source.sendSuccess(() -> Component.literal("Water Shockwave item test ready: scroll_water_shockwave+suiton prepared, "
                + describeJutsuDefinitionState(stack, SuitonItem.WATER_SHOCKWAVE)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFutonVacuumItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_FUTON_VACUUM.get());
        ItemStack stack = prepareFutonJutsuStack(player, FutonItem.VACUUM);
        source.sendSuccess(() -> Component.literal("Futon Vacuum item test ready: scroll_futon_vacuum+futon prepared, "
                + describeJutsuDefinitionState(stack, FutonItem.VACUUM)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareFutonChakraFlowItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_FUTON_CHAKRA_FLOW.get());
        ItemStack stack = prepareFutonJutsuStack(player, FutonItem.CHAKRA_FLOW);
        source.sendSuccess(() -> Component.literal("Futon Chakra Flow item test ready: scroll_futon_chakra_flow+futon prepared, "
                + describeJutsuDefinitionState(stack, FutonItem.CHAKRA_FLOW)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareRasenshurikenItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        getOrGiveItem(player, ModItems.SCROLL_RASENSHURIKEN.get());
        ItemStack stack = prepareFutonJutsuStack(player, FutonItem.RASENSHURIKEN);
        source.sendSuccess(() -> Component.literal("Rasenshuriken item test ready: scroll_rasenshuriken+futon prepared, "
                + describeJutsuDefinitionState(stack, FutonItem.RASENSHURIKEN)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSenjutsuRasenshurikenTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 50000.0D);
        getOrGiveItem(player, ModItems.SCROLL_RASENSHURIKEN.get());
        ItemStack futonStack = prepareFutonJutsuStack(player, FutonItem.RASENSHURIKEN);
        ItemStack senjutsuStack = prepareOwnedInactiveSenjutsuStack(player, true);
        if (senjutsuStack.getItem() instanceof SenjutsuItem senjutsuItem) {
            senjutsuStack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, SenjutsuItem.SAGE_RASENSHURIKEN.index());
            senjutsuItem.enableJutsu(senjutsuStack, SenjutsuItem.SAGE_RASENSHURIKEN, true);
            senjutsuItem.setJutsuXp(senjutsuStack, SenjutsuItem.SAGE_RASENSHURIKEN,
                    senjutsuItem.getRequiredXp(senjutsuStack, SenjutsuItem.SAGE_RASENSHURIKEN));
            SenjutsuItem.setSageModeActivated(senjutsuStack, true);
            senjutsuStack.getOrCreateTag().putDouble(SenjutsuItem.SAGE_CHAKRA_DEPLETION_AMOUNT_TAG, 0.0D);
            ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_MODE_ACTIVATED_TAG, true);
            ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_TYPE_TAG, SenjutsuItem.getSageType(senjutsuStack).id());
        }
        source.sendSuccess(() -> Component.literal("Sage Rasenshuriken test ready: futon prerequisite and active senjutsu prepared, "
                + "hold right-click briefly, release to spawn Sage Rasenshuriken. futon="
                + describeJutsuDefinitionState(futonStack, FutonItem.RASENSHURIKEN)
                + ", senjutsu=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.SAGE_RASENSHURIKEN)
                + ", sage_type=" + SenjutsuItem.getSageType(senjutsuStack)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int prepareSenjutsuWoodBuddhaTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 100000.0D);
        ItemStack mokutonStack = getOrGiveItem(player, ModItems.MOKUTON.get());
        prepareAdvancedNatureJutsuStack(player, mokutonStack, 3);
        ItemStack senjutsuStack = prepareOwnedInactiveSenjutsuStack(player, true);
        if (senjutsuStack.getItem() instanceof SenjutsuItem senjutsuItem) {
            senjutsuStack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, SenjutsuItem.WOOD_BUDDHA.index());
            senjutsuItem.enableJutsu(senjutsuStack, SenjutsuItem.WOOD_BUDDHA, true);
            senjutsuItem.setJutsuXp(senjutsuStack, SenjutsuItem.WOOD_BUDDHA,
                    senjutsuItem.getRequiredXp(senjutsuStack, SenjutsuItem.WOOD_BUDDHA));
            SenjutsuItem.setSageModeActivated(senjutsuStack, true);
            senjutsuStack.getOrCreateTag().putDouble(SenjutsuItem.SAGE_CHAKRA_DEPLETION_AMOUNT_TAG, 0.0D);
            ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_MODE_ACTIVATED_TAG, true);
            ProcedureSync.EntityNBTTag.setAndSync(player, SenjutsuItem.SAGE_TYPE_TAG, SenjutsuItem.getSageType(senjutsuStack).id());
        }
        JutsuItem.JutsuDefinition woodGolem = AdvancedNatureJutsuItem.AdvancedNatureKind.MOKUTON.definitionByIndex(3);
        source.sendSuccess(() -> Component.literal("Wood Buddha test ready: active senjutsu and Mokuton Wood Golem prerequisite prepared. "
                + "Right-click Senjutsu Wood Buddha to spawn, right-click again while riding sitting Buddha to stand, "
                + "then run /narutoport m5_jutsu senjutsu_wood_buddha_shoot. mokuton="
                + describeJutsuDefinitionState(mokutonStack, woodGolem)
                + ", senjutsu=" + describeJutsuDefinitionState(senjutsuStack, SenjutsuItem.WOOD_BUDDHA)
                + ", sage_type=" + SenjutsuItem.getSageType(senjutsuStack)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", nearby_buddha_1000=" + countNearbyBuddha1000(player)), false);
        return 1;
    }

    private static int shootSenjutsuWoodBuddhaArms(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.getVehicle() instanceof Buddha1000Entity buddha) || !buddha.isOwnedBy(player)) {
            source.sendFailure(Component.literal("Owned Wood Buddha vehicle is required. Run /narutoport m5_jutsu senjutsu_wood_buddha_ready, then right-click Senjutsu Wood Buddha."));
            return 0;
        }
        int spawned = buddha.shootArms();
        source.sendSuccess(() -> Component.literal("Wood Buddha shoot: spawned_arms=" + spawned
                + ", sitting=" + buddha.isSitting()
                + ", ticks_alive=" + buddha.getTicksAlive()
                + ", chakra_burn=" + String.format("%.2f", buddha.getChakraBurn())
                + ", nearby_buddha_1000=" + countNearbyBuddha1000(player)
                + ", nearby_buddha_arms=" + countNearbyBuddhaArms(player)), false);
        return spawned > 0 ? 1 : 0;
    }

    private static int shootSenjutsuSnake8HeadsLookTarget(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.getVehicle() instanceof Snake8HeadsEntity snake) || !snake.isOwnedBy(player)) {
            source.sendFailure(Component.literal("Owned Snake 8 Heads vehicle is required. Run /narutoport m5_jutsu senjutsu_snake_8_heads_ready, then look near a target."));
            return 0;
        }
        boolean spawned = snake.shootLookTarget(player, 2.0D);
        source.sendSuccess(() -> Component.literal("Snake 8 Heads shoot: spawned_head=" + spawned
                + ", vehicle_ticks=" + snake.getTicksAlive()
                + ", nearby_snake_8_heads=" + countNearbySnake8Heads(player)
                + ", nearby_snake_8_head1=" + countNearbySnake8Head1(player)), false);
        return spawned ? 1 : 0;
    }

    private static int dismissSenjutsuSnake8Heads(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.getVehicle() instanceof Snake8HeadsEntity snake) || !snake.isOwnedBy(player)) {
            source.sendFailure(Component.literal("Owned Snake 8 Heads vehicle is required. Run /narutoport m5_jutsu senjutsu_snake_8_heads_ready first."));
            return 0;
        }
        snake.discard();
        source.sendSuccess(() -> Component.literal("Snake 8 Heads dismissed: nearby_snake_8_heads="
                + countNearbySnake8Heads(player)
                + ", feather_falling=" + describeEffectInstance(player.getEffect(ModEffects.FEATHER_FALLING.get()))), false);
        return 1;
    }

    private static int prepareBigBlowItemTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 5000.0D);
        getOrGiveItem(player, ModItems.SCROLL_BIG_BLOW.get());
        ItemStack stack = prepareFutonJutsuStack(player, FutonItem.BIG_BLOW);
        source.sendSuccess(() -> Component.literal("Great Breakthrough item test ready: scroll_big_blow+futon prepared, "
                + describeJutsuDefinitionState(stack, FutonItem.BIG_BLOW)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static ItemStack prepareKatonJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.KATON.get());
        if (stack.getItem() instanceof KatonItem katonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            katonItem.enableJutsu(stack, definition, true);
            katonItem.setJutsuXp(stack, definition, katonItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareDotonJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.DOTON.get());
        if (stack.getItem() instanceof DotonItem dotonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            dotonItem.enableJutsu(stack, definition, true);
            dotonItem.setJutsuXp(stack, definition, dotonItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareFutonJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.FUTON.get());
        if (stack.getItem() instanceof FutonItem futonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            futonItem.enableJutsu(stack, definition, true);
            futonItem.setJutsuXp(stack, definition, futonItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareSuitonJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.SUITON.get());
        if (stack.getItem() instanceof SuitonItem suitonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            suitonItem.enableJutsu(stack, definition, true);
            suitonItem.setJutsuXp(stack, definition, suitonItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareIryoJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.IRYO_JUTSU.get());
        if (stack.getItem() instanceof IryoJutsuItem iryoJutsuItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            iryoJutsuItem.enableJutsu(stack, definition, true);
            iryoJutsuItem.setJutsuXp(stack, definition, iryoJutsuItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareIntonJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.INTON.get());
        if (stack.getItem() instanceof IntonItem intonItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            intonItem.enableJutsu(stack, definition, true);
            intonItem.setJutsuXp(stack, definition, intonItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareNinjutsuJutsuStack(ServerPlayer player, JutsuItem.JutsuDefinition definition) {
        ItemStack stack = getOrGiveItem(player, ModItems.NINJUTSU.get());
        if (stack.getItem() instanceof NinjutsuItem ninjutsuItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, definition.index());
            ninjutsuItem.enableJutsu(stack, definition, true);
            ninjutsuItem.setJutsuXp(stack, definition, ninjutsuItem.getRequiredXp(stack, definition));
        }
        return stack;
    }

    private static ItemStack prepareOwnedLearnedRasenganStack(ServerPlayer player) {
        ItemStack stack = getOrGiveItem(player, ModItems.NINJUTSU.get());
        NinjutsuItem.setOwner(stack, player);
        NinjutsuItem.setRasenganLearned(stack, true);
        return stack;
    }

    private static ItemStack prepareOwnedInactiveSenjutsuStack(ServerPlayer player, boolean deterministicSageType) {
        ItemStack stack = getOrGiveItem(player, ModItems.SENJUTSU.get());
        NinjutsuItem.setOwner(stack, player);
        SenjutsuItem.setSageModeActivated(stack, false);
        if (deterministicSageType && SenjutsuItem.getSageType(stack) == SenjutsuItem.SageType.NONE) {
            SenjutsuItem.setSageType(stack, SenjutsuItem.SageType.TOAD);
        }
        return stack;
    }

    private static ItemStack prepareSenjutsuSnake8HeadsStack(ServerPlayer player) {
        ItemStack stack = getOrGiveItem(player, ModItems.SENJUTSU.get());
        if (stack.getItem() instanceof SenjutsuItem senjutsuItem) {
            JutsuItem.setOwner(stack, player);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, SenjutsuItem.SNAKE_8_HEADS.index());
            senjutsuItem.enableJutsu(stack, SenjutsuItem.SNAKE_8_HEADS, true);
            senjutsuItem.setJutsuXp(stack, SenjutsuItem.SNAKE_8_HEADS, senjutsuItem.getRequiredXp(stack, SenjutsuItem.SNAKE_8_HEADS));
            SenjutsuItem.setSageType(stack, SenjutsuItem.SageType.SNAKE);
            SenjutsuItem.setSageModeActivated(stack, true);
            stack.getOrCreateTag().putDouble(SenjutsuItem.SAGE_CHAKRA_DEPLETION_AMOUNT_TAG, 0.0D);
        }
        return stack;
    }

    private static void prepareJutsuResourcePool(ServerPlayer player, double minimumBattleExperience) {
        NarutomodModVariables.setBattleExperience(player, Math.max(NarutomodModVariables.getBattleExperience(player), minimumBattleExperience));
        NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, Chakra.pathway(player).getMax());
        NarutomodModVariables.sync(player);
    }

    private static ItemStack getOrGiveItem(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(item)) {
                return candidate;
            }
        }

        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return stack;
    }

    private static ItemStack ensureInventoryCount(ServerPlayer player, Item item, int count) {
        ItemStack stack = getOrGiveItem(player, item);
        int targetCount = Math.min(count, stack.getMaxStackSize());
        if (stack.getCount() < targetCount) {
            stack.setCount(targetCount);
        }
        return stack;
    }

    private static ItemStack findItem(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(item)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String describeNinjutsuState(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        return "learned=" + NinjutsuItem.hasLearnedRasengan(stack)
                + "/owned=" + NinjutsuItem.isOwnedByOrUnbound(player, stack)
                + "/rasengan=" + describeJutsuDefinitionState(stack, NinjutsuItem.RASENGAN);
    }

    private static String describeSenjutsuState(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "missing";
        }
        return "active=" + SenjutsuItem.isSageModeActivated(stack)
                + "/owned=" + NinjutsuItem.isOwnedByOrUnbound(player, stack)
                + "/type=" + SenjutsuItem.getSageType(stack)
                + "/sage_rasengan=" + describeJutsuDefinitionState(stack, SenjutsuItem.SAGE_RASENGAN)
                + "/sage_rasenshuriken=" + describeJutsuDefinitionState(stack, SenjutsuItem.SAGE_RASENSHURIKEN)
                + "/wood_buddha=" + describeJutsuDefinitionState(stack, SenjutsuItem.WOOD_BUDDHA)
                + "/snake_8_heads=" + describeJutsuDefinitionState(stack, SenjutsuItem.SNAKE_8_HEADS);
    }

    private static String describeWoodBuddhaVehicle(ServerPlayer player) {
        if (!(player.getVehicle() instanceof Buddha1000Entity buddha)) {
            return "none";
        }
        return "owned=" + buddha.isOwnedBy(player)
                + "/sitting=" + buddha.isSitting()
                + "/ticks=" + buddha.getTicksAlive()
                + "/chakra_burn=" + String.format("%.2f", buddha.getChakraBurn());
    }

    private static String describeSnake8HeadsVehicle(ServerPlayer player) {
        if (!(player.getVehicle() instanceof Snake8HeadsEntity snake)) {
            return "none";
        }
        return "owned=" + snake.isOwnedBy(player)
                + "/ticks=" + snake.getTicksAlive();
    }

    private static String describeJutsuDefinitionState(ItemStack stack, JutsuItem.JutsuDefinition definition) {
        if (stack.isEmpty()) {
            return "missing";
        }
        if (!(stack.getItem() instanceof JutsuItem jutsuItem)) {
            return "not_jutsu_item";
        }
        return definition.translationKey()
                + "{enabled=" + jutsuItem.isJutsuEnabled(stack, definition)
                + ",xp=" + jutsuItem.getJutsuXp(stack, definition)
                + "/" + jutsuItem.getRequiredXp(stack, definition)
                + ",cooldown=" + jutsuItem.getJutsuCooldown(stack, definition)
                + "}";
    }

    private static int spawnM4RasenganTargets(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        int count = spawnM4TargetArmorStand(level, basis.point(7.0D, 0.0D, 0.0D), player);
        count += placeM4BreakableBlocks(level, basis);
        return count;
    }

    private static int spawnM4TargetArmorStand(ServerLevel level, Vec3 point, ServerPlayer player) {
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        if (target == null) {
            return 0;
        }
        target.moveTo(point.x(), player.getY(), point.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal("M4 Rasengan damage target"));
        target.setCustomNameVisible(true);
        target.setNoGravity(true);
        level.addFreshEntity(target);
        return 1;
    }

    private static int placeM4BreakableBlocks(ServerLevel level, DirectionBasis basis) {
        int count = 0;
        for (int y = 0; y < 2; y++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos pos = BlockPos.containing(basis.point(4.5D, side * 0.8D, y));
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                count++;
            }
        }
        return count;
    }

    private static int spawnM3ModelGrid(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        int count = 0;
        count += spawnM3Entity(level, ModRegistries.PORTING_DUMMY.get(), basis.point(7.0D, -4.5D, 0.0D), player);
        count += spawnM3Entity(level, ModEntityTypes.KANKURO.get(), basis.point(7.0D, -1.5D, 0.0D), player);
        count += spawnM3Rasengan(level, basis.point(7.0D, 1.5D, 1.2D), player);
        count += spawnM3Entity(level, ModEntityTypes.BUDDHA_1000.get(), basis.point(28.0D, 9.0D, 0.0D), player);
        return count;
    }

    private static int spawnM3ParticleGrid(ServerPlayer player) {
        DirectionBasis basis = DirectionBasis.from(player);
        ServerLevel level = player.serverLevel();
        NarutoParticleKind[] kinds = NarutoParticleKind.values();
        for (int i = 0; i < kinds.length; i++) {
            int row = i / 4;
            int column = i % 4;
            Vec3 point = basis.point(12.0D + row * 3.0D, (column - 1.5D) * 3.0D, 1.25D);
            NarutoParticleKind kind = kinds[i];
            level.sendParticles(
                    ModParticleTypes.options(kind, defaultParticleArgs(kind, player)),
                    point.x(),
                    point.y(),
                    point.z(),
                    smokeParticleCount(kind),
                    0.45D,
                    0.45D,
                    0.45D,
                    0.02D
            );
        }
        spawnM3ParticleFeedbackTargets(level, basis, player);
        return kinds.length;
    }

    private static void spawnM3ParticleFeedbackTargets(ServerLevel level, DirectionBasis basis, ServerPlayer player) {
        spawnM3ArmorStand(level, basis.pointForParticle(NarutoParticleKind.BURNING_ASH), player, "M3 burning_ash target");
        Vec3 acidPoint = basis.pointForParticle(NarutoParticleKind.ACID_SPIT);
        spawnM3ArmorStand(level, acidPoint, player, "M3 acid_spit target");
        level.setBlock(BlockPos.containing(acidPoint), Blocks.STONE.defaultBlockState(), 3);
    }

    private static void spawnM3ArmorStand(ServerLevel level, Vec3 particlePoint, ServerPlayer player, String label) {
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        if (target == null) {
            return;
        }
        target.moveTo(particlePoint.x(), player.getY(), particlePoint.z(), player.getYRot() + 180.0F, 0.0F);
        target.setCustomName(Component.literal(label));
        target.setCustomNameVisible(true);
        target.setInvulnerable(true);
        target.setNoGravity(true);
        level.addFreshEntity(target);
    }

    private static <T extends Entity> int spawnM3Entity(ServerLevel level, EntityType<T> entityType, Vec3 point, ServerPlayer player) {
        T entity = entityType.create(level);
        if (entity == null) {
            return 0;
        }
        entity.moveTo(point.x(), point.y(), point.z(), player.getYRot() + 180.0F, 0.0F);
        level.addFreshEntity(entity);
        return 1;
    }

    private static int spawnM3Rasengan(ServerLevel level, Vec3 point, ServerPlayer player) {
        RasenganEntity entity = ModEntityTypes.RASENGAN.get().create(level);
        if (entity == null) {
            return 0;
        }
        entity.configureStationary(1.0F);
        entity.moveTo(point.x(), point.y(), point.z(), player.getYRot() + 180.0F, 0.0F);
        level.addFreshEntity(entity);
        return 1;
    }

    private static int smokeParticleCount(NarutoParticleKind kind) {
        return switch (kind) {
            case EXPANDING_SPHERE, MOB_APPEARANCE, SEAL_FORMULA, WHIRLPOOL -> 1;
            case BURNING_ASH, ACID_SPIT, HOMING_ORB, PORTAL_SPIRAL -> 12;
            default -> 30;
        };
    }

    private static int getVariables(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double battleExperience = NarutomodModVariables.getBattleExperience(player);
        source.sendSuccess(() -> Component.literal("battle_experience=" + battleExperience
                + ", ninja_level=" + NarutomodModVariables.getNinjaLevel(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int setBattleExperience(CommandSourceStack source, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NarutomodModVariables.setBattleExperience(player, value);
        source.sendSuccess(() -> Component.literal("Set battle_experience=" + NarutomodModVariables.getBattleExperience(player)), false);
        return 1;
    }

    private static int addBattleExperience(CommandSourceStack source, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NarutomodModVariables.addBattleExperience(player, value);
        source.sendSuccess(() -> Component.literal("Set battle_experience=" + NarutomodModVariables.getBattleExperience(player)), false);
        return 1;
    }

    private static int getChakra(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Chakra.Pathway pathway = Chakra.pathway(player);
        source.sendSuccess(() -> Component.literal("chakra=" + pathway.getAmount() + "/" + pathway.getMax()), false);
        return 1;
    }

    private static int setChakra(CommandSourceStack source, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, value);
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Set chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int consumeChakra(CommandSourceStack source, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean changed = Chakra.pathway(player).consume(value);
        source.sendSuccess(() -> Component.literal("consume result=" + changed
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return changed ? 1 : 0;
    }

    private static int fillChakra(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Chakra.Pathway pathway = Chakra.pathway(player);
        NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, pathway.getMax());
        NarutomodModVariables.sync(player);
        source.sendSuccess(() -> Component.literal("Filled chakra=" + Chakra.pathway(player).getAmount() + "/" + Chakra.pathway(player).getMax()), false);
        return 1;
    }

    private static int loadSavedData(CommandSourceStack source) {
        NarutomodSavedData.loadAll(source.getServer());
        int restored = BijuManager.restoreSpawnedTailedBeasts(source.getServer());
        source.sendSuccess(() -> Component.literal("Loaded Narutomod SavedData scaffolds; restored_tailed_beasts=" + restored), false);
        return 1;
    }

    private static int savedDataSummary(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int tailedBeastCount = NarutomodSavedData.tailedBeasts(source.getServer()).size();
        int mapKeys = NarutomodSavedData.mapVariables(source.getServer()).data().size();
        int worldKeys = NarutomodSavedData.worldVariables(level).data().size();
        int specialKeys = NarutomodSavedData.specialEvents(source.getServer()).data().size();
        int specialPending = SpecialEvent.pendingCount(source.getServer());
        source.sendSuccess(() -> Component.literal("saveddata map=" + mapKeys
                + ", world=" + worldKeys
                + ", specialevents=" + specialKeys
                + ", specialevent_pending=" + specialPending
                + ", tailed_beast_files=" + tailedBeastCount
                + ", assigned_jinchuriki=" + BijuManager.assignedJinchurikiCount(source.getServer())
                + ", available_biju=" + BijuManager.availableTailedBeasts(source.getServer())
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())), false);
        return 1;
    }

    private static int listSavedTailedBeasts(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(String.join("; ", BijuManager.listSpawnedTailedBeasts(source.getServer()))), false);
        return 1;
    }

    private static int listGedoSealedTails(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", list=" + BijuManager.listGedoSealedTails(source.getServer())), false);
        return 1;
    }

    private static int clearGedoSealedTails(CommandSourceStack source) {
        int cleared = BijuManager.clearGedoSealedTails(source.getServer());
        source.sendSuccess(() -> Component.literal("gedo_sealed_cleared=" + cleared
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())), false);
        return cleared;
    }

    private static int setGedoSealedTail(CommandSourceStack source, int tails, boolean sealed) {
        boolean changed = BijuManager.setGedoSealedTail(source.getServer(), tails, sealed);
        source.sendSuccess(() -> Component.literal("gedo_sealed_changed=" + changed
                + ", tails=" + tails
                + ", sealed=" + BijuManager.isGedoSealedTail(source.getServer(), tails)
                + ", gedo_sealed_tails=" + BijuManager.countGedoSealedTails(source.getServer())
                + ", list=" + BijuManager.listGedoSealedTails(source.getServer())), false);
        return changed ? 1 : 0;
    }

    private static int saveNearbyTailedBeasts(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<TailedBeastEntity> beasts = player.serverLevel().getEntitiesOfClass(
                TailedBeastEntity.class,
                player.getBoundingBox().inflate(256.0D),
                entity -> entity.isAlive());
        int saved = 0;
        for (TailedBeastEntity beast : beasts) {
            if (BijuManager.saveSpawnedTailedBeast(beast)) {
                saved++;
            }
        }
        List<TenTailsEntity> tenTails = player.serverLevel().getEntitiesOfClass(
                TenTailsEntity.class,
                player.getBoundingBox().inflate(256.0D),
                entity -> entity.isAlive());
        for (TenTailsEntity beast : tenTails) {
            if (BijuManager.saveSpawnedTenTails(beast)) {
                saved++;
            }
        }
        int savedCount = saved;
        int nearbyCount = beasts.size() + tenTails.size();
        source.sendSuccess(() -> Component.literal("saved_nearby_tailed_beasts=" + savedCount
                + ", nearby_tailed_beasts=" + nearbyCount
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())), false);
        return savedCount;
    }

    private static int restoreSavedTailedBeasts(CommandSourceStack source) {
        int restored = BijuManager.restoreSpawnedTailedBeasts(source.getServer());
        source.sendSuccess(() -> Component.literal("restored_tailed_beasts=" + restored
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())), false);
        return restored;
    }

    private static int clearSavedTailedBeast(CommandSourceStack source, int tails) {
        boolean cleared = BijuManager.clearSavedSpawnedTailedBeast(source.getServer(), tails);
        source.sendSuccess(() -> Component.literal("cleared_saved_tailed_beast=" + cleared
                + ", tails=" + tails
                + ", beast=" + BijuManager.displayName(tails)
                + ", saved_spawned_tailed_beasts=" + BijuManager.savedSpawnedTailedBeastCount(source.getServer())), false);
        return cleared ? 1 : 0;
    }

    private static int listJinchurikiAssignments(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(String.join("; ", BijuManager.listAssignments(source.getServer()))), false);
        return 1;
    }

    private static int setCurrentPlayerAsJinchuriki(CommandSourceStack source, int tails) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean assigned = BijuManager.setPlayerAsJinchuriki(player, tails);
        source.sendSuccess(() -> Component.literal("jinchuriki_set=" + assigned
                + ", player=" + player.getScoreboardName()
                + ", tails=" + tails
                + ", beast=" + BijuManager.displayName(tails)
                + ", side_effect_items=" + describeBijuSideEffectItems(player)
                + ", synced_tails=" + NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS)), false);
        return assigned ? 1 : 0;
    }

    private static int prepareFuuinSealingTest(CommandSourceStack source, int tails) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        prepareJutsuResourcePool(player, 50000.0D);
        BijuManager.revokePlayer(player);
        BijuManager.revokeByTail(source.getServer(), tails);
        BlockPos center = placeSealingTargetPlatform(player);
        SealingEntity seal = SealingEntity.spawnAt(level, center, player);
        Vec3 targetPos = new Vec3(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        LivingEntity target;
        if (tails == BijuManager.MAX_TAILS) {
            TenTailsEntity beast = TenTailsEntity.spawnAt(level, targetPos, player.getYRot() + 180.0F, player);
            if (beast != null) {
                beast.setAngerLevel(0);
            }
            target = beast;
        } else {
            TailedBeastEntity beast = TailedBeastEntity.spawnAt(
                    level,
                    TailedBeastEntity.Variant.byTailCount(tails),
                    targetPos,
                    player.getYRot() + 180.0F,
                    player);
            if (beast != null) {
                beast.setAngerLevel(0);
            }
            target = beast;
        }
        if (seal != null) {
            player.startRiding(seal, true);
        }
        source.sendSuccess(() -> Component.literal("Fuuin sealing test ready: seal_spawned=" + (seal != null)
                + ", target_spawned=" + (target != null)
                + ", tails=" + tails
                + ", beast=" + BijuManager.displayName(tails)
                + ", player_riding_seal=" + player.isPassenger()
                + ", center=" + center.getX() + "," + center.getY() + "," + center.getZ()
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ". Stay seated until the fuuin progress reaches 400/400, then check /narutoport saveddata jinchuriki list."), false);
        return seal != null && target != null ? 1 : 0;
    }

    private static int prepareBijuCloakTest(CommandSourceStack source, int tails) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 12000.0D);
        BijuManager.setPlayerAsJinchuriki(player, tails);
        BijuManager.setCloakXp(player, 1, 400);
        BijuManager.toggleBijuCloak(player);
        int cloneCount = countNearbyJinchurikiClones(player);
        boolean fullSet = BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.HEAD))
                && BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.CHEST))
                && BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.LEGS));
        source.sendSuccess(() -> Component.literal("Biju cloak test ready: player=" + player.getScoreboardName()
                + ", tails=" + tails
                + ", beast=" + BijuManager.displayName(tails)
                + ", cloak_level=" + BijuManager.getCloakLevel(player)
                + ", cloak_xp=" + BijuManager.getCurrentCloakXp(player)
                + ", full_set=" + fullSet
                + ", nearby_jinchuriki_clones=" + cloneCount
                + ", side_effect_items=" + describeBijuSideEffectItems(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return fullSet ? 1 : 0;
    }

    private static int toggleCurrentPlayerBijuCloak(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean toggled = BijuManager.toggleBijuCloak(player);
        source.sendSuccess(() -> Component.literal("biju_cloak_toggled=" + toggled
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", cloak_level=" + BijuManager.getCloakLevel(player)
                + ", wearing_ticks=" + BijuCloakItem.getWearingTicks(player)
                + ", cloak_xp=" + BijuManager.getCurrentCloakXp(player)), false);
        return toggled ? 1 : 0;
    }

    private static int increaseCurrentPlayerBijuCloak(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int level = BijuManager.increaseCloakLevel(player);
        source.sendSuccess(() -> Component.literal("biju_cloak_level=" + level
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", cloak_xp=" + BijuManager.getCurrentCloakXp(player)
                + ", nearby_tailed_beasts=" + countNearbyTailedBeasts(player)
                + ", nearby_ten_tails=" + countNearbyTenTails(player)
                + ", loaded_ten_tails=" + BijuManager.countLoadedTenTails(source.getServer())), false);
        return level;
    }

    private static int preparePowerIncreaseBijuCloakTest(CommandSourceStack source, int tails)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 12000.0D);
        BijuManager.revokePlayer(player);
        BijuManager.revokeByTail(source.getServer(), tails);
        BijuManager.setPlayerAsJinchuriki(player, tails);
        BijuManager.setCloakXp(player, 1, 4000);
        boolean toggled = BijuManager.toggleBijuCloak(player);
        source.sendSuccess(() -> Component.literal("PowerIncrease Biju cloak ready: toggled=" + toggled
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", cloak_level=" + BijuManager.getCloakLevel(player)
                + ", cloak_xp=" + BijuManager.getCurrentCloakXp(player)
                + ", full_set=" + PowerIncreaseKeyHandler.isWearingBijuCloakFullSet(player)
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()
                + ", use /narutoport saveddata jinchuriki powerincrease_cloak or press Up to increase."), false);
        return toggled ? 1 : 0;
    }

    private static int powerIncreaseCurrentPlayerBijuCloak(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int beforeLevel = BijuManager.getCloakLevel(player);
        int beforeXp = BijuManager.getCurrentCloakXp(player);
        boolean beforeFullSet = PowerIncreaseKeyHandler.isWearingBijuCloakFullSet(player);
        boolean handled = PowerIncreaseKeyHandler.handle(player, false);
        source.sendSuccess(() -> Component.literal("PowerIncrease Biju cloak: handled=" + handled
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", before_level=" + beforeLevel
                + ", after_level=" + BijuManager.getCloakLevel(player)
                + ", before_xp=" + beforeXp
                + ", after_xp=" + BijuManager.getCurrentCloakXp(player)
                + ", before_full_set=" + beforeFullSet
                + ", after_full_set=" + PowerIncreaseKeyHandler.isWearingBijuCloakFullSet(player)
                + ", nearby_tailed_beasts=" + countNearbyTailedBeasts(player)
                + ", nearby_ten_tails=" + countNearbyTenTails(player)
                + ", loaded_ten_tails=" + BijuManager.countLoadedTenTails(source.getServer())
                + ", wearing_ticks=" + BijuCloakItem.getWearingTicks(player)), false);
        return handled ? 1 : 0;
    }

    private static int triggerSpecialJutsu2Release(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean handled = BijuManager.handleSpecialJutsuKey(player, 2, false);
        source.sendSuccess(() -> Component.literal("special_jutsu_2_release handled=" + handled
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", cloak_level=" + BijuManager.getCloakLevel(player)
                + ", wearing_ticks=" + BijuCloakItem.getWearingTicks(player)), false);
        return handled ? 1 : 0;
    }

    private static int prepareSpecialJutsu3TailBeastBallTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        prepareJutsuResourcePool(player, 12000.0D);
        BijuManager.setPlayerAsJinchuriki(player, 4);
        BijuManager.toggleBijuCloak(player);
        BijuManager.setCloakXp(player, 1, 4000);
        BijuManager.increaseCloakLevel(player);
        for (int i = 0; i < 25; i++) {
            BijuManager.handleSpecialJutsuKey(player, 3, true);
        }
        boolean released = BijuManager.handleSpecialJutsuKey(player, 3, false);
        int ballCount = countNearbyTailBeastBalls(player);
        source.sendSuccess(() -> Component.literal("special_jutsu_3_tail_beast_ball released=" + released
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", cloak_level=" + BijuManager.getCloakLevel(player)
                + ", nearby_tail_beast_balls=" + ballCount
                + ", chakra=" + Chakra.pathway(player).getAmount()
                + "/" + Chakra.pathway(player).getMax()), false);
        return released ? 1 : 0;
    }

    private static int tryNaturalJinchurikiGrant(CommandSourceStack source, boolean force) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int grantedTail = BijuManager.tryNaturalJinchurikiGrant(player, force);
        source.sendSuccess(() -> Component.literal("natural_jinchuriki_grant force=" + force
                + ", granted_tail=" + grantedTail
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", available_biju=" + BijuManager.availableTailedBeasts(source.getServer())
                + ", synced_tails=" + NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS)
                + ", side_effect_items=" + describeBijuSideEffectItems(player)
                + ", battle_xp=" + NarutomodModVariables.getBattleExperience(player)
                + ", ninja_level=" + NarutomodModVariables.getNinjaLevel(player)), false);
        return grantedTail > 0 ? 1 : 0;
    }

    private static int revokeCurrentPlayerJinchuriki(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean revoked = BijuManager.revokePlayer(player);
        source.sendSuccess(() -> Component.literal("jinchuriki_player_revoked=" + revoked
                + ", player=" + player.getScoreboardName()
                + ", synced_tails=" + NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS)), false);
        return revoked ? 1 : 0;
    }

    private static int revokeTailJinchuriki(CommandSourceStack source, int tails) {
        boolean revoked = BijuManager.revokeByTail(source.getServer(), tails);
        source.sendSuccess(() -> Component.literal("jinchuriki_tail_revoked=" + revoked
                + ", tails=" + tails
                + ", beast=" + BijuManager.displayName(tails)), false);
        return revoked ? 1 : 0;
    }

    private static int revokeAllJinchuriki(CommandSourceStack source) {
        int revoked = BijuManager.revokeAll(source.getServer());
        source.sendSuccess(() -> Component.literal("jinchuriki_revoked_all=" + revoked
                + ", available_biju=" + BijuManager.availableTailedBeasts(source.getServer())), false);
        return revoked;
    }

    private static int resolveCurrentPlayerJinchuriki(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BijuManager.resolvePlayer(player);
        source.sendSuccess(() -> Component.literal("jinchuriki_resolved player=" + player.getScoreboardName()
                + ", assigned_tail=" + BijuManager.getAssignedTail(player)
                + ", synced_tails=" + NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS)), false);
        return 1;
    }

    private static int spawnParticle(CommandSourceStack source, String typeName, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NarutoParticleKind kind = NarutoParticleKind.byRegistryName(typeName).orElse(null);
        if (kind == null) {
            source.sendFailure(Component.literal("Unknown narutomod particle type: " + typeName));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        level.sendParticles(
                ModParticleTypes.options(kind, defaultParticleArgs(kind, player)),
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                count,
                0.6D,
                0.6D,
                0.6D,
                0.02D
        );
        source.sendSuccess(() -> Component.literal("Spawned " + count + " narutomod:" + kind.registryName()
                + " particles using legacy id " + kind.legacyId()), false);
        return 1;
    }

    private static int[] defaultParticleArgs(NarutoParticleKind kind, ServerPlayer player) {
        return switch (kind) {
            case SMOKE_COLORED -> new int[] {0xCCFFFFFF, 10, 40, 240, -1, 4};
            case SUSPENDED_COLORED -> new int[] {0xCCB0E0FF, 10, 40};
            case FALLING_DUST -> new int[] {0xCCB09060};
            case FLAME_COLORED -> new int[] {0xCCFF6633, 10};
            case MOB_APPEARANCE -> new int[] {player.getId()};
            case BURNING_ASH -> new int[] {player.getId()};
            case HOMING_ORB -> new int[] {2, 10};
            case EXPANDING_SPHERE -> new int[] {20, 30, 0x80FFFFFF};
            case PORTAL_SPIRAL -> new int[] {2, 0xCCB080FF, 10};
            case SEAL_FORMULA -> new int[] {20, 0, 40};
            case ACID_SPIT -> new int[] {player.getId(), 0x80FFD6BA};
            case WHIRLPOOL -> new int[] {0xCC80C0FF, 10, 40, 240};
        };
    }

    private static int getSyncData(CommandSourceStack source, String key) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String value = NarutomodModVariables.get(player).save().contains(key)
                ? NarutomodModVariables.get(player).save().get(key).toString()
                : "<missing>";
        source.sendSuccess(() -> Component.literal(key + "=" + value), false);
        return 1;
    }

    private static int removeSyncData(CommandSourceStack source, String key) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProcedureSync.EntityNBTTag.removeAndSync(player, key);
        source.sendSuccess(() -> Component.literal("Removed synced key " + key), false);
        return 1;
    }

    private static int setSyncDataInt(CommandSourceStack source, String key, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProcedureSync.EntityNBTTag.setAndSync(player, key, value);
        source.sendSuccess(() -> Component.literal("Set synced int " + key + "=" + value), false);
        return 1;
    }

    private static int setSyncDataDouble(CommandSourceStack source, String key, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProcedureSync.EntityNBTTag.setAndSync(player, key, value);
        source.sendSuccess(() -> Component.literal("Set synced double " + key + "=" + value), false);
        return 1;
    }

    private static int setSyncDataBoolean(CommandSourceStack source, String key, boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProcedureSync.EntityNBTTag.setAndSync(player, key, value);
        source.sendSuccess(() -> Component.literal("Set synced boolean " + key + "=" + value), false);
        return 1;
    }

    private record DirectionBasis(Vec3 origin, Vec3 forward, Vec3 right) {
        static DirectionBasis from(ServerPlayer player) {
            double yaw = Math.toRadians(player.getYRot());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            return new DirectionBasis(player.position(), forward, right);
        }

        Vec3 point(double forwardDistance, double rightDistance, double yOffset) {
            return this.origin
                    .add(this.forward.scale(forwardDistance))
                    .add(this.right.scale(rightDistance))
                    .add(0.0D, yOffset, 0.0D);
        }

        Vec3 pointForParticle(NarutoParticleKind kind) {
            int index = kind.ordinal();
            int row = index / 4;
            int column = index % 4;
            return point(12.0D + row * 3.0D, (column - 1.5D) * 3.0D, 1.25D);
        }
    }
}
