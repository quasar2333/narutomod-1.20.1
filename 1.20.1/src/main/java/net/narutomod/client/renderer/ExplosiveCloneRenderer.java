package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.entity.ExplosiveCloneEntity;

public final class ExplosiveCloneRenderer extends HumanoidMobRenderer<ExplosiveCloneEntity, PlayerModel<ExplosiveCloneEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private final PlayerModel<ExplosiveCloneEntity> wideModel;
    private final PlayerModel<ExplosiveCloneEntity> slimModel;

    public ExplosiveCloneRenderer(EntityRendererProvider.Context context) {
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
        this.addLayer(new OwnerInventoryItemLayer<>(this, ExplosiveCloneEntity::getOwner));
    }

    @Override
    public void render(ExplosiveCloneEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        this.model = PlayerSkinTextures.isSlimModel(entity.getOwner()) ? this.slimModel : this.wideModel;
        CloneArmPoses.apply(this.model, entity);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(ExplosiveCloneEntity entity, PoseStack poseStack, float partialTick) {
        CloneRenderScales.applyOwnerPlayerScale(entity.getOwner(), poseStack);
        if (entity.isIgnited()) {
            float fuse = Mth.clamp(entity.getIgnitionProgress(partialTick), 0.0F, 1.0F);
            float pulse = 1.0F + Mth.sin(fuse * 100.0F) * fuse * 0.01F;
            float eased = fuse * fuse;
            eased *= eased;
            float horizontal = (1.0F + eased * 0.4F) * pulse;
            float vertical = (1.0F + eased * 0.1F) / pulse;
            poseStack.scale(horizontal, vertical, horizontal);
        }
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(ExplosiveCloneEntity entity) {
        return PlayerSkinTextures.textureOrDefault(entity.getOwner(), DEFAULT_TEXTURE);
    }
}
