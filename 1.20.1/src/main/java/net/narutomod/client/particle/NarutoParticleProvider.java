package net.narutomod.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.particle.NarutoParticleOptions;

public final class NarutoParticleProvider implements ParticleProvider<NarutoParticleOptions> {
    private final SpriteSet sprites;

    public NarutoParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Nullable
    @Override
    public Particle createParticle(
            NarutoParticleOptions options,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
    ) {
        switch (options.kind()) {
            case SMOKE_COLORED -> {
                return new SmokeAtlasParticle(
                        level,
                        x,
                        y,
                        z,
                        xSpeed,
                        ySpeed,
                        zSpeed,
                        options.arg(0, -1),
                        Math.max(options.arg(1, 10), 1) / 10.0F,
                        options.arg(2, 0),
                        options.arg(3, 0),
                        options.arg(4, -1),
                        options.arg(5, 4) / 1000.0D
                );
            }
            case SUSPENDED_COLORED -> {
                return new SuspendedColoredParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
            }
            case FALLING_DUST -> {
                return new FallingDustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
            }
            case FLAME_COLORED -> {
                return new FlameAtlasParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
            }
            case MOB_APPEARANCE -> {
                return new MobAppearanceParticle(level, x, y, z, options);
            }
            case BURNING_ASH -> {
                return new BurningAshParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options.arg(0, -1));
            }
            case ACID_SPIT -> {
                return new AcidSpitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
            }
            case HOMING_ORB -> {
                return new HomingOrbParticle(level, x, y, z, options, this.sprites);
            }
            case PORTAL_SPIRAL -> {
                return new PortalSpiralParticle(level, x, y, z, options, this.sprites);
            }
            case EXPANDING_SPHERE -> {
                return new ExpandingSphereParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
            }
            case SEAL_FORMULA -> {
                return new SealFormulaParticle(level, x, y, z, options);
            }
            case WHIRLPOOL -> {
                return new WhirlpoolParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
            }
            default -> {
                return new NarutoSpriteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, this.sprites);
            }
        }
    }
}
