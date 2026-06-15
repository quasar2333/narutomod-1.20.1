package net.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.narutomod.NarutomodMod;
import net.narutomod.menu.JutsuScrollMenu;
import net.narutomod.network.JutsuScrollLearnMessage;
import net.narutomod.network.NetworkHandler;

public final class JutsuScrollScreen extends AbstractContainerScreen<JutsuScrollMenu> {
    private static final ResourceLocation SCROLL_BACKGROUND = NarutomodMod.location("textures/scoll_screen.png");

    public JutsuScrollScreen(JutsuScrollMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Learn"), button -> NetworkHandler.sendToServer(
                        new JutsuScrollLearnMessage(this.menu.definition().id())))
                .bounds(this.leftPos - 56, this.topPos + 127, 39, 20)
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
        graphics.blit(SCROLL_BACKGROUND, this.leftPos - 70, this.topPos - 70, 0, 0, 320, 320, 320, 320);
        graphics.blit(this.menu.definition().iconTexture(), this.leftPos + 89, this.topPos + 49, 0, 0, 48, 48, 48, 48);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, Component.translatable(this.menu.definition().jutsuNameKey()), 38, 13, 0x000000, false);
    }
}
