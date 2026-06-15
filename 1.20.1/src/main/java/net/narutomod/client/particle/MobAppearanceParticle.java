package net.narutomod.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.particle.NarutoParticleOptions;
import net.narutomod.registry.ModEntityTypes;

final class MobAppearanceParticle extends Particle {
    private static final int LEGACY_ITACHI_ENTITY_ID = 117;
    private final int entityId;
    private Entity entity;

    MobAppearanceParticle(ClientLevel level, double x, double y, double z, NarutoParticleOptions options) {
        super(level, x, y, z);
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 30;
        this.entityId = options.arg(0, -1);
        this.entity = findOrCreateEntity(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.entity == null) {
            this.entity = findOrCreateEntity(this.level);
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks) {
        Entity renderEntity = this.entity;
        if (renderEntity == null) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPosition.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPosition.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPosition.z());
        float progress = this.lifetime <= 0 ? 1.0F : Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
        float alpha = 0.05F + 0.5F * Mth.sin(progress * Mth.PI);
        float scale = 1.0F + progress * 1.5F;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(x, y, z);
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - camera.getYRot()));
        poseStack.translate(0.0D, -scale * 1.5D, -1.5D);
        poseStack.scale(scale, scale, scale);

        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        float oldYaw = renderEntity.getYRot();
        float oldYawO = renderEntity.yRotO;
        float oldHeadYaw = 0.0F;
        float oldHeadYawO = 0.0F;
        float oldBodyYaw = 0.0F;
        float oldBodyYawO = 0.0F;
        if (renderEntity instanceof LivingEntity living) {
            oldHeadYaw = living.yHeadRot;
            oldHeadYawO = living.yHeadRotO;
            oldBodyYaw = living.yBodyRot;
            oldBodyYawO = living.yBodyRotO;
            living.setYHeadRot(0.0F);
            living.yHeadRotO = 0.0F;
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        try {
            renderEntity.setYRot(0.0F);
            renderEntity.yRotO = 0.0F;
            dispatcher.render(renderEntity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
            bufferSource.endBatch();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            renderEntity.setYRot(oldYaw);
            renderEntity.yRotO = oldYawO;
            if (renderEntity instanceof LivingEntity living) {
                living.setYHeadRot(oldHeadYaw);
                living.yHeadRotO = oldHeadYawO;
                living.yBodyRot = oldBodyYaw;
                living.yBodyRotO = oldBodyYawO;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    private Entity findOrCreateEntity(ClientLevel level) {
        if (this.entityId < 0 || this.entityId == LEGACY_ITACHI_ENTITY_ID) {
            Entity itachi = ModEntityTypes.ITACHI.get().create(level);
            if (itachi != null) {
                itachi.setPos(this.x, this.y, this.z);
            }
            return itachi;
        }
        Entity levelEntity = this.entityId >= 0 ? level.getEntity(this.entityId) : null;
        if (levelEntity != null) {
            return levelEntity;
        }
        return null;
    }
}
