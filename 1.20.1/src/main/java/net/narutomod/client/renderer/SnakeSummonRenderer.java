package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.client.model.SnakeModel;
import net.narutomod.entity.AbstractSummonAnimalEntity;

public final class SnakeSummonRenderer<T extends AbstractSummonAnimalEntity> extends MobRenderer<T, SnakeModel<T>> {
    private final Function<T, ResourceLocation> textureResolver;

    public SnakeSummonRenderer(EntityRendererProvider.Context context, Function<T, ResourceLocation> textureResolver) {
        super(context, new SnakeModel<>(context.bakeLayer(SnakeModel.LAYER_LOCATION)), 0.3F);
        this.textureResolver = textureResolver;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isVisibleSummon()) {
            return;
        }
        this.shadowRadius = 0.3F * entity.getSummonScale();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getSummonScale();
        poseStack.translate(0.0F, 1.5F - 1.5F * scale, 0.0F);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.textureResolver.apply(entity);
    }
}
