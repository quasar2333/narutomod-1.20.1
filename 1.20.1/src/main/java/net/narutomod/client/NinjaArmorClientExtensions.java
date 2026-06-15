package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.NinjaArmorModel;
import net.narutomod.item.NinjaArmorItem;

public final class NinjaArmorClientExtensions {
    private static NinjaArmorModel<LivingEntity> model;

    private NinjaArmorClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                NinjaArmorModel<LivingEntity> armorModel = model();
                NinjaArmorItem item = (NinjaArmorItem) itemStack.getItem();
                boolean slim = livingEntity instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
                armorModel.copyStandardPoseFrom(original);
                armorModel.configureFor(item.getStyle(), equipmentSlot, slim);
                return armorModel;
            }
        });
    }

    private static NinjaArmorModel<LivingEntity> model() {
        if (model == null) {
            model = new NinjaArmorModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(NinjaArmorModel.LAYER_LOCATION));
        }
        return model;
    }
}
