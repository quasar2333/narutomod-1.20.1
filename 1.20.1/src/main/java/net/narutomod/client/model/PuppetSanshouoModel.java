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
import net.narutomod.entity.PuppetSanshouoEntity;

public final class PuppetSanshouoModel extends EntityModel<PuppetSanshouoEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("puppet_sanshouo_legacy"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart[] tail;

    public PuppetSanshouoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leg1 = this.body.getChild("leg1");
        this.leg2 = this.body.getChild("leg2");
        this.leg3 = this.body.getChild("leg3");
        this.leg4 = this.body.getChild("leg4");
        this.tail = new ModelPart[] {
                this.body.getChild("tail_0"),
                this.body.getChild("tail_0").getChild("tail_1"),
                this.body.getChild("tail_0").getChild("tail_1").getChild("tail_2"),
                this.body.getChild("tail_0").getChild("tail_1").getChild("tail_2").getChild("tail_3"),
                this.body.getChild("tail_0").getChild("tail_1").getChild("tail_2").getChild("tail_3").getChild("tail_4"),
                this.body.getChild("tail_0").getChild("tail_1").getChild("tail_2").getChild("tail_3").getChild("tail_4")
                        .getChild("tail_5")
        };
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityPuppetSanshouo_ModelSanShouo_232();
    }

    @Override
    public void setupAnim(PuppetSanshouoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.leg1.zRot = Mth.cos(limbSwing * 0.5F) * 0.5F * limbSwingAmount;
        this.leg2.zRot = -Mth.cos(limbSwing * 0.5F + Mth.PI) * 0.5F * limbSwingAmount;
        this.leg3.zRot = Mth.cos(limbSwing * 0.5F + Mth.PI) * 0.5F * limbSwingAmount;
        this.leg4.zRot = -Mth.cos(limbSwing * 0.5F) * 0.5F * limbSwingAmount;
        for (int index = 1; index < this.tail.length; ++index) {
            this.tail[index].zRot = Mth.cos(limbSwing * 0.4F + index * 0.15F * Mth.PI)
                    * Mth.PI
                    * 0.04F
                    * (1 + Math.abs(index - 2));
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
