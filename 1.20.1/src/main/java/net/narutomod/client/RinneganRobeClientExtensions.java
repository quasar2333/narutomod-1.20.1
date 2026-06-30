package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.RinneganRobeModel;

public final class RinneganRobeClientExtensions {
    private static RinneganRobeModel<LivingEntity> model;

    private RinneganRobeClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                RinneganRobeModel<LivingEntity> armorModel = model();
                armorModel.copyStandardPoseFrom(original);
                armorModel.configureFor(equipmentSlot);
                return armorModel;
            }
        });
    }

    private static RinneganRobeModel<LivingEntity> model() {
        if (model == null) {
            model = new RinneganRobeModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(RinneganRobeModel.LAYER_LOCATION));
        }
        return model;
    }
}
