package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.AsakujakuFireballEntity;
import net.narutomod.entity.HirudoraEntity;
import net.narutomod.entity.NightGuyDragonEntity;
import net.narutomod.entity.SekizoEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class EightGatesItem extends Item {
    public static final String GATE_OPENED_TAG = "gateOpened";
    public static final String SEKIZO_PUNCH_COUNT_TAG = "sekizoPunchCount";
    public static final String BATTLE_EXPERIENCE_TAG = "battleExperience";
    private static final UUID GATE_HEALTH_MODIFIER = UUID.fromString("f6944d0f-5c81-45db-9261-6a9ad9fe4840");
    private static final float OPEN_INCREMENT = 0.05F;
    private static final int MAX_USE_DURATION = 72000;
    private static final GateProperties[] GATES = {
            new GateProperties(0, "", 0, 0, 0, 0, 0, 0, 0, 0.0F, false),
            new GateProperties(1, "chattext.eightgates.gate1", 220, 0, 0, 3, 2, 0, 10, -1.0F, false),
            new GateProperties(2, "chattext.eightgates.gate2", 240, 0, 0, 4, 16, 0, 40, -5.0F, false),
            new GateProperties(3, "chattext.eightgates.gate3", 280, 20, 0x10FFFFFF, 6, 32, 1, 60, -3.0F, false),
            new GateProperties(4, "chattext.eightgates.gate4", 360, 25, 0x18FFFFFF, 9, 64, 2, 60, 1.2F, false),
            new GateProperties(5, "chattext.eightgates.gate5", 520, 30, 0x20FFFFFF, 21, 68, 2, 60, 1.4F, false),
            new GateProperties(6, "chattext.eightgates.gate6", 840, 30, 0x3000FF00, 44, 72, 3, 60, 1.6F, false),
            new GateProperties(7, "chattext.eightgates.gate7", 1480, 30, 0x300000FF, 84, 76, 4, 60, 1.8F, true),
            new GateProperties(8, "chattext.eightgates.gate8", 2760, 30, 0x30FF0000, 349, 80, 5, 60, 2.0F, true)
    };

    public EightGatesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            if (!isOwnedBy(player, stack)) {
                stack.shrink(1);
                player.displayClientMessage(Component.literal("This Eight Gates Release item belongs to another player."), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !livingEntity.isShiftKeyDown()) {
            return;
        }
        float previous = getGateOpened(stack);
        float next = setGateOpened(stack, livingEntity, previous + OPEN_INCREMENT);
        if ((int) next > (int) previous && livingEntity instanceof Player player) {
            int openedGate = (int) next;
            if (openedGate > 0) {
                player.displayClientMessage(Component.translatable(GATES[openedGate].translationKey()), true);
                if (player instanceof ServerPlayer serverPlayer) {
                    ProcedureUtils.grantAdvancement(serverPlayer, "narutomod:openedgates", true);
                }
            }
        }
        livingEntity.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 4.0D);
        if (previous >= 4.0F && level instanceof ServerLevel serverLevel) {
            spawnOpeningParticles(serverLevel, livingEntity, previous);
            playOpeningSounds(level, livingEntity, previous);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide || player.isShiftKeyDown()) {
            return;
        }
        int gate = (int) getGateOpened(stack);
        if (gate == 7) {
            boolean spawned = HirudoraEntity.spawnFrom(player);
            player.displayClientMessage(spawned
                    ? Component.translatable("entity.narutomod.entityhirudora")
                    : Component.translatable("entity.narutomod.entityhirudora").append(Component.literal(" failed to spawn.")), true);
            if (spawned && !player.isCreative()) {
                player.getCooldowns().addCooldown(this, 400);
            }
        } else if (gate == 8) {
            boolean spawned = NightGuyDragonEntity.spawnFrom(player);
            player.displayClientMessage(spawned
                    ? Component.translatable("entity.narutomod.entityngdragon")
                    : Component.translatable("entity.narutomod.entityngdragon").append(Component.literal(" failed to spawn.")), true);
            if (spawned && !player.isCreative()) {
                ProcedureUtils.setDeathAnimations(player, 2, 200);
                player.getCooldowns().addCooldown(this, 200);
            }
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide) {
            int gate = (int) getGateOpened(stack);
            if (gate == 8) {
                int punch = getSekizoPunchNum(stack);
                if (punch >= 0) {
                    boolean spawned = SekizoEntity.spawnFrom(player, punch);
                    player.displayClientMessage(spawned
                            ? Component.translatable("entity.narutomod.entitysekizo", punch + 1)
                            : Component.translatable("entity.narutomod.entitysekizo", punch + 1).append(Component.literal(" failed to spawn.")), true);
                    return spawned;
                } else {
                    return true;
                }
            } else if (gate == 6) {
                int spawned = AsakujakuFireballEntity.spawnBurst(player);
                player.displayClientMessage(Component.translatable("entity.narutomod.entityasakujaku"), true);
                return spawned > 0;
            } else if (gate >= 2 && target != player) {
                ProcedureUtils.pushEntity(player, target, 10.0D, gate * 0.2F + 2.0F);
            }
        }
        return super.onLeftClickEntity(stack, player, target);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        setOwnerIfMissing(stack, living);
        if (!isOwnedBy(living, stack)) {
            stack.shrink(1);
            return;
        }
        float gateOpened = getGateOpened(stack);
        boolean held = living.getMainHandItem() == stack || living.getOffhandItem() == stack;
        if (held) {
            activateGate(stack, level, living, (int) gateOpened);
            if (gateOpened >= 1.0F && gateOpened >= getMaxOpenableGate(stack, living) && entity.tickCount % 40 == 8) {
                addBattleXP(stack, 1);
            }
        } else if (gateOpened > 0.0F) {
            deactivateGate(stack, living, (int) gateOpened);
            setGateOpened(stack, living, 0.0F);
            if (living instanceof Player player && !player.isCreative()) {
                player.getCooldowns().addCooldown(this, (int) gateOpened * 200);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && getGateOpened(stack) >= 1.0F) {
            int gate = Mth.clamp((int) getGateOpened(stack), 1, 8);
            return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.MAX_HEALTH, new AttributeModifier(
                            GATE_HEALTH_MODIFIER,
                            "8gates.maxhealth",
                            GATES[gate].health(),
                            AttributeModifier.Operation.ADDITION))
                    .build();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.eightgates.opengates").withStyle(ChatFormatting.GRAY));
        int max = (int) getMaxOpenableGate(stack);
        for (int i = 1; i <= 8; i++) {
            ChatFormatting color = i <= max ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
            tooltip.add(Component.translatable(GATES[i].translationKey())
                    .append(Component.literal(" "))
                    .append(Component.translatable("tooltip.eightgates.requiredxp", GATES[i].xpRequired()))
                    .withStyle(color));
        }
        tooltip.add(Component.translatable("tooltip.eightgates.currentxp", getBattleXP(stack)).withStyle(ChatFormatting.GREEN));
    }

    public float getGateOpened(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getFloat(GATE_OPENED_TAG) : 0.0F;
    }

    public float getMaxOpenableGate(ItemStack stack) {
        int xp = getBattleXP(stack);
        for (int i = 8; i > 0; i--) {
            if (xp >= GATES[i].xpRequired()) {
                return i;
            }
        }
        return 0.0F;
    }

    public int getBattleXP(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getInt(BATTLE_EXPERIENCE_TAG) : 0;
    }

    public void setBattleXP(ItemStack stack, int xp) {
        stack.getOrCreateTag().putInt(BATTLE_EXPERIENCE_TAG, Math.max(xp, 0));
    }

    public void addBattleXP(ItemStack stack, int amount) {
        setBattleXP(stack, getBattleXP(stack) + amount);
    }

    public void bindOwner(ItemStack stack, LivingEntity owner) {
        ProcedureUtils.setOriginalOwner(owner, stack);
    }

    public String describeState(ItemStack stack, @javax.annotation.Nullable LivingEntity owner) {
        float gate = getGateOpened(stack);
        return "gate=" + String.format("%.2f", gate)
                + "/battle_xp=" + getBattleXP(stack)
                + "/max_gate=" + (int) getMaxOpenableGate(stack, owner)
                + "/owner=" + (owner != null && isOwnedBy(owner, stack))
                + "/sekizo=" + (stack.getTag() != null ? stack.getTag().getInt(SEKIZO_PUNCH_COUNT_TAG) : 0);
    }

    private static void setOwnerIfMissing(ItemStack stack, LivingEntity owner) {
        if (ProcedureUtils.getOwnerId(stack) == null) {
            ProcedureUtils.setOriginalOwner(owner, stack);
        }
    }

    private static boolean isOwnedBy(LivingEntity entity, ItemStack stack) {
        UUID ownerId = ProcedureUtils.getOwnerId(stack);
        return ownerId == null || ownerId.equals(entity.getUUID());
    }

    private float getMaxOpenableGate(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.isCreative()) {
            return 8.0F;
        }
        return getMaxOpenableGate(stack);
    }

    private float setGateOpened(ItemStack stack, LivingEntity entity, float gate) {
        float clamped = Mth.clamp(gate, 0.0F, getMaxOpenableGate(stack, entity));
        stack.getOrCreateTag().putFloat(GATE_OPENED_TAG, clamped);
        return clamped;
    }

    private void activateGate(ItemStack stack, Level level, LivingEntity living, int gateIndex) {
        if (gateIndex < 1 || gateIndex > 8) {
            return;
        }
        GateProperties gate = GATES[gateIndex];
        living.fallDistance = 0.0F;
        living.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, 8, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2, 3, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, gate.strength(), false, false));
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, gate.resistance(), false, false));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, gate.speed(), false, false));
        if (gate.canFly() && living instanceof Player player) {
            player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 2, 0, false, false));
        }
        if (gate.particles() > 0 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, gate.particleColor(), 40, 5, 0xF0, living.getId(), 4),
                    living.getX(),
                    living.getY() + 0.8D,
                    living.getZ(),
                    gate.particles(),
                    0.2D,
                    0.4D,
                    0.2D,
                    0.1D);
        }
        if (living.tickCount % 10 == 0 && living.getHealth() > 0.0F
                && (!(living instanceof Player player) || !player.isCreative())) {
            living.setHealth(Mth.clamp(living.getHealth() - gate.damage(), 0.0F, living.getMaxHealth()));
        }
        if (gateIndex == 8 && living.tickCount % 20 == 0) {
            ProcedureUtils.setDeathAnimations(living, 2, 200);
        }
    }

    private void deactivateGate(ItemStack stack, LivingEntity living, int gateIndex) {
        if (gateIndex > 1 && (!(living instanceof Player player) || !player.isCreative())) {
            if (gateIndex == 8) {
                ProcedureUtils.setDeathAnimations(living, 2, 200);
            }
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, gateIndex * 600, (gateIndex - 2) * 2, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, gateIndex * 600, gateIndex - 2, false, false));
        }
        if (living.getHealth() > living.getMaxHealth()) {
            living.setHealth(living.getMaxHealth());
        }
        stack.getOrCreateTag().putInt(SEKIZO_PUNCH_COUNT_TAG, 0);
    }

    private int getSekizoPunchNum(ItemStack stack) {
        int punch = stack.getOrCreateTag().getInt(SEKIZO_PUNCH_COUNT_TAG);
        int next = punch >= 0 && punch < 4 ? punch + 1 : -1;
        stack.getOrCreateTag().putInt(SEKIZO_PUNCH_COUNT_TAG, next);
        return punch;
    }

    private static void spawnOpeningParticles(ServerLevel level, LivingEntity living, float gateOpened) {
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x10FFFFFF, 30, 0, 0xF0, living.getId(), 4),
                living.getX(),
                living.getY(),
                living.getZ(),
                Math.max((int) gateOpened * 10, 1),
                1.0D,
                0.0D,
                1.0D,
                0.5D);
    }

    private static void playOpeningSounds(Level level, LivingEntity living, float gateOpened) {
        if (gateOpened < 4.0F + OPEN_INCREMENT) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    ModSounds.SOUND_OPENGATE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (gateOpened >= 8.0F - OPEN_INCREMENT && gateOpened < 8.0F) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    ModSounds.SOUND_EIGHTGATESRELEASE.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        }
        if (gateOpened >= 4.0F + OPEN_INCREMENT && living.tickCount % 10 == 0) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    ModSounds.SOUND_EXPLOSION.get(), SoundSource.NEUTRAL, 0.1F, 0.75F + living.getRandom().nextFloat() * 0.15F);
        }
    }

    public static void logBattleXP(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EightGatesItem)) {
            stack = player.getOffhandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem && eightGatesItem.getMaxOpenableGate(stack) < 1.0F) {
            eightGatesItem.addBattleXP(stack, 1);
        }
    }

    public static void addBattleXP(Player player, int amount) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EightGatesItem)) {
            stack = player.getOffhandItem();
        }
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.addBattleXP(stack, amount);
        }
    }

    private record GateProperties(
            int gate,
            String translationKey,
            int xpRequired,
            int particles,
            int particleColor,
            int strength,
            int speed,
            int resistance,
            int health,
            float damage,
            boolean canFly) {
    }
}
