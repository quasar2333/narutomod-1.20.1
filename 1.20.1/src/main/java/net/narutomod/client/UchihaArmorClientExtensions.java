package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.UchihaArmorModel;

public final class UchihaArmorClientExtensions {
    private static UchihaArmorModel<LivingEntity> model;

    private UchihaArmorClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                UchihaArmorModel<LivingEntity> armorModel = model();
                boolean slim = livingEntity instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
                ArmorModelPoseHelper.copyStandardPose(original, armorModel);
                armorModel.configureArmPivot(slim);
                return armorModel;
            }
        });
    }

    private static UchihaArmorModel<LivingEntity> model() {
        if (model == null) {
            model = new UchihaArmorModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(UchihaArmorModel.LAYER_LOCATION));
        }
        return model;
    }
}
