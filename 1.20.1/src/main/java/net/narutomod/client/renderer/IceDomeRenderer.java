package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.IceDomeModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.IceDomeEntity;

public final class IceDomeRenderer extends EntityRenderer<IceDomeEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dome_ice.png");
    private static final float ENTITY_SCALE = 8.0F;

    private final IceDomeModel model;

    public IceDomeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new IceDomeModel(context.bakeLayer(IceDomeModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRender(IceDomeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(IceDomeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float alpha = Mth.clamp(age / entity.getGrowAndTalkTime(), 0.0F, 1.0F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
        poseStack.scale(ENTITY_SCALE, ENTITY_SCALE, ENTITY_SCALE);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(IceDomeEntity entity) {
        return TEXTURE;
    }
}
