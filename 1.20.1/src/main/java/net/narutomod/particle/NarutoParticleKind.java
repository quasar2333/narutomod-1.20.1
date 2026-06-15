package net.narutomod.particle;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum NarutoParticleKind {
    SMOKE_COLORED("smoke_colored", 54678400, 6),
    SUSPENDED_COLORED("suspended_colored", 54678401, 3),
    FALLING_DUST("falling_dust", 54678402, 1),
    FLAME_COLORED("flame_colored", 54678403, 2),
    MOB_APPEARANCE("mob_appearance", 54678404, 1),
    BURNING_ASH("burning_ash", 54678405, 1),
    HOMING_ORB("homing_orb", 54678406, 2),
    EXPANDING_SPHERE("expanding_sphere", 54678407, 3),
    PORTAL_SPIRAL("portal_spiral", 54678408, 3),
    SEAL_FORMULA("seal_formula", 54678409, 3),
    ACID_SPIT("acid_spit", 54678410, 2),
    WHIRLPOOL("whirlpool", 54678411, 4);

    private final String registryName;
    private final int legacyId;
    private final int argumentCount;

    NarutoParticleKind(String registryName, int legacyId, int argumentCount) {
        this.registryName = registryName;
        this.legacyId = legacyId;
        this.argumentCount = argumentCount;
    }

    public String registryName() {
        return this.registryName;
    }

    public int legacyId() {
        return this.legacyId;
    }

    public int argumentCount() {
        return this.argumentCount;
    }

    public static Optional<NarutoParticleKind> byRegistryName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(kind -> kind.registryName.equals(normalized))
                .findFirst();
    }
}
