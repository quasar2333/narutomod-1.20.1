package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.SteamArmorModel;

public final class SteamArmorClientExtensions {
    private static SteamArmorModel<LivingEntity> model;

    private SteamArmorClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                SteamArmorModel<LivingEntity> armorModel = model();
                ArmorModelPoseHelper.copyStandardPose(original, armorModel);
                return armorModel;
            }
        });
    }

    private static SteamArmorModel<LivingEntity> model() {
        if (model == null) {
            model = new SteamArmorModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(SteamArmorModel.LAYER_LOCATION));
        }
        return model;
    }
}
