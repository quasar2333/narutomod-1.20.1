package net.narutomod.registry;

import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.entity.AltCamViewEntity;
import net.narutomod.entity.AsakujakuFireballEntity;
import net.narutomod.entity.AsuraCannonballEntity;
import net.narutomod.entity.BiggerMeEntity;
import net.narutomod.entity.BrackenDanceEntity;
import net.narutomod.entity.BuddhaArmEntity;
import net.narutomod.entity.Buddha1000Entity;
import net.narutomod.entity.BugSwarmEntity;
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
import net.narutomod.entity.GroundShockEntity;
import net.narutomod.entity.HakkeshoKeitenEntity;
import net.narutomod.entity.HakuEntity;
import net.narutomod.entity.HirudoraEntity;
import net.narutomod.entity.HiramekareiEffectEntity;
import net.narutomod.entity.HidingInRockEntity;
import net.narutomod.entity.HidingInAshEntity;
import net.narutomod.entity.IceDomeEntity;
import net.narutomod.entity.IcePrisonEntity;
import net.narutomod.entity.IceSpearEntity;
import net.narutomod.entity.IceSpikeEntity;
import net.narutomod.entity.IntonRaihaEntity;
import net.narutomod.entity.IrukaSenseiEntity;
import net.narutomod.entity.ItachiEntity;
import net.narutomod.entity.JinchurikiCloneEntity;
import net.narutomod.entity.JintonBeamEntity;
import net.narutomod.entity.JintonCubeEntity;
import net.narutomod.entity.KarasuScrollProjectileEntity;
import net.narutomod.entity.KatonFireballEntity;
import net.narutomod.entity.KatonFireStreamEntity;
import net.narutomod.entity.KageBunshinEntity;
import net.narutomod.entity.KagutsuchiFireballEntity;
import net.narutomod.entity.KamuiShurikenEntity;
import net.narutomod.entity.KibaBladeAuraEntity;
import net.narutomod.entity.KirinEntity;
import net.narutomod.entity.KingOfHellEntity;
import net.narutomod.entity.KusanagiSwordEntity;
import net.narutomod.entity.LaserCircusEntity;
import net.narutomod.entity.LaserRingEntity;
import net.narutomod.entity.LavaChakraModeEntity;
import net.narutomod.entity.LightningBeastEntity;
import net.narutomod.entity.LightningArcEntity;
import net.narutomod.entity.LimboCloneEntity;
import net.narutomod.entity.MagmaBallEntity;
import net.narutomod.entity.MandaEntity;
import net.narutomod.entity.MeltingJutsuEntity;
import net.narutomod.entity.MightGuyEntity;
import net.narutomod.entity.MindTransferEntity;
import net.narutomod.entity.MindTransferSelfEntity;
import net.narutomod.entity.NightGuyDragonEntity;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.entity.NuibariSwordEntity;
import net.narutomod.entity.PortingDummyEntity;
import net.narutomod.entity.PoisonMistEntity;
import net.narutomod.entity.PretaShieldEntity;
import net.narutomod.entity.PurpleDragonEntity;
import net.narutomod.entity.PuppetHirukoEntity;
import net.narutomod.entity.PuppetKarasuEntity;
import net.narutomod.entity.PuppetSanshouoEntity;
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
import net.narutomod.entity.SakuraHarunoEntity;
import net.narutomod.entity.ScorchOrbEntity;
import net.narutomod.entity.SekizoEntity;
import net.narutomod.entity.SealingChainsEntity;
import net.narutomod.entity.SealingEntity;
import net.narutomod.entity.ShadowImitationEntity;
import net.narutomod.entity.SenjutsuSitPlatformEntity;
import net.narutomod.entity.SlugSummonEntity;
import net.narutomod.entity.Snake8HeadEntity;
import net.narutomod.entity.Snake8HeadsEntity;
import net.narutomod.entity.SnakeSummonEntity;
import net.narutomod.entity.SpecialEffectEntity;
import net.narutomod.entity.SpikeEntity;
import net.narutomod.entity.SusanooClothedEntity;
import net.narutomod.entity.SusanooSkeletonEntity;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.entity.SwampPitEntity;
import net.narutomod.entity.SuitonMistEntity;
import net.narutomod.entity.SuitonStreamEntity;
import net.narutomod.entity.TailBeastBallEntity;
import net.narutomod.entity.TailedBeastEntity;
import net.narutomod.entity.TenTailsEntity;
import net.narutomod.entity.TenseiBakuGoldEntity;
import net.narutomod.entity.TenseiBakuSilverEntity;
import net.narutomod.entity.TenseiganOrbEntity;
import net.narutomod.entity.TentenEntity;
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
import net.narutomod.entity.WhiteZetsuEntity;
import net.narutomod.entity.WoodArmEntity;
import net.narutomod.entity.WoodBurialEntity;
import net.narutomod.entity.WoodGolemEntity;
import net.narutomod.entity.WoodPrisonEntity;
import net.narutomod.entity.YasakaMagatamaEntity;

