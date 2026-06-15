package net.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.narutomod.NarutomodMod;
import net.narutomod.menu.MedicalScrollMenu;
import net.narutomod.network.MedicalScrollActivateMessage;
import net.narutomod.network.NetworkHandler;

public final class MedicalScrollScreen extends AbstractContainerScreen<MedicalScrollMenu> {
    private static final ResourceLocation BACKGROUND = NarutomodMod.location("textures/medicalscrollgui.png");
    private static final ResourceLocation SEAL = NarutomodMod.location("textures/seal_blank1.png");
    private static final ResourceLocation PRIMARY = NarutomodMod.location("textures/primary.png");
    private static final ResourceLocation SECONDARY = NarutomodMod.location("textures/secondary.png");

    public MedicalScrollScreen(MedicalScrollMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 175;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Activate"),
                        button -> NetworkHandler.sendToServer(new MedicalScrollActivateMessage()))
                .bounds(this.leftPos + 48, this.topPos + 63, 74, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        blitScaled(graphics, SEAL, this.leftPos + 61, this.topPos + 5, 48, 48);
        blitScaled(graphics, PRIMARY, this.leftPos + 14, this.topPos + 35, 32, 32);
        blitScaled(graphics, SECONDARY, this.leftPos + 131, this.topPos + 35, 32, 32);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private static void blitScaled(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
    }
}
