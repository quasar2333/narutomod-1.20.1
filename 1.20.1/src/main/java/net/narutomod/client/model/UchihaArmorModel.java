package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;

public final class UchihaArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("uchiha_armor_legacy"),
            "main"
    );

    public UchihaArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemUchiha_ModelArmorCustom_105();
    }

    public void configureArmPivot(boolean slimModel) {
        this.rightArm.y = slimModel ? 2.5F : 2.0F;
        this.leftArm.y = slimModel ? 2.5F : 2.0F;
    }
}