public final class ModEntityTypes {
    public static final RegistryObject<EntityType<AltCamViewEntity>> ALTCAMVIEWENTITY = registerAltCamView("altcamviewentity");
    public static final RegistryObject<EntityType<NinjaMobEntity>> ANBU = registerNinjaMob("anbu");
    public static final RegistryObject<EntityType<AsakujakuFireballEntity>> ASAKUJAKU_FIREBALL =
            registerAsakujakuFireball("asakujaku_fireball");
    public static final RegistryObject<EntityType<BiggerMeEntity>> BIGGERME = registerBiggerMe("biggerme");
    public static final RegistryObject<EntityType<Buddha1000Entity>> BUDDHA_1000 = registerBuddha1000("buddha_1000");
    public static final RegistryObject<EntityType<BuddhaArmEntity>> BUDDHA_ARM = registerBuddhaArm("buddha_arm");
    public static final RegistryObject<EntityType<BugSwarmEntity>> BUGBALL = registerBugSwarm("bugball");
    public static final RegistryObject<EntityType<ExplosiveClayEntity>> C_1 = registerExplosiveClay("c_1", 0.4F, 0.8F);
    public static final RegistryObject<EntityType<ExplosiveClayEntity>> C_2 = registerExplosiveClay("c_2", 2.0F, 1.2F);
    public static final RegistryObject<EntityType<ExplosiveClayEntity>> C_3 = registerExplosiveClay("c_3", 0.6F, 1.8F);
    public static final RegistryObject<EntityType<CellularActivationEntity>> CELLULAR_ACTIVATION = registerCellularActivation("cellular_activation");
    public static final RegistryObject<EntityType<ChibakuSatelliteEntity>> CHIBAKU_SATELLITE = registerChibakuSatellite("chibaku_satellite");
    public static final RegistryObject<EntityType<ChibakuTenseiBallEntity>> CHIBAKU_TENSEI_BALL = registerChibakuTenseiBall("chibaku_tensei_ball");
    public static final RegistryObject<EntityType<ChidoriEntity>> CHIDORI = registerChidori("chidori");
    public static final RegistryObject<EntityType<ChidoriSpearEntity>> CHIDORI_SPEAR = registerChidoriSpear("chidori_spear");
    public static final RegistryObject<EntityType<CrowEntity>> CROW = registerCrow("crow");
    public static final RegistryObject<EntityType<EarthBlocksEntity>> EARTH_BLOCKS = registerEarthBlocks("earth_blocks");
    public static final RegistryObject<EntityType<EarthSandwichEntity>> EARTH_SANDWICH = registerEarthSandwich("earth_sandwich");
    public static final RegistryObject<EntityType<EarthSpearsEntity>> EARTH_SPEARS = registerEarthSpears("earth_spears");
    public static final RegistryObject<EntityType<TailedBeastEntity>> EIGHT_TAILS =
            registerTailedBeast("eight_tails", TailedBeastEntity.Variant.EIGHT);
    public static final RegistryObject<EntityType<EightTrigramsEntity>> EIGHTTRIGRAMSENTITY = registerEightTrigrams("eighttrigramsentity");
    public static final RegistryObject<EntityType<EightyGodsEntity>> ENTITY80GODS = registerEightyGods("entity80gods");
    public static final RegistryObject<EntityType<BrackenDanceEntity>> ENTITYBRACKENDANCE = registerBrackenDance("entitybrackendance");
    public static final RegistryObject<EntityType<ThrownSpecialWeaponEntity>> ENTITYBULLETASHBONES =
            registerThrownSpecialWeapon("entitybulletashbones");
    public static final RegistryObject<EntityType<AsuraCannonballEntity>> ENTITYBULLETASURACANON =
            registerAsuraCannonball("entitybulletasuracanon");
    public static final RegistryObject<EntityType<ThrownSpecialWeaponEntity>> ENTITYBULLETBLACK_RECEIVER =
            registerThrownSpecialWeapon("entitybulletblack_receiver");
    public static final RegistryObject<EntityType<FoldingFanProjectileEntity>> ENTITYBULLETFOLDING_FAN =
            registerFoldingFanProjectile("entitybulletfolding_fan");
    public static final RegistryObject<EntityType<HiramekareiEffectEntity>> ENTITYBULLETHIRAMEKAREI_SWORD =
            registerHiramekareiEffect("entitybullethiramekarei_sword");
    public static final RegistryObject<EntityType<KamuiShurikenEntity>> ENTITYBULLETKAMUISHURIKEN =
            registerKamuiShuriken("entitybulletkamuishuriken");
    public static final RegistryObject<EntityType<KibaBladeAuraEntity>> ENTITYBULLETKIBA_BLADES =
            registerKibaBladeAura("entitybulletkiba_blades");
    public static final RegistryObject<EntityType<ThrownNinjaToolEntity>> ENTITYBULLETKUNAI = registerThrownNinjaTool("entitybulletkunai");
    public static final RegistryObject<EntityType<ThrownNinjaToolEntity>> ENTITYBULLETKUNAI_EXPLOSIVE =
            registerThrownNinjaTool("entitybulletkunai_explosive");
    public static final RegistryObject<EntityType<KusanagiSwordEntity>> ENTITYBULLETKUSANAGI_SWORD =
            registerKusanagiSword("entitybulletkusanagi_sword");
    public static final RegistryObject<EntityType<NuibariSwordEntity>> ENTITYBULLETNUIBARI_SWORD =
            registerNuibariSword("entitybulletnuibari_sword");
    public static final RegistryObject<EntityType<KarasuScrollProjectileEntity>> ENTITYBULLETSCROLL_KARASU =
            registerKarasuScrollProjectile("entitybulletscroll_karasu");
    public static final RegistryObject<EntityType<SanshouoScrollProjectileEntity>> ENTITYBULLETSCROLL_SANSHOUO =
            registerSanshouoScrollProjectile("entitybulletscroll_sanshouo");
    public static final RegistryObject<EntityType<SenjutsuSitPlatformEntity>> ENTITYBULLETSENJUTSU =
            registerSenjutsuSitPlatform("entitybulletsenjutsu");
    public static final RegistryObject<EntityType<ThrownNinjaToolEntity>> ENTITYBULLETSHURIKEN = registerThrownNinjaTool("entitybulletshuriken");
    public static final RegistryObject<EntityType<ThrownNinjaToolEntity>> ENTITYBULLETSMOKE_BOMB =
            registerThrownNinjaTool("entitybulletsmoke_bomb");
    public static final RegistryObject<EntityType<EarthWallEntity>> ENTITYEARTHWALL = registerEarthWall("entityearthwall");
    public static final RegistryObject<EntityType<HidingInRockEntity>> ENTITYHIDINGINROCK = registerHidingInRock("entityhidinginrock");
    public static final RegistryObject<EntityType<HirudoraEntity>> ENTITYHIRUDORA = registerHirudora("entityhirudora");
    public static final RegistryObject<EntityType<SandShieldEntity>> ENTITYJITONSHIELD = registerSandShield("entityjitonshield");
    public static final RegistryObject<EntityType<KagutsuchiFireballEntity>> ENTITYKAGUTSUCHISWORDBIGFIREBALL =
            registerKagutsuchiFireball("entitykagutsuchiswordbigfireball", 3.6F);
    public static final RegistryObject<EntityType<KagutsuchiFireballEntity>> ENTITYKAGUTSUCHISWORDFIREBALL =
            registerKagutsuchiFireball("entitykagutsuchiswordfireball", 1.2F);
    public static final RegistryObject<EntityType<NightGuyDragonEntity>> ENTITYNGDRAGON = registerNightGuyDragon("entityngdragon");
    public static final RegistryObject<EntityType<SekizoEntity>> ENTITYSEKIZO = registerSekizo("entitysekizo");
    public static final RegistryObject<EntityType<ExplosiveCloneEntity>> EXPLOSIVE_CLONE = registerExplosiveClone("explosive_clone");
    public static final RegistryObject<EntityType<FalseDarknessEntity>> FALSE_DARKNESS = registerFalseDarkness("false_darkness");
    public static final RegistryObject<EntityType<FingerBoneEntity>> FINGER_BONE = registerFingerBone("finger_bone");
    public static final RegistryObject<EntityType<TailedBeastEntity>> FIVE_TAILS =
            registerTailedBeast("five_tails", TailedBeastEntity.Variant.FIVE);
    public static final RegistryObject<EntityType<TailedBeastEntity>> FOUR_TAILS =
            registerTailedBeast("four_tails", TailedBeastEntity.Variant.FOUR);
    public static final RegistryObject<EntityType<FutonGreatBreakthroughEntity>> FUTON_GREAT_BREAKTHROUGH =
            registerFutonGreatBreakthrough("futon_great_breakthrough");
    public static final RegistryObject<EntityType<FutonVacuumEntity>> FUTON_VACUUM = registerFutonVacuum("futon_vacuum");
    public static final RegistryObject<EntityType<FutonChakraFlowEntity>> FUTONCHAKRAFLOW = registerFutonChakraFlow("futonchakraflow");
    public static final RegistryObject<EntityType<FuttonMistEntity>> FUTTON_MIST = registerFuttonMist("futton_mist");
    public static final RegistryObject<EntityType<NinjaMobEntity>> GAARA = registerNinjaMob("gaara");
    public static final RegistryObject<EntityType<GamabuntaEntity>> GAMABUNTA = registerGamabunta("gamabunta");
    public static final RegistryObject<EntityType<GedoStatueEntity>> GEDO_STATUE = registerGedoStatue("gedo_statue");
    public static final RegistryObject<EntityType<GiantDog2hEntity>> GIANT_DOG_2H = registerGiantDog2h("giant_dog_2h");
    public static final RegistryObject<EntityType<GroundShockEntity>> GROUND_SHOCK = registerGroundShock("ground_shock");
    public static final RegistryObject<EntityType<HakkeshoKeitenEntity>> HAKKESHOKEITENENTITY = registerHakkeshoKeiten("hakkeshokeitenentity");
    public static final RegistryObject<EntityType<HakuEntity>> HAKU = registerHaku("haku");
    public static final RegistryObject<EntityType<HidingInAshEntity>> HIDING_IN_ASH = registerHidingInAsh("hiding_in_ash");
    public static final RegistryObject<EntityType<IceDomeEntity>> ICE_DOME = registerIceDome("ice_dome");
    public static final RegistryObject<EntityType<IcePrisonEntity>> ICE_PRISON = registerIcePrison("ice_prison");
    public static final RegistryObject<EntityType<IceSpearEntity>> ICE_SPEAR = registerIceSpear("ice_spear");
    public static final RegistryObject<EntityType<IceSpikeEntity>> ICE_SPIKE = registerIceSpike("ice_spike");
    public static final RegistryObject<EntityType<IntonRaihaEntity>> INTON_RAIHA = registerIntonRaiha("inton_raiha");
    public static final RegistryObject<EntityType<IrukaSenseiEntity>> IRUKA_SENSEI = registerIrukaSensei("iruka_sensei");
    public static final RegistryObject<EntityType<ItachiEntity>> ITACHI = registerItachi("itachi");
    public static final RegistryObject<EntityType<JinchurikiCloneEntity>> JINCHURIKI_CLONE = registerJinchurikiClone("jinchuriki_clone");
    public static final RegistryObject<EntityType<JintonBeamEntity>> JINTONBEAM = registerJintonBeam("jintonbeam");
    public static final RegistryObject<EntityType<JintonCubeEntity>> JINTONCUBE = registerJintonCube("jintoncube");
    public static final RegistryObject<EntityType<KageBunshinEntity>> KAGE_BUNSHIN = registerKageBunshin("kage_bunshin");
    public static final RegistryObject<EntityType<NinjaMobEntity>> KAKASHI = registerNinjaMob("kakashi");
    public static final RegistryObject<EntityType<NinjaMobEntity>> KANKURO = registerNinjaMob("kankuro");
    public static final RegistryObject<EntityType<KatonFireballEntity>> KATONFIREBALL = registerKatonFireball("katonfireball");
    public static final RegistryObject<EntityType<KatonFireStreamEntity>> KATONFIRESTREAM = registerKatonFireStream("katonfirestream");
    public static final RegistryObject<EntityType<KingOfHellEntity>> KINGOFHELLENTITY = registerKingOfHell("kingofhellentity");
    public static final RegistryObject<EntityType<KirinEntity>> KIRIN = registerKirin("kirin");
    public static final RegistryObject<EntityType<NinjaMobEntity>> KISAME_HOSHIGAKI = registerNinjaMob("kisame_hoshigaki", 0.6F, 2.1F);
    public static final RegistryObject<EntityType<NinjaMobEntity>> KUROTSUCHI = registerNinjaMob("kurotsuchi");
    public static final RegistryObject<EntityType<LaserCircusEntity>> LASER_CIRCUS = registerLaserCircus("laser_circus");
    public static final RegistryObject<EntityType<LaserRingEntity>> LASER_RING = registerLaserRing("laser_ring");
    public static final RegistryObject<EntityType<LavaChakraModeEntity>> LAVA_CHAKRA_MODE = registerLavaChakraMode("lava_chakra_mode");
    public static final RegistryObject<EntityType<LightningArcEntity>> LIGHTNING_ARC = registerLightningArc("lightning_arc");
    public static final RegistryObject<EntityType<LightningBeastEntity>> LIGHTNING_BEAST = registerLightningBeast("lightning_beast");
    public static final RegistryObject<EntityType<LimboCloneEntity>> LIMBO_CLONE = registerLimboClone("limbo_clone");
    public static final RegistryObject<EntityType<MagmaBallEntity>> MAGMABALL = registerMagmaBall("magmaball");
    public static final RegistryObject<EntityType<MandaEntity>> MANDA = registerManda("manda");
    public static final RegistryObject<EntityType<MeltingJutsuEntity>> MELTING_JUTSU = registerMeltingJutsu("melting_jutsu");
    public static final RegistryObject<EntityType<MightGuyEntity>> MIGHTGUY = registerMightGuy("mightguy");
    public static final RegistryObject<EntityType<MindTransferEntity>> MIND_TRANSFER = registerMindTransfer("mind_transfer");
    public static final RegistryObject<EntityType<MindTransferSelfEntity>> MIND_TRANSFER_SELF = registerMindTransferSelf("mind_transfer_self");
    public static final RegistryObject<EntityType<TailedBeastEntity>> NINE_TAILS =
            registerTailedBeast("nine_tails", TailedBeastEntity.Variant.NINE);
    public static final RegistryObject<EntityType<NinjaMobEntity>> NINJA_IWA = registerNinjaMob("ninja_iwa");
    public static final RegistryObject<EntityType<NinjaMobEntity>> NINJA_KIRI = registerNinjaMob("ninja_kiri");
    public static final RegistryObject<EntityType<NinjaMobEntity>> NINJA_KONOHA = registerNinjaMob("ninja_konoha");
    public static final RegistryObject<EntityType<NinjaMobEntity>> NINJA_KUMO = registerNinjaMob("ninja_kumo");
    public static final RegistryObject<EntityType<NinjaMobEntity>> NINJA_SUNA = registerNinjaMob("ninja_suna");
    public static final RegistryObject<EntityType<TailedBeastEntity>> ONE_TAIL =
            registerTailedBeast("one_tail", TailedBeastEntity.Variant.ONE);
    public static final RegistryObject<EntityType<PoisonMistEntity>> POISON_MIST = registerPoisonMist("poison_mist");
    public static final RegistryObject<EntityType<PretaShieldEntity>> PRETASHIELDENTITY = registerPretaShield("pretashieldentity");
    public static final RegistryObject<EntityType<PuppetHirukoEntity>> PUPPET_HIRUKO = registerPuppetHiruko("puppet_hiruko");
    public static final RegistryObject<EntityType<PuppetKarasuEntity>> PUPPET_KARASU = registerPuppetKarasu("puppet_karasu");
    public static final RegistryObject<EntityType<PuppetSanshouoEntity>> PUPPET_SANSHOUO = registerPuppetSanshouo("puppet_sanshouo");
    public static final RegistryObject<EntityType<PurpleDragonEntity>> PURPLE_DRAGON = registerPurpleDragon("purple_dragon");
    public static final RegistryObject<EntityType<RaitonChakraModeEntity>> RAITONCHAKRAMODE = registerRaitonChakraMode("raitonchakramode");
    public static final RegistryObject<EntityType<RantonKogaEntity>> RANTON_KOGA = registerRantonKoga("ranton_koga");
    public static final RegistryObject<EntityType<RantonCloudEntity>> RANTONCLOUD = registerRantonCloud("rantoncloud");
    public static final RegistryObject<EntityType<RasenganEntity>> RASENGAN = registerRasengan("rasengan");
    public static final RegistryObject<EntityType<RasenshurikenEntity>> RASENSHURIKEN = registerRasenshuriken("rasenshuriken");
    public static final RegistryObject<EntityType<ReplacementCloneEntity>> REPLACEMENTCLONE = registerReplacementClone("replacementclone");
    public static final RegistryObject<EntityType<SakuraHarunoEntity>> SAKURA_HARUNO = registerSakuraHaruno("sakura_haruno");
    public static final RegistryObject<EntityType<SandBindEntity>> SAND_BIND = registerSandBind("sand_bind");
    public static final RegistryObject<EntityType<SandBulletEntity>> SAND_BULLET = registerSandBullet("sand_bullet");
    public static final RegistryObject<EntityType<SandLevitationEntity>> SAND_LEVITATION = registerSandLevitation("sand_levitation");
    public static final RegistryObject<EntityType<ScorchOrbEntity>> SCORCHORB = registerScorchOrb("scorchorb");
    public static final RegistryObject<EntityType<SealingEntity>> SEALING = registerSealing("sealing");
    public static final RegistryObject<EntityType<SealingChainsEntity>> SEALING_CHAINS = registerSealingChains("sealing_chains");
    public static final RegistryObject<EntityType<TailedBeastEntity>> SEVEN_TAILS =
            registerTailedBeast("seven_tails", TailedBeastEntity.Variant.SEVEN);
    public static final RegistryObject<EntityType<ShadowImitationEntity>> SHADOW_IMITATION = registerShadowImitation("shadow_imitation");
    public static final RegistryObject<EntityType<TailedBeastEntity>> SIX_TAILS =
            registerTailedBeast("six_tails", TailedBeastEntity.Variant.SIX);
    public static final RegistryObject<EntityType<SlugSummonEntity>> SLUG = registerSlugSummon("slug");
    public static final RegistryObject<EntityType<Snake8HeadEntity>> SNAKE_8_HEAD1 = registerSnake8Head("snake_8_head1");
    public static final RegistryObject<EntityType<Snake8HeadsEntity>> SNAKE_8_HEADS = registerSnake8Heads("snake_8_heads");
    public static final RegistryObject<EntityType<SnakeSummonEntity>> SNAKE_SUMMON = registerSnakeSummon("snake_summon");
    public static final RegistryObject<EntityType<SpecialEffectEntity>> SPECIALEFFECTENTITY = registerSpecialEffect("specialeffectentity");
    public static final RegistryObject<EntityType<SpikeEntity>> SPIKE = registerSpike("spike");
    public static final RegistryObject<EntityType<WaterSharkEntity>> SUITON_SHARK = registerWaterShark("suiton_shark");
    public static final RegistryObject<EntityType<SuitonMistEntity>> SUITONMIST = registerSuitonMist("suitonmist");
    public static final RegistryObject<EntityType<SuitonStreamEntity>> SUITONSTREAM = registerSuitonStream("suitonstream");
    public static final RegistryObject<EntityType<SusanooClothedEntity>> SUSANOOCLOTHED = registerSusanooClothed("susanooclothed");
    public static final RegistryObject<EntityType<SusanooSkeletonEntity>> SUSANOOSKELETON = registerSusanooSkeleton("susanooskeleton");
    public static final RegistryObject<EntityType<SusanooWingedEntity>> SUSANOOWINGED = registerSusanooWinged("susanoowinged");
    public static final RegistryObject<EntityType<SwampPitEntity>> SWAMP_PIT = registerSwampPit("swamp_pit");
    public static final RegistryObject<EntityType<TailBeastBallEntity>> TAILBEASTBALL = registerTailBeastBall("tailbeastball");
    public static final RegistryObject<EntityType<NinjaMobEntity>> TEMARI = registerNinjaMob("temari");
    public static final RegistryObject<EntityType<TenTailsEntity>> TEN_TAILS = registerTenTails("ten_tails");
    public static final RegistryObject<EntityType<TenseiBakuGoldEntity>> TENSEI_BAKU_GOLD = registerTenseiBakuGold("tensei_baku_gold");
    public static final RegistryObject<EntityType<TenseiBakuSilverEntity>> TENSEI_BAKU_SILVER = registerTenseiBakuSilver("tensei_baku_silver");
    public static final RegistryObject<EntityType<TenseiganOrbEntity>> TENSEIGANGUN = registerTenseiganOrb("tenseigangun");
    public static final RegistryObject<EntityType<TentenEntity>> TENTEN = registerTenten("tenten");
    public static final RegistryObject<EntityType<TailedBeastEntity>> THREE_TAILS =
            registerTailedBeast("three_tails", TailedBeastEntity.Variant.THREE);
    public static final RegistryObject<EntityType<ToadSummonEntity>> TOAD_SUMMON = registerToadSummon("toad_summon");
    public static final RegistryObject<EntityType<TransformationJutsuEntity>> TRANSFORMATION_JUTSU = registerTransformationJutsu("transformation_jutsu");
    public static final RegistryObject<EntityType<TruthSeekerBallEntity>> TRUTHSEEKERBALL = registerTruthSeekerBall("truthseekerball");
    public static final RegistryObject<EntityType<TailedBeastEntity>> TWO_TAILS =
            registerTailedBeast("two_tails", TailedBeastEntity.Variant.TWO);
    public static final RegistryObject<EntityType<UnrivaledStrengthEntity>> UNRIVALED_STRENGTH =
            registerUnrivaledStrength("unrivaled_strength");
    public static final RegistryObject<EntityType<WaterDragonEntity>> WATER_DRAGON = registerWaterDragon("water_dragon");
    public static final RegistryObject<EntityType<WaterPrisonEntity>> WATER_PRISON = registerWaterPrison("water_prison");
    public static final RegistryObject<EntityType<WaterShockwaveEntity>> WATER_SHOCKWAVE = registerWaterShockwave("water_shockwave");
    public static final RegistryObject<EntityType<WhiteZetsuEntity>> WHITEZETSU = registerWhiteZetsu("whitezetsu");
    public static final RegistryObject<EntityType<WoodArmEntity>> WOOD_ARM = registerWoodArm("wood_arm");
    public static final RegistryObject<EntityType<WoodBurialEntity>> WOOD_BURIAL = registerWoodBurial("wood_burial");
    public static final RegistryObject<EntityType<WoodGolemEntity>> WOOD_GOLEM = registerWoodGolem("wood_golem");
    public static final RegistryObject<EntityType<WoodPrisonEntity>> WOOD_PRISON = registerWoodPrison("wood_prison");
    public static final RegistryObject<EntityType<YasakaMagatamaEntity>> YASAKA_MAGATAMA = registerYasakaMagatama("yasaka_magatama");
    public static final RegistryObject<EntityType<NinjaMobEntity>> ZABUZA_MOMOCHI = registerNinjaMob("zabuza_momochi", 0.6F, 2.0F);

