package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.Snake8HeadsEntity;

public final class Snake8HeadsModel extends EntityModel<Snake8HeadsEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("snake_8_heads"),
            "main"
    );
    private static final int HEAD_COUNT = 8;
    private static final int NECK_SEGMENT_COUNT = 10;
    private static final float UP_TIME = 40.0F;
    private static final float WAIT_TIME = 20.0F;
    private static final float[][][] NECK_ROTATION = {
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.0436F, -0.0436F, 0.0F},
                    {-0.2618F, -0.0436F, 0.0F}, {0.2618F, -0.0436F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.5236F, 0.0F, -0.0436F}, {0.5236F, 0.0F, 0.0F},
                    {0.4363F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.0436F, 0.0436F, 0.0F},
                    {-0.2618F, 0.0436F, 0.0F}, {0.2618F, 0.0436F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.5236F, 0.0F, 0.0436F}, {0.5236F, 0.0F, 0.0F},
                    {0.4363F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.0873F, 0.1309F, 0.0F},
                    {-0.2618F, 0.1309F, 0.0F}, {0.2618F, 0.1309F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.3927F, 0.0F, 0.0873F}, {0.3927F, 0.0F, 0.0F},
                    {0.3927F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.0873F, -0.1309F, 0.0F},
                    {-0.2618F, -0.1309F, 0.0F}, {0.2618F, -0.1309F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.3927F, 0.0F, -0.0873F}, {0.3927F, 0.0F, 0.0F},
                    {0.3927F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.2618F, 0.2618F, 0.0F},
                    {-0.2618F, 0.2618F, 0.0F}, {0.2618F, 0.2618F, 0.0F},
                    {0.2618F, 0.2618F, 0.0F}, {0.2618F, -0.2618F, 0.1745F},
                    {0.4363F, -0.2618F, 0.0873F}, {0.4363F, 0.0F, 0.0873F},
                    {0.5236F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.2618F, -0.2618F, 0.0F},
                    {-0.2618F, -0.2618F, 0.0F}, {0.2618F, -0.2618F, 0.0F},
                    {0.2618F, -0.2618F, 0.0F}, {0.2618F, 0.2618F, -0.1745F},
                    {0.4363F, 0.2618F, -0.1745F}, {0.4363F, 0.0F, 0.0F},
                    {0.5236F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.2618F, 0.1309F, 0.0F},
                    {-0.2618F, 0.1309F, 0.0F}, {0.2618F, 0.1309F, 0.0F},
                    {0.2618F, -0.1309F, 0.0F}, {0.2618F, -0.0873F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.3491F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            },
            {
                    {-1.5708F, 0.0F, 0.0F}, {-0.2618F, -0.1309F, 0.0F},
                    {-0.2618F, -0.1309F, 0.0F}, {0.2618F, -0.1309F, 0.0F},
                    {0.2618F, 0.1309F, 0.0F}, {0.2618F, 0.1309F, 0.0F},
                    {0.2618F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F},
                    {0.3491F, 0.0F, 0.0F}, {0.2618F, 0.0F, 0.0F}
            }
    };

    private final ModelPart root;
    private final ModelPart tails;
    private final ModelPart[][] necks = new ModelPart[HEAD_COUNT][NECK_SEGMENT_COUNT];
    private final ModelPart[] heads = new ModelPart[HEAD_COUNT];
    private final float[][] neckSway = new float[HEAD_COUNT][NECK_SEGMENT_COUNT];

    public Snake8HeadsModel(ModelPart root) {
        this.root = root;
        this.tails = root.getChild("tails");
        Random random = new Random();
        for (int i = 0; i < this.necks.length; i++) {
            ModelPart segment = this.tails.getChild("neck_" + i + "_0");
            this.necks[i][0] = segment;
            for (int j = 1; j < this.necks[i].length; j++) {
                segment = segment.getChild("neck_" + i + "_" + j);
                this.necks[i][j] = segment;
                this.neckSway[i][j] = (random.nextFloat() - 0.5F) * 2.0F;
            }
            this.heads[i] = segment.getChild("head" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySnake8Heads_ModelSnake8h_341();
    }

    @Override
    public void setupAnim(Snake8HeadsEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float localAge = Math.max(0.0F, entity.getTicksAlive() + ageInTicks - entity.tickCount);
        applyNeckAnimation(localAge);
    }

    private void applyNeckAnimation(float ageInTicks) {
        if (ageInTicks <= UP_TIME) {
            for (int i = 0; i < this.necks.length; i++) {
                applyBaseRotation(i, 0);
                applyBaseRotation(i, 1);
                for (int j = 2; j < this.necks[i].length; j++) {
                    ModelPart neck = this.necks[i][j];
                    neck.xRot = 0.0F;
                    neck.yRot = Mth.sin((ageInTicks + j) * 0.4F) * this.neckSway[i][j] * 0.2618F;
                    neck.zRot = 0.0F;
                }
            }
            return;
        }

        if (ageInTicks <= UP_TIME + WAIT_TIME) {
            float factor = (ageInTicks - UP_TIME) / WAIT_TIME;
            float eased = factor * factor;
            for (int i = 0; i < this.necks.length; i++) {
                applyBaseRotation(i, 0);
                applyBaseRotation(i, 1);
                for (int j = 2; j < this.necks[i].length; j++) {
                    applyScaledBaseRotation(i, j, eased);
                }
            }
            return;
        }

        for (int i = 0; i < this.necks.length; i++) {
            for (int j = 0; j < this.necks[i].length; j++) {
                applyBaseRotation(i, j);
            }
        }
        for (int i = 1; i < this.necks.length; i++) {
            for (int j = 1; j < this.necks[i].length; j++) {
                float[] base = NECK_ROTATION[i][j];
                float phase = (ageInTicks - j * (i + 1)) * 0.05F;
                ModelPart neck = this.necks[i][j];
                neck.xRot = base[0] + Mth.sin(phase) * this.neckSway[i][j] * 0.0436F;
                neck.yRot = base[1] + Mth.cos(phase) * this.neckSway[i][j] * 0.0436F;
                neck.zRot = base[2];
            }
        }
    }

    private void applyBaseRotation(int head, int segment) {
        applyScaledBaseRotation(head, segment, 1.0F);
    }

    private void applyScaledBaseRotation(int head, int segment, float scale) {
        float[] rotation = NECK_ROTATION[head][segment];
        ModelPart neck = this.necks[head][segment];
        neck.xRot = rotation[0] * scale;
        neck.yRot = rotation[1] * scale;
        neck.zRot = rotation[2] * scale;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
