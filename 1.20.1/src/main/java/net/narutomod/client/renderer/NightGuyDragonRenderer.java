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
import net.narutomod.client.model.NightGuyDragonModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.NightGuyDragonEntity;

public final class NightGuyDragonRenderer extends EntityRenderer<NightGuyDragonEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dragon_red.png");

    private final NightGuyDragonModel model;

    public NightGuyDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = new NightGuyDragonModel(context.bakeLayer(NightGuyDragonModel.LAYER_LOCATION));
    }

    @Override
    public void render(NightGuyDragonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getDragonScale();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float limbAmount = Mth.lerp(partialTick, entity.getPrevLimbSwingAmount(), entity.getLimbSwingAmount());
        float limbSwing = entity.getLimbSwing() - entity.getLimbSwingAmount() * (1.0F - partialTick);
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.0D, scale, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.energyFullAdditive(TEXTURE));
        this.model.setupAnim(entity, limbSwing, limbAmount, age, 0.0F, 0.0F);
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
        this.shadowRadius = 0.1F * scale;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(NightGuyDragonEntity entity) {
        return TEXTURE;
    }
}
