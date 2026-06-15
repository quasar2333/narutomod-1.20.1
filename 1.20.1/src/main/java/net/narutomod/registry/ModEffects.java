package net.narutomod.registry;

import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.effect.AmaterasuFlameEffect;
import net.narutomod.effect.ChakraEnhancedStrengthEffect;
import net.narutomod.effect.ChakraRegenerationEffect;
import net.narutomod.effect.CorrosionEffect;
import net.narutomod.effect.FeatherFallingEffect;
import net.narutomod.effect.FlightEffect;
import net.narutomod.effect.HeavinessEffect;
import net.narutomod.effect.ParalysisEffect;
import net.narutomod.effect.ReachEffect;

public final class ModEffects {
    public static final RegistryObject<MobEffect> AMATERASUFLAME = ModRegistries.MOB_EFFECTS.register(
            "amaterasuflame", AmaterasuFlameEffect::new);
    public static final RegistryObject<MobEffect> CHAKRA_ENHANCED_STRENGTH = ModRegistries.MOB_EFFECTS.register(
            "chakra_enhanced_strength", ChakraEnhancedStrengthEffect::new);
    public static final RegistryObject<MobEffect> CHAKRA_REGENERATION = ModRegistries.MOB_EFFECTS.register(
            "chakra_regeneration", ChakraRegenerationEffect::new);
    public static final RegistryObject<MobEffect> CORROSION = ModRegistries.MOB_EFFECTS.register("corrosion", CorrosionEffect::new);
    public static final RegistryObject<MobEffect> FEATHER_FALLING = ModRegistries.MOB_EFFECTS.register(
            "feather_falling", FeatherFallingEffect::new);
    public static final RegistryObject<MobEffect> FLIGHT = ModRegistries.MOB_EFFECTS.register("flight", FlightEffect::new);
    public static final RegistryObject<MobEffect> HEAVINESS = ModRegistries.MOB_EFFECTS.register("heaviness", HeavinessEffect::new);
    public static final RegistryObject<MobEffect> PARALYSIS = ModRegistries.MOB_EFFECTS.register("paralysis", ParalysisEffect::new);
    public static final RegistryObject<MobEffect> REACH = ModRegistries.MOB_EFFECTS.register("reach", ReachEffect::new);

    private ModEffects() {
    }

    public static List<RegistryObject<MobEffect>> all() {
        return List.of(
                AMATERASUFLAME,
                CHAKRA_ENHANCED_STRENGTH,
                CHAKRA_REGENERATION,
                CORROSION,
                FEATHER_FALLING,
                FLIGHT,
                HEAVINESS,
                PARALYSIS,
                REACH
        );
    }

    public static void touch() {
    }
}
