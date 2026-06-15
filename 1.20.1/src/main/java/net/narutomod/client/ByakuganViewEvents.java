package net.narutomod.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.AltCamViewEntity;
import net.narutomod.item.ByakuganHandler;
import net.narutomod.registry.ModEntityTypes;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class ByakuganViewEvents {
    private static final List<LivingEntity> GLOWING_ENTITIES = new ArrayList<>();
    private static AltCamViewEntity cameraEntity;
    private static int previousRenderDistance = -1;

    private ByakuganViewEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickByakuganCameraAndGlow();
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov() && ClientByakuganViewState.isActiveForLocalPlayer()) {
            event.setFOV(ClientByakuganViewState.fov());
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!ClientByakuganViewState.isActiveForLocalPlayer()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphics graphics = event.getGuiGraphics();

        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.INVERT);
        graphics.fill(0, 0, width, height, 0x1AFFFFFF);
        RenderSystem.logicOp(GlStateManager.LogicOp.COPY);
        RenderSystem.disableColorLogicOp();

        int centerX = width / 2;
        int centerY = height / 2;
        graphics.fill(centerX - 5, centerY, centerX + 5, centerY + 1, -1);
        graphics.fill(centerX, centerY - 5, centerX + 1, centerY + 5, -1);
    }

    private static void tickByakuganCameraAndGlow() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || !ClientByakuganViewState.isActiveForLocalPlayer()) {
            resetByakuganCameraAndGlow(minecraft);
            return;
        }

        ensureCameraEntity(minecraft, player);
        updateCameraEntity(minecraft, player);
        updateGlowingEntities(minecraft, player);
    }

    private static void ensureCameraEntity(Minecraft minecraft, LocalPlayer player) {
        if (cameraEntity == null || cameraEntity.level() != minecraft.level) {
            cameraEntity = ModEntityTypes.ALTCAMVIEWENTITY.get().create(minecraft.level);
        }
        if (previousRenderDistance < 0) {
            previousRenderDistance = minecraft.options.renderDistance().get();
            minecraft.options.renderDistance().set(ByakuganHandler.targetRenderDistance(ClientByakuganViewState.ninjaLevel()));
        }
        if (cameraEntity != null && minecraft.getCameraEntity() != cameraEntity) {
            minecraft.setCameraEntity(cameraEntity);
        }
    }

    private static void updateCameraEntity(Minecraft minecraft, LocalPlayer player) {
        if (cameraEntity == null) {
            return;
        }
        float fov = ClientByakuganViewState.fov();
        double distance = ByakuganHandler.projectedCameraDistance(fov, ClientByakuganViewState.ninjaLevel());
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        cameraEntity.moveTo(
                eye.x + look.x * distance,
                eye.y + look.y * distance,
                eye.z + look.z * distance,
                player.getYRot(),
                player.getXRot());
        cameraEntity.setYHeadRot(player.getYHeadRot());
        cameraEntity.setDeltaMovement(Vec3.ZERO);
        if (minecraft.getCameraEntity() != cameraEntity) {
            minecraft.setCameraEntity(cameraEntity);
        }
    }

    private static void updateGlowingEntities(Minecraft minecraft, LocalPlayer player) {
        double radius = minecraft.options.renderDistance().get() * 8.0D;
        for (LivingEntity entity : minecraft.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && !entity.isCurrentlyGlowing())) {
            entity.setGlowingTag(true);
            GLOWING_ENTITIES.add(entity);
        }
    }

    private static void resetByakuganCameraAndGlow(Minecraft minecraft) {
        if (cameraEntity != null) {
            if (minecraft.player != null && minecraft.getCameraEntity() == cameraEntity) {
                minecraft.setCameraEntity(minecraft.player);
            }
            cameraEntity.discard();
            cameraEntity = null;
        }
        if (previousRenderDistance >= 0) {
            minecraft.options.renderDistance().set(previousRenderDistance);
            previousRenderDistance = -1;
        }
        if (!GLOWING_ENTITIES.isEmpty()) {
            for (LivingEntity entity : GLOWING_ENTITIES) {
                if (entity.isAlive() && !entity.isInvisible()) {
                    entity.setGlowingTag(false);
                }
            }
            GLOWING_ENTITIES.clear();
        }
    }
}
