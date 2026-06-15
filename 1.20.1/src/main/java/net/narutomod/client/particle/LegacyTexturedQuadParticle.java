package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;

abstract class LegacyTexturedQuadParticle extends Particle {
    protected final float quadScale;
    protected final float alphaStart;
    protected final int packedLight;
    protected float rotateX;
    protected float rotateY;
    protected float rotateZ;

    LegacyTexturedQuadParticle(
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
            int brightness
    ) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.friction = 0.98F;
        this.gravity = 0.0F;
        this.alphaStart = ((color >> 24) & 0xFF) / 255.0F;
        this.alpha = this.alphaStart;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        applyLegacyColorJitter();
        this.quadScale = Math.max(scale, 0.01F);
        this.lifetime = maxAge > 0 ? maxAge : randomLifetime(this.quadScale);
        this.packedLight = brightness != 0 ? (brightness << 16) | brightness : 0;
        this.setSize(this.quadScale, this.quadScale);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.packedLight != 0 ? this.packedLight : super.getLightColor(partialTick);
    }

    protected float ageProgress(float partialTicks) {
        return this.lifetime <= 0 ? 1.0F : Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
    }

    protected static int toColor(float value) {
        return Mth.clamp((int)(Mth.clamp(value, 0.0F, 1.0F) * 255.0F), 0, 255);
    }

    private void applyLegacyColorJitter() {
        float jitter = (this.random.nextFloat() - 0.5F) * 0.1F;
        if (this.rCol + jitter >= 0.0F && this.rCol + jitter <= 1.0F) {
            this.rCol += jitter;
        }
        if (this.gCol + jitter >= 0.0F && this.gCol + jitter <= 1.0F) {
            this.gCol += jitter;
        }
        if (this.bCol + jitter >= 0.0F && this.bCol + jitter <= 1.0F) {
            this.bCol += jitter;
        }
    }

    private int randomLifetime(float scale) {
        int baseLifetime = (int)(4.0D / (this.random.nextDouble() * 0.8D + 0.2D));
        return Math.max((int)(baseLifetime * scale), 1);
    }
}
