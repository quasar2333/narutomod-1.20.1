package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PuppetSanshouoModel;
import net.narutomod.entity.PuppetSanshouoEntity;

public final class PuppetSanshouoRenderer extends MobRenderer<PuppetSanshouoEntity, PuppetSanshouoModel> {
    private static final float MODEL_SCALE = 2.0F;
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/sanshouo.png");

    public PuppetSanshouoRenderer(EntityRendererProvider.Context context) {
        super(context, new PuppetSanshouoModel(context.bakeLayer(PuppetSanshouoModel.LAYER_LOCATION)), 0.5F * MODEL_SCALE);
        this.addLayer(new PuppetChakraStringsLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PuppetSanshouoEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(PuppetSanshouoEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0D, 1.5D - (1.5D * MODEL_SCALE), 0.0D);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    protected boolean shouldShowName(PuppetSanshouoEntity entity) {
        return false;
    }
}
