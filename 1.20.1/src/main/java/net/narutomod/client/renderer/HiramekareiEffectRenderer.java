package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.HiramekareiEffectEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;

public final class HiramekareiEffectRenderer extends EntityRenderer<HiramekareiEffectEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final Vec3 LEGACY_WEAPON_START = new Vec3(0.0D, -0.725D, 0.1D);
    private static final Vec3 LEGACY_WEAPON_END = new Vec3(0.0D, -0.725D, 1.5D);

    public HiramekareiEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HiramekareiEffectEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwnerForRender();
        if (owner != null && entity.isOwnerHoldingHiramekareiForRender()) {
            spawnLegacyBladeParticles(entity, owner, partialTick);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(HiramekareiEffectEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(HiramekareiEffectEntity entity) {
        return TEXTURE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void spawnLegacyBladeParticles(HiramekareiEffectEntity entity, LivingEntity owner, float partialTick) {
        HumanoidModel model = getHumanoidModel(owner);
        if (model == null) {
            return;
        }
        model.setupAnim(owner, 0.0F, 0.0F, owner.tickCount + partialTick, 0.0F, owner.getXRot());
        ModelPart rightArm = model.rightArm;
        Vec3 start = transformThirdPerson(LEGACY_WEAPON_START, rightArm, owner, partialTick);
        Vec3 end = transformThirdPerson(LEGACY_WEAPON_END, rightArm, owner, partialTick);
        Vec3 segment = end.subtract(start);
        int ownerId = owner.getId();
        RandomSource random = owner.getRandom();
        for (int i = 0; i < 50; i++) {
            double speedScale = random.nextDouble() * 0.4D + 0.6D;
            Vec3 motion = segment.scale(speedScale);
            entity.level().addParticle(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x206AD1FF, 5, 40, 0xF0, ownerId),
                start.x() + random.nextGaussian() * 0.08D,
                start.y() + random.nextGaussian() * 0.2D,
                start.z() + random.nextGaussian() * 0.08D,
                motion.x(),
                motion.y(),
                motion.z()
            );
        }
    }

    private static Vec3 transformThirdPerson(Vec3 local, ModelPart arm, LivingEntity owner, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 relative = local
                .zRot(-arm.zRot)
                .yRot(-arm.yRot)
                .xRot(-arm.xRot)
                .add(-0.3125D, 1.5D - (owner.isCrouching() ? 0.3D : 0.0D), -0.05D)
                .yRot(-bodyYaw);
        return relative.add(
                Mth.lerp(partialTick, owner.xOld, owner.getX()),
                Mth.lerp(partialTick, owner.yOld, owner.getY()),
                Mth.lerp(partialTick, owner.zOld, owner.getZ()));
    }

    @SuppressWarnings("rawtypes")
    private HumanoidModel getHumanoidModel(LivingEntity owner) {
        if (this.entityRenderDispatcher.getRenderer(owner) instanceof LivingEntityRenderer renderer
                && renderer.getModel() instanceof HumanoidModel model) {
            return model;
        }
        return null;
    }
}
