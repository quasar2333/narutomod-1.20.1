package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.narutomod.entity.LimboCloneEntity;

public final class LimboCloneRenderer extends HumanoidMobRenderer<LimboCloneEntity, PlayerModel<LimboCloneEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private final PlayerModel<LimboCloneEntity> wideModel;
    private final PlayerModel<LimboCloneEntity> slimModel;

    public LimboCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new ClonePlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
        this.wideModel = this.model;
        this.slimModel = new ClonePlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
        this.addLayer(new OwnerInventoryItemLayer<>(this, LimboCloneEntity::getOwner));
    }

    @Override
    public void render(LimboCloneEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null || !entity.canBeDetectedBy(cameraEntity)) {
            return;
        }
        this.model = PlayerSkinTextures.isSlimModel(entity.getOwner()) ? this.slimModel : this.wideModel;
        CloneArmPoses.apply(this.model, entity);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(LimboCloneEntity entity, PoseStack poseStack, float partialTick) {
        CloneRenderScales.applyOwnerPlayerScale(entity.getOwner(), poseStack);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(LimboCloneEntity entity) {
        return PlayerSkinTextures.textureOrDefault(entity.getOwner(), DEFAULT_TEXTURE);
    }

    @Override
    protected RenderType getRenderType(LimboCloneEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (bodyVisible) {
            return RenderType.entityTranslucent(getTextureLocation(entity));
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }
}
