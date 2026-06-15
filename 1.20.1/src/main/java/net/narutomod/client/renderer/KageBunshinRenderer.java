package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.entity.KageBunshinEntity;

public final class KageBunshinRenderer extends HumanoidMobRenderer<KageBunshinEntity, PlayerModel<KageBunshinEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private final PlayerModel<KageBunshinEntity> wideModel;
    private final PlayerModel<KageBunshinEntity> slimModel;

    public KageBunshinRenderer(EntityRendererProvider.Context context) {
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
        this.addLayer(new OwnerInventoryItemLayer<>(this, KageBunshinEntity::getOwner));
    }

    @Override
    public void render(KageBunshinEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        this.model = PlayerSkinTextures.isSlimModel(entity.getOwner()) ? this.slimModel : this.wideModel;
        CloneArmPoses.apply(this.model, entity);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(KageBunshinEntity entity, PoseStack poseStack, float partialTick) {
        CloneRenderScales.applyOwnerPlayerScale(entity.getOwner(), poseStack);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(KageBunshinEntity entity) {
        return PlayerSkinTextures.textureOrDefault(entity.getOwner(), DEFAULT_TEXTURE);
    }
}
