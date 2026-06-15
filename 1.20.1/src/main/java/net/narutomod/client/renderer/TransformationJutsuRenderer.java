package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.narutomod.entity.TransformationJutsuEntity;

public final class TransformationJutsuRenderer extends HumanoidMobRenderer<TransformationJutsuEntity, PlayerModel<TransformationJutsuEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private final PlayerModel<TransformationJutsuEntity> wideModel;
    private final PlayerModel<TransformationJutsuEntity> slimModel;

    public TransformationJutsuRenderer(EntityRendererProvider.Context context) {
        super(context, new ClonePlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = this.model;
        this.slimModel = new ClonePlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
    }

    @Override
    public void render(TransformationJutsuEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        LivingEntity target = entity.getDisguiseTarget();
        if (target != null && !(target instanceof Player)) {
            renderNonPlayerTarget(entity, target, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        this.model = PlayerSkinTextures.isSlimModel(target) ? this.slimModel : this.wideModel;
        CloneArmPoses.apply(this.model, entity);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderNonPlayerTarget(TransformationJutsuEntity entity, LivingEntity target, float entityYaw,
                                       float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        float oldYRot = target.getYRot();
        float oldYRotO = target.yRotO;
        float oldXRot = target.getXRot();
        float oldXRotO = target.xRotO;
        float oldHeadRot = target.yHeadRot;
        float oldHeadRotO = target.yHeadRotO;
        float oldBodyRot = target.yBodyRot;
        float oldBodyRotO = target.yBodyRotO;
        float renderYRot = entity.getYRot();
        float renderXRot = entity.getXRot();
        poseStack.pushPose();
        try {
            target.setYRot(renderYRot);
            target.yRotO = entity.yRotO;
            target.setXRot(renderXRot);
            target.xRotO = entity.xRotO;
            target.setYHeadRot(entity.getYHeadRot());
            target.yHeadRotO = entity.yHeadRotO;
            target.yBodyRot = entity.yBodyRot;
            target.yBodyRotO = entity.yBodyRotO;
            dispatcher.render(target, 0.0D, 0.0D, 0.0D, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            target.setYRot(oldYRot);
            target.yRotO = oldYRotO;
            target.setXRot(oldXRot);
            target.xRotO = oldXRotO;
            target.setYHeadRot(oldHeadRot);
            target.yHeadRotO = oldHeadRotO;
            target.yBodyRot = oldBodyRot;
            target.yBodyRotO = oldBodyRotO;
            poseStack.popPose();
        }
    }

    @Override
    protected void scale(TransformationJutsuEntity entity, PoseStack poseStack, float partialTick) {
        CloneRenderScales.applyOwnerPlayerScale(entity.getDisguiseTarget(), poseStack);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(TransformationJutsuEntity entity) {
        LivingEntity target = entity.getDisguiseTarget();
        return PlayerSkinTextures.textureOrDefault(target, DEFAULT_TEXTURE);
    }
}
