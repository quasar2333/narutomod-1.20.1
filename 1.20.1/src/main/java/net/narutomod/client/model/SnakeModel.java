package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.AbstractSummonAnimalEntity;

public final class SnakeModel<T extends AbstractSummonAnimalEntity> extends EntityModel<T> {
    private static final int SEGMENT_COUNT = 21;
    private static final float BASE_HEIGHT = 0.25F;
    private static final float SEGMENT_SPACING = 4.0F;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("snake_legacy"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart headNeck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] segments = new ModelPart[SEGMENT_COUNT];

    public SnakeModel(ModelPart root) {
        this.root = root;
        this.headNeck = root.getChild("headNeck");
        this.head = this.headNeck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < this.segments.length; i++) {
            this.segments[i] = root.getChild("segment_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition headNeck = root.addOrReplaceChild(
                "headNeck",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -2.0F, -5.0F, 5.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 22.0F, 0.0F)
        );
        PartDefinition head = headNeck.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, -5.0F)
        );
        PartDefinition bone2 = head.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create()
                        .texOffs(17, 22)
                        .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.4F, -0.7F, -5.35F, 0.7854F, 0.0F, 0.6109F)
        );
        bone2.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create()
                        .texOffs(22, 5)
                        .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, -0.9599F, 0.0F, 0.0F)
        );
        PartDefinition bone4 = head.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create()
                        .texOffs(17, 22)
                        .mirror()
                        .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-1.4F, -0.7F, -5.35F, 0.7854F, 0.0F, -0.6109F)
        );
        bone4.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create()
                        .texOffs(22, 5)
                        .mirror()
                        .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, -0.9599F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone6",
                CubeListBuilder.create()
                        .texOffs(13, 10)
                        .addBox(-0.0076F, -1.5F, -3.8257F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0436F, 0.0873F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone7",
                CubeListBuilder.create()
                        .texOffs(13, 10)
                        .mirror()
                        .addBox(-2.9924F, -1.5F, -3.8257F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0436F, -0.0873F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone8",
                CubeListBuilder.create()
                        .texOffs(17, 17)
                        .addBox(-0.05F, -1.5F, -3.0757F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.15F, -1.1F, -2.5F, 0.5236F, 0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone9",
                CubeListBuilder.create()
                        .texOffs(17, 17)
                        .mirror()
                        .addBox(-2.95F, -1.5F, -3.0757F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.15F, -1.1F, -2.5F, 0.5236F, -0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone11",
                CubeListBuilder.create()
                        .texOffs(10, 19)
                        .addBox(-2.0F, -1.0F, -2.75F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19)
                        .addBox(-2.0F, -0.4F, -2.75F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.6F, 0.1F, -3.95F, 0.0F, 0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone19",
                CubeListBuilder.create()
                        .texOffs(10, 19)
                        .mirror()
                        .addBox(0.05F, -1.0F, -2.75F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19)
                        .addBox(0.05F, -0.4F, -2.75F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.65F, 0.1F, -3.95F, 0.0F, -0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
                "bone20",
                CubeListBuilder.create()
                        .texOffs(0, 1)
                        .addBox(-0.2F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 1)
                        .mirror()
                        .addBox(-3.0F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(1.6F, 1.8F, -5.95F)
        );
        PartDefinition jaw = head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.5F, 0.0F)
        );
        jaw.addOrReplaceChild(
                "bone21",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-3.0F, -1.0F, -6.7F, 3.0F, 2.0F, 7.0F, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(3.0F, 0.9F, 0.0F, 0.0F, 0.2182F, 0.0F)
        );
        jaw.addOrReplaceChild(
                "bone22",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .mirror()
                        .addBox(0.0F, -1.0F, -6.7F, 3.0F, 2.0F, 7.0F, new CubeDeformation(-0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(-3.0F, 0.9F, 0.0F, 0.0F, -0.2182F, 0.0F)
        );
        jaw.addOrReplaceChild(
                "bone23",
                CubeListBuilder.create()
                        .texOffs(0, 1)
                        .addBox(1.2F, -0.5F, -0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 1)
                        .mirror()
                        .addBox(-1.2F, -0.5F, -0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -0.2F, -5.5F, 3.1416F, 3.1416F, 0.0F)
        );
        PartDefinition horns = head.addOrReplaceChild(
                "horns",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.6F, 0.0F)
        );
        addHorn(horns, "bone24", -2.3F, -2.5F, -1.6F, 0.2618F, -0.5236F, false, true);
        addHorn(horns, "bone25", -1.2F, -2.5F, -1.2F, 0.4363F, -0.3491F, false, false);
        addHorn(horns, "bone26", 1.2F, -2.5F, -1.2F, 0.4363F, 0.3491F, true, false);
        addHorn(horns, "bone37", 2.3F, -2.5F, -1.6F, 0.2618F, 0.5236F, true, true);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float grow = i >= 12 ? (11 - i) * 0.2F : 0.0F;
            root.addOrReplaceChild(
                    "segment_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(-2.5F, -2.0F, -1.0F, 5.0F, 4.0F, 6.0F, new CubeDeformation(grow)),
                    PartPose.offset(0.0F, 22.0F, 0.0F)
            );
        }

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        float halfHeadYaw = netHeadYaw * 0.5F * Mth.DEG_TO_RAD;
        this.headNeck.yRot = halfHeadYaw;
        this.head.yRot = halfHeadYaw;
        if (headPitch == 0.0F) {
            this.headNeck.xRot = -0.2618F;
            this.head.xRot = 0.2618F;
        } else {
            float halfHeadPitch = headPitch * 0.5F * Mth.DEG_TO_RAD;
            this.headNeck.xRot = halfHeadPitch;
            this.head.xRot = halfHeadPitch;
        }
        this.jaw.xRot = this.attackTime > 0.0F || entity.getTarget() != null ? 0.5236F : 0.0F;

        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float scaledSwing = limbSwing * BASE_HEIGHT / Math.max(entity.getBbHeight(), 0.001F);
        float wave = scaledSwing * 0.75F + ageInTicks * 0.04F;
        float x = this.headNeck.x;
        float y = this.headNeck.y;
        float z = this.headNeck.z;
        float carryYaw = 0.0F;
        for (int i = 0; i < this.segments.length; i++) {
            float phase = wave - i * 0.45F;
            float xRot = Mth.cos(phase * 0.7F) * 0.04F * movement;
            float yRot = carryYaw + Mth.sin(phase) * (0.08F + 0.28F * movement);
            ModelPart segment = this.segments[i];
            segment.setPos(x, y, z);
            segment.xRot = xRot;
            segment.yRot = yRot;
            segment.zRot = 0.0F;
            x += Mth.sin(yRot) * Mth.cos(xRot) * SEGMENT_SPACING;
            y -= Mth.sin(xRot) * SEGMENT_SPACING;
            z += Mth.cos(yRot) * Mth.cos(xRot) * SEGMENT_SPACING;
            carryYaw = yRot * 0.2F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void addHorn(PartDefinition parent, String name, float x, float y, float z,
            float xRot, float yRot, boolean mirror, boolean longHorn) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(28, 0);
        if (mirror) {
            builder.mirror();
        }
        builder.addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F));
        builder.texOffs(28, 0).addBox(-0.5F, -0.5F, longHorn ? 1.0F : 0.9F, 1.0F, 1.0F, 1.0F,
                new CubeDeformation(longHorn ? 0.0F : -0.05F));
        builder.texOffs(28, 0).addBox(-0.5F, -0.5F, longHorn ? 1.9F : 1.6F, 1.0F, 1.0F, 1.0F,
                new CubeDeformation(-0.1F));
        builder.texOffs(28, 0).addBox(-0.5F, -0.5F, longHorn ? 2.6F : 2.1F, 1.0F, 1.0F, 1.0F,
                new CubeDeformation(-0.2F));
        if (longHorn) {
            builder.texOffs(28, 0).addBox(-0.5F, -0.5F, 3.1F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F));
        }
        if (mirror) {
            builder.mirror(false);
        }
        parent.addOrReplaceChild(name, builder, PartPose.offsetAndRotation(x, y, z, xRot, yRot, 0.0F));
    }
}
