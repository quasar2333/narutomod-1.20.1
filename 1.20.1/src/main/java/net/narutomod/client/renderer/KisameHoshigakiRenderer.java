package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KisameHoshigakiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class KisameHoshigakiRenderer extends MobRenderer<NinjaMobEntity, KisameHoshigakiModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/kisame.png");
    private static final ResourceLocation FUSED_TEXTURE = NarutomodMod.location("textures/kisamefinal.png");

    public KisameHoshigakiRenderer(EntityRendererProvider.Context context) {
        super(context, new KisameHoshigakiModel(context.bakeLayer(KisameHoshigakiModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new KisameStoredItemLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return entity.isLegacyKisameFusedForRender() ? FUSED_TEXTURE : TEXTURE;
    }

    @Override
    protected void scale(NinjaMobEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.0625F, 1.0625F, 1.0625F);
    }
}
