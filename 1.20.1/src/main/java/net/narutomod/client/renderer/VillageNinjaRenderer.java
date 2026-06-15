package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.VillageNinjaModel;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.registry.ModEntityTypes;

public final class VillageNinjaRenderer extends MobRenderer<NinjaMobEntity, VillageNinjaModel> {
    private static final ResourceLocation IWA = NarutomodMod.location("textures/ninja_iwa.png");
    private static final ResourceLocation KIRI = NarutomodMod.location("textures/ninja_kiri.png");
    private static final ResourceLocation KONOHA = NarutomodMod.location("textures/ninja_konoha.png");
    private static final ResourceLocation KUMO = NarutomodMod.location("textures/ninja_kumo.png");
    private static final ResourceLocation SUNA = NarutomodMod.location("textures/ninja_suna.png");

    public VillageNinjaRenderer(EntityRendererProvider.Context context) {
        super(context, new VillageNinjaModel(context.bakeLayer(VillageNinjaModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        if (entity.getType() == ModEntityTypes.NINJA_KIRI.get()) {
            return KIRI;
        }
        if (entity.getType() == ModEntityTypes.NINJA_KONOHA.get()) {
            return KONOHA;
        }
        if (entity.getType() == ModEntityTypes.NINJA_KUMO.get()) {
            return KUMO;
        }
        if (entity.getType() == ModEntityTypes.NINJA_SUNA.get()) {
            return SUNA;
        }
        return IWA;
    }
}
