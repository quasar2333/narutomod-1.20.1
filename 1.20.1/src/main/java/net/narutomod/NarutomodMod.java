package net.narutomod;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.narutomod.entity.BiggerMeEntity;
import net.narutomod.entity.CrowEntity;
import net.narutomod.entity.ExplosiveClayEntity;
import net.narutomod.entity.ExplosiveCloneEntity;
import net.narutomod.entity.GamabuntaEntity;
import net.narutomod.entity.GedoStatueEntity;
import net.narutomod.entity.GiantDog2hEntity;
import net.narutomod.entity.HakuEntity;
import net.narutomod.entity.IrukaSenseiEntity;
import net.narutomod.entity.JinchurikiCloneEntity;
import net.narutomod.entity.KageBunshinEntity;
import net.narutomod.entity.KingOfHellEntity;
import net.narutomod.entity.LightningBeastEntity;
import net.narutomod.entity.LimboCloneEntity;
import net.narutomod.entity.MandaEntity;
import net.narutomod.entity.MightGuyEntity;
import net.narutomod.entity.MindTransferSelfEntity;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.entity.PuppetHirukoEntity;
import net.narutomod.entity.PuppetKarasuEntity;
import net.narutomod.entity.PuppetSanshouoEntity;
import net.narutomod.entity.ReplacementCloneEntity;
import net.narutomod.entity.AbstractSummonAnimalEntity;
import net.narutomod.entity.SakuraHarunoEntity;
import net.narutomod.entity.AbstractSusanooEntity;
import net.narutomod.entity.Snake8HeadEntity;
import net.narutomod.entity.Snake8HeadsEntity;
import net.narutomod.entity.TailedBeastEntity;
import net.narutomod.entity.TenTailsEntity;
import net.narutomod.entity.TentenEntity;
import net.narutomod.entity.TransformationJutsuEntity;
import net.narutomod.entity.WhiteZetsuEntity;
import net.narutomod.entity.WoodGolemEntity;
import net.narutomod.network.NetworkHandler;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModRegistries;
import net.narutomod.registry.ModSpawnPlacements;
import org.slf4j.Logger;

