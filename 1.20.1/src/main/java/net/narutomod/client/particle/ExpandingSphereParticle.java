package net.narutomod.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutomod.client.render.SphereMesh;
import net.narutomod.particle.NarutoParticleOptions;

final class ExpandingSphereParticle extends Particle {
    private static final int MAX_RENDERED_SHELLS = 32;

    private final float maxScale;

    ExpandingSphereParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            NarutoParticleOptions options
    ) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.maxScale = Math.max(options.arg(0, 10), 1) / 10.0F;
        this.lifetime = Math.max(options.arg(1, 1), 1);
        int color = options.arg(2, 0x80FFFFFF);
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.alpha = 1.0F;
        float bounds = Math.max(this.maxScale, 1.0F);
        this.setBoundingBox(new AABB(x - bounds, y - bounds, z - bounds, x + bounds, y + bounds, z + bounds));
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPosition.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPosition.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPosition.z());
        float fade = fade(partialTicks);
        float age = Math.max(0.0F, this.age + partialTicks);
        int firstShell = Math.max(Mth.ceil(age - this.maxScale * 2.0F), 0);
        int lastShellExclusive = Math.max(Mth.ceil(age), firstShell + 1);
        int activeShells = Math.max(lastShellExclusive - firstShell, 1);
        int stride = Math.max(Mth.ceil((float)activeShells / MAX_RENDERED_SHELLS), 1);
        int rendered = 0;

        for (int shell = firstShell; shell < lastShellExclusive && rendered < MAX_RENDERED_SHELLS; shell += stride) {
            float scale = (age - shell) * 0.5F;
            if (scale > 0.0F && scale <= this.maxScale) {
                PoseStack poseStack = new PoseStack();
                poseStack.translate(x, y, z);
                poseStack.scale(scale, scale, scale);
                SphereMesh.renderDoubleSidedLegacySphere(
                        poseStack,
                        consumer,
                        1.0F,
                        toColor(1.0F - 0.05F * shell * (1.0F - this.rCol)),
                        toColor(1.0F - 0.05F * shell * (1.0F - this.gCol)),
                        toColor(1.0F - 0.05F * shell * (1.0F - this.bCol)),
                        toColor(0.05F * fade)
                );
                rendered++;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return NarutoParticleRenderTypes.EXPANDING_SPHERE;
    }

    private float fade(float partialTicks) {
        float age = this.age + partialTicks;
        float fadeStart = 0.8F * this.lifetime;
        if (age <= fadeStart) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (age - fadeStart) / Math.max(0.2F * this.lifetime, 1.0F), 0.0F, 1.0F);
    }

    private static int toColor(float value) {
        return Mth.clamp((int)(Mth.clamp(value, 0.0F, 1.0F) * 255.0F), 0, 255);
    }
}
