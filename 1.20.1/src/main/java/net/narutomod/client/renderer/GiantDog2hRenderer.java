package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.GiantDog2hModel;
import net.narutomod.entity.GiantDog2hEntity;

public final class GiantDog2hRenderer extends MobRenderer<GiantDog2hEntity, GiantDog2hModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dog.png");

    public GiantDog2hRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantDog2hModel(context.bakeLayer(GiantDog2hModel.LAYER_LOCATION)), 0.5F * GiantDog2hEntity.ENTITY_SCALE);
    }

    @Override
    protected void scale(GiantDog2hEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(GiantDog2hEntity.ENTITY_SCALE, GiantDog2hEntity.ENTITY_SCALE, GiantDog2hEntity.ENTITY_SCALE);
        poseStack.translate(0.0F, 1.5F - GiantDog2hEntity.ENTITY_SCALE * 1.5F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantDog2hEntity entity) {
        return TEXTURE;
    }
}
