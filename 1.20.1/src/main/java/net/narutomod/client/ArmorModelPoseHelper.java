package net.narutomod.client;

import net.minecraft.client.model.HumanoidModel;

public final class ArmorModelPoseHelper {
    private ArmorModelPoseHelper() {
    }

    public static void copyStandardPose(HumanoidModel<?> source, HumanoidModel<?> target) {
        target.head.copyFrom(source.head);
        target.hat.copyFrom(source.hat);
        target.body.copyFrom(source.body);
        target.rightArm.copyFrom(source.rightArm);
        target.leftArm.copyFrom(source.leftArm);
        target.rightLeg.copyFrom(source.rightLeg);
        target.leftLeg.copyFrom(source.leftLeg);
        target.attackTime = source.attackTime;
        target.riding = source.riding;
        target.young = source.young;
        target.crouching = source.crouching;
        target.rightArmPose = source.rightArmPose;
        target.leftArmPose = source.leftArmPose;
    }
}
