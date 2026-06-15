package net.narutomod.registry;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.menu.JutsuScrollMenu;
import net.narutomod.menu.MedicalScrollMenu;
import net.narutomod.menu.RasenganScrollMenu;

public final class ModMenuTypes {
    public static final RegistryObject<MenuType<JutsuScrollMenu>> JUTSU_SCROLL = ModRegistries.MENU_TYPES.register(
            "jutsu_scroll",
            () -> IForgeMenuType.create(JutsuScrollMenu::new));

    public static final RegistryObject<MenuType<RasenganScrollMenu>> RASENGAN_SCROLL = ModRegistries.MENU_TYPES.register(
            "rasengan_scroll",
            () -> new MenuType<>(RasenganScrollMenu::new, FeatureFlags.VANILLA_SET));

    public static final RegistryObject<MenuType<MedicalScrollMenu>> MEDICAL_SCROLL = ModRegistries.MENU_TYPES.register(
            "medical_scroll",
            () -> new MenuType<>(MedicalScrollMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {
    }

    public static void touch() {
    }
}