    private ModEntityTypes() {
    }

    private static RegistryObject<EntityType<PortingDummyEntity>> register(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PortingDummyEntity>of(PortingDummyEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<AltCamViewEntity>> registerAltCamView(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<AltCamViewEntity>of(AltCamViewEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<Buddha1000Entity>> registerBuddha1000(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<Buddha1000Entity>of(Buddha1000Entity::new, MobCategory.MISC)
                .sized(12.0F, 40.0F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<BuddhaArmEntity>> registerBuddhaArm(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<BuddhaArmEntity>of(BuddhaArmEntity::new, MobCategory.MISC)
                .sized(5.0F, 5.0F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ThrownNinjaToolEntity>> registerThrownNinjaTool(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ThrownNinjaToolEntity>of(ThrownNinjaToolEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<ThrownSpecialWeaponEntity>> registerThrownSpecialWeapon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ThrownSpecialWeaponEntity>of(ThrownSpecialWeaponEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<SenjutsuSitPlatformEntity>> registerSenjutsuSitPlatform(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SenjutsuSitPlatformEntity>of(SenjutsuSitPlatformEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<AsuraCannonballEntity>> registerAsuraCannonball(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<AsuraCannonballEntity>of(AsuraCannonballEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<AsakujakuFireballEntity>> registerAsakujakuFireball(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<AsakujakuFireballEntity>of(AsakujakuFireballEntity::new, MobCategory.MISC)
                .sized(0.3125F, 0.3125F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<KamuiShurikenEntity>> registerKamuiShuriken(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KamuiShurikenEntity>of(KamuiShurikenEntity::new, MobCategory.MISC)
                .sized(0.4F, 0.4F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<NuibariSwordEntity>> registerNuibariSword(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<NuibariSwordEntity>of(NuibariSwordEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<FoldingFanProjectileEntity>> registerFoldingFanProjectile(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FoldingFanProjectileEntity>of(FoldingFanProjectileEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<FingerBoneEntity>> registerFingerBone(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FingerBoneEntity>of(FingerBoneEntity::new, MobCategory.MISC)
                .sized(0.2F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<BrackenDanceEntity>> registerBrackenDance(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<BrackenDanceEntity>of(BrackenDanceEntity::new, MobCategory.MISC)
                .sized(0.5F, 1.82F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<KibaBladeAuraEntity>> registerKibaBladeAura(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KibaBladeAuraEntity>of(KibaBladeAuraEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<HiramekareiEffectEntity>> registerHiramekareiEffect(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HiramekareiEffectEntity>of(HiramekareiEffectEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<KusanagiSwordEntity>> registerKusanagiSword(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KusanagiSwordEntity>of(KusanagiSwordEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<MindTransferEntity>> registerMindTransfer(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MindTransferEntity>of(MindTransferEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<MindTransferSelfEntity>> registerMindTransferSelf(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MindTransferSelfEntity>of(MindTransferSelfEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<ShadowImitationEntity>> registerShadowImitation(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ShadowImitationEntity>of(ShadowImitationEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<RasenganEntity>> registerRasengan(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<RasenganEntity>of(RasenganEntity::new, MobCategory.MISC)
                .sized(0.35F, 0.35F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<RasenshurikenEntity>> registerRasenshuriken(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<RasenshurikenEntity>of(RasenshurikenEntity::new, MobCategory.MISC)
                .sized(2.5F, 0.5F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<ReplacementCloneEntity>> registerReplacementClone(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ReplacementCloneEntity>of(ReplacementCloneEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<ChidoriEntity>> registerChidori(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ChidoriEntity>of(ChidoriEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<CellularActivationEntity>> registerCellularActivation(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<CellularActivationEntity>of(CellularActivationEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<ChidoriSpearEntity>> registerChidoriSpear(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ChidoriSpearEntity>of(ChidoriSpearEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<LightningArcEntity>> registerLightningArc(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LightningArcEntity>of(LightningArcEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<IntonRaihaEntity>> registerIntonRaiha(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IntonRaihaEntity>of(IntonRaihaEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<RantonCloudEntity>> registerRantonCloud(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<RantonCloudEntity>of(RantonCloudEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<LaserCircusEntity>> registerLaserCircus(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LaserCircusEntity>of(LaserCircusEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<LaserRingEntity>> registerLaserRing(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LaserRingEntity>of(LaserRingEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<RantonKogaEntity>> registerRantonKoga(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<RantonKogaEntity>of(RantonKogaEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TenseiganOrbEntity>> registerTenseiganOrb(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TenseiganOrbEntity>of(TenseiganOrbEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TenseiBakuSilverEntity>> registerTenseiBakuSilver(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TenseiBakuSilverEntity>of(TenseiBakuSilverEntity::new, MobCategory.MISC)
                .sized(2.0F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TenseiBakuGoldEntity>> registerTenseiBakuGold(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TenseiBakuGoldEntity>of(TenseiBakuGoldEntity::new, MobCategory.MISC)
                .sized(0.125F, 0.125F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ToadSummonEntity>> registerToadSummon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ToadSummonEntity>of(ToadSummonEntity::new, MobCategory.CREATURE)
                .sized(0.8F, 1.125F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<GamabuntaEntity>> registerGamabunta(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<GamabuntaEntity>of(GamabuntaEntity::new, MobCategory.CREATURE)
                .sized(0.8F, 1.125F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<GedoStatueEntity>> registerGedoStatue(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<GedoStatueEntity>of(GedoStatueEntity::new, MobCategory.CREATURE)
                .sized(GedoStatueEntity.BASE_WIDTH, GedoStatueEntity.BASE_HEIGHT)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SnakeSummonEntity>> registerSnakeSummon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SnakeSummonEntity>of(SnakeSummonEntity::new, MobCategory.CREATURE)
                .sized(0.3F, 0.25F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<Snake8HeadEntity>> registerSnake8Head(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<Snake8HeadEntity>of(Snake8HeadEntity::new, MobCategory.MONSTER)
                .sized(Snake8HeadEntity.WIDTH, Snake8HeadEntity.HEIGHT)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<Snake8HeadsEntity>> registerSnake8Heads(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<Snake8HeadsEntity>of(Snake8HeadsEntity::new, MobCategory.CREATURE)
                .sized(Snake8HeadsEntity.WIDTH, Snake8HeadsEntity.HEIGHT)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<MandaEntity>> registerManda(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MandaEntity>of(MandaEntity::new, MobCategory.CREATURE)
                .sized(0.3F, 0.25F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SlugSummonEntity>> registerSlugSummon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SlugSummonEntity>of(SlugSummonEntity::new, MobCategory.CREATURE)
                .sized(0.75F, 0.75F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<FalseDarknessEntity>> registerFalseDarkness(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FalseDarknessEntity>of(FalseDarknessEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<LightningBeastEntity>> registerLightningBeast(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LightningBeastEntity>of(LightningBeastEntity::new, MobCategory.CREATURE)
                .sized(1.2F, 1.7F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<KageBunshinEntity>> registerKageBunshin(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KageBunshinEntity>of(KageBunshinEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<BiggerMeEntity>> registerBiggerMe(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<BiggerMeEntity>of(BiggerMeEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<LimboCloneEntity>> registerLimboClone(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LimboCloneEntity>of(LimboCloneEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<CrowEntity>> registerCrow(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<CrowEntity>of(CrowEntity::new, MobCategory.AMBIENT)
                .sized(0.5F, 0.9F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<GroundShockEntity>> registerGroundShock(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<GroundShockEntity>of(GroundShockEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<EarthBlocksEntity>> registerEarthBlocks(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EarthBlocksEntity>of(EarthBlocksEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SpikeEntity>> registerSpike(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SpikeEntity>of(SpikeEntity::new, MobCategory.MISC)
                .sized(0.5F, 1.82F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<EightTrigramsEntity>> registerEightTrigrams(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EightTrigramsEntity>of(EightTrigramsEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<HakkeshoKeitenEntity>> registerHakkeshoKeiten(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HakkeshoKeitenEntity>of(HakkeshoKeitenEntity::new, MobCategory.MISC)
                .sized(3.0F, 3.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SpecialEffectEntity>> registerSpecialEffect(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SpecialEffectEntity>of(SpecialEffectEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ExplosiveCloneEntity>> registerExplosiveClone(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ExplosiveCloneEntity>of(ExplosiveCloneEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<EightyGodsEntity>> registerEightyGods(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EightyGodsEntity>of(EightyGodsEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<HirudoraEntity>> registerHirudora(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HirudoraEntity>of(HirudoraEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.5F)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<NightGuyDragonEntity>> registerNightGuyDragon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<NightGuyDragonEntity>of(NightGuyDragonEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ExplosiveClayEntity>> registerExplosiveClay(String name, float width, float height) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ExplosiveClayEntity>of(ExplosiveClayEntity::new, MobCategory.CREATURE)
                .sized(width, height)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<KirinEntity>> registerKirin(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KirinEntity>of(KirinEntity::new, MobCategory.MISC)
                .sized(10.0F, 10.0F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<TailBeastBallEntity>> registerTailBeastBall(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TailBeastBallEntity>of(TailBeastBallEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TailedBeastEntity>> registerTailedBeast(String name, TailedBeastEntity.Variant variant) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TailedBeastEntity>of(
                        (entityType, level) -> new TailedBeastEntity(entityType, level, variant), MobCategory.MONSTER)
                .sized(variant.width(), variant.height())
                .clientTrackingRange(96)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TenTailsEntity>> registerTenTails(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TenTailsEntity>of(TenTailsEntity::new, MobCategory.MONSTER)
                .sized(TenTailsEntity.WIDTH, TenTailsEntity.HEIGHT)
                .clientTrackingRange(96)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ChibakuTenseiBallEntity>> registerChibakuTenseiBall(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ChibakuTenseiBallEntity>of(ChibakuTenseiBallEntity::new, MobCategory.MISC)
                .sized(ChibakuTenseiBallEntity.BASE_SIZE, ChibakuTenseiBallEntity.BASE_SIZE)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ChibakuSatelliteEntity>> registerChibakuSatellite(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ChibakuSatelliteEntity>of(ChibakuSatelliteEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(128)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<YasakaMagatamaEntity>> registerYasakaMagatama(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<YasakaMagatamaEntity>of(YasakaMagatamaEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<RaitonChakraModeEntity>> registerRaitonChakraMode(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<RaitonChakraModeEntity>of(RaitonChakraModeEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<KatonFireballEntity>> registerKatonFireball(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KatonFireballEntity>of(KatonFireballEntity::new, MobCategory.MISC)
                .sized(0.8F, 0.8F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<MagmaBallEntity>> registerMagmaBall(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MagmaBallEntity>of(MagmaBallEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<MeltingJutsuEntity>> registerMeltingJutsu(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MeltingJutsuEntity>of(MeltingJutsuEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<LavaChakraModeEntity>> registerLavaChakraMode(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<LavaChakraModeEntity>of(LavaChakraModeEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<KagutsuchiFireballEntity>> registerKagutsuchiFireball(String name, float size) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KagutsuchiFireballEntity>of(KagutsuchiFireballEntity::new, MobCategory.MISC)
                .sized(size, size)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<HidingInAshEntity>> registerHidingInAsh(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HidingInAshEntity>of(HidingInAshEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<HidingInRockEntity>> registerHidingInRock(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HidingInRockEntity>of(HidingInRockEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<EarthSpearsEntity>> registerEarthSpears(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EarthSpearsEntity>of(EarthSpearsEntity::new, MobCategory.MISC)
                .sized(0.5F, 1.82F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<IceSpearEntity>> registerIceSpear(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IceSpearEntity>of(IceSpearEntity::new, MobCategory.MISC)
                .sized(0.5F, 1.82F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<IceDomeEntity>> registerIceDome(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IceDomeEntity>of(IceDomeEntity::new, MobCategory.MISC)
                .sized(IceDomeEntity.WIDTH, IceDomeEntity.HEIGHT)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<IceSpikeEntity>> registerIceSpike(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IceSpikeEntity>of(IceSpikeEntity::new, MobCategory.MISC)
                .sized(0.5F, 1.82F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<IcePrisonEntity>> registerIcePrison(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IcePrisonEntity>of(IcePrisonEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<WoodPrisonEntity>> registerWoodPrison(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WoodPrisonEntity>of(WoodPrisonEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WoodBurialEntity>> registerWoodBurial(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WoodBurialEntity>of(WoodBurialEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WoodArmEntity>> registerWoodArm(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WoodArmEntity>of(WoodArmEntity::new, MobCategory.MISC)
                .sized(0.2F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WoodGolemEntity>> registerWoodGolem(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WoodGolemEntity>of(WoodGolemEntity::new, MobCategory.CREATURE)
                .sized(WoodGolemEntity.WIDTH, WoodGolemEntity.HEIGHT)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SealingEntity>> registerSealing(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SealingEntity>of(SealingEntity::new, MobCategory.MISC)
                .sized(12.0F, 0.01F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<JintonBeamEntity>> registerJintonBeam(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<JintonBeamEntity>of(JintonBeamEntity::new, MobCategory.MISC)
                .sized(0.125F, 0.125F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SekizoEntity>> registerSekizo(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SekizoEntity>of(SekizoEntity::new, MobCategory.MISC)
                .sized(0.125F, 0.125F)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<JintonCubeEntity>> registerJintonCube(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<JintonCubeEntity>of(JintonCubeEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<EarthSandwichEntity>> registerEarthSandwich(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EarthSandwichEntity>of(EarthSandwichEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<EarthWallEntity>> registerEarthWall(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<EarthWallEntity>of(EarthWallEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<SwampPitEntity>> registerSwampPit(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SwampPitEntity>of(SwampPitEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<KatonFireStreamEntity>> registerKatonFireStream(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KatonFireStreamEntity>of(KatonFireStreamEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<SuitonStreamEntity>> registerSuitonStream(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SuitonStreamEntity>of(SuitonStreamEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<WaterDragonEntity>> registerWaterDragon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WaterDragonEntity>of(WaterDragonEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WaterSharkEntity>> registerWaterShark(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WaterSharkEntity>of(WaterSharkEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.5F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WaterPrisonEntity>> registerWaterPrison(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WaterPrisonEntity>of(WaterPrisonEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SuitonMistEntity>> registerSuitonMist(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SuitonMistEntity>of(SuitonMistEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<WaterShockwaveEntity>> registerWaterShockwave(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WaterShockwaveEntity>of(WaterShockwaveEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SealingChainsEntity>> registerSealingChains(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SealingChainsEntity>of(SealingChainsEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<BugSwarmEntity>> registerBugSwarm(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<BugSwarmEntity>of(BugSwarmEntity::new, MobCategory.MISC)
                .sized(2.5F, 1.5F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<TransformationJutsuEntity>> registerTransformationJutsu(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TransformationJutsuEntity>of(TransformationJutsuEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<PuppetKarasuEntity>> registerPuppetKarasu(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PuppetKarasuEntity>of(PuppetKarasuEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<PuppetSanshouoEntity>> registerPuppetSanshouo(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PuppetSanshouoEntity>of(PuppetSanshouoEntity::new, MobCategory.CREATURE)
                .sized(4.0F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<PuppetHirukoEntity>> registerPuppetHiruko(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PuppetHirukoEntity>of(PuppetHirukoEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<KarasuScrollProjectileEntity>> registerKarasuScrollProjectile(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KarasuScrollProjectileEntity>of(KarasuScrollProjectileEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<SanshouoScrollProjectileEntity>> registerSanshouoScrollProjectile(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SanshouoScrollProjectileEntity>of(SanshouoScrollProjectileEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<FutonVacuumEntity>> registerFutonVacuum(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FutonVacuumEntity>of(FutonVacuumEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<FutonGreatBreakthroughEntity>> registerFutonGreatBreakthrough(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FutonGreatBreakthroughEntity>of(FutonGreatBreakthroughEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<PoisonMistEntity>> registerPoisonMist(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PoisonMistEntity>of(PoisonMistEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<FutonChakraFlowEntity>> registerFutonChakraFlow(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FutonChakraFlowEntity>of(FutonChakraFlowEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<FuttonMistEntity>> registerFuttonMist(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<FuttonMistEntity>of(FuttonMistEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(name));
    }

    private static RegistryObject<EntityType<UnrivaledStrengthEntity>> registerUnrivaledStrength(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<UnrivaledStrengthEntity>of(UnrivaledStrengthEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<ScorchOrbEntity>> registerScorchOrb(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ScorchOrbEntity>of(ScorchOrbEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TruthSeekerBallEntity>> registerTruthSeekerBall(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TruthSeekerBallEntity>of(TruthSeekerBallEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(96)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SandBulletEntity>> registerSandBullet(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SandBulletEntity>of(SandBulletEntity::new, MobCategory.MISC)
                .sized(0.2F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SandBindEntity>> registerSandBind(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SandBindEntity>of(SandBindEntity::new, MobCategory.MISC)
                .sized(0.2F, 0.2F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SandLevitationEntity>> registerSandLevitation(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SandLevitationEntity>of(SandLevitationEntity::new, MobCategory.MISC)
                .sized(2.0F, 0.5F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SandShieldEntity>> registerSandShield(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SandShieldEntity>of(SandShieldEntity::new, MobCategory.MISC)
                .sized(3.0F, 3.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<PretaShieldEntity>> registerPretaShield(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PretaShieldEntity>of(PretaShieldEntity::new, MobCategory.MISC)
                .sized(1.2F, 2.2F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<NinjaMobEntity>> registerNinjaMob(String name) {
        return registerNinjaMob(name, 0.6F, 1.8F);
    }

    private static RegistryObject<EntityType<NinjaMobEntity>> registerNinjaMob(String name, float width, float height) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<NinjaMobEntity>of(NinjaMobEntity::new, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<HakuEntity>> registerHaku(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<HakuEntity>of(HakuEntity::new, MobCategory.MONSTER)
                .sized(0.525F, 1.75F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<WhiteZetsuEntity>> registerWhiteZetsu(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<WhiteZetsuEntity>of(WhiteZetsuEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<ItachiEntity>> registerItachi(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<ItachiEntity>of(ItachiEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<TentenEntity>> registerTenten(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<TentenEntity>of(TentenEntity::new, MobCategory.AMBIENT)
                .sized(0.525F, 1.75F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<IrukaSenseiEntity>> registerIrukaSensei(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<IrukaSenseiEntity>of(IrukaSenseiEntity::new, MobCategory.AMBIENT)
                .sized(0.6F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<SakuraHarunoEntity>> registerSakuraHaruno(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SakuraHarunoEntity>of(SakuraHarunoEntity::new, MobCategory.AMBIENT)
                .sized(0.525F, 1.75F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<MightGuyEntity>> registerMightGuy(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<MightGuyEntity>of(MightGuyEntity::new, MobCategory.AMBIENT)
                .sized(0.6F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<GiantDog2hEntity>> registerGiantDog2h(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<GiantDog2hEntity>of(GiantDog2hEntity::new, MobCategory.MONSTER)
                .sized(GiantDog2hEntity.WIDTH, GiantDog2hEntity.HEIGHT)
                .clientTrackingRange(96)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<JinchurikiCloneEntity>> registerJinchurikiClone(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<JinchurikiCloneEntity>of(JinchurikiCloneEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build(name));
    }

    private static RegistryObject<EntityType<PurpleDragonEntity>> registerPurpleDragon(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PurpleDragonEntity>of(PurpleDragonEntity::new, MobCategory.MISC)
                .sized(PurpleDragonEntity.BASE_SIZE, PurpleDragonEntity.BASE_SIZE)
                .clientTrackingRange(128)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<KingOfHellEntity>> registerKingOfHell(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<KingOfHellEntity>of(KingOfHellEntity::new, MobCategory.CREATURE)
                .sized(KingOfHellEntity.WIDTH, KingOfHellEntity.HEIGHT)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SusanooSkeletonEntity>> registerSusanooSkeleton(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SusanooSkeletonEntity>of(SusanooSkeletonEntity::new, MobCategory.CREATURE)
                .sized(SusanooSkeletonEntity.WIDTH, SusanooSkeletonEntity.HEIGHT)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SusanooClothedEntity>> registerSusanooClothed(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SusanooClothedEntity>of(SusanooClothedEntity::new, MobCategory.CREATURE)
                .sized(SusanooClothedEntity.WIDTH, SusanooClothedEntity.FULL_HEIGHT)
                .clientTrackingRange(64)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    private static RegistryObject<EntityType<SusanooWingedEntity>> registerSusanooWinged(String name) {
        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<SusanooWingedEntity>of(SusanooWingedEntity::new, MobCategory.CREATURE)
                .sized(SusanooWingedEntity.WIDTH, SusanooWingedEntity.HEIGHT)
                .clientTrackingRange(96)
                .updateInterval(1)
                .fireImmune()
                .build(name));
    }

    public static List<RegistryObject<EntityType<PortingDummyEntity>>> dummyTypes() {
        return List.of();
    }

    public static List<RegistryObject<EntityType<PortingDummyEntity>>> all() {
        return dummyTypes();
    }

    public static void touch() {
    }
}
