package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.Snake8HeadModel;
import net.narutomod.entity.Snake8HeadEntity;

public final class Snake8HeadRenderer extends MobRenderer<Snake8HeadEntity, Snake8HeadModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/snake_8h1.png");

    public Snake8HeadRenderer(EntityRendererProvider.Context context) {
        super(context, new Snake8HeadModel(context.bakeLayer(Snake8HeadModel.LAYER_LOCATION)), Snake8HeadEntity.WIDTH * 0.5F);
    }

    @Override
    protected void scale(Snake8HeadEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(Snake8HeadEntity.MODEL_SCALE, Snake8HeadEntity.MODEL_SCALE, Snake8HeadEntity.MODEL_SCALE);
        this.shadowRadius = Snake8HeadEntity.WIDTH * 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(Snake8HeadEntity entity) {
        return TEXTURE;
    }
}
