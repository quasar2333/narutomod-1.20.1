package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.MeltingJutsuModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.MeltingJutsuEntity;

public final class MeltingJutsuRenderer extends EntityRenderer<MeltingJutsuEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/lava.png");

    private final MeltingJutsuModel model;

    public MeltingJutsuRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
        this.model = new MeltingJutsuModel(context.bakeLayer(MeltingJutsuModel.LAYER_LOCATION));
    }

    @Override
    public void render(MeltingJutsuEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getScale();
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, 0.0F, 0.01F));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(MeltingJutsuEntity entity) {
        return TEXTURE;
    }
}
