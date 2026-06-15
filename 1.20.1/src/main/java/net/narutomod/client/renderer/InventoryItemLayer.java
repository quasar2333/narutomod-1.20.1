package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import net.narutomod.client.ClientInventoryTrackerSync;

public final class InventoryItemLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public InventoryItemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) {
            renderLocalInventory(poseStack, bufferSource, packedLight, player);
        } else {
            renderTrackedInventory(poseStack, bufferSource, packedLight, player);
        }
    }

    private void renderLocalInventory(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player) {
        List<ItemStack> items = player.getInventory().items;
        int selectedSlot = player.getInventory().selected;
        for (int index = 0; index < items.size(); index++) {
            if (index != selectedSlot) {
                BodyMountedItemRenderer.renderStack(poseStack, bufferSource, packedLight, player, getParentModel(), items.get(index));
            }
        }
    }

    private void renderTrackedInventory(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player) {
        int selectedSlot = ClientInventoryTrackerSync.getSelectedSlot(player);
        for (Map.Entry<Integer, ItemStack> entry : ClientInventoryTrackerSync.getSlots(player).entrySet()) {
            if (entry.getKey() != selectedSlot) {
                BodyMountedItemRenderer.renderStack(poseStack, bufferSource, packedLight, player, getParentModel(), entry.getValue());
            }
        }
    }
}
