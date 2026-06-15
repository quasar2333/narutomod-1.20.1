package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.AkatsukiRobeModel;

public final class AkatsukiRobeClientExtensions {
    private static AkatsukiRobeModel<LivingEntity> model;

    private AkatsukiRobeClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                AkatsukiRobeModel<LivingEntity> armorModel = model();
                boolean slim = livingEntity instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
                armorModel.copyStandardPoseFrom(original);
                armorModel.configureFor(equipmentSlot, slim);
                return armorModel;
            }
        });
    }

    private static AkatsukiRobeModel<LivingEntity> model() {
        if (model == null) {
            model = new AkatsukiRobeModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(AkatsukiRobeModel.LAYER_LOCATION));
        }
        return model;
    }
}
