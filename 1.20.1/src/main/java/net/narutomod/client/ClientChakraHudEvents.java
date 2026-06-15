package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.item.SenjutsuItem;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class ClientChakraHudEvents {
    private static final ResourceLocation SAGE_FLAMES = NarutomodMod.location("textures/flames_green.png");
    private static final int BAR_WIDTH = 80;
    private static int warningTime;

    private ClientChakraHudEvents() {
    }

    public static void warn(int ticks) {
        warningTime = Math.max(warningTime, Math.max(0, ticks));
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HELMET.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !PlayerTracker.isNinja(player)) {
            tickWarning();
            return;
        }
        Chakra.Pathway pathway = Chakra.pathway(player);
        double max = pathway.getMax();
        if (max <= 0.0D) {
            tickWarning();
            return;
        }
        renderChakraBar(event.getGuiGraphics(), minecraft, player, pathway, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());
        tickWarning();
    }

    private static void renderChakraBar(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, Chakra.Pathway pathway,
            int width, int height) {
        double ratio = pathway.getAmount() / pathway.getMax();
        double partial = ratio - Math.floor(ratio);
        if (ratio != 0.0D && partial == 0.0D) {
            partial = 1.0D;
        }
        int top = height - (4 * ((int)Math.ceil(ratio) - 1) + 9);
        int left = width / 2 - 200;
        int warningColor = warningTime % 20 < 10 ? 0xFF00FFFF : 0xFFFF0000;

        if (isSageModeActive(player)) {
            int flameWidth = BAR_WIDTH + 10;
            int flameHeight = height - top + 20;
            int frame = player.tickCount % 8 / 2;
            if (flameHeight > 0) {
                graphics.blit(SAGE_FLAMES, left - 5, top - 20, 0.0F, (float)frame * flameHeight,
                        flameWidth, flameHeight, flameWidth, flameHeight * 4);
            }
        }
        graphics.fill(left - 1, top - 1, left + BAR_WIDTH + 1, height - 5, 0xFF202020);
        for (int y = top; y <= height - 9; y += 4) {
            int fillWidth = (int)((y == top ? partial : 1.0D) * BAR_WIDTH);
            int color = y == height - 9 ? warningColor : 0xFFFFFF00;
            graphics.fill(left, y, left + fillWidth, y + 3, color);
        }
        graphics.drawString(
                minecraft.font,
                String.format("%d/%d", (int)pathway.getAmount(), (int)pathway.getMax()),
                left + 12,
                top - 10,
                warningColor,
                true);
    }

    private static void tickWarning() {
        if (warningTime > 0) {
            warningTime--;
        }
    }

    private static boolean isSageModeActive(LocalPlayer player) {
        return player.getCapability(NarutomodModVariables.PLAYER_VARIABLES)
                .map(variables -> variables.getBoolean(SenjutsuItem.SAGE_MODE_ACTIVATED_TAG))
                .orElse(false);
    }
}
