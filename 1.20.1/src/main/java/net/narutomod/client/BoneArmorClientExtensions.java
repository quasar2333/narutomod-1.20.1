package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.BoneArmorModel;
import net.narutomod.item.BoneArmorItem;

public final class BoneArmorClientExtensions {
    private static BoneArmorModel<LivingEntity> model;

    private BoneArmorClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                BoneArmorModel<LivingEntity> armorModel = model();
                ArmorModelPoseHelper.copyStandardPose(original, armorModel);
                armorModel.configureForState(BoneArmorItem.isLarchActive(itemStack), BoneArmorItem.isWillowActive(itemStack));
                return armorModel;
            }
        });
    }

    private static BoneArmorModel<LivingEntity> model() {
        if (model == null) {
            model = new BoneArmorModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(BoneArmorModel.LAYER_LOCATION));
        }
        return model;
    }
}
