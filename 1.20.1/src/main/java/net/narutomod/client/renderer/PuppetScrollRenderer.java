package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PuppetScrollModel;
import net.narutomod.entity.AbstractPuppetScrollEntity;
import net.narutomod.entity.KarasuScrollProjectileEntity;
import net.narutomod.entity.SanshouoScrollProjectileEntity;

public final class PuppetScrollRenderer<T extends AbstractPuppetScrollEntity> extends EntityRenderer<T> {
    private static final ResourceLocation KARASU_TEXTURE = NarutomodMod.location("textures/scroll_karasu.png");
    private static final ResourceLocation SANSHOUO_TEXTURE = NarutomodMod.location("textures/scroll_sanshouo.png");
    private static final float MODEL_SCALE = 2.0F;

    private final ResourceLocation texture;
    private final PuppetScrollModel<T> model;

    private PuppetScrollRenderer(EntityRendererProvider.Context context, ResourceLocation texture, ModelLayerLocation layer) {
        super(context);
        this.shadowRadius = 0.1F;
        this.texture = texture;
        this.model = new PuppetScrollModel<>(context.bakeLayer(layer));
    }

    public static PuppetScrollRenderer<KarasuScrollProjectileEntity> karasu(EntityRendererProvider.Context context) {
        return new PuppetScrollRenderer<>(context, KARASU_TEXTURE, PuppetScrollModel.KARASU_LAYER_LOCATION);
    }

    public static PuppetScrollRenderer<SanshouoScrollProjectileEntity> sanshouo(EntityRendererProvider.Context context) {
        return new PuppetScrollRenderer<>(context, SANSHOUO_TEXTURE, PuppetScrollModel.SANSHOUO_LAYER_LOCATION);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ResourceLocation texture = getTextureLocation(entity);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        float age = entity.getAge() + partialTick;

        poseStack.pushPose();
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F - Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
