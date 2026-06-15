package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.item.NinjaArmorItem;

public final class NinjaArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("ninja_armor_legacy"),
            "main"
    );

    private final ModelPart maskAme;
    private final ModelPart maskSamurai;
    private final ModelPart collar;
    private final ModelPart shirt;
    private final ModelPart vest;
    private final ModelPart vestGroupKonoha;
    private final ModelPart vestGroupSuna;
    private final ModelPart vestGroupKiri;
    private final ModelPart vestGroupKumo;
    private final ModelPart vestGroupSamurai;
    private final ModelPart shirtRightArm;
    private final ModelPart rightArmVestLayer;
    private final ModelPart rightShoulder;
    private final ModelPart war1RightShoulder;
    private final ModelPart shirtLeftArm;
    private final ModelPart leftArmVestLayer;
    private final ModelPart leftShoulder;
    private final ModelPart war1LeftShoulder;
    private final ModelPart rightLegLayer;
    private final ModelPart stoneCloth;
    private final ModelPart rightLegPad;
    private final ModelPart leftLegLayer;
    private final ModelPart leftLegPad;

    public NinjaArmorModel(ModelPart root) {
        super(root);
        this.maskAme = this.head.getChild("maskAme");
        this.maskSamurai = this.head.getChild("maskSamurai");
        this.collar = this.hat.getChild("collar");
        this.shirt = this.body.getChild("shirt");
        this.vest = this.body.getChild("vest");
        this.vestGroupKonoha = this.vest.getChild("vestGroupKonoha");
        this.vestGroupSuna = this.vest.getChild("vestGroupSuna");
        this.vestGroupKiri = this.vest.getChild("vestGroupKiri");
        this.vestGroupKumo = this.vest.getChild("vestGroupKumo");
        this.vestGroupSamurai = this.vest.getChild("vestGroupSamurai");
        this.shirtRightArm = this.rightArm.getChild("shirtRightArm");
        this.rightArmVestLayer = this.rightArm.getChild("rightArmVestLayer");
        this.rightShoulder = this.rightArmVestLayer.getChild("rightShoulder");
        this.war1RightShoulder = this.rightShoulder.getChild("war1RightShoulder");
        this.shirtLeftArm = this.leftArm.getChild("shirtLeftArm");
        this.leftArmVestLayer = this.leftArm.getChild("leftArmVestLayer");
        this.leftShoulder = this.leftArmVestLayer.getChild("leftShoulder");
        this.war1LeftShoulder = this.leftShoulder.getChild("war1LeftShoulder");
        this.rightLegLayer = this.rightLeg.getChild("rightLegLayer");
        this.stoneCloth = this.rightLeg.getChild("stoneCloth");
        this.rightLegPad = this.rightLeg.getChild("rightLegPad");
        this.leftLegLayer = this.leftLeg.getChild("leftLegLayer");
        this.leftLegPad = this.leftLeg.getChild("leftLegPad");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F))
                        .texOffs(0, 48)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.15F)),
                PartPose.ZERO
        );
        PartDefinition maskAme = head.addOrReplaceChild(
                "maskAme",
                CubeListBuilder.create()
                        .texOffs(39, 9)
                        .addBox(-2.0F, -1.6F, -0.9F, 4.0F, 3.0F, 2.0F, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(0.0F, -1.125F, -4.4F, 0.0873F, 0.0F, 0.0F)
        );
        maskAme.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create()
                        .texOffs(50, 11)
                        .addBox(-0.5F, -0.1F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F))
                        .texOffs(54, 11)
                        .addBox(-0.5F, 1.7F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(-2.1645F, -0.6361F, -0.2913F, -0.2618F, 0.0F, 0.1309F)
        );
        PartDefinition maskSamurai = head.addOrReplaceChild(
                "maskSamurai",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -0.775F, -3.175F, 0.6109F, 0.0F, 0.0F)
        );
        maskSamurai.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(28, 0)
                        .addBox(-2.0F, -1.975F, -0.975F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.7071F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F)
        );

        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        hat.addOrReplaceChild(
                "collar",
                CubeListBuilder.create()
                        .texOffs(34, 8)
                        .addBox(-4.0F, -25.1F, -3.1F, 8.0F, 1.0F, 7.0F, new CubeDeformation(0.8F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild(
                "shirt",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        PartDefinition vest = body.addOrReplaceChild(
                "vest",
                CubeListBuilder.create()
                        .texOffs(40, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.2F))
                        .texOffs(16, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.4F))
                        .texOffs(52, 0)
                        .addBox(0.1F, 8.3F, 1.75F, 4.0F, 4.0F, 2.0F, new CubeDeformation(-0.5F)),
                PartPose.ZERO
        );
        vest.addOrReplaceChild(
                "vestGroupKonoha",
                CubeListBuilder.create()
                        .texOffs(26, 0)
                        .addBox(-4.3F, 2.5F, -3.1F, 4.0F, 5.0F, 3.0F, new CubeDeformation(-0.7F))
                        .texOffs(26, 0)
                        .mirror()
                        .addBox(0.3F, 2.5F, -3.1F, 4.0F, 5.0F, 3.0F, new CubeDeformation(-0.7F))
                        .mirror(false),
                PartPose.ZERO
        );
        vest.addOrReplaceChild(
                "vestGroupSuna",
                CubeListBuilder.create()
                        .texOffs(26, 0)
                        .addBox(-4.3F, 5.1F, -3.1F, 4.0F, 5.0F, 3.0F, new CubeDeformation(-0.7F))
                        .texOffs(26, 0)
                        .mirror()
                        .addBox(0.3F, 5.1F, -3.1F, 4.0F, 5.0F, 3.0F, new CubeDeformation(-0.7F))
                        .mirror(false),
                PartPose.ZERO
        );
        vest.addOrReplaceChild(
                "vestGroupKiri",
                CubeListBuilder.create()
                        .texOffs(48, 8)
                        .addBox(-4.0F, 10.3F, -2.275F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 8)
                        .addBox(-4.0F, 10.3F, 2.275F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );
        vest.addOrReplaceChild(
                "vestGroupKumo",
                CubeListBuilder.create()
                        .texOffs(40, 9)
                        .mirror()
                        .addBox(-4.0F, 11.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.31F))
                        .mirror(false),
                PartPose.ZERO
        );
        PartDefinition vestGroupSamurai = vest.addOrReplaceChild("vestGroupSamurai", CubeListBuilder.create(), PartPose.ZERO);
        vestGroupSamurai.addOrReplaceChild(
                "flapRight",
                CubeListBuilder.create()
                        .texOffs(25, 50)
                        .mirror()
                        .addBox(-7.3F, 0.425F, -2.0F, 7.0F, 1.0F, 4.0F, new CubeDeformation(0.31F))
                        .mirror(false),
                PartPose.offsetAndRotation(-4.25F, 10.05F, 0.0F, 0.0F, 0.0F, -1.309F)
        );
        vestGroupSamurai.addOrReplaceChild(
                "flapLeft",
                CubeListBuilder.create()
                        .texOffs(25, 50)
                        .addBox(0.3F, 0.425F, -2.0F, 7.0F, 1.0F, 4.0F, new CubeDeformation(0.31F)),
                PartPose.offsetAndRotation(4.25F, 10.05F, 0.0F, 0.0F, 0.0F, 1.309F)
        );

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild(
                "shirtRightArm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-8.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offset(5.0F, 22.0F, 0.0F)
        );
        PartDefinition rightArmVestLayer = rightArm.addOrReplaceChild(
                "rightArmVestLayer",
                CubeListBuilder.create()
                        .texOffs(48, 48)
                        .mirror()
                        .addBox(-8.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F))
                        .mirror(false),
                PartPose.offset(5.0F, 22.0F, 0.0F)
        );
        PartDefinition rightShoulder = rightArmVestLayer.addOrReplaceChild(
                "rightShoulder",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.31F))
                        .mirror(false),
                PartPose.offsetAndRotation(-4.5F, -25.25F, 0.0F, 0.0F, 0.0F, -0.3054F)
        );
        PartDefinition war1RightShoulder = rightShoulder.addOrReplaceChild(
                "war1RightShoulder",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3491F)
        );
        war1RightShoulder.addOrReplaceChild(
                "rightShoulder3",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3491F)
        );

        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild(
                "shirtLeftArm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(4.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(-5.0F, 22.0F, 0.0F)
        );
        PartDefinition leftArmVestLayer = leftArm.addOrReplaceChild(
                "leftArmVestLayer",
                CubeListBuilder.create()
                        .texOffs(48, 48)
                        .addBox(-2.0F, -6.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.offset(1.0F, 4.0F, 0.0F)
        );
        PartDefinition leftShoulder = leftArmVestLayer.addOrReplaceChild(
                "leftShoulder",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.31F)),
                PartPose.offsetAndRotation(-1.5F, -7.25F, 0.0F, 0.0F, 0.0F, 0.3054F)
        );
        PartDefinition war1LeftShoulder = leftShoulder.addOrReplaceChild(
                "war1LeftShoulder",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3491F)
        );
        war1LeftShoulder.addOrReplaceChild(
                "leftShoulder3",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3491F)
        );

        PartDefinition rightLeg = root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        rightLeg.addOrReplaceChild(
                "rightLegLayer",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F))
                        .texOffs(0, 0)
                        .addBox(-2.6F, 1.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );
        rightLeg.addOrReplaceChild(
                "stoneCloth",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(-3.2F, -6.8F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.1745F)
        );
        PartDefinition rightLegPad = rightLeg.addOrReplaceChild(
                "rightLegPad",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.31F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.35F, -2.25F, 0.0F, 0.0F, 0.0F, -1.309F)
        );
        PartDefinition rightLegPad1 = rightLegPad.addOrReplaceChild(
                "rightLegPad1",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0873F)
        );
        rightLegPad1.addOrReplaceChild(
                "rightLegPad2",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .mirror()
                        .addBox(-4.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0873F)
        );

        PartDefinition leftLeg = root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offset(1.9F, 12.0F, 0.0F)
        );
        leftLeg.addOrReplaceChild(
                "leftLegLayer",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .mirror()
                        .addBox(-0.1F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        PartDefinition leftLegPad = leftLeg.addOrReplaceChild(
                "leftLegPad",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.31F)),
                PartPose.offsetAndRotation(2.35F, -2.25F, 0.0F, 0.0F, 0.0F, 1.309F)
        );
        PartDefinition leftLegPad1 = leftLegPad.addOrReplaceChild(
                "leftLegPad1",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0873F)
        );
        leftLegPad1.addOrReplaceChild(
                "leftLegPad2",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(0.3F, 0.3F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0873F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void copyStandardPoseFrom(HumanoidModel<?> original) {
        this.head.copyFrom(original.head);
        this.hat.copyFrom(original.hat);
        this.body.copyFrom(original.body);
        this.rightArm.copyFrom(original.rightArm);
        this.leftArm.copyFrom(original.leftArm);
        this.rightLeg.copyFrom(original.rightLeg);
        this.leftLeg.copyFrom(original.leftLeg);
        this.attackTime = original.attackTime;
        this.riding = original.riding;
        this.young = original.young;
        this.crouching = original.crouching;
        this.rightArmPose = original.rightArmPose;
        this.leftArmPose = original.leftArmPose;
    }

    public void configureFor(NinjaArmorItem.Style style, EquipmentSlot slot, boolean slimModel) {
        this.setAllVisible(false);
        this.rightArm.y = slimModel ? 2.5F : 2.0F;
        this.leftArm.y = slimModel ? 2.5F : 2.0F;

        if (slot == EquipmentSlot.HEAD) {
            this.head.visible = true;
            this.maskAme.visible = style == NinjaArmorItem.Style.AME;
            this.maskSamurai.visible = style == NinjaArmorItem.Style.SAMURAI;
            return;
        }

        if (slot == EquipmentSlot.CHEST) {
            this.body.visible = true;
            this.rightArm.visible = true;
            this.leftArm.visible = true;
            this.vest.visible = true;
            this.rightArmVestLayer.visible = true;
            this.leftArmVestLayer.visible = true;
            configureVestStyle(style);
            configureShoulders(style);
            if (style == NinjaArmorItem.Style.KONOHA || style == NinjaArmorItem.Style.SUNA || style == NinjaArmorItem.Style.WAR1) {
                this.hat.visible = true;
                this.collar.visible = true;
            }
            if (style == NinjaArmorItem.Style.WAR1) {
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
                configureLegStyle(style);
            }
            return;
        }

        if (slot == EquipmentSlot.LEGS) {
            this.rightLeg.visible = true;
            this.leftLeg.visible = true;
            configureLegStyle(style);
            configureLegSlotArms(style);
            return;
        }

        if (slot == EquipmentSlot.FEET) {
            this.rightLeg.visible = true;
            this.leftLeg.visible = true;
            this.rightLegLayer.visible = true;
            this.leftLegLayer.visible = true;
        }
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.maskAme.visible = visible;
        this.maskSamurai.visible = visible;
        this.collar.visible = visible;
        this.shirt.visible = visible;
        this.vest.visible = visible;
        this.vestGroupKonoha.visible = visible;
        this.vestGroupSuna.visible = visible;
        this.vestGroupKiri.visible = visible;
        this.vestGroupKumo.visible = visible;
        this.vestGroupSamurai.visible = visible;
        this.shirtRightArm.visible = visible;
        this.rightArmVestLayer.visible = visible;
        this.rightShoulder.visible = visible;
        this.war1RightShoulder.visible = visible;
        this.shirtLeftArm.visible = visible;
        this.leftArmVestLayer.visible = visible;
        this.leftShoulder.visible = visible;
        this.war1LeftShoulder.visible = visible;
        this.rightLegLayer.visible = visible;
        this.stoneCloth.visible = visible;
        this.rightLegPad.visible = visible;
        this.leftLegLayer.visible = visible;
        this.leftLegPad.visible = visible;
    }

    private void configureVestStyle(NinjaArmorItem.Style style) {
        this.vestGroupKonoha.visible = style == NinjaArmorItem.Style.KONOHA;
        this.vestGroupSuna.visible = style == NinjaArmorItem.Style.SUNA;
        this.vestGroupKiri.visible = style == NinjaArmorItem.Style.KIRI;
        this.vestGroupKumo.visible = style == NinjaArmorItem.Style.KUMO;
        this.vestGroupSamurai.visible = style == NinjaArmorItem.Style.SAMURAI;
    }

    private void configureShoulders(NinjaArmorItem.Style style) {
        boolean shoulders = style == NinjaArmorItem.Style.SUNA || style == NinjaArmorItem.Style.KIRI
                || style == NinjaArmorItem.Style.WAR1 || style == NinjaArmorItem.Style.SAMURAI;
        boolean stackedShoulders = style == NinjaArmorItem.Style.WAR1 || style == NinjaArmorItem.Style.SAMURAI;
        this.rightShoulder.visible = shoulders;
        this.leftShoulder.visible = shoulders;
        this.war1RightShoulder.visible = stackedShoulders;
        this.war1LeftShoulder.visible = stackedShoulders;
    }

    private void configureLegStyle(NinjaArmorItem.Style style) {
        this.rightLegLayer.visible = true;
        this.stoneCloth.visible = style == NinjaArmorItem.Style.IWA;
        this.rightLegPad.visible = style == NinjaArmorItem.Style.WAR1;
        this.leftLegLayer.visible = style == NinjaArmorItem.Style.KIRI || style == NinjaArmorItem.Style.KUMO
                || style == NinjaArmorItem.Style.JUMPSUIT || style == NinjaArmorItem.Style.SAMURAI;
        this.leftLegPad.visible = style == NinjaArmorItem.Style.WAR1;
    }

    private void configureLegSlotArms(NinjaArmorItem.Style style) {
        if (style == NinjaArmorItem.Style.IWA) {
            this.leftArm.visible = true;
            this.shirtLeftArm.visible = true;
            return;
        }
        boolean bothArms = style == NinjaArmorItem.Style.KONOHA || style == NinjaArmorItem.Style.SUNA
                || style == NinjaArmorItem.Style.KIRI || style == NinjaArmorItem.Style.ANBU
                || style == NinjaArmorItem.Style.FISHNET || style == NinjaArmorItem.Style.JUMPSUIT
                || style == NinjaArmorItem.Style.SAMURAI;
        if (bothArms) {
            this.rightArm.visible = true;
            this.leftArm.visible = true;
            this.shirtRightArm.visible = true;
            this.shirtLeftArm.visible = true;
        }
    }
}
