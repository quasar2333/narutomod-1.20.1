package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KingOfHellModel;
import net.narutomod.entity.KingOfHellEntity;

public final class KingOfHellRenderer extends MobRenderer<KingOfHellEntity, KingOfHellModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/kingofhell.png");
    private static final float MODEL_SCALE = 3.0F;

    public KingOfHellRenderer(EntityRendererProvider.Context context) {
        super(context, new KingOfHellModel(context.bakeLayer(KingOfHellModel.LAYER_LOCATION)), 4.8F);
    }

    @Override
    protected void scale(KingOfHellEntity entity, PoseStack poseStack, float partialTick) {
        float translate = entity.getPopoutFactor(partialTick) * MODEL_SCALE;
        poseStack.translate(0.0F, 1.5F - 1.5F * translate, 0.0F);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(KingOfHellEntity entity) {
        return TEXTURE;
    }
}
