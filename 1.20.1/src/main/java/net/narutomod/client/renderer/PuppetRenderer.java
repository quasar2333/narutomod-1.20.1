package net.narutomod.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.entity.AbstractPuppetEntity;

public final class PuppetRenderer<T extends AbstractPuppetEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public PuppetRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new PuppetChakraStringsLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return DEFAULT_TEXTURE;
    }
}
