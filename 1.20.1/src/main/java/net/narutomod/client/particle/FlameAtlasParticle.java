package net.narutomod.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.narutomod.particle.NarutoParticleOptions;

final class FlameAtlasParticle extends LegacyAtlasParticle {
    private final float flameScale;
    private final float baseGreen;

    FlameAtlasParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            NarutoParticleOptions options
    ) {
        super(
                level,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed,
                options.arg(0, -1),
                0xF0,
                -1
        );
        this.xd = this.xd * 0.009999999776482582D + xSpeed;
        this.yd = this.yd * 0.009999999776482582D + ySpeed;
        this.zd = this.zd * 0.009999999776482582D + zSpeed;
        this.setPos(
                this.x + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F,
                this.y + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F,
                this.z + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F
        );
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.flameScale = this.quadSize * (Math.max(options.arg(1, 10), 1) / 10.0F);
        this.baseGreen = this.gCol;
        this.lifetime = (int)(8.0D / (this.random.nextDouble() * 0.8D + 0.2D)) + 4;
        this.textureIndexY = 1;
        float bounds = this.flameScale * 2.0F;
        this.setBoundingBox(new AABB(this.x - bounds, this.y - bounds, this.z - bounds, this.x + bounds, this.y + bounds, this.z + bounds));
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        float progress = ageProgress(partialTicks);
        float curve = flameCurve(progress);
        this.alpha = this.alphaStart * curve;
        this.gCol = this.baseGreen * (1.0F - progress);
        super.render(consumer, camera, partialTicks);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.flameScale * flameCurve(ageProgress(partialTicks));
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

        this.textureIndexX = (this.age / 2) % 8;
        this.yd += 0.003D;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }
    }

    private static float flameCurve(float progress) {
        float centered = Math.min(progress, 1.0F) - 0.5F;
        return 1.0F - centered * centered * 3.5F;
    }
}
