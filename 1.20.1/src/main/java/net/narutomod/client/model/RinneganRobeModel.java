package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;

public final class RinneganRobeModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("rinnegan_robe_legacy"),
            "main"
    );

    public RinneganRobeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemRinnegan_ModelSizPathRobe_316();
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

    public void configureFor(EquipmentSlot slot) {
        this.head.visible = false;
        this.hat.visible = false;
        this.body.visible = slot == EquipmentSlot.CHEST;
        this.rightArm.visible = slot == EquipmentSlot.CHEST;
        this.leftArm.visible = slot == EquipmentSlot.CHEST;
        this.rightLeg.visible = slot == EquipmentSlot.LEGS;
        this.leftLeg.visible = slot == EquipmentSlot.LEGS;
    }
}
