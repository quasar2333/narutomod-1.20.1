package net.narutomod.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

abstract class LegacyAtlasParticle extends SingleQuadParticle {
    private static final float ATLAS_STEP = 1.0F / 8.0F;
    private static final float ATLAS_CELL = 0.124875F;

    protected int textureIndexX;
    protected int textureIndexY;
    protected final float alphaStart;
    protected final int packedLight;
    private final int viewerId;

    LegacyAtlasParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vanillaXSpeed,
            double vanillaYSpeed,
            double vanillaZSpeed,
            int color,
            int brightness,
            int viewerId
    ) {
        super(level, x, y, z, vanillaXSpeed, vanillaYSpeed, vanillaZSpeed);
        this.alphaStart = ((color >> 24) & 0xFF) / 255.0F;
        this.alpha = this.alphaStart;
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        applyLegacyColorJitter();
        this.packedLight = brightness != 0 ? (brightness << 16) | brightness : 0;
        this.viewerId = viewerId;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        if (!hiddenFromViewer(camera)) {
            super.render(consumer, camera, partialTicks);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return NarutoParticleRenderTypes.PARTICLES_ATLAS;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.packedLight != 0 ? this.packedLight : super.getLightColor(partialTick);
    }

    @Override
    protected float getU0() {
        return this.textureIndexX * ATLAS_STEP;
    }

    @Override
    protected float getU1() {
        return getU0() + ATLAS_CELL;
    }

    @Override
    protected float getV0() {
        return this.textureIndexY * ATLAS_STEP;
    }

    @Override
    protected float getV1() {
        return getV0() + ATLAS_CELL;
    }

    protected float ageProgress(float partialTicks) {
        return this.lifetime <= 0 ? 1.0F : Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
    }

    private boolean hiddenFromViewer(Camera camera) {
        if (this.viewerId < 0 || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return false;
        }
        Entity cameraEntity = camera.getEntity();
        return cameraEntity != null && cameraEntity.getId() == this.viewerId;
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
