package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.Snake8HeadsModel;
import net.narutomod.entity.Snake8HeadsEntity;

public final class Snake8HeadsRenderer extends MobRenderer<Snake8HeadsEntity, Snake8HeadsModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/snake_8h.png");

    public Snake8HeadsRenderer(EntityRendererProvider.Context context) {
        super(context, new Snake8HeadsModel(context.bakeLayer(Snake8HeadsModel.LAYER_LOCATION)), 4.8F);
    }

    @Override
    protected void scale(Snake8HeadsEntity entity, PoseStack poseStack, float partialTick) {
        float growth = entity.getRenderGrowth(partialTick);
        float scale = Snake8HeadsEntity.MODEL_SCALE * growth;
        poseStack.translate(0.0F, 1.5F - scale * 1.5F, 0.0F);
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = Snake8HeadsEntity.WIDTH * 0.5F * growth;
    }

    @Override
    public ResourceLocation getTextureLocation(Snake8HeadsEntity entity) {
        return TEXTURE;
    }
}
