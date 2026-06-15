package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;

public final class ProcedureUtils {
    private static final Random RNG = new Random();

    private ProcedureUtils() {
    }

    public static double rngGaussian() {
        return RNG.nextGaussian();
    }

    public static boolean rngBoolean() {
        return RNG.nextBoolean();
    }

    public static double name2Id(String string) {
        long id = 0L;
        for (int index = 0; index < string.length() - 2 && index < 8; ++index) {
            id = id << 8 | string.charAt(index);
        }
        return id;
    }

    @Nullable
    public static LivingEntity searchPlayerMatchingId(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        Entity entity = server.overworld().getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public static Entity getEntityFromUUID(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(uuid);
        }
        return null;
    }

    @Nullable
    public static LivingEntity getLivingByUuid(Level level, UUID uuid) {
        Entity entity = getEntityFromUUID(level, uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static void setOriginalOwner(LivingEntity entity, ItemStack stack) {
        stack.getOrCreateTag().putUUID("player_id", entity.getUUID());
    }

    @Nullable
    public static UUID getOwnerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID("player_id") ? tag.getUUID("player_id") : null;
    }

    public static boolean isOriginalOwner(LivingEntity entity, ItemStack stack) {
        return entity.getUUID().equals(getOwnerId(stack));
    }

    @Nullable
    public static ItemStack getMatchingItemStack(Player player, Item item) {
        return getItemStackIgnoreDurability(player.getInventory(), new ItemStack(item));
    }

    @Nullable
    public static ItemStack getItemStackIgnoreDurability(Inventory inventory, ItemStack requested) {
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && stack.getItem() == requested.getItem()) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.armor) {
            if (!stack.isEmpty() && stack.getItem() == requested.getItem()) {
                return stack;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && stack.getItem() == requested.getItem()) {
                return stack;
            }
        }
        return null;
    }

    public static boolean hasAnyItemOfSubtype(Player player, Class<? extends Item> itemType) {
        return !getAllItemsOfSubType(player, itemType).isEmpty();
    }

    public static List<ItemStack> getAllItemsOfSubType(Player player, Class<? extends Item> itemType) {
        List<ItemStack> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        collectItemsOfSubtype(inventory.items, itemType, result);
        collectItemsOfSubtype(inventory.armor, itemType, result);
        collectItemsOfSubtype(inventory.offhand, itemType, result);
        return result;
    }

    private static void collectItemsOfSubtype(Iterable<ItemStack> stacks, Class<? extends Item> itemType, List<ItemStack> result) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && itemType.isAssignableFrom(stack.getItem().getClass())) {
                result.add(stack);
            }
        }
    }

    public static boolean hasItemStackIgnoreDurability(Inventory inventory, ItemStack itemStack) {
        return getItemStackIgnoreDurability(inventory, itemStack) != null;
    }

    public static boolean hasItemInInventory(Player player, Item item) {
        return hasItemStackIgnoreDurability(player.getInventory(), new ItemStack(item));
    }

    public static int getSlotFor(ItemStack requested, Player player) {
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.items.size(); index++) {
            if (stackEqualExact(requested, inventory.items.get(index))) {
                return index;
            }
        }
        for (int index = 0; index < inventory.armor.size(); index++) {
            if (stackEqualExact(requested, inventory.armor.get(index))) {
                return inventory.items.size() + index;
            }
        }
        for (int index = 0; index < inventory.offhand.size(); index++) {
            if (stackEqualExact(requested, inventory.offhand.get(index))) {
                return inventory.items.size() + inventory.armor.size() + index;
            }
        }
        return -1;
    }

    private static boolean stackEqualExact(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
    }

    public static void swapItemToSlot(Player player, EquipmentSlot slot, ItemStack itemStack) {
        ItemStack current = player.getItemBySlot(slot);
        ItemStack matching = getItemStackIgnoreDurability(player.getInventory(), itemStack);
        ItemStack replacement = itemStack.copy();
        if (matching != null && !matching.isEmpty()) {
            replacement = matching.copyWithCount(1);
            matching.shrink(1);
        }
        if (!current.isEmpty()) {
            if (current.getItem() == replacement.getItem()) {
                return;
            }
            ItemHandlerHelper.giveItemToPlayer(player, current);
        }
        player.setItemSlot(slot, replacement);
    }

    public static boolean attackEntityAsMob(LivingEntity attacker, Entity target) {
        float damage = (float) getModifiedAttackDamage(attacker);
        boolean hurt = target.hurt(attacker.damageSources().mobAttack(attacker), damage);
        if (hurt) {
            ItemStack stack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            if (!stack.isEmpty() && target instanceof LivingEntity livingTarget) {
                stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                EnchantmentHelper.doPostHurtEffects(livingTarget, attacker);
                EnchantmentHelper.doPostDamageEffects(attacker, livingTarget);
            }
            attacker.setLastHurtMob(target);
        }
        return hurt;
    }

    public static Vec3 getMotion(Entity entity) {
        return entity.getDeltaMovement();
    }

    public static double getVelocity(Entity entity) {
        return getMotion(entity).length();
    }

    public static void setVelocity(Entity target, double motionX, double motionY, double motionZ) {
        target.setDeltaMovement(motionX, motionY, motionZ);
        target.hasImpulse = true;
    }

    public static void multiplyVelocity(Entity target, double multiplier) {
        multiplyVelocity(target, multiplier, multiplier, multiplier);
    }

    public static void multiplyVelocity(Entity target, double mulX, double mulY, double mulZ) {
        Vec3 motion = target.getDeltaMovement();
        setVelocity(target, motion.x * mulX, motion.y * mulY, motion.z * mulZ);
    }

    public static Vec3 pushEntity(Entity attacker, Entity target, double range, float multiplier) {
        return pushEntity(attacker.position(), target, range, multiplier);
    }

    public static Vec3 pushEntity(Vec3 source, Entity target, double range, float multiplier) {
        Vec3 delta = target.position().subtract(source);
        double distance = delta.length();
        if (distance > range || distance == 0.0D) {
            return Vec3.ZERO;
        }
        double height = target.getBoundingBox().getYsize();
        double verticalScale = Math.sqrt(2.0D / Math.max(height, 0.01D));
        Vec3 pushed = delta.normalize().scale(verticalScale);
        if (target.onGround() && pushed.y < verticalScale * 0.6D) {
            pushed = pushed.add(0.0D, verticalScale * 0.6D, 0.0D);
        }
        multiplier *= (float) (range - distance) * 0.1F;
        Vec3 next = pushed.scale(multiplier).add(target.getDeltaMovement());
        setVelocity(target, next.x, next.y, next.z);
        return next;
    }

    public static boolean breakBlockAndDropWithChance(Level level, BlockPos pos, float hardnessLimit, float breakChance, float dropChance) {
        return breakBlockAndDropWithChance(level, pos, hardnessLimit, breakChance, dropChance, true);
    }

    public static boolean breakBlockAndDropWithChance(Level level, BlockPos pos, float hardnessLimit, float breakChance, float dropChance, boolean sound) {
        BlockState blockState = level.getBlockState(pos);
        float hardness = blockState.getDestroySpeed(level, pos);
        if (blockState.isAir() || hardness < 0.0F || hardness > hardnessLimit || RNG.nextFloat() > breakChance) {
            return false;
        }

        if (sound) {
            level.levelEvent(2001, pos, Block.getId(blockState));
        }
        if (!(level instanceof ServerLevel serverLevel) || !ForgeEventFactory.getMobGriefingEvent(serverLevel, null)) {
            return false;
        }
        if (RNG.nextFloat() <= dropChance) {
            Block.dropResources(blockState, serverLevel, pos);
        }
        return serverLevel.destroyBlock(pos, false);
    }

    public static void pullEntity(Vec3 source, Entity target, float multiplier) {
        Vec3 delta = source.subtract(target.position());
        double distance = delta.length();
        if (distance == 0.0D) {
            return;
        }
        Vec3 next = delta.normalize().scale(multiplier * distance * 0.1F);
        setVelocity(target, next.x, next.y, next.z);
    }

    public static boolean purgeHarmfulEffects(LivingEntity entity) {
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!effect.getEffect().isBeneficial()) {
                harmful.add(effect);
            }
        }
        harmful.forEach(effect -> entity.removeEffect(effect.getEffect()));
        return harmful.isEmpty();
    }

    @Nullable
    public static UUID getUniqueId(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(key) ? tag.getUUID(key) : null;
    }

    public static void removeUniqueIdTag(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(key);
            tag.remove(key + "Most");
            tag.remove(key + "Least");
        }
    }

    public static boolean isEntityInFOV(LivingEntity looker, Entity target) {
        double yaw = -Mth.atan2(target.getX() - looker.getX(), target.getZ() - looker.getZ()) * (180.0D / Math.PI);
        return Math.abs(Mth.wrapDegrees(yaw - looker.getYHeadRot())) < 85.0D && looker.hasLineOfSight(target);
    }

    public static BlockHitResult raytraceBlocks(Entity entity, double distance) {
        Vec3 start = entity.getEyePosition();
        Vec3 end = start.add(entity.getLookAngle().scale(distance));
        return entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range) {
        return objectEntityLookingAt(entity, range, 0.0D, false, false, target -> true);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, double bbGrow) {
        return objectEntityLookingAt(entity, range, bbGrow, false, false, target -> true);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, @Nullable Entity excluded) {
        return objectEntityLookingAt(entity, range, 0.0D, false, false, target -> target != excluded);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, boolean trackAll) {
        return objectEntityLookingAt(entity, range, 0.0D, trackAll, false, target -> true);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, boolean trackAll, boolean stopOnLiquid) {
        return objectEntityLookingAt(entity, range, 0.0D, trackAll, stopOnLiquid, target -> true);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, boolean trackAll, boolean stopOnLiquid, @Nullable Predicate<Entity> filter) {
        return objectEntityLookingAt(entity, range, 0.0D, trackAll, stopOnLiquid, filter);
    }

    public static HitResult objectEntityLookingAt(Entity entity, double range, double bbGrow, boolean trackAll, boolean stopOnLiquid, @Nullable Predicate<Entity> filter) {
        Vec3 start = entity.getEyePosition();
        Vec3 look = entity.getLookAngle().scale(range);
        Vec3 end = start.add(look);
        BlockHitResult blockHit = entity.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                stopOnLiquid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                entity));
        double bestDistance = blockHit.getType() == HitResult.Type.MISS ? range : blockHit.getLocation().distanceTo(start);
        Entity bestEntity = null;
        Vec3 bestLocation = null;
        AABB search = entity.getBoundingBox().expandTowards(look).inflate(Math.max(1.0D, bbGrow));
        for (Entity candidate : entity.level().getEntities(entity, search, target ->
                target != null
                        && target.getRootVehicle() != entity.getRootVehicle()
                        && (trackAll || target.isPickable())
                        && (filter == null || filter.test(target)))) {
            AABB box = candidate.getBoundingBox().inflate(bbGrow);
            var optional = box.clip(start, end);
            if (box.contains(start)) {
                bestEntity = candidate;
                bestLocation = optional.orElse(start);
                bestDistance = 0.0D;
            } else if (optional.isPresent()) {
                double distance = optional.get().distanceTo(start);
                if (distance < bestDistance || bestDistance == 0.0D) {
                    bestEntity = candidate;
                    bestLocation = optional.get();
                    bestDistance = distance;
                }
            }
        }
        return bestEntity != null ? new EntityHitResult(bestEntity, bestLocation) : blockHit;
    }

    public static boolean advancementAchieved(ServerPlayer player, String advancementName) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.parse(advancementName));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void grantAdvancement(ServerPlayer player, String advancementName, boolean playToast) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.parse(advancementName));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
            if (playToast) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    public static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    public static double getFollowRange(LivingEntity entity) {
        return attributeValue(entity, Attributes.FOLLOW_RANGE);
    }

    public static double getModifiedSpeed(LivingEntity entity) {
        return attributeValue(entity, Attributes.MOVEMENT_SPEED);
    }

    public static double getAttackSpeed(LivingEntity entity) {
        return attributeValue(entity, Attributes.ATTACK_SPEED);
    }

    public static double getModifiedAttackDamage(LivingEntity entity) {
        return attributeValue(entity, Attributes.ATTACK_DAMAGE);
    }

    public static double getPunchDamage(LivingEntity entity) {
        return entity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
    }

    public static double getArmorValue(LivingEntity entity) {
        return attributeValue(entity, Attributes.ARMOR);
    }

    public static double getReachDistance(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(ForgeMod.ENTITY_REACH.get());
        return attribute != null ? attribute.getValue() : entity.getBbWidth() * 2.0D;
    }

    public static double getReachDistanceSq(LivingEntity entity) {
        double reach = getReachDistance(entity);
        return reach * reach;
    }

    private static double attributeValue(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null ? instance.getValue() : 0.0D;
    }

    public static void setDeathAnimations(LivingEntity entity, int type, int duration) {
        if (entity instanceof Player player && player.isCreative()) {
            return;
        }
        if (entity.getPersistentData().getDouble("deathAnimationType") == 0.0D) {
            ProcedureSync.EntityNBTTag.setAndSync(entity, "deathAnimationType", (double) type);
            ProcedureSync.EntityNBTTag.setAndSync(entity, NarutomodModVariables.DEATH_ANIMATION_TIME, (double) duration);
        }
    }

    public static void setInvulnerableDimensionChange(ServerPlayer player) {
        player.invulnerableTime = Math.max(player.invulnerableTime, 60);
    }

    public static float subtractDegreesWrap(float current, float previous) {
        return current - unwindDegrees(current, previous);
    }

    public static float interpolateRotation(float previous, float current, float partialTick) {
        return previous + Mth.wrapDegrees(current - previous) * partialTick;
    }

    private static float unwindDegrees(float current, float previous) {
        while (current - previous < -180.0F) {
            previous -= 360.0F;
        }
        while (current - previous >= 180.0F) {
            previous += 360.0F;
        }
        return previous;
    }

    public static Vec3 rotateRoll(Vec3 vec, float roll) {
        float cos = Mth.cos(roll);
        float sin = Mth.sin(roll);
        return new Vec3(vec.x * cos - vec.y * sin, vec.y * cos + vec.x * sin, vec.z);
    }

    public static float getYawFromVec(double x, double z) {
        return (float) (-Mth.atan2(x, z) * (180.0D / Math.PI));
    }

    public static float getYawFromVec(Vec3 vec) {
        return getYawFromVec(vec.x, vec.z);
    }

    public static float getPitchFromVec(double x, double y, double z) {
        float horizontal = Mth.sqrt((float) (x * x + z * z));
        return (float) (-Mth.atan2(y, horizontal) * (180.0D / Math.PI));
    }

    public static float getPitchFromVec(Vec3 vec) {
        return getPitchFromVec(vec.x, vec.y, vec.z);
    }

    public static Vec2f getYawPitchFromVec(Vec3 vec) {
        return new Vec2f(getYawFromVec(vec), getPitchFromVec(vec));
    }

    public static double getCDModifier(double modifier) {
        return 1.0D / (0.5D + 0.02D * modifier);
    }

    public static double getCooldownModifier(Player player) {
        return getCDModifier(PlayerTracker.getNinjaLevel(player));
    }

    public static double modifiedCooldown(double cooldown, Player player) {
        return cooldown * getCooldownModifier(player);
    }

    public static boolean isPlayerDisconnected(Entity entity) {
        return entity instanceof ServerPlayer player && player.hasDisconnected();
    }

    public static void sendChatAll(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    public static final class BlockposSorter implements Comparator<BlockPos> {
        private final BlockPos pos;

        public BlockposSorter(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public int compare(BlockPos first, BlockPos second) {
            return Double.compare(pos.distSqr(first), pos.distSqr(second));
        }
    }

    public static final class EntitySorter implements Comparator<Entity> {
        private final double x;
        private final double y;
        private final double z;

        public EntitySorter(Entity entity) {
            this(entity.getX(), entity.getY(), entity.getZ());
        }

        public EntitySorter(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int compare(Entity first, Entity second) {
            return Double.compare(first.distanceToSqr(x, y, z), second.distanceToSqr(x, y, z));
        }
    }

    public static final class VecSorter implements Comparator<Vec3> {
        private final Vec3 vec;
        private final boolean reverse;

        public VecSorter(Vec3 vec) {
            this(vec, false);
        }

        public VecSorter(Vec3 vec, boolean reverse) {
            this.vec = vec;
            this.reverse = reverse;
        }

        @Override
        public int compare(Vec3 first, Vec3 second) {
            int result = Double.compare(vec.distanceTo(first), vec.distanceTo(second));
            return reverse ? -result : result;
        }
    }

    public static final class BB {
        private BB() {
        }

        public static double calculateInvXOffset(AABB main, AABB other, double offsetX) {
            if (other.maxY > main.minY && other.minY < main.maxY && other.maxZ > main.minZ && other.minZ < main.maxZ) {
                if (offsetX > 0.0D && other.maxX <= main.maxX) {
                    offsetX = Math.min(main.maxX - other.maxX, offsetX);
                } else if (offsetX < 0.0D && other.minX >= main.minX) {
                    offsetX = Math.max(main.minX - other.minX, offsetX);
                }
            }
            return offsetX;
        }

        public static double calculateInvYOffset(AABB main, AABB other, double offsetY) {
            if (other.maxX > main.minX && other.minX < main.maxX && other.maxZ > main.minZ && other.minZ < main.maxZ) {
                if (offsetY > 0.0D && other.maxY <= main.maxY) {
                    offsetY = Math.min(main.maxY - other.maxY, offsetY);
                } else if (offsetY < 0.0D && other.minY >= main.minY) {
                    offsetY = Math.max(main.minY - other.minY, offsetY);
                }
            }
            return offsetY;
        }

        public static double calculateInvZOffset(AABB main, AABB other, double offsetZ) {
            if (other.maxX > main.minX && other.minX < main.maxX && other.maxY > main.minY && other.minY < main.maxY) {
                if (offsetZ > 0.0D && other.maxZ <= main.maxZ) {
                    offsetZ = Math.min(main.maxZ - other.maxZ, offsetZ);
                } else if (offsetZ < 0.0D && other.minZ >= main.minZ) {
                    offsetZ = Math.max(main.minZ - other.minZ, offsetZ);
                }
            }
            return offsetZ;
        }

        public static double getCenterX(AABB aabb) {
            return (aabb.minX + aabb.maxX) * 0.5D;
        }

        public static double getCenterY(AABB aabb) {
            return (aabb.minY + aabb.maxY) * 0.5D;
        }

        public static double getCenterZ(AABB aabb) {
            return (aabb.minZ + aabb.maxZ) * 0.5D;
        }

        public static Vec3 getCenter(AABB aabb) {
            return new Vec3(getCenterX(aabb), getCenterY(aabb), getCenterZ(aabb));
        }

        public static double getVolume(AABB aabb) {
            return (aabb.maxX - aabb.minX) * (aabb.maxY - aabb.minY) * (aabb.maxZ - aabb.minZ);
        }
    }

    public static final class Vec2f {
        public static final Vec2f ZERO = new Vec2f(0.0F, 0.0F);
        public final float x;
        public final float y;

        public Vec2f(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Vec2f vec) {
            return vec == this || wrapDegrees(vec.x) == wrapDegrees(x) && wrapDegrees(vec.y) == wrapDegrees(y);
        }

        public Vec2f wrapDegrees() {
            return new Vec2f(wrapDegrees(x), wrapDegrees(y));
        }

        public Vec2f add(float x, float y) {
            return new Vec2f(wrapDegrees(this.x + x), wrapDegrees(this.y + y));
        }

        public Vec2f add(Vec2f vec) {
            return add(vec.x, vec.y);
        }

        public Vec2f subtract(float x, float y) {
            return new Vec2f(wrapDegrees(this.x - x), wrapDegrees(this.y - y));
        }

        public Vec2f subtract(Vec2f vec) {
            return subtract(vec.x, vec.y);
        }

        public Vec2f scale(float scale) {
            return new Vec2f(wrapDegrees(x * scale), wrapDegrees(y * scale));
        }

        public float lengthVector() {
            return Mth.sqrt(x * x + y * y);
        }

        public Vec2f rad2Deg() {
            return scale(180.0F / (float) Math.PI);
        }

        public static float wrapDegrees(float value) {
            return Mth.wrapDegrees(value);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
