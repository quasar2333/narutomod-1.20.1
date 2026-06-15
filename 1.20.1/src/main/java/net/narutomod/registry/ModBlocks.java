package net.narutomod.registry;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.block.KamuiBlock;
import net.narutomod.block.LightSourceBlock;
import net.narutomod.block.PortalBlock;
import net.narutomod.block.WaterStillBlock;
import net.narutomod.event.SpecialEvent;

public final class ModBlocks {
    public static final RegistryObject<Block> AMATERASUBLOCK = registerAmaterasuBlock("amaterasublock");
    public static final RegistryObject<Item> AMATERASUBLOCK_ITEM = registerBlockItem("amaterasublock", AMATERASUBLOCK);
    public static final RegistryObject<Block> EXPLOSIVE_TAG = registerBlock("explosive_tag");
    public static final RegistryObject<Item> EXPLOSIVE_TAG_ITEM = registerBlockItem("explosive_tag", EXPLOSIVE_TAG);
    public static final RegistryObject<Block> KAMUIBLOCK = registerKamuiBlock("kamuiblock");
    public static final RegistryObject<Item> KAMUIBLOCK_ITEM = registerBlockItem("kamuiblock", KAMUIBLOCK);
    public static final RegistryObject<Block> LIGHT_SOURCE = registerLightSourceBlock("light_source");
    public static final RegistryObject<Item> LIGHT_SOURCE_ITEM = registerBlockItem("light_source", LIGHT_SOURCE);
    public static final RegistryObject<Block> METEOR = registerMeteorBlock("meteor");
    public static final RegistryObject<Item> METEOR_ITEM = registerBlockItem("meteor", METEOR);
    public static final RegistryObject<Block> METEORITE = registerBlock("meteorite");
    public static final RegistryObject<Item> METEORITE_ITEM = registerBlockItem("meteorite", METEORITE);
    public static final RegistryObject<Block> MUD = registerMudBlock("mud");
    public static final RegistryObject<Item> MUD_ITEM = registerBlockItem("mud", MUD);
    public static final RegistryObject<Block> PORTALBLOCK = registerPortalBlock("portalblock");
    public static final RegistryObject<Item> PORTALBLOCK_ITEM = registerBlockItem("portalblock", PORTALBLOCK);
    public static final RegistryObject<Block> WATER_STILL = registerWaterStillBlock("water_still");
    public static final RegistryObject<Item> WATER_STILL_ITEM = registerBlockItem("water_still", WATER_STILL);

    private ModBlocks() {
    }

    private static RegistryObject<Block> registerBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.0F, 6.0F)
                .noOcclusion()));
    }

    private static RegistryObject<Block> registerAmaterasuBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new AmaterasuBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 15)
                .noLootTable()
                .noOcclusion()
                .noCollission()));
    }

    private static RegistryObject<Block> registerLightSourceBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new LightSourceBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 15)
                .noLootTable()
                .noOcclusion()
                .noCollission()));
    }

    private static RegistryObject<Block> registerMudBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new MudBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIRT)
                .strength(1.0F, 6.0F)
                .noOcclusion()));
    }

    private static RegistryObject<Block> registerWaterStillBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new WaterStillBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .strength(100.0F, 5.0F)
                .noLootTable()
                .noCollission()
                .noOcclusion()));
    }

    private static RegistryObject<Block> registerMeteorBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new MeteorBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 64000.0F)
                .lightLevel(state -> 15)
                .noLootTable()
                .noOcclusion()));
    }

    private static RegistryObject<Block> registerKamuiBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new KamuiBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()));
    }

    private static RegistryObject<Block> registerPortalBlock(String name) {
        return ModRegistries.BLOCKS.register(name, () -> new PortalBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 11)
                .noOcclusion()
                .noCollission()));
    }

    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {
        return ModRegistries.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static List<RegistryObject<Item>> blockItems() {
        return List.of(
                AMATERASUBLOCK_ITEM,
                EXPLOSIVE_TAG_ITEM,
                KAMUIBLOCK_ITEM,
                LIGHT_SOURCE_ITEM,
                METEOR_ITEM,
                METEORITE_ITEM,
                MUD_ITEM,
                PORTALBLOCK_ITEM,
                WATER_STILL_ITEM
        );
    }

    public static void touch() {
    }

    private static final class MudBlock extends Block {
        private MudBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            entity.clearFire();
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
            }
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x() * -0.1D, -0.01D, motion.z() * -0.1D);
            entity.hasImpulse = true;
            if (!level.isClientSide
                    && level.getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(MUD.get())) {
                entity.hurt(entity.damageSources().drown(), 2.0F);
            }
        }
    }

    private static final class AmaterasuBlock extends Block {
        private static final int EFFECT_DURATION = 10000;
        private static final int FIRE_SECONDS = 10000;

        private AmaterasuBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), EFFECT_DURATION, 0, false, false));
            }
            entity.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    private static final class MeteorBlock extends FallingBlock {
        private static final int EXPLOSION_RADIUS = 12;
        private static final double DAMAGE_RADIUS = 12.0D;

        private MeteorBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
            super.onLand(level, pos, state, replaceableState, fallingBlock);
            impact(level, pos, fallingBlock);
        }

        @Override
        public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
            super.onBrokenAfterFall(level, pos, fallingBlock);
            impact(level, pos, fallingBlock);
        }

        private void impact(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            SpecialEvent.setSphericalExplosionEvent(serverLevel, null, pos.getX(), pos.getY() - 1, pos.getZ(),
                    EXPLOSION_RADIUS, serverLevel.getGameTime(), true, 0.33F, true, true, true);
            Vec3 center = Vec3.atCenterOf(pos);
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(DAMAGE_RADIUS),
                    LivingEntity::isAlive)) {
                double distance = Math.max(target.distanceToSqr(center), 1.0D);
                float damage = (float) Math.max(6.0D, 30.0D - Math.sqrt(distance) * 2.0D);
                target.invulnerableTime = 0;
                target.hurt(serverLevel.damageSources().fallingBlock(fallingBlock), damage);
                Vec3 knockback = target.position().subtract(center).normalize().scale(1.5D);
                target.push(knockback.x(), 0.45D, knockback.z());
                target.hurtMarked = true;
            }
            serverLevel.setBlock(pos, METEORITE.get().defaultBlockState(), 3);
        }
    }
}
