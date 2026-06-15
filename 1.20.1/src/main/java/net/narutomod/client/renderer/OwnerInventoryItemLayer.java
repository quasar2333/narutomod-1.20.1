package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutomod.client.ClientInventoryTrackerSync;

public final class OwnerInventoryItemLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final Function<T, LivingEntity> ownerResolver;

    public OwnerInventoryItemLayer(RenderLayerParent<T, M> parent, Function<T, LivingEntity> ownerResolver) {
        super(parent);
        this.ownerResolver = ownerResolver;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        LivingEntity owner = this.ownerResolver.apply(entity);
        if (!(owner instanceof Player player)) {
            return;
        }

        ItemStack heldStack = entity.getMainHandItem();
        if (player == Minecraft.getInstance().player) {
            renderLocalOwnerInventory(poseStack, bufferSource, packedLight, entity, player, heldStack);
        } else {
            renderTrackedOwnerInventory(poseStack, bufferSource, packedLight, entity, player, heldStack);
        }
    }

    private void renderLocalOwnerInventory(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                                           Player owner, ItemStack heldStack) {
        List<ItemStack> items = owner.getInventory().items;
        for (ItemStack stack : items) {
            renderOwnerStack(poseStack, bufferSource, packedLight, entity, heldStack, stack);
        }
    }

    private void renderTrackedOwnerInventory(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                                             Player owner, ItemStack heldStack) {
        for (Map.Entry<Integer, ItemStack> entry : ClientInventoryTrackerSync.getSlots(owner).entrySet()) {
            renderOwnerStack(poseStack, bufferSource, packedLight, entity, heldStack, entry.getValue());
        }
    }

    private void renderOwnerStack(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                                  ItemStack heldStack, ItemStack stack) {
        if (!stack.isEmpty() && !ItemStack.isSameItem(heldStack, stack)) {
            BodyMountedItemRenderer.renderStack(poseStack, bufferSource, packedLight, entity, getParentModel(), stack);
        }
    }
}
