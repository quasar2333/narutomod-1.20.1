package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.CellularActivationEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;

public final class CellularActivationRenderer extends EntityRenderer<CellularActivationEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");

    public CellularActivationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CellularActivationEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwnerForRender();
        if (owner != null && entity.getReductionAmountForRender() > 0) {
            spawnReductionParticle(entity, owner, partialTick);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(CellularActivationEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(CellularActivationEntity entity) {
        return TEXTURE;
    }

    private static void spawnReductionParticle(CellularActivationEntity entity, LivingEntity owner, float partialTick) {
        RandomSource random = owner.getRandom();
        Vec3 position = new Vec3(
            Mth.lerp(partialTick, owner.xOld, owner.getX()),
            Mth.lerp(partialTick, owner.yOld, owner.getY()) + owner.getBbHeight() * 0.5D,
            Mth.lerp(partialTick, owner.zOld, owner.getZ())
        );
        double xSpread = owner.getBbWidth() / 3.0D;
        double ySpread = owner.getBbHeight() / 2.0D;
        double zSpread = owner.getBbWidth() / 3.0D;
        int alpha = 0x10 + random.nextInt(0x20);
        int color = 0x0000FFF6 | (alpha << 24);
        int lifetime = 10 + random.nextInt(25);
        entity.level().addParticle(
            ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, color, lifetime, 5, 0xF0, -1, 0),
            position.x() + (random.nextDouble() - 0.5D) * xSpread,
            position.y() + (random.nextDouble() - 0.5D) * ySpread,
            position.z() + (random.nextDouble() - 0.5D) * zSpread,
            0.0D,
            0.0D,
            0.0D
        );
    }
}
