package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.narutomod.item.EightGatesItem;

public final class EightGatesPlayerOverlayLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float LEGACY_GATE_OVERLAY_THRESHOLD = 3.0F;
    private static final float LEGACY_GATE_OVERLAY_RED = 176.0F / 255.0F;
    private static final float LEGACY_GATE_OVERLAY_ALPHA = 176.0F / 255.0F;

    public EightGatesPlayerOverlayLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (player.isInvisible() || getOpenedGate(player) < LEGACY_GATE_OVERLAY_THRESHOLD) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        this.getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                LEGACY_GATE_OVERLAY_RED, 0.0F, 0.0F, LEGACY_GATE_OVERLAY_ALPHA);
    }

    private static float getOpenedGate(AbstractClientPlayer player) {
        return Math.max(getOpenedGate(player.getMainHandItem()), getOpenedGate(player.getOffhandItem()));
    }

    private static float getOpenedGate(ItemStack stack) {
        return stack.getItem() instanceof EightGatesItem item ? item.getGateOpened(stack) : 0.0F;
    }
}
