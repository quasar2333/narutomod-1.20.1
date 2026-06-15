package net.narutomod.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.particle.NarutoParticleOptions;
import org.joml.Matrix4f;

final class SealFormulaParticle extends Particle {
    private final float scale;
    private final float yRotation;
    private final int growTime;

    SealFormulaParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            NarutoParticleOptions options
    ) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.scale = Math.max(options.arg(0, 10), 1) / 10.0F;
        this.yRotation = options.arg(1, 0) / 10.0F;
        this.lifetime = Math.max(options.arg(2, 20), 1);
        this.growTime = Math.min(this.lifetime, 20);
        this.setSize(this.scale, this.scale);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPosition.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPosition.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPosition.z());
        float age = this.age + partialTicks;
        float grow = Mth.clamp(age / this.growTime, 0.0F, 1.0F);
        float size = this.scale * grow;
        float fade = fade(age);
        float u0 = 0.5F * (1.0F - grow);
        float u1 = 0.5F * (1.0F + grow);
        float v0 = 0.5F * (1.0F - grow);
        float v1 = 0.5F * (1.0F + grow);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.yRotation));
        Matrix4f matrix = poseStack.last().pose();
        int alpha = LegacyTexturedQuadParticle.toColor(fade);

        emit(consumer, matrix, -0.5F * size, 0.0F, -0.5F * size, u0, v1, alpha);
        emit(consumer, matrix, -0.5F * size, 0.0F, 0.5F * size, u0, v0, alpha);
        emit(consumer, matrix, 0.5F * size, 0.0F, 0.5F * size, u1, v0, alpha);
        emit(consumer, matrix, 0.5F * size, 0.0F, -0.5F * size, u1, v1, alpha);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return NarutoParticleRenderTypes.SEAL_FORMULA;
    }

    private float fade(float age) {
        float fadeStart = 0.8F * this.lifetime;
        if (age < fadeStart) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (age - fadeStart) / Math.max(0.2F * this.lifetime, 1.0F), 0.0F, 1.0F);
    }

    private static void emit(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .uv(u, v)
                .color(255, 255, 255, alpha)
                .endVertex();
    }
}
