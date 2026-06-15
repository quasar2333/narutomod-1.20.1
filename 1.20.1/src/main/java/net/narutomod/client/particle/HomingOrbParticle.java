package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.narutomod.particle.NarutoParticleOptions;

final class HomingOrbParticle extends TextureSheetParticle {
    HomingOrbParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            NarutoParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.hasPhysics = false;
        double radius = options.arg(0, 0);
        if (radius == 0.0D) {
            radius = this.random.nextDouble() * 4.0D + 1.0D;
        }
        this.setPos(
                x + this.random.nextGaussian() * radius,
                y + this.random.nextGaussian() * radius,
                z + this.random.nextGaussian() * radius
        );
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.lifetime = (int)(this.random.nextDouble() * 10.0D) + 50;
        this.xd = (x - this.x) / this.lifetime;
        this.yd = (y - this.y) / this.lifetime;
        this.zd = (z - this.z) / this.lifetime;
        float scale = options.arg(1, 0) / 10.0F;
        if (scale != 0.0F) {
            this.quadSize *= scale;
        }
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return (0xF0 << 16) | 0xF0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
