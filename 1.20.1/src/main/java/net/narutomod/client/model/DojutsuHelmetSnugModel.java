package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;

public final class DojutsuHelmetSnugModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("dojutsu_helmet_snug"),
        "main"
    );

    private final ModelPart highlight;
    private final ModelPart forehead;

    public boolean headwearHidden;
    public boolean highlightHidden;
    public boolean foreheadHidden;

    public DojutsuHelmetSnugModel(ModelPart root) {
        super(root);
        this.highlight = root.getChild("highlight");
        this.forehead = root.getChild("forehead");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.05F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "hat",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "highlight",
            CubeListBuilder.create()
                .texOffs(24, 0)
                .addBox(-4.0F, -8.0F, -4.15F, 8.0F, 8.0F, 0.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "forehead",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -8.0F, -4.25F, 4.0F, 4.0F, 0.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        syncExtraHeadParts();
        this.hat.visible = !this.headwearHidden;
        this.highlight.visible = !this.highlightHidden && this.head.visible;
        this.forehead.visible = !this.foreheadHidden && this.head.visible;
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.highlight.visible = visible && !this.highlightHidden;
        this.forehead.visible = visible && !this.foreheadHidden;
    }

    public void renderHighlight(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!this.highlightHidden && this.head.visible) {
            syncExtraHeadParts();
            this.highlight.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    public void renderForehead(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!this.foreheadHidden && this.head.visible) {
            syncExtraHeadParts();
            this.forehead.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private void syncExtraHeadParts() {
        this.highlight.copyFrom(this.head);
        this.forehead.copyFrom(this.head);
    }
}
