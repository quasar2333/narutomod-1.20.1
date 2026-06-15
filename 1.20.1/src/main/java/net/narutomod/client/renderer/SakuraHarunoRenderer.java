package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SakuraHarunoModel;
import net.narutomod.entity.SakuraHarunoEntity;

public final class SakuraHarunoRenderer extends MobRenderer<SakuraHarunoEntity, SakuraHarunoModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/sakura_slim.png");

    public SakuraHarunoRenderer(EntityRendererProvider.Context context) {
        super(context, new SakuraHarunoModel(context.bakeLayer(SakuraHarunoModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(SakuraHarunoEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.875F, 0.875F, 0.875F);
    }

    @Override
    public ResourceLocation getTextureLocation(SakuraHarunoEntity entity) {
        return TEXTURE;
    }
}
