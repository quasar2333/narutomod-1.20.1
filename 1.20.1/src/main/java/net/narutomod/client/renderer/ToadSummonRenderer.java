package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.client.model.ToadModel;
import net.narutomod.entity.AbstractSummonAnimalEntity;

public final class ToadSummonRenderer<T extends AbstractSummonAnimalEntity> extends MobRenderer<T, ToadModel<T>> {
    private final Function<T, ResourceLocation> textureResolver;
    private final boolean pipeVisible;

    public ToadSummonRenderer(EntityRendererProvider.Context context,
            Function<T, ResourceLocation> textureResolver,
            boolean pipeVisible) {
        super(context, new ToadModel<>(context.bakeLayer(ToadModel.LAYER_LOCATION)), 0.5F);
        this.textureResolver = textureResolver;
        this.pipeVisible = pipeVisible;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isVisibleSummon()) {
            return;
        }
        this.model.setPipeVisible(this.pipeVisible);
        this.shadowRadius = 0.5F * entity.getSummonScale();
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
