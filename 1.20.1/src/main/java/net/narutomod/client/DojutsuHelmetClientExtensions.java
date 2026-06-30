package net.narutomod.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.model.DojutsuHelmetSnugModel;
import net.narutomod.item.ByakuganHelmetItem;

public final class DojutsuHelmetClientExtensions {
    private static DojutsuHelmetSnugModel<LivingEntity> model;

    private DojutsuHelmetClientExtensions() {
    }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                    net.minecraft.world.entity.EquipmentSlot armorSlot, HumanoidModel<?> original) {
                DojutsuHelmetSnugModel<LivingEntity> helmetModel = model();
                ArmorModelPoseHelper.copyStandardPose(original, helmetModel);
                helmetModel.headwearHidden = false;
                helmetModel.highlightHidden = false;
                helmetModel.foreheadHidden = !ByakuganHelmetItem.isRinnesharinganStack(itemStack);
                helmetModel.showHelmetOnly(ByakuganHelmetItem.isRinnesharinganStack(itemStack));
                return helmetModel;
            }
        });
    }

    private static DojutsuHelmetSnugModel<LivingEntity> model() {
        if (model == null) {
            model = new DojutsuHelmetSnugModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(DojutsuHelmetSnugModel.LAYER_LOCATION));
        }
        return model;
    }
}
