package net.narutomod.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.PortingDummyEntity;
import net.narutomod.item.PortingMarkerItem;
import net.narutomod.world.KamuiChunkGenerator;

public final class ModRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NarutomodMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NarutomodMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NarutomodMod.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NarutomodMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NarutomodMod.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, NarutomodMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, NarutomodMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NarutomodMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, NarutomodMod.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NarutomodMod.MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, NarutomodMod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NarutomodMod.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, NarutomodMod.MODID);

    public static final RegistryObject<Item> PORTING_MARKER = ITEMS.register("porting_marker", PortingMarkerItem::new);
    public static final RegistryObject<EntityType<PortingDummyEntity>> PORTING_DUMMY = ENTITY_TYPES.register("porting_dummy", () ->
            EntityType.Builder.<PortingDummyEntity>of(PortingDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("porting_dummy"));
    public static final RegistryObject<CreativeModeTab> NARUTO_TAB = CREATIVE_MODE_TABS.register("narutomod", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.narutomod"))
            .icon(() -> PORTING_MARKER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PORTING_MARKER.get());
                ModBlocks.blockItems().forEach(item -> output.accept(item.get()));
                ModItems.all().forEach(item -> output.accept(item.get()));
            })
            .build());
    public static final RegistryObject<Codec<? extends ChunkGenerator>> KAMUI_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("kamui_chunk_generator", () -> KamuiChunkGenerator.CODEC);

    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ModBlocks.touch();
        ModBlockEntities.touch();
        ModItems.touch();
        ModEntityTypes.touch();
        ModEffects.touch();
        ModMenuTypes.touch();
        ModParticleTypes.touch();
        ModSounds.touch();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        ATTRIBUTES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);
    }
}
