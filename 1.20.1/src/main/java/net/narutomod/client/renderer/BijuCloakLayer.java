package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.narutomod.client.model.BijuCloakModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.AbstractSusanooEntity;
import net.narutomod.item.BijuCloakItem;

public final class BijuCloakLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final BijuCloakModel<T> model;

    public BijuCloakLayer(RenderLayerParent<T, M> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new BijuCloakModel<>(modelSet.bakeLayer(BijuCloakModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        if (entity.getVehicle() instanceof AbstractSusanooEntity susanoo && susanoo.isOwnedBy(entity)) {
            return;
        }

        ItemStack chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!BijuCloakItem.isBijuCloak(chestStack)) {
            return;
        }

        int tails = BijuCloakItem.getTails(chestStack);
        if (!BijuCloakItem.hasFullSet(entity, tails)) {
            return;
        }

        this.getParentModel().copyPropertiesTo(this.model);
        this.model.configureTailVisibility(tails);
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float alpha = Mth.clamp((float) BijuCloakItem.getWearingTicks(entity) / 80.0F, 0.0F, 1.0F);
        ResourceLocation texture = BijuCloakItem.getCloakTexture(chestStack);
        int baseLight = BijuCloakItem.hasKuramaShine(chestStack) ? LightTexture.FULL_BRIGHT : packedLight;

        VertexConsumer baseConsumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingEntityTranslucent(texture, 0.0F, 0.01F));
        this.model.renderBase(poseStack, baseConsumer, baseLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);

        VertexConsumer overlayConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(texture));
        this.model.renderOverlay(poseStack, overlayConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }
}
