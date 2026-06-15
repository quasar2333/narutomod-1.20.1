package net.narutomod.client.model;

import java.util.Random;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.TailedBeastEntity;

public final class SixTailsModel extends HumanoidModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("six_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart[][] horns = new ModelPart[2][5];
    private final ModelPart[][] tails = new ModelPart[6][6];
    private final float[][] hornSwayX = new float[2][5];
    private final float[][] hornSwayZ = new float[2][5];
    private final float[][] tailSwayX = new float[6][6];
    private final float[][] tailSwayZ = new float[6][6];

    public SixTailsModel(ModelPart root) {
        super(root);
        this.root = root;
        this.hat.visible = false;

        for (int i = 0; i < this.horns.length; i++) {
            ModelPart segment = this.head.getChild("Horn_" + i + "_0");
            this.horns[i][0] = segment;
            for (int j = 1; j < this.horns[i].length; j++) {
                segment = segment.getChild("Horn_" + i + "_" + j);
                this.horns[i][j] = segment;
            }
        }

        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = this.body.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
            }
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.hornSwayX.length; i++) {
            for (int j = 1; j < this.hornSwayX[i].length; j++) {
                this.hornSwayX[i][j] = (random.nextFloat() * 0.1745F + 0.0873F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.hornSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.0873F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
            }
        }
        for (int i = 0; i < this.tailSwayX.length; i++) {
            for (int j = 1; j < this.tailSwayX[i].length; j++) {
                this.tailSwayX[i][j] = (random.nextFloat() * 0.1745F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySixTails_ModelSixTails_218();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.hat.visible = false;
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.head.y += 14.0F;
        this.rightArm.z += 1.5F;
        this.rightArm.x += 2.0F;
        this.leftArm.z += 1.5F;
        this.leftArm.x += -2.0F;
        this.rightLeg.y += 7.0F;
        this.leftLeg.y += 7.0F;

        for (int i = 0; i < this.horns.length; i++) {
            for (int j = 1; j < this.horns[i].length; j++) {
                this.horns[i][j].xRot = -0.1745F + Mth.sin(ageInTicks * 0.1F) * this.hornSwayX[i][j];
                this.horns[i][j].zRot = Mth.cos(ageInTicks * 0.1F) * this.hornSwayZ[i][j];
            }
        }

        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i][0].visible = i < entity.getTailCount();
            for (int j = 1; j < this.tails[i].length; j++) {
                this.tails[i][j].xRot = 0.2618F + Mth.sin((ageInTicks - j) * 0.05F) * this.tailSwayX[i][j];
                this.tails[i][j].yRot = 0.0F;
                this.tails[i][j].zRot = Mth.cos((ageInTicks - j) * 0.05F) * this.tailSwayZ[i][j];
            }
        }
    }
}