@Mod(NarutomodMod.MODID)
public final class NarutomodMod {
    public static final String MODID = "narutomod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NarutomodMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModRegistries.register(modEventBus);
        modEventBus.addListener(NarutomodModVariables::registerCapabilities);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(NarutomodModVariables.ForgeEvents.class);
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            ModSpawnPlacements.register();
        });
        LOGGER.info("Narutomod 1.20.1 port scaffold initialized");
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.BIGGERME.get(), BiggerMeEntity.createAttributes().build());
        event.put(ModEntityTypes.C_1.get(), ExplosiveClayEntity.createAttributes().build());
        event.put(ModEntityTypes.C_2.get(), ExplosiveClayEntity.createAttributes().build());
        event.put(ModEntityTypes.C_3.get(), ExplosiveClayEntity.createAttributes().build());
        event.put(ModEntityTypes.EXPLOSIVE_CLONE.get(), ExplosiveCloneEntity.createAttributes().build());
        event.put(ModEntityTypes.REPLACEMENTCLONE.get(), ReplacementCloneEntity.createAttributes().build());
        event.put(ModEntityTypes.CROW.get(), CrowEntity.createAttributes().build());
        event.put(ModEntityTypes.EIGHT_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.FIVE_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.FOUR_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.GAMABUNTA.get(), AbstractSummonAnimalEntity.createSummonAttributes().build());
        event.put(ModEntityTypes.GEDO_STATUE.get(), GedoStatueEntity.createAttributes().build());
        event.put(ModEntityTypes.GIANT_DOG_2H.get(), GiantDog2hEntity.createAttributes().build());
        event.put(ModEntityTypes.JINCHURIKI_CLONE.get(), JinchurikiCloneEntity.createAttributes().build());
        event.put(ModEntityTypes.KAGE_BUNSHIN.get(), KageBunshinEntity.createAttributes().build());
        event.put(ModEntityTypes.KINGOFHELLENTITY.get(), KingOfHellEntity.createAttributes().build());
        event.put(ModEntityTypes.LIGHTNING_BEAST.get(), LightningBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.LIMBO_CLONE.get(), LimboCloneEntity.createAttributes().build());
        event.put(ModEntityTypes.MANDA.get(), AbstractSummonAnimalEntity.createSummonAttributes().build());
        event.put(ModEntityTypes.MIGHTGUY.get(), MightGuyEntity.createAttributes().build());
        event.put(ModEntityTypes.MIND_TRANSFER_SELF.get(), MindTransferSelfEntity.createAttributes().build());
        event.put(ModEntityTypes.NINE_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.ANBU.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.GAARA.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.HAKU.get(), HakuEntity.createAttributes().build());
        event.put(ModEntityTypes.ITACHI.get(), NinjaMobEntity.createAttributes(50.0D, 100.0D, 0.5D, 10.0D, 48.0D).build());
        event.put(ModEntityTypes.IRUKA_SENSEI.get(), IrukaSenseiEntity.createAttributes().build());
        event.put(ModEntityTypes.KAKASHI.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.KANKURO.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.KISAME_HOSHIGAKI.get(), NinjaMobEntity.createLegacyKisameAttributes().build());
        event.put(ModEntityTypes.KUROTSUCHI.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.NINJA_IWA.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.NINJA_KIRI.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.NINJA_KONOHA.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.NINJA_KUMO.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.NINJA_SUNA.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.SAKURA_HARUNO.get(), SakuraHarunoEntity.createAttributes().build());
        event.put(ModEntityTypes.TEMARI.get(), NinjaMobEntity.createAttributes().build());
        event.put(ModEntityTypes.TENTEN.get(), TentenEntity.createAttributes().build());
        event.put(ModEntityTypes.WHITEZETSU.get(), WhiteZetsuEntity.createAttributes().build());
        event.put(ModEntityTypes.ZABUZA_MOMOCHI.get(), NinjaMobEntity.createLegacyZabuzaAttributes().build());
        event.put(ModEntityTypes.PUPPET_HIRUKO.get(), PuppetHirukoEntity.createAttributes().build());
        event.put(ModEntityTypes.PUPPET_KARASU.get(), PuppetKarasuEntity.createAttributes().build());
        event.put(ModEntityTypes.PUPPET_SANSHOUO.get(), PuppetSanshouoEntity.createAttributes().build());
        event.put(ModEntityTypes.ONE_TAIL.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.SEVEN_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.SIX_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.SLUG.get(), AbstractSummonAnimalEntity.createSummonAttributes().build());
        event.put(ModEntityTypes.SNAKE_8_HEAD1.get(), Snake8HeadEntity.createAttributes().build());
        event.put(ModEntityTypes.SNAKE_8_HEADS.get(), Snake8HeadsEntity.createAttributes().build());
        event.put(ModEntityTypes.SNAKE_SUMMON.get(), AbstractSummonAnimalEntity.createSummonAttributes().build());
        event.put(ModEntityTypes.SUSANOOCLOTHED.get(), AbstractSusanooEntity.createSusanooAttributes().build());
        event.put(ModEntityTypes.SUSANOOSKELETON.get(), AbstractSusanooEntity.createSusanooAttributes().build());
        event.put(ModEntityTypes.SUSANOOWINGED.get(), AbstractSusanooEntity.createSusanooAttributes().build());
        event.put(ModEntityTypes.TEN_TAILS.get(), TenTailsEntity.createAttributes().build());
        event.put(ModEntityTypes.TOAD_SUMMON.get(), AbstractSummonAnimalEntity.createSummonAttributes().build());
        event.put(ModEntityTypes.TRANSFORMATION_JUTSU.get(), TransformationJutsuEntity.createAttributes().build());
        event.put(ModEntityTypes.THREE_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.TWO_TAILS.get(), TailedBeastEntity.createAttributes().build());
        event.put(ModEntityTypes.WOOD_GOLEM.get(), WoodGolemEntity.createAttributes().build());
    }
}
