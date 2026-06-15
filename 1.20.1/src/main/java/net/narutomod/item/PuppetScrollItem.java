package net.narutomod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.narutomod.entity.AbstractPuppetScrollEntity;
import net.narutomod.entity.PuppetKarasuEntity;
import net.narutomod.entity.PuppetSanshouoEntity;
import net.narutomod.registry.ModEntityTypes;

public final class PuppetScrollItem extends Item implements ItemOnBody.Interface {
    private static final String SEALED_TAG = "sealed";
    private final PuppetScrollKind kind;

    public PuppetScrollItem(PuppetScrollKind kind) {
        super(new Item.Properties().stacksTo(1).durability(kind.maxHealth));
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null || context.getClickedFace() != Direction.UP
                || !level.getBlockState(context.getClickedPos()).isFaceSturdy(level, context.getClickedPos(), Direction.UP)) {
            return InteractionResult.PASS;
        }
        if (!isSealed(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            AbstractPuppetScrollEntity scroll = this.kind.createOpeningScroll(serverLevel);
            if (scroll == null) {
                return InteractionResult.FAIL;
            }
            BlockPos pos = context.getClickedPos().above();
            scroll.configure(player, Mth.clamp(this.kind.maxHealth - stack.getDamageValue(), 1.0F, this.kind.maxHealth));
            scroll.moveTo(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.getYRot(), 0.0F);
            serverLevel.addFreshEntity(scroll);
            setSealed(stack, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1.0F,
                    1.0F / (player.getRandom().nextFloat() * 0.5F + 1.0F) + 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!this.kind.matches(target) || isSealed(stack)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            int damage = Mth.clamp(Math.round(this.kind.maxHealth - target.getHealth()), 0, stack.getMaxDamage());
            stack.setDamageValue(damage);
            setSealed(stack, true);
            AbstractPuppetScrollEntity.spawnPoof(player.level(), target);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
            target.discard();
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal(isSealed(stack) ? "sealed" : "deployed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("puppet_health=" + Math.max(this.kind.maxHealth - stack.getDamageValue(), 0)
                + "/" + this.kind.maxHealth).withStyle(ChatFormatting.GRAY));
    }

    public PuppetScrollKind kind() {
        return this.kind;
    }

    public static boolean isSealed(ItemStack stack) {
        return !stack.getOrCreateTag().contains(SEALED_TAG) || stack.getOrCreateTag().getBoolean(SEALED_TAG);
    }

    public static void setSealed(ItemStack stack, boolean sealed) {
        stack.getOrCreateTag().putBoolean(SEALED_TAG, sealed);
    }

    public enum PuppetScrollKind {
        KARASU((int) PuppetKarasuEntity.MAX_HEALTH) {
            @Override
            AbstractPuppetScrollEntity createOpeningScroll(ServerLevel level) {
                return ModEntityTypes.ENTITYBULLETSCROLL_KARASU.get().create(level);
            }

            @Override
            boolean matches(LivingEntity target) {
                return target instanceof PuppetKarasuEntity;
            }
        },
        SANSHOUO((int) PuppetSanshouoEntity.MAX_HEALTH) {
            @Override
            AbstractPuppetScrollEntity createOpeningScroll(ServerLevel level) {
                return ModEntityTypes.ENTITYBULLETSCROLL_SANSHOUO.get().create(level);
            }

            @Override
            boolean matches(LivingEntity target) {
                return target instanceof PuppetSanshouoEntity;
            }
        };

        private final int maxHealth;

        PuppetScrollKind(int maxHealth) {
            this.maxHealth = maxHealth;
        }

        abstract AbstractPuppetScrollEntity createOpeningScroll(ServerLevel level);

        abstract boolean matches(LivingEntity target);
    }
}
