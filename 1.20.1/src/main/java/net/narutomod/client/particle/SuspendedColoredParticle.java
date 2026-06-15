package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.narutomod.particle.NarutoParticleOptions;

final class SuspendedColoredParticle extends TextureSheetParticle {
    SuspendedColoredParticle(
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
        float scale = Math.max(options.arg(1, 10), 1) / 10.0F;
        int color = options.arg(0, -1);
        this.alpha = ((color >> 24) & 0xFF) / 255.0F;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        applyLegacyColorJitter();
        this.setSize(0.02F * scale, 0.02F * scale);
        this.quadSize *= scale * (this.random.nextFloat() * 0.6F + 0.5F);
        this.xd = this.xd * 0.019999999552965164D + xSpeed;
        this.yd = this.yd * 0.019999999552965164D + ySpeed;
        this.zd = this.zd * 0.019999999552965164D + zSpeed;
        int age = options.arg(2, 0);
        this.lifetime = age > 0
                ? Math.max((int)((this.random.nextFloat() * 0.4F + 0.8F) * age), 1)
                : Math.max((int)(20.0D / (this.random.nextDouble() * 0.8D + 0.2D)), 1);
        this.setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.8D;
        this.yd *= 0.8D;
        this.zd *= 0.8D;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
}
