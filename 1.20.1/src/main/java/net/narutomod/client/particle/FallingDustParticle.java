package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.narutomod.particle.NarutoParticleOptions;

final class FallingDustParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    FallingDustParticle(
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
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        int color = options.arg(0, -1);
        this.alpha = ((color >> 24) & 0xFF) / 255.0F;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        applyLegacyColorJitter();
        this.quadSize *= 0.75F;
        this.lifetime = 60 + this.random.nextInt(12);
        this.setSpriteFromAge(sprites);
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

        this.yd += -0.025D;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.2D;
        this.yd *= 0.2D;
        this.zd *= 0.2D;
        this.setSpriteFromAge(this.sprites);
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
