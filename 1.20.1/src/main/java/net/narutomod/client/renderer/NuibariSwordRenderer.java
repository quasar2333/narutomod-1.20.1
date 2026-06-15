package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.NuibariSwordEntity;
import net.narutomod.registry.ModItems;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class NuibariSwordRenderer extends EntityRenderer<NuibariSwordEntity> {
    private static final int THREAD_RED = 150;
    private static final int THREAD_GREEN = 150;
    private static final int THREAD_BLUE = 150;
    private static final int THREAD_ALPHA = 100;
    private static final Vec3 NEEDLE_EYE_OFFSET = new Vec3(0.0D, 2.08D, 0.0D);

    private final ItemRenderer itemRenderer;
    private final ItemStack fallbackStack;

    public NuibariSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.fallbackStack = new ItemStack(ModItems.NUIBARI_SWORD.get());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(NuibariSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = entity.getItem().isEmpty() ? this.fallbackStack : entity.getItem();
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.level(),
                entity.getId());
        poseStack.popPose();

        renderThread(entity, partialTick, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(NuibariSwordEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(NuibariSwordEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static void renderThread(NuibariSwordEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource) {
        LivingEntity owner = entity.getOwnerForRender();
        Vec3 entityPosition = interpolatedPosition(entity, partialTick);
        Vec3 previous = needleEyePosition(entity, entityPosition, partialTick);

        for (LivingEntity skewered : entity.getSkeweredEntitiesForRender()) {
            Vec3 current = interpolatedPosition(skewered, partialTick).add(0.0D, 1.0D, 0.0D);
            renderLine(poseStack, bufferSource, previous.subtract(entityPosition), current.subtract(entityPosition));
            previous = current;
        }
        if (owner != null) {
            Vec3 current = interpolatedPosition(owner, partialTick).add(0.0D, 1.0D, 0.0D);
            renderLine(poseStack, bufferSource, previous.subtract(entityPosition), current.subtract(entityPosition));
        }
    }

    private static Vec3 needleEyePosition(NuibariSwordEntity entity, Vec3 entityPosition, float partialTick) {
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90.0F;
        return NEEDLE_EYE_OFFSET
                .xRot(-pitch * Mth.DEG_TO_RAD)
                .yRot(yaw * Mth.DEG_TO_RAD)
                .add(entityPosition);
    }

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 from, Vec3 to) {
        Vec3 normalVector = to.subtract(from);
        if (normalVector.lengthSqr() <= 1.0E-8D) {
            return;
        }
        normalVector = normalVector.normalize();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(matrix, normal, consumer, from, normalVector);
        vertex(matrix, normal, consumer, to, normalVector);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 point, Vec3 normalVector) {
        consumer.vertex(matrix, (float)point.x(), (float)point.y(), (float)point.z())
                .color(THREAD_RED, THREAD_GREEN, THREAD_BLUE, THREAD_ALPHA)
                .normal(normal, (float)normalVector.x(), (float)normalVector.y(), (float)normalVector.z())
                .endVertex();
    }
}
