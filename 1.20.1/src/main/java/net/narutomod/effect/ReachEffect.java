package net.narutomod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

public final class ReachEffect extends MobEffect {
    public static final String REACH_MODIFIER_ID = "0a0f65da-11f0-4219-9dc9-dab0efa743c0";

    public ReachEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(ForgeMod.ENTITY_REACH.get(), REACH_MODIFIER_ID, 1.0D, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
