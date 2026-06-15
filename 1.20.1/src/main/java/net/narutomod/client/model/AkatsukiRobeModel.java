package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;

public final class AkatsukiRobeModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("akatsuki_robe_legacy"),
            "main"
    );

    public AkatsukiRobeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemAkatsukiRobe_ModelAkatsukiRobe_97();
    }

    public void copyStandardPoseFrom(HumanoidModel<?> original) {
        this.head.copyFrom(original.head);
        this.hat.copyFrom(original.hat);
        this.body.copyFrom(original.body);
        this.rightArm.copyFrom(original.rightArm);
        this.leftArm.copyFrom(original.leftArm);
        this.rightLeg.copyFrom(original.rightLeg);
        this.leftLeg.copyFrom(original.leftLeg);
        this.attackTime = original.attackTime;
        this.riding = original.riding;
        this.young = original.young;
        this.crouching = original.crouching;
        this.rightArmPose = original.rightArmPose;
        this.leftArmPose = original.leftArmPose;
    }

    public void configureFor(EquipmentSlot slot, boolean slimModel) {
        this.setAllVisible(false);
        this.rightArm.y = slimModel ? 2.5F : 2.0F;
        this.leftArm.y = slimModel ? 2.5F : 2.0F;
        if (slot == EquipmentSlot.HEAD) {
            this.head.visible = true;
            this.hat.visible = true;
        } else if (slot == EquipmentSlot.CHEST) {
            this.body.visible = true;
            this.rightArm.visible = true;
            this.leftArm.visible = true;
        }
    }
}
