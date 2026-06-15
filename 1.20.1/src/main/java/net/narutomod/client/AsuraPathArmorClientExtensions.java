package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.AsuraPathArmorModel;

public final class AsuraPathArmorClientExtensions {
    private static AsuraPathArmorModel<LivingEntity> model;

    private AsuraPathArmorClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                AsuraPathArmorModel<LivingEntity> armorModel = model();
                ArmorModelPoseHelper.copyStandardPose(original, armorModel);
                armorModel.configureForChestplate();
                return armorModel;
            }
        });
    }

    private static AsuraPathArmorModel<LivingEntity> model() {
        if (model == null) {
            model = new AsuraPathArmorModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(AsuraPathArmorModel.LAYER_LOCATION));
        }
        return model;
    }
}
