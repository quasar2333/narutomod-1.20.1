package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;

public final class BoneArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("bone_armor_legacy"),
            "main"
    );

    private final ModelPart bodyBones;
    private final ModelPart rightArmBones;
    private final ModelPart leftArmBones;

    public BoneArmorModel(ModelPart root) {
        super(root);
        this.bodyBones = this.body.getChild("bodyBones");
        this.rightArmBones = this.rightArm.getChild("rightArmBones");
        this.leftArmBones = this.leftArm.getChild("leftArmBones");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemBoneArmor_ModelBoneArmor_153();
    }

    public void configureForState(boolean larchActive, boolean willowActive) {
        this.setAllVisible(false);
        this.body.visible = true;
        this.rightArm.visible = true;
        this.leftArm.visible = true;
        this.bodyBones.visible = larchActive;
        this.rightArmBones.visible = willowActive;
        this.leftArmBones.visible = willowActive;
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.bodyBones.visible = visible;
        this.rightArmBones.visible = visible;
        this.leftArmBones.visible = visible;
    }
}
