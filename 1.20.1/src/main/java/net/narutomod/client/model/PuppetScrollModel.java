package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.AbstractPuppetScrollEntity;

public final class PuppetScrollModel<T extends AbstractPuppetScrollEntity> extends EntityModel<T> {
    public static final ModelLayerLocation KARASU_LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("puppet_scroll_karasu_legacy"),
            "main"
    );
    public static final ModelLayerLocation SANSHOUO_LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("puppet_scroll_sanshouo_legacy"),
            "main"
    );

    private static final int BONE_COUNT = 14;
    private static final float OPEN_ANGLE = -1.0472F;

    private final ModelPart hinge;
    private final ModelPart[] bones = new ModelPart[BONE_COUNT];

    public PuppetScrollModel(ModelPart root) {
        this.hinge = root.getChild("hinge");
        this.bones[0] = root.getChild("bone_0");
        for (int i = 1; i < this.bones.length; i++) {
            this.bones[i] = this.bones[i - 1].getChild("bone_" + i);
        }
    }

    public static LayerDefinition createKarasuBodyLayer() {
        return LegacyModelLayerDefinitions.ItemScrollKarasu_ModelScroll_214();
    }

    public static LayerDefinition createSanshouoBodyLayer() {
        return LegacyModelLayerDefinitions.ItemScrollSanshouo_ModelScrollSanshouo_214();
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        for (int i = 1; i < this.bones.length; i++) {
            this.bones[i].xRot = Mth.clamp(1.0F - ageInTicks + i, 0.0F, 1.0F) * OPEN_ANGLE;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.hinge.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bones[0].render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
