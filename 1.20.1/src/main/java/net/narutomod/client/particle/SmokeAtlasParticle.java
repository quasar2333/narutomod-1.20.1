package net.narutomod.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

class SmokeAtlasParticle extends LegacyAtlasParticle {
    private final float smokeScale;
    protected final float legacyScale;
    private final double floatMotionY;

    SmokeAtlasParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            int color,
            float scale,
            int maxAge,
            int brightness,
            int viewerId,
            double floatSpeed
    ) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, color, brightness, viewerId);
        this.textureIndexY = 2;
        this.xd = this.xd * 0.1D + xSpeed;
        this.yd = this.yd * 0.1D + ySpeed;
        this.zd = this.zd * 0.1D + zSpeed;
        this.legacyScale = scale;
        this.quadSize *= 0.75F * scale;
        this.smokeScale = this.quadSize;
        this.lifetime = maxAge > 0 ? maxAge : randomLifetime(scale);
        this.floatMotionY = floatSpeed;
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer consumer, Camera camera, float partialTicks) {
        float progress = ageProgress(partialTicks);
        this.alpha = this.alphaStart * (1.0F - progress * progress * 0.5F);
        super.render(consumer, camera, partialTicks);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.smokeScale * Mth.clamp(ageProgress(partialTicks) * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime <= 0 || this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.textureIndexX = Mth.clamp(7 - this.age * 8 / this.lifetime, 0, 7);
        this.yd += this.floatMotionY;
        this.move(this.xd, this.yd, this.zd);
        if (this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }
    }

    private int randomLifetime(float scale) {
        return Math.max((int)((8.0D / (this.random.nextDouble() * 0.8D + 0.2D)) * scale), 1);
    }
}
