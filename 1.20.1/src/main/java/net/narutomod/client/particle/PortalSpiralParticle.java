package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.particle.NarutoParticleOptions;
import net.narutomod.procedure.ProcedureUtils;

final class PortalSpiralParticle extends TextureSheetParticle {
    private final double portalX;
    private final double portalY;
    private final double portalZ;
    private final ProcedureUtils.Vec2f originalRotation;
    private final double originalLength;

    PortalSpiralParticle(
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
                x + (this.random.nextDouble() - 0.5D) * radius * 2.0D,
                y + (this.random.nextDouble() - 0.5D) * radius * 2.0D,
                z + (this.random.nextDouble() - 0.5D) * radius * 2.0D
        );
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.portalX = x;
        this.portalY = y;
        this.portalZ = z;
        this.lifetime = this.random.nextInt(11) + 50;

        Vec3 originalOffset = new Vec3(this.x - x, this.y - y, this.z - z);
        this.originalRotation = ProcedureUtils.getYawPitchFromVec(originalOffset);
        this.originalLength = originalOffset.length();

        int color = options.arg(1, -1);
        this.alpha = ((color >> 24) & 0xFF) / 255.0F;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        applyLegacyColorJitter();
        float scale = options.arg(2, 0) / 10.0F;
        if (scale != 0.0F) {
            this.quadSize *= scale;
        }
        this.setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            this.remove();
        }
        float progress = (float)this.age / (float)this.lifetime;
        float quadrant = (this.originalRotation.x + 180.0F) / 90.0F;
        float spin = quadrant > 3.0F ? -progress : quadrant > 2.0F ? progress : quadrant > 1.0F ? -progress : progress;
        Vec3 target = new Vec3(0.0D, 0.0D, this.originalLength * (1.0D - progress))
                .xRot((-this.originalRotation.y + spin * 360.0F) * Mth.DEG_TO_RAD)
                .yRot(-this.originalRotation.x * Mth.DEG_TO_RAD)
                .add(this.portalX, this.portalY, this.portalZ)
                .subtract(this.x, this.y, this.z);
        this.xd = target.x;
        this.yd = target.y;
        this.zd = target.z;
        this.move(this.xd, this.yd, this.zd);
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
