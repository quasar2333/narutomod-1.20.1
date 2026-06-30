package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SlugModel;
import net.narutomod.entity.SlugSummonEntity;

public final class SlugSummonRenderer extends MobRenderer<SlugSummonEntity, SlugModel<SlugSummonEntity>> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/slug.png");

    public SlugSummonRenderer(EntityRendererProvider.Context context) {
        super(context, new SlugModel<>(context.bakeLayer(SlugModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public void render(SlugSummonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isVisibleSummon()) {
            return;
        }
        this.shadowRadius = 0.5F * entity.getSummonScale();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(SlugSummonEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getSummonScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(SlugSummonEntity entity) {
        return TEXTURE;
    }
}
