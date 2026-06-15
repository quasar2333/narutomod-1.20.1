package net.narutomod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class RyoItem extends Item {
    private final String valueLabel;

    public RyoItem(String valueLabel) {
        super(new Item.Properties());
        this.valueLabel = valueLabel;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal(this.valueLabel).withStyle(ChatFormatting.GOLD));
    }
}
