package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.client.model.DojutsuHelmetSnugModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.item.SenjutsuItem;
import net.narutomod.item.SenjutsuItem.SageType;

public final class SageModeHelmetLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final DojutsuHelmetSnugModel<AbstractClientPlayer> model;

    public SageModeHelmetLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new DojutsuHelmetSnugModel<>(modelSet.bakeLayer(DojutsuHelmetSnugModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !isSageModeActive(player)) {
            return;
        }

        this.getParentModel().copyPropertiesTo(this.model);
        this.model.headwearHidden = false;
        this.model.highlightHidden = false;
        this.model.foreheadHidden = true;
        this.model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        ResourceLocation texture = getSageModeTexture(player);
        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        this.model.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer shineConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(texture));
        this.model.renderHighlight(
                poseStack,
                shineConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F);
    }

    private static boolean isSageModeActive(AbstractClientPlayer player) {
        return NarutomodModVariables.getOptional(player)
                .map(variables -> variables.getBoolean(SenjutsuItem.SAGE_MODE_ACTIVATED_TAG))
                .orElse(false);
    }

    private static ResourceLocation getSageModeTexture(AbstractClientPlayer player) {
        SageType sageType = NarutomodModVariables.getOptional(player)
                .map(variables -> SageType.byId(variables.getInt(SenjutsuItem.SAGE_TYPE_TAG)))
                .orElse(SageType.TOAD);
        if (sageType == SageType.NONE) {
            sageType = SageType.TOAD;
        }
        return NarutomodMod.location(sageType.texturePath());
    }
}
