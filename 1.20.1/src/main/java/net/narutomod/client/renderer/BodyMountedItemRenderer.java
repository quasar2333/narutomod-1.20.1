package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.narutomod.item.ItemOnBody;

final class BodyMountedItemRenderer {
    private BodyMountedItemRenderer() {
    }

    static <T extends LivingEntity, M extends HumanoidModel<T>> void renderStack(PoseStack poseStack, MultiBufferSource bufferSource,
                                                                                int packedLight, T entity, M model, ItemStack stack) {
        renderStack(poseStack, bufferSource, packedLight, entity, stack,
                (stackPose, bodyPart) -> attachToBodyPart(stackPose, model, bodyPart));
    }

    static <T extends LivingEntity> void renderStack(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                     T entity, ItemStack stack, BodyPartAttacher bodyPartAttacher) {
        if (!(stack.getItem() instanceof ItemOnBody.Interface itemOnBody)) {
            return;
        }
        ItemOnBody.BodyPart bodyPart = itemOnBody.showOnBody(stack);
        if (bodyPart == ItemOnBody.BodyPart.NONE) {
            return;
        }

        poseStack.pushPose();
        bodyPartAttacher.attach(poseStack, bodyPart);
        Vec3 offset = itemOnBody.getOffset(stack);
        poseStack.translate(offset.x, -0.25D + offset.y, offset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity,
                stack,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                bufferSource,
                entity.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                entity.getId());
        poseStack.popPose();
    }

    @FunctionalInterface
    interface BodyPartAttacher {
        void attach(PoseStack poseStack, ItemOnBody.BodyPart bodyPart);
    }

    private static <T extends LivingEntity, M extends HumanoidModel<T>> void attachToBodyPart(PoseStack poseStack, M model,
                                                                                              ItemOnBody.BodyPart bodyPart) {
        ModelPart part = switch (bodyPart) {
            case HEAD -> model.head;
            case TORSO -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            case NONE -> model.body;
        };
        part.translateAndRotate(poseStack);
    }
}
