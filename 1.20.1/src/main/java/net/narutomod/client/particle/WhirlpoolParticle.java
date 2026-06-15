package net.narutomod.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.particle.NarutoParticleOptions;
import net.narutomod.procedure.ProcedureUtils;
import org.joml.Matrix4f;

final class WhirlpoolParticle extends LegacyTexturedQuadParticle {
    WhirlpoolParticle(
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
                options.arg(0, 0xFFFFFFFF),
                Math.max(options.arg(1, 10), 1) / 10.0F,
                options.arg(2, 0),
                options.arg(3, 0)
        );
        this.rotateX = ProcedureUtils.getPitchFromVec(this.xd, this.yd, this.zd);
        this.rotateY = ProcedureUtils.getYawFromVec(this.xd, this.zd);
        this.rotateZ = this.random.nextFloat() * 360.0F;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPosition.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPosition.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPosition.z());
        float age = this.age + partialTicks;
        float progress = ageProgress(partialTicks);
        float size = this.quadScale * (progress * 0.6F + 0.7F);
        int alpha = toColor(alpha(progress));
        int red = toColor(this.rCol);
        int green = toColor(this.gCol);
        int blue = toColor(this.bCol);
        int light = this.getLightColor(partialTicks);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.rotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees(this.rotateX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(this.rotateZ - 30.0F * age));
        Matrix4f matrix = poseStack.last().pose();

        emit(consumer, matrix, -0.5F * size, -0.5F * size, 0.0F, 0.0F, 1.0F, red, green, blue, alpha, light);
        emit(consumer, matrix, 0.5F * size, -0.5F * size, 0.0F, 1.0F, 1.0F, red, green, blue, alpha, light);
        emit(consumer, matrix, 0.5F * size, 0.5F * size, 0.0F, 1.0F, 0.0F, red, green, blue, alpha, light);
        emit(consumer, matrix, -0.5F * size, 0.5F * size, 0.0F, 0.0F, 0.0F, red, green, blue, alpha, light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return NarutoParticleRenderTypes.WHIRLPOOL;
    }

    private float alpha(float progress) {
        if (progress <= 0.1F) {
            return progress / 0.1F;
        }
        return this.alphaStart * (1.0F - Mth.square(progress - 0.1F));
    }

    private static void emit(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        consumer.vertex(matrix, x, y, z)
                .uv(u, v)
                .color(red, green, blue, alpha)
                .uv2(light)
                .endVertex();
    }
}
