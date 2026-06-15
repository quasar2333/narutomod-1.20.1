package net.narutomod.registry;

import java.util.List;
import net.minecraft.core.particles.ParticleType;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.particle.NarutoParticleOptions;
import net.narutomod.particle.NarutoParticleType;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticleTypes {
    public static final RegistryObject<NarutoParticleType> ACID_SPIT = register(NarutoParticleKind.ACID_SPIT);
    public static final RegistryObject<NarutoParticleType> BURNING_ASH = register(NarutoParticleKind.BURNING_ASH);
    public static final RegistryObject<NarutoParticleType> EXPANDING_SPHERE = register(NarutoParticleKind.EXPANDING_SPHERE);
    public static final RegistryObject<NarutoParticleType> FALLING_DUST = register(NarutoParticleKind.FALLING_DUST);
    public static final RegistryObject<NarutoParticleType> FLAME_COLORED = register(NarutoParticleKind.FLAME_COLORED);
    public static final RegistryObject<NarutoParticleType> HOMING_ORB = register(NarutoParticleKind.HOMING_ORB);
    public static final RegistryObject<NarutoParticleType> MOB_APPEARANCE = register(NarutoParticleKind.MOB_APPEARANCE);
    public static final RegistryObject<NarutoParticleType> PORTAL_SPIRAL = register(NarutoParticleKind.PORTAL_SPIRAL);
    public static final RegistryObject<NarutoParticleType> SEAL_FORMULA = register(NarutoParticleKind.SEAL_FORMULA);
    public static final RegistryObject<NarutoParticleType> SMOKE_COLORED = register(NarutoParticleKind.SMOKE_COLORED);
    public static final RegistryObject<NarutoParticleType> SUSPENDED_COLORED = register(NarutoParticleKind.SUSPENDED_COLORED);
    public static final RegistryObject<NarutoParticleType> WHIRLPOOL = register(NarutoParticleKind.WHIRLPOOL);

    private ModParticleTypes() {
    }

    private static RegistryObject<NarutoParticleType> register(NarutoParticleKind kind) {
        return ModRegistries.PARTICLE_TYPES.register(kind.registryName(), () -> new NarutoParticleType(kind));
    }

    public static List<RegistryObject<NarutoParticleType>> all() {
        return List.of(
                ACID_SPIT,
                BURNING_ASH,
                EXPANDING_SPHERE,
                FALLING_DUST,
                FLAME_COLORED,
                HOMING_ORB,
                MOB_APPEARANCE,
                PORTAL_SPIRAL,
                SEAL_FORMULA,
                SMOKE_COLORED,
                SUSPENDED_COLORED,
                WHIRLPOOL
        );
    }

    public static NarutoParticleOptions options(NarutoParticleKind kind, int... args) {
        return byKind(kind).get().options(args);
    }

    public static RegistryObject<NarutoParticleType> byKind(NarutoParticleKind kind) {
        return switch (kind) {
            case ACID_SPIT -> ACID_SPIT;
            case BURNING_ASH -> BURNING_ASH;
            case EXPANDING_SPHERE -> EXPANDING_SPHERE;
            case FALLING_DUST -> FALLING_DUST;
            case FLAME_COLORED -> FLAME_COLORED;
            case HOMING_ORB -> HOMING_ORB;
            case MOB_APPEARANCE -> MOB_APPEARANCE;
            case PORTAL_SPIRAL -> PORTAL_SPIRAL;
            case SEAL_FORMULA -> SEAL_FORMULA;
            case SMOKE_COLORED -> SMOKE_COLORED;
            case SUSPENDED_COLORED -> SUSPENDED_COLORED;
            case WHIRLPOOL -> WHIRLPOOL;
        };
    }

    public static void touch() {
    }
}
