package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class ClonePlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    private static final float HELD_ITEM_Y_OFFSET = 0.1875F;

    public ClonePlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        super.translateToHand(side, poseStack);
        poseStack.translate(0.0F, HELD_ITEM_Y_OFFSET, 0.0F);
    }
}
