package net.narutomod.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.block.PortalBlock;
import net.narutomod.block.PortalBlockEntity;
import net.narutomod.entity.BiggerMeEntity;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.BrackenDanceEntity;
import net.narutomod.entity.EightyGodsEntity;
import net.narutomod.entity.ExplosiveClayEntity;
import net.narutomod.entity.ExplosiveCloneEntity;
import net.narutomod.entity.IceDomeEntity;
import net.narutomod.entity.FingerBoneEntity;
import net.narutomod.entity.IcePrisonEntity;
import net.narutomod.entity.IceSpearEntity;
import net.narutomod.entity.IceSpikeEntity;
import net.narutomod.entity.FuttonMistEntity;
import net.narutomod.entity.JintonBeamEntity;
import net.narutomod.entity.JintonCubeEntity;
import net.narutomod.entity.LaserCircusEntity;
import net.narutomod.entity.LaserRingEntity;
import net.narutomod.entity.LavaChakraModeEntity;
import net.narutomod.entity.MagmaBallEntity;
import net.narutomod.entity.MeltingJutsuEntity;
import net.narutomod.entity.RantonCloudEntity;
import net.narutomod.entity.SandBindEntity;
import net.narutomod.entity.SandBulletEntity;
import net.narutomod.entity.SandLevitationEntity;
import net.narutomod.entity.SandShieldEntity;
import net.narutomod.entity.ScorchOrbEntity;
import net.narutomod.entity.SealingEntity;
import net.narutomod.entity.TruthSeekerBallEntity;
import net.narutomod.entity.UnrivaledStrengthEntity;
import net.narutomod.entity.WoodArmEntity;
import net.narutomod.entity.WoodBurialEntity;
import net.narutomod.entity.WoodGolemEntity;
import net.narutomod.entity.WoodPrisonEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class AdvancedNatureJutsuItem extends JutsuItem {
    private static final int REQ_DOTON = 1;
    private static final int REQ_FUTON = 1 << 1;
    private static final int REQ_KATON = 1 << 2;
    private static final int REQ_RAITON = 1 << 3;
    private static final int REQ_SUITON = 1 << 4;
    private static final String JIRAIKEN_ACTIVE_TAG = "isJiraikenActivated";
    private static final String JIRAIKEN_POWER_TAG = "JiraikenPower";
    private static final String SHAKUTON_BALLS_TAG = "SpawnedBallsId";

    private final AdvancedNatureKind kind;

    public AdvancedNatureJutsuItem(AdvancedNatureKind kind) {
        super(kind.jutsuType, true, kind.definitions);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginAdvancedNature(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide && isRantonLaserCircus(getCurrentJutsu(stack))) {
            LaserRingEntity.spawnOrGet(player);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!(livingEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (isKekkeiMoraEightyGods(definition) && usedTicks % 4 == 1
                && !activateEightyGods(level, player, stack, false)) {
            player.stopUsingItem();
        }
        if (!isChargeFeedbackJutsu(definition)) {
            return;
        }
        if (usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, chargeParticleColor(definition), 30, 4, 0xF0, player.getId(), 4),
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    8,
                    0.2D,
                    0.1D,
                    0.2D,
                    0.05D);
        }
        if (usedTicks % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 0.05F, level.random.nextFloat() + 0.5F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (isMagmaBall(definition)) {
            activateMagmaBall(level, player, stack, remainingUseDuration);
        } else if (isMeltingJutsu(definition)) {
            activateMeltingJutsu(level, player, stack, remainingUseDuration);
        } else if (isLavaChakraMode(definition)) {
            toggleLavaChakraMode(level, player, stack);
        } else if (isBakutonJiraiken(definition)) {
            toggleJiraiken(level, player, stack, remainingUseDuration);
        } else if (isBakutonExplosiveClay(definition)) {
            activateExplosiveClay(level, player, stack, remainingUseDuration);
        } else if (isBakutonExplosiveClone(definition)) {
            activateExplosiveClone(level, player, stack);
        } else if (isFuttonMist(definition)) {
            activateFuttonMist(level, player, stack, remainingUseDuration);
        } else if (isFuttonUnrivaledStrength(definition)) {
            activateUnrivaledStrength(level, player, stack, remainingUseDuration);
        } else if (isHyotonIceSpike(definition)) {
            activateIceSpike(level, player, stack, remainingUseDuration);
        } else if (isHyotonIceSpear(definition)) {
            activateIceSpear(level, player, stack, remainingUseDuration);
        } else if (isHyotonIceDome(definition)) {
            activateIceDome(level, player, stack, remainingUseDuration);
        } else if (isHyotonIcePrison(definition)) {
            activateIcePrison(level, player, stack, remainingUseDuration);
        } else if (isJintonBeam(definition)) {
            activateJintonBeam(level, player, stack, remainingUseDuration);
        } else if (isJintonCube(definition)) {
            activateJintonCube(level, player, stack, remainingUseDuration);
        } else if (isRantonCloud(definition)) {
            toggleRantonCloud(level, player, stack);
        } else if (isRantonLaserCircus(definition)) {
            activateLaserCircus(level, player, stack, remainingUseDuration);
        } else if (isShakutonScorchOrb(definition)) {
            activateScorchOrb(level, player, stack);
        } else if (isShakutonScorchKill(definition)) {
            activateScorchKill(level, player, stack);
        } else if (isShakutonScorchBlast(definition)) {
            activateScorchBlast(level, player, stack);
        } else if (isJitonSandShield(definition)) {
            activateSandShield(level, player, stack);
        } else if (isJitonSandBullet(definition)) {
            activateSandBullet(level, player, stack);
        } else if (isJitonSandBind(definition)) {
            activateSandBind(level, player, stack);
        } else if (isJitonSandLevitation(definition)) {
            activateSandLevitation(level, player, stack);
        } else if (isShikotsumyakuLarchDance(definition)) {
            toggleLarchDance(level, player, stack);
        } else if (isShikotsumyakuWillowDance(definition)) {
            toggleWillowDance(level, player, stack);
        } else if (isShikotsumyakuCamelliaDance(definition)) {
            activateCamelliaDance(level, player, stack);
        } else if (isShikotsumyakuFingerBone(definition)) {
            activateFingerBone(level, player, stack);
        } else if (isShikotsumyakuClematisFlower(definition)) {
            activateClematisFlower(level, player, stack);
        } else if (isShikotsumyakuBrackenDance(definition)) {
            activateBrackenDance(level, player, stack, remainingUseDuration);
        } else if (isKekkeiMoraEightyGods(definition)) {
            activateEightyGods(level, player, stack, true);
        } else if (isKekkeiMoraYomotsuHirasaka(definition)) {
            activateYomotsuHirasaka(level, player, stack);
        } else if (isKekkeiMoraExpansiveTruthSeekingBall(definition)) {
            activateExpansiveTruthSeekingBall(level, player, stack);
        } else if (isKekkeiMoraAshBones(definition)) {
            activateKekkeiMoraAshBones(level, player, stack);
        } else if (isMokutonWoodBurial(definition)) {
            activateWoodBurial(level, player, stack);
        } else if (isMokutonWoodPrison(definition)) {
            activateWoodPrison(level, player, stack, remainingUseDuration);
        } else if (isMokutonWoodHouse(definition)) {
            activateWoodHouse(level, player, stack);
        } else if (isMokutonWoodGolem(definition)) {
            activateWoodGolem(level, player, stack);
        } else if (isMokutonWoodArm(definition)) {
            activateWoodArm(level, player, stack);
        } else if (isYotonBiggerMe(definition)) {
            activateBiggerMe(level, player, stack, remainingUseDuration);
        } else if (isYotonSealing(definition)) {
            activateSealing(level, player, stack);
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        Entity resolvedTarget = target;
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (isBakutonExplosiveClay(definition) && target == player) {
            HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 50.0D, 0.0D, true, false, candidate -> candidate != player);
            if (hit instanceof EntityHitResult entityHit) {
                resolvedTarget = entityHit.getEntity();
            }
        }
        if (isBakutonExplosiveClay(definition) && resolvedTarget instanceof LivingEntity livingTarget && resolvedTarget != player) {
            player.setLastHurtMob(livingTarget);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("Explosive clay target: ").append(livingTarget.getDisplayName()), true);
            }
        }
        return super.onLeftClickEntity(stack, player, resolvedTarget);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        this.kind.applyPassive(livingEntity);
        if (isJiraikenActive(stack)) {
            if (!isOwnedByOrUnbound(livingEntity, stack)) {
                setJiraikenActive(stack, false);
                return;
            }
            int amplifier = Math.max((int)(getJiraikenPower(stack) * 19.0F), 1);
            livingEntity.addEffect(new MobEffectInstance(ModEffects.CHAKRA_ENHANCED_STRENGTH.get(), 3, amplifier, false, false));
        }
        if (this.kind == AdvancedNatureKind.SHAKUTON
                && livingEntity.getMainHandItem() != stack
                && livingEntity.getOffhandItem() != stack) {
            clearScorchBalls(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (this.kind.requirementTooltipKey != null) {
            tooltip.add(Component.translatable(this.kind.requirementTooltipKey).withStyle(ChatFormatting.GREEN));
        }
        JutsuDefinition current = getCurrentJutsu(stack);
        tooltip.add(Component.translatable(current.translationKey())
                .append(Component.literal(" [" + current.rank() + "]"))
                .withStyle(ChatFormatting.GRAY));
    }

    public AdvancedNatureKind kind() {
        return this.kind;
    }

    public String describeCurrentJutsu(ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        return current.translationKey()
                + "#" + current.index()
                + "/rank=" + current.rank()
                + "/enabled=" + isJutsuEnabled(stack, current)
                + "/xp=" + getJutsuXp(stack, current)
                + "/" + getRequiredXp(stack, current);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private boolean canBeginAdvancedNature(Level level, Player player, ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (isBakutonJiraiken(current) && isJiraikenActive(stack)) {
            return true;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            if (!this.kind.hasRequirements(player)) {
                player.displayClientMessage(Component.translatable(this.kind.requirementTooltipKey), true);
                return false;
            }
            if (!isPortedAdvancedNatureJutsu(current)) {
                player.displayClientMessage(Component.translatable(current.translationKey())
                        .append(Component.literal(" runtime is not ported yet.")), true);
                return false;
            }
            if (isRantonCloud(current) && RantonCloudEntity.findActive((ServerLevel) level, player) != null) {
                return true;
            }
            if (isJitonSandBind(current) && canTriggerSandFuneral(player)) {
                return true;
            }
            if (isJitonSandLevitation(current) && SandLevitationEntity.findActive((ServerLevel) level, player) != null) {
                return true;
            }
            if (isJitonSandShield(current) && SandShieldEntity.findActive((ServerLevel) level, player) != null) {
                return true;
            }
            if (isBakutonExplosiveClone(current) && player.isShiftKeyDown()
                    && !ExplosiveCloneEntity.aliveClones(player).isEmpty()) {
                return true;
            }
            if (isYotonBiggerMe(current) && BiggerMeEntity.isOwnedBiggerMe(player.getVehicle(), player)) {
                return true;
            }
            if ((isShikotsumyakuLarchDance(current) && BoneArmorItem.isLarchActive(player.getItemBySlot(EquipmentSlot.CHEST)))
                    || (isShikotsumyakuWillowDance(current) && BoneArmorItem.isWillowActive(player.getItemBySlot(EquipmentSlot.CHEST)))) {
                return true;
            }
            if (isShikotsumyakuCamelliaDance(current) && player.getMainHandItem().is(ModItems.BONE_SWORD.get())) {
                return true;
            }
            if (isShikotsumyakuClematisFlower(current) && ProcedureUtils.hasItemInInventory(player, ModItems.BONE_DRILL.get())) {
                return true;
            }
            if (isKekkeiMoraAshBones(current) && player.getMainHandItem().is(ModItems.ASHBONES.get())) {
                return true;
            }
            if (isLavaChakraMode(current)) {
                if (findActiveLavaChakraMode((ServerLevel) level, player) != null) {
                    return true;
                }
                if (!hasFourTails(player)) {
                    player.displayClientMessage(Component.literal("Lava Chakra Mode requires the Four-Tails jinchuriki flag."), true);
                    return false;
                }
            }
        }
        return canActivateAdvancedNature(level, player, stack, getCurrentJutsu(stack));
    }

    private boolean canActivateAdvancedNature(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this advanced nature jutsu."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Jutsu XP " + getJutsuXp(stack, definition)
                        + "/" + getRequiredXp(stack, definition)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, definition);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        if (Chakra.pathway(player).getAmount() < definition.chakraUsage()) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private void activateMagmaBall(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        MagmaBallEntity magmaBall = ModEntityTypes.MAGMABALL.get().create(level);
        if (magmaBall == null) {
            return;
        }
        magmaBall.configure(player, power);
        level.addFreshEntity(magmaBall);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_FLAMETHROW.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateMeltingJutsu(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        MeltingJutsuEntity meltingJutsu = ModEntityTypes.MELTING_JUTSU.get().create(level);
        if (meltingJutsu == null) {
            return;
        }
        meltingJutsu.configureEmitter(player, power);
        level.addFreshEntity(meltingJutsu);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_FLAMETHROW.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        addCurrentJutsuXp(stack, 1);
    }

    private void toggleLavaChakraMode(Level level, Player player, ItemStack stack) {
        ServerLevel serverLevel = (ServerLevel) level;
        LavaChakraModeEntity active = findActiveLavaChakraMode(serverLevel, player);
        if (active != null) {
            active.stopChakraMode();
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!hasFourTails(player)) {
            player.displayClientMessage(Component.literal("Lava Chakra Mode requires the Four-Tails jinchuriki flag."), true);
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        LavaChakraModeEntity entity = ModEntityTypes.LAVA_CHAKRA_MODE.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player);
        level.addFreshEntity(entity);
        player.getPersistentData().putInt(LavaChakraModeEntity.ENTITY_ID_KEY, entity.getId());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.8F);
        addCurrentJutsuXp(stack, 1);
    }

    private void toggleJiraiken(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (isJiraikenActive(stack)) {
            setJiraikenActive(stack, false);
            player.removeEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
            player.displayClientMessage(Component.literal("Jiraiken ended."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        setJiraikenPower(stack, power);
        setJiraikenActive(stack, true);
        addCurrentJutsuXp(stack, 1);
        player.displayClientMessage(Component.literal(String.format("Jiraiken activated: power %.2f", power)), true);
    }

    private void activateExplosiveClone(Level level, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            int removed = ExplosiveCloneEntity.removeAllFor(player);
            player.displayClientMessage(Component.literal("Explosive clones removed: " + removed), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (ExplosiveCloneEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateExplosiveClay(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = Math.min(3.1F, getAdvancedNaturePower(stack, player, definition, remainingUseDuration));
        power = (float)Math.floor(power);
        if (power < 1.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (ExplosiveClayEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
            player.displayClientMessage(Component.literal("Explosive clay C-" + (int)power + " created."), true);
        }
    }

    private void activateFuttonMist(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (FuttonMistEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateUnrivaledStrength(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (UnrivaledStrengthEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateIceSpear(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        int spawned = IceSpearEntity.spawnFrom(player, power);
        if (spawned > 0) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateIceDome(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (IceDomeEntity.spawnOrTrigger(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateIcePrison(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (IcePrisonEntity.findTarget(player) == null) {
            player.displayClientMessage(Component.literal("Ice Prison requires a living target in sight."), true);
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (IcePrisonEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateWoodPrison(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (WoodPrisonEntity.findTarget(player) == null) {
            player.displayClientMessage(Component.literal("Wood Prison requires a block target within 20 blocks."), true);
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (WoodPrisonEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
            player.displayClientMessage(Component.literal(String.format("Wood Prison created: power %.2f", power)), true);
        }
    }

    private void activateWoodBurial(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (WoodBurialEntity.findTarget(player) == null) {
            player.displayClientMessage(Component.literal("Wood Burial requires a living target within 20 blocks."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (WoodBurialEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateWoodHouse(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(player, 30.0D);
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP || !canSpawnWoodStructureOn(level, hit.getBlockPos())) {
            player.displayClientMessage(Component.literal("Four-Pillar House requires a solid earth, sand, or stone top face."), true);
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Optional<StructureTemplate> templateOptional = serverLevel.getStructureManager().get(NarutomodMod.location("wood_house_2"));
        if (templateOptional.isEmpty()) {
            player.displayClientMessage(Component.literal("Missing structure template narutomod:wood_house_2."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        StructureTemplate template = templateOptional.get();
        WoodHousePlacement placement = woodHousePlacement(player, hit.getBlockPos(), template);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(placement.rotation())
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
        spawnWoodHouseParticles(serverLevel, placement.scanOrigin(), template.getSize(placement.rotation()));
        boolean placed = template.placeInWorld(serverLevel, placement.spawnTo(), placement.spawnTo(), settings, serverLevel.getRandom(), 2);
        if (placed) {
            level.playSound(null, hit.getBlockPos(), ModSounds.SOUND_WOODSPAWN.get(),
                    SoundSource.BLOCKS, 2.0F, level.random.nextFloat() * 0.4F + 0.8F);
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateWoodArm(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (WoodArmEntity.findTarget(player) == null) {
            player.displayClientMessage(Component.literal("Wood Arm requires a living target within 30 blocks."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (WoodArmEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateWoodGolem(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (WoodGolemEntity.isOwnedGolem(player.getVehicle(), player)) {
            player.displayClientMessage(Component.literal("You are already riding your Wood Golem."), true);
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        double chakraBurn = definition.chakraUsage() * 0.05D * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
        if (WoodGolemEntity.spawnFrom(player, chakraBurn)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateSealing(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!SealingEntity.hasValidPlacementTarget(player)) {
            player.displayClientMessage(Component.literal("Sealing requires a clear 13x13 top-face circle with the eight legacy torch anchors."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (SealingEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateBiggerMe(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (BiggerMeEntity.isOwnedBiggerMe(player.getVehicle(), player)) {
            player.displayClientMessage(Component.literal("You are already riding Bigger Me."), true);
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float rawPower = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (rawPower <= 0.0F) {
            return;
        }
        float power = Mth.clamp(rawPower, 2.0F, 10.0F);
        double chakraCost = definition.chakraUsage() * power;
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < chakraCost) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            return;
        }
        if (BiggerMeEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateIceSpike(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!IceSpikeEntity.hasUpwardGroundTarget(player)) {
            player.displayClientMessage(Component.literal("Ice Spike requires an upward ground target."), true);
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        int spawned = IceSpikeEntity.spawnFrom(player, power);
        if (spawned > 0) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateJintonBeam(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (JintonBeamEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateJintonCube(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (JintonCubeEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void toggleRantonCloud(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        RantonCloudEntity active = RantonCloudEntity.findActive(serverLevel, player);
        if (active != null) {
            active.discard();
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        int damageXp = getJutsuXp(stack, definition);
        if (RantonCloudEntity.spawnFrom(player, damageXp)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateLaserCircus(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        int damageXp = getJutsuXp(stack, definition);
        if (LaserCircusEntity.spawnFrom(player, power, damageXp)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateScorchOrb(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (countActiveScorchBalls(level, stack) >= 20) {
            player.displayClientMessage(Component.literal("Scorch Orb limit reached."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        ScorchOrbEntity orb = ScorchOrbEntity.spawnFrom(player);
        if (orb != null) {
            saveScorchBall(stack, orb);
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateScorchKill(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        ScorchOrbEntity orb = getFirstScorchBallAndRotate(level, stack);
        if (orb == null) {
            player.displayClientMessage(Component.literal("Scorch Kill requires an active Scorch Orb."), true);
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 30.0D);
        if (!(hit instanceof EntityHitResult entityHitResult)) {
            player.displayClientMessage(Component.literal("Scorch Kill requires a target in sight."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        orb.setTarget(entityHitResult.getEntity());
        addCurrentJutsuXp(stack, 1);
    }

    private void activateScorchBlast(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        List<ScorchOrbEntity> balls = getActiveScorchBalls(level, stack);
        if (balls.isEmpty()) {
            player.displayClientMessage(Component.literal("Scorch Blast requires active Scorch Orbs."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        for (int i = 0; i < balls.size(); i++) {
            balls.get(i).setMaxScale(i == 0 ? 0.5F * balls.size() : 0.0F);
        }
        clearScorchBalls(stack);
        addCurrentJutsuXp(stack, 1);
    }

    private void toggleLarchDance(Level level, Player player, ItemStack stack) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (BoneArmorItem.isLarchActive(chest)) {
            BoneArmorItem.setLarchActive(chest, false);
            player.displayClientMessage(Component.literal("Larch Dance ended."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        BoneArmorItem.toggleLarch(player);
        playBoneCrack(level, player);
        addCurrentJutsuXp(stack, 1);
        player.displayClientMessage(Component.literal("Larch Dance activated."), true);
    }

    private void toggleWillowDance(Level level, Player player, ItemStack stack) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (BoneArmorItem.isWillowActive(chest)) {
            BoneArmorItem.setWillowActive(chest, false);
            player.displayClientMessage(Component.literal("Willow Dance ended."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        BoneArmorItem.toggleWillow(player);
        playBoneCrack(level, player);
        addCurrentJutsuXp(stack, 1);
        player.displayClientMessage(Component.literal("Willow Dance activated."), true);
    }

    private void playBoneCrack(Level level, Player player) {
        playBoneCrack(level, player, 1.0F);
    }

    private void playBoneCrack(Level level, Player player, float volume) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_BONECRACK.get(), SoundSource.PLAYERS, volume, 1.0F);
    }

    private void activateCamelliaDance(Level level, Player player, ItemStack stack) {
        if (player.getMainHandItem().is(ModItems.BONE_SWORD.get())) {
            player.displayClientMessage(Component.literal("Camellia Dance already has a bone sword equipped."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        addCurrentJutsuXp(stack, 1);
        equipMainHand(player, new ItemStack(ModItems.BONE_SWORD.get()));
        playBoneCrack(level, player);
        player.displayClientMessage(Component.literal("Camellia Dance bone sword equipped."), true);
    }

    private void activateClematisFlower(Level level, Player player, ItemStack stack) {
        if (ProcedureUtils.hasItemInInventory(player, ModItems.BONE_DRILL.get())) {
            player.displayClientMessage(Component.literal("Clematis Flower bone drill is already available."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        addCurrentJutsuXp(stack, 1);
        setCurrentJutsuCooldown(stack, level, 1200L);
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.BONE_DRILL.get()));
        playBoneCrack(level, player);
        player.displayClientMessage(Component.literal("Clematis Flower bone drill created."), true);
    }

    private void activateKekkeiMoraAshBones(Level level, Player player, ItemStack stack) {
        if (player.getMainHandItem().is(ModItems.ASHBONES.get())) {
            player.displayClientMessage(Component.literal("Ash Bones already equipped."), true);
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        addCurrentJutsuXp(stack, 1);
        equipMainHand(player, new ItemStack(ModItems.ASHBONES.get()));
        playBoneCrack(level, player, 0.5F);
        player.displayClientMessage(Component.literal("Ash Bones equipped."), true);
    }

    private boolean activateEightyGods(Level level, Player player, ItemStack stack, boolean awardXp) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return false;
        }
        if (EightyGodsEntity.spawnFrom(player)) {
            if (awardXp) {
                addCurrentJutsuXp(stack, 1);
            }
            return true;
        }
        return false;
    }

    private void activateExpansiveTruthSeekingBall(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (TruthSeekerBallEntity.hasActiveExpansive(player)) {
            player.displayClientMessage(Component.literal("Expansive Truth-Seeking Ball is already active."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (TruthSeekerBallEntity.spawnExpansiveFrom(player)) {
            addCurrentJutsuXp(stack, 1);
            setCurrentJutsuCooldown(stack, level, 1200L);
        }
    }

    private void activateYomotsuHirasaka(Level level, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (placeYomotsuHirasakaPortals(serverLevel, player, true)) {
            addCurrentJutsuXp(stack, 1);
        } else if (!player.isCreative()) {
            Chakra.pathway(player).consume(-definition.chakraUsage(), true);
        }
    }

    public static boolean placeYomotsuHirasakaPortals(ServerLevel level, Player player, boolean showMessages) {
        PortalPlacement placement = resolveYomotsuPlacement(player);
        if (placement == null) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("No valid portal destination found."), true);
            }
            return false;
        }
        if (!isPortalColumnInBounds(level, placement.entryTop()) || !isPortalColumnInBounds(level, placement.exitTop())) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("Portal destination is outside build height."), true);
            }
            return false;
        }
        placePortalPair(level, placement);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PORTAL_TRAVEL,
                SoundSource.BLOCKS, 0.3F, 1.0F);
        return true;
    }

    @Nullable
    private static PortalPlacement resolveYomotsuPlacement(Player player) {
        Direction entryFacing = Direction.fromYRot(player.getYRot()).getOpposite();
        BlockPos entryTop = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2.0D)));
        HitResult result = ProcedureUtils.objectEntityLookingAt(player, 150.0D, true, false, target -> target != player);
        Direction exitFacing;
        BlockPos exitTop;
        if (result instanceof EntityHitResult entityHit) {
            exitFacing = Direction.fromYRot(entityHit.getEntity().getYRot());
            exitTop = BlockPos.containing(entityHit.getLocation()).above();
        } else {
            exitFacing = Direction.fromYRot(player.getYRot());
            exitTop = findPortalExitTop(player.level(), result);
            if (exitTop == null) {
                return null;
            }
        }
        return new PortalPlacement(entryTop, entryFacing, exitTop, exitFacing);
    }

    @Nullable
    private static BlockPos findPortalExitTop(Level level, HitResult result) {
        BlockPos pos = result instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos()
                : BlockPos.containing(result.getLocation());
        int maxY = level.getMaxBuildHeight() - 1;
        while (pos.getY() < maxY) {
            if (pos.getY() > level.getMinBuildHeight()
                    && level.isEmptyBlock(pos)
                    && level.isEmptyBlock(pos.below())) {
                return pos;
            }
            pos = pos.above();
        }
        return null;
    }

    private static boolean isPortalColumnInBounds(Level level, BlockPos top) {
        return top.getY() > level.getMinBuildHeight() && top.getY() < level.getMaxBuildHeight();
    }

    private static void placePortalPair(ServerLevel level, PortalPlacement placement) {
        placePortalColumn(level, placement.entryTop(), placement.entryFacing());
        placePortalColumn(level, placement.exitTop(), placement.exitFacing());
        bindPortal(level, placement.entryTop(), placement.exitTop());
        bindPortal(level, placement.entryTop().below(), placement.exitTop().below());
        bindPortal(level, placement.exitTop(), placement.entryTop());
        bindPortal(level, placement.exitTop().below(), placement.entryTop().below());
    }

    private static void placePortalColumn(ServerLevel level, BlockPos top, Direction facing) {
        BlockState state = ModBlocks.PORTALBLOCK.get().defaultBlockState()
                .setValue(PortalBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
        level.setBlock(top, state, 3);
        level.setBlock(top.below(), state, 3);
    }

    private static void bindPortal(ServerLevel level, BlockPos pos, BlockPos pair) {
        if (level.getBlockEntity(pos) instanceof PortalBlockEntity portal) {
            portal.setPair(pair);
        }
    }

    private record PortalPlacement(BlockPos entryTop, Direction entryFacing, BlockPos exitTop, Direction exitFacing) {
    }

    private static void equipMainHand(Player player, ItemStack replacement) {
        ItemStack current = player.getMainHandItem();
        if (!current.isEmpty()) {
            ItemStack displaced = current.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!player.getInventory().add(displaced)) {
                player.drop(displaced, false);
            }
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
    }

    private void activateFingerBone(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (FingerBoneEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateBrackenDance(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!BrackenDanceEntity.hasUpwardGroundTarget(player)) {
            player.displayClientMessage(Component.literal("Bracken Dance requires a visible ground surface."), true);
            return;
        }
        float power = getAdvancedNaturePower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        if (BrackenDanceEntity.spawnFrom(player, power) > 0) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateSandShield(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (level instanceof ServerLevel serverLevel && SandShieldEntity.findActive(serverLevel, player) != null) {
            SandShieldEntity.spawnFrom(player, SandShieldEntity.DEFAULT_COLOR);
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (SandShieldEntity.spawnFrom(player, SandShieldEntity.DEFAULT_COLOR)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateSandBullet(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (SandBulletEntity.spawnFrom(player, SandBulletEntity.DEFAULT_COLOR)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateSandBind(Level level, Player player, ItemStack stack) {
        SandBindEntity activeBind = SandBindEntity.findLookedAtOwnedBind(player, 50.0D);
        if (activeBind != null) {
            if (!player.isCreative() && !Chakra.pathway(player).consume(SandBindEntity.FUNERAL_CHAKRA_COST)) {
                return;
            }
            if (activeBind.triggerSandFuneral(player)) {
                addCurrentJutsuXp(stack, 1);
            }
            return;
        }
        JutsuDefinition definition = getCurrentJutsu(stack);
        LivingEntity target = SandBindEntity.findLookedAtTarget(player, 30.0D);
        if (target == null) {
            player.displayClientMessage(Component.literal("No Sand Bind target in sight."), true);
            return;
        }
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage())) {
            return;
        }
        if (SandBindEntity.spawnFrom(player, target, SandBindEntity.DEFAULT_COLOR)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateSandLevitation(Level level, Player player, ItemStack stack) {
        JutsuDefinition definition = getCurrentJutsu(stack);
        if (!canActivateAdvancedNature(level, player, stack, definition)) {
            return;
        }
        if (SandLevitationEntity.spawnFrom(player, SandLevitationEntity.DEFAULT_COLOR)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private static boolean canSpawnWoodStructureOn(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRASS_BLOCK);
    }

    private static WoodHousePlacement woodHousePlacement(Player player, BlockPos targetPos, StructureTemplate template) {
        Rotation rotation;
        BlockPos spawnTo;
        BlockPos scanOrigin;
        float yaw = Mth.wrapDegrees(player.getYRot());
        if (yaw >= 135.0F || yaw < -135.0F) {
            spawnTo = targetPos.offset(-8, 1, -16);
            scanOrigin = spawnTo;
            rotation = Rotation.NONE;
        } else if (yaw >= -45.0F && yaw < 45.0F) {
            spawnTo = targetPos.offset(8, 1, 16);
            scanOrigin = spawnTo.offset(-template.getSize().getX(), 0, -template.getSize().getZ());
            rotation = Rotation.CLOCKWISE_180;
        } else if (yaw >= 45.0F && yaw < 135.0F) {
            spawnTo = targetPos.offset(-16, 1, 8);
            scanOrigin = spawnTo.offset(0, 0, -template.getSize().getX());
            rotation = Rotation.COUNTERCLOCKWISE_90;
        } else {
            spawnTo = targetPos.offset(16, 1, -8);
            scanOrigin = spawnTo.offset(-template.getSize().getZ(), 0, 0);
            rotation = Rotation.CLOCKWISE_90;
        }
        return new WoodHousePlacement(spawnTo, scanOrigin, rotation);
    }

    private static void spawnWoodHouseParticles(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_FENCE.defaultBlockState());
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX(), size.getY(), size.getZ()))) {
            level.sendParticles(
                    particle,
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    2,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.15D);
        }
    }

    private float getAdvancedNaturePower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        if (player.isCreative()) {
            return Math.max(power, definition.basePower());
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
        return Math.min(power, maxPower);
    }

    private boolean isMagmaBall(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.YOOTON
                && definition.index() == 0
                && "magmaball".equals(definition.translationKey());
    }

    private boolean isMeltingJutsu(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.YOOTON
                && definition.index() == 1
                && "melting_jutsu".equals(definition.translationKey());
    }

    private boolean isLavaChakraMode(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.YOOTON
                && definition.index() == 2
                && "lava_chakra_mode".equals(definition.translationKey());
    }

    private boolean isBakutonJiraiken(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.BAKUTON
                && definition.index() == 0
                && "tooltip.bakuton.jiraiken".equals(definition.translationKey());
    }

    private boolean isBakutonExplosiveClay(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.BAKUTON
                && definition.index() == 1
                && "c_1".equals(definition.translationKey());
    }

    private boolean isBakutonExplosiveClone(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.BAKUTON
                && definition.index() == 2
                && "explosive_clone".equals(definition.translationKey());
    }

    private boolean isFuttonMist(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.FUTTON
                && definition.index() == 0
                && "futton_mist".equals(definition.translationKey());
    }

    private boolean isFuttonUnrivaledStrength(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.FUTTON
                && definition.index() == 1
                && "unrivaled_strength".equals(definition.translationKey());
    }

    private boolean isHyotonIceSpike(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.HYOTON
                && definition.index() == 0
                && "ice_spike".equals(definition.translationKey());
    }

    private boolean isHyotonIceSpear(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.HYOTON
                && definition.index() == 1
                && "ice_spear".equals(definition.translationKey());
    }

    private boolean isHyotonIceDome(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.HYOTON
                && definition.index() == 2
                && "ice_dome".equals(definition.translationKey());
    }

    private boolean isHyotonIcePrison(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.HYOTON
                && definition.index() == 3
                && "ice_prison".equals(definition.translationKey());
    }

    private boolean isJintonBeam(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JINTON
                && definition.index() == 0
                && "jintonbeam".equals(definition.translationKey());
    }

    private boolean isJintonCube(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JINTON
                && definition.index() == 1
                && "jintoncube".equals(definition.translationKey());
    }

    private boolean isRantonCloud(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.RANTON
                && definition.index() == 0
                && "rantoncloud".equals(definition.translationKey());
    }

    private boolean isRantonLaserCircus(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.RANTON
                && definition.index() == 1
                && "laser_circus".equals(definition.translationKey());
    }

    private boolean isShakutonScorchOrb(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHAKUTON
                && definition.index() == 0
                && "scorchorb".equals(definition.translationKey());
    }

    private boolean isShakutonScorchKill(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHAKUTON
                && definition.index() == 1
                && "tooltip.shakuton.scorchkill".equals(definition.translationKey());
    }

    private boolean isShakutonScorchBlast(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHAKUTON
                && definition.index() == 2
                && "tooltip.shakuton.scorchblast".equals(definition.translationKey());
    }

    private boolean isJitonSandShield(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JITON
                && definition.index() == 0
                && "entityjitonshield".equals(definition.translationKey());
    }

    private boolean isJitonSandBullet(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JITON
                && definition.index() == 1
                && "sand_bullet".equals(definition.translationKey());
    }

    private boolean isJitonSandBind(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JITON
                && definition.index() == 2
                && "sand_bind".equals(definition.translationKey());
    }

    private boolean isJitonSandLevitation(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.JITON
                && definition.index() == 3
                && "sand_levitation".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuLarchDance(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 0
                && "tooltip.shikotsumyaku.dancelarch".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuWillowDance(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 1
                && "tooltip.shikotsumyaku.dancewillow".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuCamelliaDance(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 2
                && "tooltip.shikotsumyaku.dancecamellia".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuFingerBone(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 3
                && "finger_bone".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuClematisFlower(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 4
                && "tooltip.shikotsumyaku.danceclementisflower".equals(definition.translationKey());
    }

    private boolean isShikotsumyakuBrackenDance(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.SHIKOTSUMYAKU
                && definition.index() == 5
                && "entitybrackendance".equals(definition.translationKey());
    }

    private boolean isKekkeiMoraAshBones(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.KEKKEI_MORA
                && definition.index() == 3
                && "item.ashbones.name".equals(definition.translationKey());
    }

    private boolean isKekkeiMoraEightyGods(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.KEKKEI_MORA
                && definition.index() == 0
                && "entity80gods".equals(definition.translationKey());
    }

    private boolean isKekkeiMoraYomotsuHirasaka(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.KEKKEI_MORA
                && definition.index() == 1
                && "tooltip.byakurinnesharingan.jutsu2".equals(definition.translationKey());
    }

    private boolean isKekkeiMoraExpansiveTruthSeekingBall(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.KEKKEI_MORA
                && definition.index() == 2
                && "tooltip.kekkeimora.expansivetsb".equals(definition.translationKey());
    }

    private boolean isMokutonWoodPrison(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.MOKUTON
                && definition.index() == 1
                && "wood_prison".equals(definition.translationKey());
    }

    private boolean isMokutonWoodBurial(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.MOKUTON
                && definition.index() == 0
                && "tooltip.mokuton.leftclick".equals(definition.translationKey());
    }

    private boolean isMokutonWoodHouse(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.MOKUTON
                && definition.index() == 2
                && "tooltip.mokuton.rightclick2".equals(definition.translationKey());
    }

    private boolean isMokutonWoodGolem(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.MOKUTON
                && definition.index() == 3
                && "wood_golem".equals(definition.translationKey());
    }

    private boolean isMokutonWoodArm(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.MOKUTON
                && definition.index() == 4
                && "wood_arm".equals(definition.translationKey());
    }

    private boolean isYotonSealing(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.YOTON
                && definition.index() == 1
                && "sealing".equals(definition.translationKey());
    }

    private boolean isYotonBiggerMe(JutsuDefinition definition) {
        return this.kind == AdvancedNatureKind.YOTON
                && definition.index() == 0
                && "biggerme".equals(definition.translationKey());
    }

    private boolean isPortedYootonJutsu(JutsuDefinition definition) {
        return isMagmaBall(definition) || isMeltingJutsu(definition) || isLavaChakraMode(definition);
    }

    private boolean isPortedAdvancedNatureJutsu(JutsuDefinition definition) {
        return isPortedYootonJutsu(definition) || isBakutonJiraiken(definition)
                || isBakutonExplosiveClay(definition) || isBakutonExplosiveClone(definition)
                || isFuttonMist(definition) || isFuttonUnrivaledStrength(definition)
                || isHyotonIceSpike(definition) || isHyotonIceSpear(definition)
                || isHyotonIceDome(definition) || isHyotonIcePrison(definition)
                || isJintonBeam(definition) || isJintonCube(definition)
                || isRantonCloud(definition) || isRantonLaserCircus(definition)
                || isShakutonScorchOrb(definition) || isShakutonScorchKill(definition)
                || isShakutonScorchBlast(definition)
                || isJitonSandShield(definition) || isJitonSandBullet(definition)
                || isJitonSandBind(definition) || isJitonSandLevitation(definition)
                || isShikotsumyakuLarchDance(definition) || isShikotsumyakuWillowDance(definition)
                || isShikotsumyakuCamelliaDance(definition) || isShikotsumyakuFingerBone(definition)
                || isShikotsumyakuClematisFlower(definition) || isShikotsumyakuBrackenDance(definition)
                || isKekkeiMoraEightyGods(definition) || isKekkeiMoraYomotsuHirasaka(definition)
                || isKekkeiMoraExpansiveTruthSeekingBall(definition) || isKekkeiMoraAshBones(definition)
                || isMokutonWoodBurial(definition) || isMokutonWoodPrison(definition)
                || isMokutonWoodHouse(definition) || isMokutonWoodGolem(definition) || isMokutonWoodArm(definition)
                || isYotonBiggerMe(definition) || isYotonSealing(definition);
    }

    private boolean isChargeFeedbackJutsu(JutsuDefinition definition) {
        return isPortedAdvancedNatureJutsu(definition);
    }

    private int chargeParticleColor(JutsuDefinition definition) {
        if (isHyotonIceSpike(definition) || isHyotonIceSpear(definition)
                || isHyotonIceDome(definition) || isHyotonIcePrison(definition)) {
            return 0x80C8F4FF;
        }
        if (isBakutonJiraiken(definition) || isBakutonExplosiveClay(definition) || isBakutonExplosiveClone(definition)) {
            return 0x80F5A000;
        }
        if (isFuttonMist(definition) || isFuttonUnrivaledStrength(definition)) {
            return 0x80F0F0F0;
        }
        if (isJintonBeam(definition) || isJintonCube(definition)) {
            return 0xB0FFFFFF;
        }
        if (isRantonCloud(definition) || isRantonLaserCircus(definition)) {
            return 0x8080D0FF;
        }
        if (isShakutonScorchOrb(definition) || isShakutonScorchKill(definition) || isShakutonScorchBlast(definition)) {
            return 0x80FF4E83;
        }
        if (isJitonSandShield(definition) || isJitonSandBullet(definition)
                || isJitonSandBind(definition) || isJitonSandLevitation(definition)) {
            return 0x80303030;
        }
        if (isShikotsumyakuLarchDance(definition) || isShikotsumyakuWillowDance(definition)
                || isShikotsumyakuCamelliaDance(definition) || isShikotsumyakuFingerBone(definition)
                || isShikotsumyakuClematisFlower(definition) || isShikotsumyakuBrackenDance(definition)) {
            return 0x80F8F0E0;
        }
        if (isKekkeiMoraEightyGods(definition) || isKekkeiMoraYomotsuHirasaka(definition)
                || isKekkeiMoraExpansiveTruthSeekingBall(definition) || isKekkeiMoraAshBones(definition)) {
            return 0x80E0E0E0;
        }
        if (isMokutonWoodBurial(definition) || isMokutonWoodPrison(definition)
                || isMokutonWoodHouse(definition) || isMokutonWoodGolem(definition) || isMokutonWoodArm(definition)) {
            return 0x8060A050;
        }
        if (isYotonBiggerMe(definition) || isYotonSealing(definition)) {
            return 0x80C030C0;
        }
        return 0x80F50000;
    }

    private boolean canTriggerSandFuneral(Player player) {
        if (SandBindEntity.findLookedAtOwnedBind(player, 50.0D) == null) {
            return false;
        }
        return player.isCreative() || Chakra.pathway(player).getAmount() >= SandBindEntity.FUNERAL_CHAKRA_COST;
    }

    private void saveScorchBall(ItemStack stack, ScorchOrbEntity entity) {
        int[] oldIds = stack.getOrCreateTag().getIntArray(SHAKUTON_BALLS_TAG);
        int[] nextIds = new int[oldIds.length + 1];
        System.arraycopy(oldIds, 0, nextIds, 0, oldIds.length);
        nextIds[oldIds.length] = entity.getId();
        stack.getOrCreateTag().putIntArray(SHAKUTON_BALLS_TAG, nextIds);
    }

    @Nullable
    private ScorchOrbEntity getFirstScorchBallAndRotate(Level level, ItemStack stack) {
        List<ScorchOrbEntity> balls = getActiveScorchBalls(level, stack);
        if (balls.isEmpty()) {
            return null;
        }
        ScorchOrbEntity first = balls.remove(0);
        balls.add(first);
        writeScorchBalls(stack, balls);
        return first;
    }

    private int countActiveScorchBalls(Level level, ItemStack stack) {
        return getActiveScorchBalls(level, stack).size();
    }

    private List<ScorchOrbEntity> getActiveScorchBalls(Level level, ItemStack stack) {
        List<ScorchOrbEntity> balls = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            for (int id : stack.getOrCreateTag().getIntArray(SHAKUTON_BALLS_TAG)) {
                if (serverLevel.getEntity(id) instanceof ScorchOrbEntity orb && orb.isAlive()) {
                    balls.add(orb);
                }
            }
        }
        writeScorchBalls(stack, balls);
        return balls;
    }

    private void writeScorchBalls(ItemStack stack, List<ScorchOrbEntity> balls) {
        int[] ids = new int[balls.size()];
        for (int i = 0; i < balls.size(); i++) {
            ids[i] = balls.get(i).getId();
        }
        stack.getOrCreateTag().putIntArray(SHAKUTON_BALLS_TAG, ids);
    }

    private void clearScorchBalls(ItemStack stack) {
        if (stack.getTag() != null) {
            stack.getTag().remove(SHAKUTON_BALLS_TAG);
        }
    }

    public static boolean isJiraikenActive(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(JIRAIKEN_ACTIVE_TAG);
    }

    public static void setJiraikenActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(JIRAIKEN_ACTIVE_TAG, active);
    }

    public static float getJiraikenPower(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getFloat(JIRAIKEN_POWER_TAG) : 0.0F;
    }

    private static void setJiraikenPower(ItemStack stack, float power) {
        stack.getOrCreateTag().putFloat(JIRAIKEN_POWER_TAG, Math.max(power, 0.0F));
    }

    private static boolean hasFourTails(Player player) {
        if (player instanceof ServerPlayer serverPlayer && BijuManager.isJinchurikiOf(serverPlayer, 4)) {
            return true;
        }
        return NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS) == 4;
    }

    @Nullable
    private static LavaChakraModeEntity findActiveLavaChakraMode(ServerLevel level, Player player) {
        int entityId = player.getPersistentData().getInt(LavaChakraModeEntity.ENTITY_ID_KEY);
        if (entityId > 0 && level.getEntity(entityId) instanceof LavaChakraModeEntity entity && entity.isOwnedBy(player)) {
            return entity;
        }
        for (LavaChakraModeEntity entity : level.getEntitiesOfClass(LavaChakraModeEntity.class, player.getBoundingBox().inflate(64.0D))) {
            if (entity.isOwnedBy(player)) {
                player.getPersistentData().putInt(LavaChakraModeEntity.ENTITY_ID_KEY, entity.getId());
                return entity;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (ProcedureUtils.hasItemInInventory(player, ModItems.YOOTON.get()) && isFireDamage(event)) {
            event.setCanceled(true);
            return;
        }
        if (ProcedureUtils.hasItemInInventory(player, ModItems.HYOTON.get())
                && event.getSource().is(DamageTypes.IN_WALL)
                && isInIce(player)) {
            event.setCanceled(true);
        }
    }

    private static boolean isFireDamage(LivingAttackEvent event) {
        return event.getSource().is(DamageTypeTags.IS_FIRE)
                || event.getSource().is(DamageTypes.LAVA)
                || event.getSource().is(DamageTypes.HOT_FLOOR);
    }

    private static boolean isInIce(LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        return isIce(entity.level(), pos) || isIce(entity.level(), pos.above());
    }

    private static boolean isIce(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.ICE)
                || level.getBlockState(pos).is(Blocks.PACKED_ICE)
                || level.getBlockState(pos).is(Blocks.BLUE_ICE)
                || level.getBlockState(pos).is(Blocks.FROSTED_ICE);
    }

    private static boolean hasItem(Player player, Item item) {
        return ProcedureUtils.hasItemInInventory(player, item);
    }

    private static boolean hasRequirementFlag(Player player, int requiredFlags, int flag, Item item) {
        return (requiredFlags & flag) == 0 || hasItem(player, item);
    }

    private static JutsuDefinition definition(int index, String key, char rank, int requiredXp, double chakraUsage) {
        return new JutsuDefinition(index, key, rank, requiredXp, chakraUsage, 0.0F, 50.0F, null);
    }

    private static JutsuDefinition ranked(int index, String key, char rank, double chakraUsage) {
        return JutsuDefinition.ranked(index, key, rank, chakraUsage);
    }

    private record WoodHousePlacement(BlockPos spawnTo, BlockPos scanOrigin, Rotation rotation) {
    }

    public enum AdvancedNatureKind {
        BAKUTON(
                JutsuType.BAKUTON,
                REQ_DOTON | REQ_RAITON,
                false,
                Passive.NONE,
                "tooltip.bakuton.musthave",
                definition(0, "tooltip.bakuton.jiraiken", 'S', 150, 30.0D).withPower(0.2F, 150.0F),
                definition(1, "c_1", 'S', 200, 75.0D).withPower(1.0F, 150.0F),
                definition(2, "explosive_clone", 'S', 200, 150.0D)),
        FUTTON(
                JutsuType.FUTTON,
                REQ_KATON | REQ_SUITON,
                false,
                Passive.NONE,
                "tooltip.futton.musthave",
                ranked(0, "futton_mist", 'S', 50.0D).withPower(0.1F, 20.0F),
                ranked(1, "unrivaled_strength", 'S', 50.0D).withPower(0.1F, 20.0F)),
        HYOTON(
                JutsuType.HYOTON,
                REQ_FUTON | REQ_SUITON,
                false,
                Passive.HYOTON,
                "tooltip.hyoton.musthave",
                definition(0, "ice_spike", 'S', 150, 100.0D).withPower(1.0F, 80.0F),
                definition(1, "ice_spear", 'S', 150, 20.0D).withPower(1.0F, 40.0F),
                definition(2, "ice_dome", 'S', 200, 5.0D).withPower(1.0F, 40.0F),
                definition(3, "ice_prison", 'S', 150, 50.0D).withPower(1.0F, 40.0F)),
        JINTON(
                JutsuType.JINTON,
                REQ_KATON | REQ_FUTON | REQ_DOTON,
                false,
                Passive.NONE,
                "tooltip.jinton.musthave",
                definition(0, "jintonbeam", 'S', 700, 500.0D),
                definition(1, "jintoncube", 'S', 700, 600.0D)),
        JITON(
                JutsuType.JITON,
                REQ_FUTON | REQ_DOTON,
                true,
                Passive.NONE,
                "tooltip.jiton.musthave",
                definition(0, "entityjitonshield", 'S', 150, 20.0D).withPower(1.0F, 1.0F),
                definition(1, "sand_bullet", 'S', 100, 20.0D).withPower(1.0F, 1.0F),
                definition(2, "sand_bind", 'S', 200, 100.0D).withPower(1.0F, 1.0F),
                definition(3, "sand_levitation", 'S', 200, 0.25D).withPower(1.0F, 1.0F)),
        KEKKEI_MORA(
                JutsuType.KEKKEIMORA,
                0,
                false,
                Passive.NONE,
                null,
                ranked(0, "entity80gods", 'S', 10.0D),
                ranked(1, "tooltip.byakurinnesharingan.jutsu2", 'S', 10.0D),
                ranked(2, "tooltip.kekkeimora.expansivetsb", 'S', 10.0D),
                ranked(3, "item.ashbones.name", 'S', 10.0D)),
        MOKUTON(
                JutsuType.MOKUTON,
                0,
                false,
                Passive.MOKUTON,
                null,
                ranked(0, "tooltip.mokuton.leftclick", 'S', 100.0D),
                ranked(1, "wood_prison", 'S', 50.0D).withPower(1.0F, 50.0F),
                ranked(2, "tooltip.mokuton.rightclick2", 'S', 100.0D),
                definition(3, "wood_golem", 'S', 800, 1000.0D),
                definition(4, "wood_arm", 'S', 400, 50.0D)),
        RANTON(
                JutsuType.RANTON,
                REQ_RAITON | REQ_SUITON,
                false,
                Passive.NONE,
                "tooltip.ranton.musthave",
                ranked(0, "rantoncloud", 'S', 1.0D),
                ranked(1, "laser_circus", 'S', 100.0D).withPower(0.1F, 50.0F)),
        SHAKUTON(
                JutsuType.SHAKUTON,
                REQ_FUTON | REQ_KATON,
                false,
                Passive.NONE,
                "tooltip.shakuton.musthave",
                definition(0, "scorchorb", 'S', 150, 100.0D).withPower(1.0F, 1.0F),
                definition(1, "tooltip.shakuton.scorchkill", 'S', 200, 50.0D).withPower(1.0F, 1.0F),
                definition(2, "tooltip.shakuton.scorchblast", 'S', 250, 50.0D).withPower(1.0F, 1.0F)),
        SHIKOTSUMYAKU(
                JutsuType.SHIKOTSUMYAKU,
                0,
                false,
                Passive.NONE,
                null,
                definition(0, "tooltip.shikotsumyaku.dancelarch", 'S', 150, 100.0D),
                definition(1, "tooltip.shikotsumyaku.dancewillow", 'S', 150, 100.0D),
                definition(2, "tooltip.shikotsumyaku.dancecamellia", 'S', 150, 100.0D),
                definition(3, "finger_bone", 'S', 150, 10.0D).withPower(1.0F, 1.0F),
                definition(4, "tooltip.shikotsumyaku.danceclementisflower", 'S', 400, 500.0D),
                definition(5, "entitybrackendance", 'S', 400, 100.0D).withPower(0.5F, 150.0F)),
        YOOTON(
                JutsuType.YOOTON,
                REQ_DOTON | REQ_KATON,
                false,
                Passive.YOOTON,
                "tooltip.yooton.musthave",
                definition(0, "magmaball", 'S', 200, 30.0D).withPower(1.0F, 15.0F),
                definition(1, "melting_jutsu", 'S', 200, 30.0D).withPower(1.0F, 30.0F),
                definition(2, "lava_chakra_mode", 'S', 250, 10.0D).withPower(1.0F, 30.0F)),
        YOTON(
                JutsuType.YOTON,
                0,
                false,
                Passive.NONE,
                null,
                ranked(0, "biggerme", 'B', 50.0D).withPower(2.0F, 50.0F),
                ranked(1, "sealing", 'S', 100.0D));

        private final JutsuType jutsuType;
        private final int requiredFlags;
        private final boolean requiresGourdBody;
        private final Passive passive;
        @Nullable
        private final String requirementTooltipKey;
        private final JutsuDefinition[] definitions;

        AdvancedNatureKind(
                JutsuType jutsuType,
                int requiredFlags,
                boolean requiresGourdBody,
                Passive passive,
                @Nullable String requirementTooltipKey,
                JutsuDefinition... definitions) {
            this.jutsuType = jutsuType;
            this.requiredFlags = requiredFlags;
            this.requiresGourdBody = requiresGourdBody;
            this.passive = passive;
            this.requirementTooltipKey = requirementTooltipKey;
            this.definitions = definitions;
        }

        public JutsuDefinition definitionByIndex(int index) {
            for (JutsuDefinition definition : this.definitions) {
                if (definition.index() == index) {
                    return definition;
                }
            }
            return this.definitions[0];
        }

        private boolean hasRequirements(Player player) {
            if (player.isCreative()) {
                return true;
            }
            return hasRequirementFlag(player, this.requiredFlags, REQ_DOTON, ModItems.DOTON.get())
                    && hasRequirementFlag(player, this.requiredFlags, REQ_FUTON, ModItems.FUTON.get())
                    && hasRequirementFlag(player, this.requiredFlags, REQ_KATON, ModItems.KATON.get())
                    && hasRequirementFlag(player, this.requiredFlags, REQ_RAITON, ModItems.RAITON.get())
                    && hasRequirementFlag(player, this.requiredFlags, REQ_SUITON, ModItems.SUITON.get())
                    && (!this.requiresGourdBody || player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GOURDBODY.get()));
        }

        private void applyPassive(LivingEntity entity) {
            this.passive.apply(entity);
        }
    }

    private enum Passive {
        NONE {
            @Override
            void apply(LivingEntity entity) {
            }
        },
        HYOTON {
            @Override
            void apply(LivingEntity entity) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 3, false, false));
                entity.clearFire();
                if (entity instanceof Player player && !hasItem(player, ModItems.ICE_SENBON.get())) {
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.ICE_SENBON.get()));
                }
            }
        },
        MOKUTON {
            @Override
            void apply(LivingEntity entity) {
                entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, false, false));
                if (entity.getHealth() < entity.getMaxHealth()) {
                    entity.heal(0.2F);
                }
            }
        },
        YOOTON {
            @Override
            void apply(LivingEntity entity) {
                entity.clearFire();
            }
        };

        abstract void apply(LivingEntity entity);
    }
}
