package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.particle.NarutoParticleOptions;

final class NarutoSpriteParticle extends TextureSheetParticle {
    private final NarutoParticleKind kind;
    private final SpriteSet sprites;
    private final float startQuadSize;
    private final float alphaStart;
    private final int fixedLight;
    private final double floatMotionY;

    NarutoSpriteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            NarutoParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.kind = options.kind();
        this.sprites = sprites;
        this.hasPhysics = true;
        this.friction = 0.96F;
        this.gravity = 0.0F;

        int color = defaultColor(options);
        this.setColor(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        );
        this.alphaStart = ((color >> 24) & 0xFF) / 255.0F;
        this.setAlpha(this.alphaStart);

        float scale = defaultScale(options);
        this.quadSize *= scale;
        this.startQuadSize = this.quadSize;
        this.lifetime = defaultLifetime(options);
        this.fixedLight = defaultBrightness(options);
        this.floatMotionY = defaultFloatMotionY(options);
        configureMotion(x, y, z, xSpeed, ySpeed, zSpeed, options);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            if (this.floatMotionY != 0.0D) {
                this.yd += this.floatMotionY;
            }
            this.setSpriteFromAge(this.sprites);
            float ageFactor = this.lifetime <= 0 ? 1.0F : (float)this.age / (float)this.lifetime;
            this.setAlpha(this.alphaStart * Mth.clamp(1.0F - ageFactor * ageFactor * 0.5F, 0.0F, 1.0F));
        }
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float ageFactor = this.lifetime <= 0
                ? 1.0F
                : Mth.clamp(((float)this.age + partialTicks) / (float)this.lifetime, 0.0F, 1.0F);
        return switch (this.kind) {
            case FLAME_COLORED -> this.startQuadSize * (1.0F - Mth.square(ageFactor - 0.5F) * 1.75F);
            case EXPANDING_SPHERE, SEAL_FORMULA -> this.startQuadSize * Mth.clamp(ageFactor * 4.0F, 0.0F, 1.0F);
            case SMOKE_COLORED, BURNING_ASH, ACID_SPIT -> this.startQuadSize * Mth.clamp(ageFactor * 32.0F, 0.0F, 1.0F);
            default -> this.startQuadSize;
        };
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.fixedLight != 0 ? this.fixedLight : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void configureMotion(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, NarutoParticleOptions options) {
        switch (this.kind) {
            case HOMING_ORB -> {
                double radius = Math.max(options.arg(0, 0), 0);
                if (radius == 0.0D) {
                    radius = this.random.nextDouble() * 4.0D + 1.0D;
                }
                this.setPos(
                        x + this.random.nextGaussian() * radius,
                        y + this.random.nextGaussian() * radius,
                        z + this.random.nextGaussian() * radius
                );
                this.xd = (x - this.x) / this.lifetime;
                this.yd = (y - this.y) / this.lifetime;
                this.zd = (z - this.z) / this.lifetime;
            }
            case PORTAL_SPIRAL -> {
                double radius = Math.max(options.arg(0, 0), 1);
                this.setPos(
                        x + (this.random.nextDouble() - 0.5D) * radius * 2.0D,
                        y + (this.random.nextDouble() - 0.5D) * radius * 2.0D,
                        z + (this.random.nextDouble() - 0.5D) * radius * 2.0D
                );
                this.xd = (x - this.x) / this.lifetime;
                this.yd = (y - this.y) / this.lifetime;
                this.zd = (z - this.z) / this.lifetime;
            }
            default -> {
                this.xd = xSpeed;
                this.yd = ySpeed;
                this.zd = zSpeed;
            }
        }
    }

    private int defaultColor(NarutoParticleOptions options) {
        return switch (this.kind) {
            case BURNING_ASH -> 0xFF606060;
            case ACID_SPIT -> options.arg(1, 0x80FFD6BA);
            case FLAME_COLORED -> options.arg(0, 0x80FF6633);
            case EXPANDING_SPHERE -> options.arg(2, 0x80FFFFFF);
            case PORTAL_SPIRAL -> options.arg(1, 0x80FFFFFF);
            case SEAL_FORMULA -> 0xFFFFFFFF;
            default -> options.arg(0, 0xFFFFFFFF);
        };
    }

    private float defaultScale(NarutoParticleOptions options) {
        return switch (this.kind) {
            case SMOKE_COLORED, SUSPENDED_COLORED, FLAME_COLORED, HOMING_ORB, PORTAL_SPIRAL, WHIRLPOOL ->
                    Math.max(options.arg(1, 10), 1) / 10.0F;
            case EXPANDING_SPHERE, SEAL_FORMULA -> Math.max(options.arg(0, 10), 1) / 10.0F;
            case BURNING_ASH -> 5.0F + this.random.nextFloat() * 5.0F;
            case ACID_SPIT -> 0.5F + this.random.nextFloat() * 4.5F;
            default -> 1.0F;
        };
    }

    private int defaultLifetime(NarutoParticleOptions options) {
        return switch (this.kind) {
            case SMOKE_COLORED -> positiveOr(options.arg(2, 0), 40);
            case SUSPENDED_COLORED -> positiveOr(options.arg(2, 0), 20);
            case FALLING_DUST -> 60 + this.random.nextInt(12);
            case FLAME_COLORED -> 12 + this.random.nextInt(8);
            case MOB_APPEARANCE -> 30;
            case BURNING_ASH -> 100;
            case HOMING_ORB, PORTAL_SPIRAL -> 50 + this.random.nextInt(11);
            case EXPANDING_SPHERE -> positiveOr(options.arg(1, 1), 20);
            case SEAL_FORMULA -> positiveOr(options.arg(2, 20), 20);
            case ACID_SPIT -> 50;
            case WHIRLPOOL -> positiveOr(options.arg(2, 0), 20);
        };
    }

    private int defaultBrightness(NarutoParticleOptions options) {
        int light = switch (this.kind) {
            case SMOKE_COLORED -> options.arg(3, 0);
            case WHIRLPOOL -> options.arg(3, 0);
            case FLAME_COLORED, HOMING_ORB, EXPANDING_SPHERE -> 0xF0;
            default -> 0;
        };
        return light == 0 ? 0 : (light << 16) | light;
    }

    private double defaultFloatMotionY(NarutoParticleOptions options) {
        return switch (this.kind) {
            case SMOKE_COLORED -> options.arg(5, 4) / 1000.0D;
            case ACID_SPIT -> -0.005D;
            default -> 0.0D;
        };
    }

    private static int positiveOr(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
