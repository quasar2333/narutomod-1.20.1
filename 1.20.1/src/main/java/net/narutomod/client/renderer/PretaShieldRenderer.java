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
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PretaShieldModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.PretaShieldEntity;

public final class PretaShieldRenderer extends EntityRenderer<PretaShieldEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/electric_armor.png");
    private static final float HEIGHT = 2.25F;
    private static final float ALPHA = 0.2F;

    private final PretaShieldModel model;

    public PretaShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new PretaShieldModel(context.bakeLayer(PretaShieldModel.LAYER_LOCATION));
    }

    @Override
    public void render(PretaShieldEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float warmup = entity.getRenderScale(partialTick);
        if (warmup <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.translate(0.0D, HEIGHT * 0.5D, 0.0D);
        poseStack.scale(warmup, warmup, warmup);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, 0.04F, 0.02F));
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderCentered(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                ALPHA);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(PretaShieldEntity entity) {
        return TEXTURE;
    }
}
