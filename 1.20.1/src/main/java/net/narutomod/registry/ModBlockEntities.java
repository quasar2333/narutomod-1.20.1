package net.narutomod.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.block.PortalBlockEntity;

public final class ModBlockEntities {
    public static final RegistryObject<BlockEntityType<PortalBlockEntity>> PORTALBLOCK =
            ModRegistries.BLOCK_ENTITY_TYPES.register("portalblock",
                    () -> BlockEntityType.Builder.of(PortalBlockEntity::new, ModBlocks.PORTALBLOCK.get()).build(null));

    private ModBlockEntities() {
    }

    public static void touch() {
    }
}
