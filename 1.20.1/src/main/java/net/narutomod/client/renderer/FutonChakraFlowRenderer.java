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
import net.narutomod.entity.FutonChakraFlowEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;

public final class FutonChakraFlowRenderer extends EntityRenderer<FutonChakraFlowEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final Vec3 LEGACY_WEAPON_START = new Vec3(0.0D, -0.725D, 0.1D);
    private static final Vec3 LEGACY_WEAPON_END = new Vec3(0.0D, -0.725D, 1.5D);

    public FutonChakraFlowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FutonChakraFlowEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwnerForRender();
        if (owner != null && entity.isOwnerHoldingWeaponForRender()) {
            spawnLegacyWeaponParticles(entity, owner, partialTick);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(FutonChakraFlowEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FutonChakraFlowEntity entity) {
        return TEXTURE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void spawnLegacyWeaponParticles(FutonChakraFlowEntity entity, LivingEntity owner, float partialTick) {
        HumanoidModel model = getHumanoidModel(owner);
        if (model == null) {
            return;
        }
        model.setupAnim(owner, 0.0F, 0.0F, owner.tickCount + partialTick, 0.0F, owner.getXRot());
        ModelPart rightArm = model.rightArm;
        Vec3 start = transformThirdPerson(LEGACY_WEAPON_START, rightArm, owner, partialTick);
        Vec3 end = transformThirdPerson(LEGACY_WEAPON_END, rightArm, owner, partialTick);
        Vec3 motion = end.subtract(start).scale(0.2D);
        RandomSource random = owner.getRandom();
        for (int i = 0; i < 30; i++) {
            entity.level().addParticle(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x106AD1FF, 5, 10, 0xF0),
                start.x() + random.nextGaussian() * 0.05D,
                start.y() + random.nextGaussian() * 0.05D,
                start.z() + random.nextGaussian() * 0.05D,
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
