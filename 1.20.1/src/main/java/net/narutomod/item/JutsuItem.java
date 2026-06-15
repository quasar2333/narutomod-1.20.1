package net.narutomod.item;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.procedure.ProcedureUtils;

public abstract class JutsuItem extends Item {
    public static final String JUTSU_INDEX_TAG = "JutsuIndexKey";
    public static final String COOLDOWN_TAG_PREFIX = "JutsuCDMapKey";
    public static final String EXPERIENCE_MAP_TAG = "JutsuExperienceMapKey";
    public static final String OWNER_ID_TAG = "OwnerIdKey";
    public static final String AFFINITY_TAG = "IsNatureAffinityKey";

    private final List<JutsuDefinition> jutsuList;
    private final long[] defaultCooldownMap;
    private final int[] defaultExperienceMap;

    protected JutsuItem(JutsuType type, JutsuDefinition... definitions) {
        this(type, false, definitions);
    }

    protected JutsuItem(JutsuType type, boolean defaultEnabled, JutsuDefinition... definitions) {
        super(new Item.Properties().stacksTo(1));
        if (definitions.length == 0) {
            throw new IllegalArgumentException("Empty jutsu list");
        }
        this.jutsuList = Arrays.stream(definitions)
                .map(definition -> definition.withType(type))
                .toList();
        this.defaultCooldownMap = new long[definitions.length];
        this.defaultExperienceMap = new int[definitions.length];
        Arrays.fill(this.defaultCooldownMap, defaultEnabled ? 0L : -1L);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity && getOwnerId(stack) == null) {
            setOwner(stack, livingEntity);
            resetJutsuMaps(stack);
        }
    }

    public List<JutsuDefinition> jutsuList() {
        return this.jutsuList;
    }

    protected JutsuDefinition getCurrentJutsu(ItemStack stack) {
        return this.jutsuList.get(getCurrentJutsuIndex(stack));
    }

    protected int getCurrentJutsuIndex(ItemStack stack) {
        int index = stack.getOrCreateTag().getInt(JUTSU_INDEX_TAG);
        if (index < 0 || index >= this.jutsuList.size()) {
            index = 0;
            stack.getOrCreateTag().putInt(JUTSU_INDEX_TAG, index);
        }
        return index;
    }

    protected void setCurrentJutsuIndex(ItemStack stack, int index) {
        if (index >= 0 && index < this.jutsuList.size()) {
            stack.getOrCreateTag().putInt(JUTSU_INDEX_TAG, index);
        }
    }

    protected boolean switchToNextUsableJutsu(ItemStack stack, LivingEntity entity) {
        int current = getCurrentJutsuIndex(stack);
        for (int offset = 1; offset <= this.jutsuList.size(); offset++) {
            int next = (current + offset) % this.jutsuList.size();
            if (canUseJutsu(stack, next, entity)) {
                setCurrentJutsuIndex(stack, next);
                return true;
            }
        }
        return false;
    }

    public boolean switchToNextUsableJutsuAndNotify(ItemStack stack, LivingEntity entity) {
        if (!switchToNextUsableJutsu(stack, entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(getCurrentJutsu(stack).translationKey()), true);
        }
        return true;
    }

    public String describeCurrentJutsuKey(ItemStack stack) {
        return getCurrentJutsu(stack).translationKey() + "#" + getCurrentJutsu(stack).index();
    }

    protected int getCurrentJutsuXp(ItemStack stack) {
        return getJutsuXp(stack, getCurrentJutsuIndex(stack));
    }

    protected void addCurrentJutsuXp(ItemStack stack, int xp) {
        addJutsuXp(stack, getCurrentJutsuIndex(stack), xp);
    }

    public int getJutsuXp(ItemStack stack, JutsuDefinition definition) {
        int index = getDefinitionIndex(definition);
        return index >= 0 ? getJutsuXp(stack, index) : 0;
    }

    protected void addJutsuXp(ItemStack stack, JutsuDefinition definition, int xp) {
        int index = getDefinitionIndex(definition);
        if (index >= 0) {
            addJutsuXp(stack, index, xp);
        }
    }

    public void setJutsuXp(ItemStack stack, JutsuDefinition definition, int xp) {
        int index = getDefinitionIndex(definition);
        if (index >= 0) {
            int[] map = getJutsuXpMap(stack);
            map[index] = Math.max(xp, 0);
            stack.getOrCreateTag().putIntArray(EXPERIENCE_MAP_TAG, map);
        }
    }

    private int getJutsuXp(ItemStack stack, int index) {
        return getJutsuXpMap(stack)[index];
    }

    private void addJutsuXp(ItemStack stack, int index, int xp) {
        int[] map = getJutsuXpMap(stack);
        map[index] += xp;
        stack.getOrCreateTag().putIntArray(EXPERIENCE_MAP_TAG, map);
    }

    protected int getCurrentJutsuRequiredXp(ItemStack stack) {
        return getRequiredXp(stack, getCurrentJutsuIndex(stack));
    }

    public int getRequiredXp(ItemStack stack, JutsuDefinition definition) {
        int index = getDefinitionIndex(definition);
        return index >= 0 ? getRequiredXp(stack, index) : definition.requiredXp();
    }

    private int getRequiredXp(ItemStack stack, int index) {
        int requiredXp = this.jutsuList.get(index).requiredXp();
        return isAffinity(stack) ? requiredXp : requiredXp * 2;
    }

    protected float getCurrentJutsuXpModifier(ItemStack stack, LivingEntity entity) {
        boolean maxed = !(entity instanceof Player player) || player.isCreative();
        int required = getCurrentJutsuRequiredXp(stack);
        int current = maxed ? required : getCurrentJutsuXp(stack);
        return current != 0 ? (float) required / (float) current : 0.0F;
    }

    protected long getCurrentJutsuCooldown(ItemStack stack) {
        return getJutsuCooldown(stack, getCurrentJutsuIndex(stack));
    }

    public long getJutsuCooldown(ItemStack stack, JutsuDefinition definition) {
        int index = getDefinitionIndex(definition);
        return index >= 0 ? getJutsuCooldown(stack, index) : -1L;
    }

    private long getJutsuCooldown(ItemStack stack, int index) {
        validateMapTags(stack, index);
        return stack.getOrCreateTag().getLong(COOLDOWN_TAG_PREFIX + index);
    }

    protected void setCurrentJutsuCooldown(ItemStack stack, Level level, long ticks) {
        setJutsuCooldown(stack, level, getCurrentJutsuIndex(stack), ticks);
    }

    protected void setJutsuCooldown(ItemStack stack, Level level, JutsuDefinition definition, long ticks) {
        int index = getDefinitionIndex(definition);
        if (index >= 0 && isJutsuEnabled(stack, index)) {
            setJutsuCooldown(stack, level, index, ticks);
        }
    }

    private void setJutsuCooldown(ItemStack stack, Level level, int index, long ticks) {
        validateMapTags(stack, index);
        stack.getOrCreateTag().putLong(COOLDOWN_TAG_PREFIX + index, level.getGameTime() + ticks);
    }

    protected long getRemainingCooldownTicks(ItemStack stack, Level level, JutsuDefinition definition) {
        long cooldownEnd = getJutsuCooldown(stack, definition);
        return Math.max(cooldownEnd - level.getGameTime(), 0L);
    }

    public void enableJutsu(ItemStack stack, JutsuDefinition definition, boolean enable) {
        int index = getDefinitionIndex(definition);
        if (index >= 0) {
            long current = getJutsuCooldown(stack, index);
            stack.getOrCreateTag().putLong(COOLDOWN_TAG_PREFIX + index, enable ? Math.max(current, 0L) : -1L);
        }
    }

    public boolean isJutsuEnabled(ItemStack stack, JutsuDefinition definition) {
        int index = getDefinitionIndex(definition);
        return index >= 0 && isJutsuEnabled(stack, index);
    }

    private boolean isJutsuEnabled(ItemStack stack, int index) {
        return getJutsuCooldown(stack, index) >= 0L;
    }

    protected boolean canUseCurrentJutsu(ItemStack stack, @Nullable LivingEntity entity) {
        return canUseJutsu(stack, getCurrentJutsuIndex(stack), entity);
    }

    public boolean activateCloneHeldJutsus(ItemStack stack, LivingEntity clone) {
        return false;
    }

    /**
     * Public access to {@link #canUseJutsu(ItemStack, JutsuDefinition, LivingEntity)} so one tome can
     * gate a jutsu on another tome's jutsu (e.g. Futon's Rasenshuriken on Ninjutsu's Rasengan).
     */
    public boolean canPlayerUseJutsu(ItemStack stack, JutsuDefinition definition, @Nullable LivingEntity entity) {
        return canUseJutsu(stack, definition, entity);
    }

    protected boolean canUseJutsu(ItemStack stack, JutsuDefinition definition, @Nullable LivingEntity entity) {
        int index = getDefinitionIndex(definition);
        return index >= 0 && canUseJutsu(stack, index, entity);
    }

    private boolean canUseJutsu(ItemStack stack, int index, @Nullable LivingEntity entity) {
        return entity instanceof Player player && player.isCreative()
                || entity != null && isOwnedByOrUnbound(entity, stack) && isJutsuEnabled(stack, index);
    }

    protected boolean hasEnoughJutsuXp(ItemStack stack, JutsuDefinition definition) {
        int index = getDefinitionIndex(definition);
        return index >= 0 && getJutsuXp(stack, index) >= getRequiredXp(stack, index);
    }

    protected float getChargingPower(ItemStack stack, LivingEntity entity, int remainingUseDuration, float basePower, float powerUpDelay) {
        float divisor = powerUpDelay * (float) Chakra.getChakraModifier(entity) * getCurrentJutsuXpModifier(stack, entity);
        return divisor > 0.0F ? basePower + (float) Math.max(getUseDuration(stack) - remainingUseDuration, 0) / divisor : 0.0F;
    }

    protected void setAffinity(ItemStack stack, boolean affinity) {
        stack.getOrCreateTag().putBoolean(AFFINITY_TAG, affinity);
    }

    protected boolean isAffinity(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(AFFINITY_TAG);
    }

    /**
     * God-mode helper for the {@code /narutomax} command: binds the tome to {@code owner}, marks it
     * as a natural affinity (so required XP is not doubled), then learns and fully levels every
     * jutsu it holds. Subclasses with extra "learned" flags should override {@link #onMaxedOut}.
     */
    public void maxOut(ItemStack stack, LivingEntity owner) {
        setOwner(stack, owner);
        setAffinity(stack, true);
        for (JutsuDefinition definition : this.jutsuList) {
            enableJutsu(stack, definition, true);
            setJutsuXp(stack, definition, getRequiredXp(stack, definition));
        }
        onMaxedOut(stack);
    }

    /** Hook for subclasses to set any extra "learned" flags after {@link #maxOut}. */
    protected void onMaxedOut(ItemStack stack) {
    }

    /** Adds experience to the currently selected jutsu (used by the {@code /addxp2jutsu} command). */
    public void addXpToCurrentJutsu(ItemStack stack, int xp) {
        addCurrentJutsuXp(stack, xp);
    }

    public static void setOwner(ItemStack stack, LivingEntity owner) {
        stack.getOrCreateTag().putUUID(OWNER_ID_TAG, owner.getUUID());
        ProcedureUtils.setOriginalOwner(owner, stack);
    }

    public static void setOwnerIfMissing(ItemStack stack, LivingEntity owner) {
        if (getOwnerId(stack) == null) {
            setOwner(stack, owner);
        }
    }

    public static boolean isOwnedByOrUnbound(LivingEntity entity, ItemStack stack) {
        UUID ownerId = getOwnerId(stack);
        return ownerId == null || ownerId.equals(entity.getUUID());
    }

    @Nullable
    public static UUID getOwnerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(OWNER_ID_TAG)) {
            return tag.getUUID(OWNER_ID_TAG);
        }
        return ProcedureUtils.getOwnerId(stack);
    }

    private void resetJutsuMaps(ItemStack stack) {
        resetCooldownMap(stack);
        stack.getOrCreateTag().putIntArray(EXPERIENCE_MAP_TAG, this.defaultExperienceMap);
    }

    private void resetCooldownMap(ItemStack stack) {
        for (int i = 0; i < this.defaultCooldownMap.length; i++) {
            stack.getOrCreateTag().putLong(COOLDOWN_TAG_PREFIX + i, this.defaultCooldownMap[i]);
        }
    }

    private void validateMapTags(ItemStack stack, int index) {
        if (index >= 0 && !stack.getOrCreateTag().contains(COOLDOWN_TAG_PREFIX + index)) {
            stack.getOrCreateTag().putLong(COOLDOWN_TAG_PREFIX + index, this.defaultCooldownMap[index]);
        }
        if (!stack.getOrCreateTag().contains(EXPERIENCE_MAP_TAG)) {
            stack.getOrCreateTag().putIntArray(EXPERIENCE_MAP_TAG, this.defaultExperienceMap);
        }
    }

    private int[] getJutsuXpMap(ItemStack stack) {
        validateMapTags(stack, -1);
        int[] map = stack.getOrCreateTag().getIntArray(EXPERIENCE_MAP_TAG);
        if (map.length < this.jutsuList.size()) {
            int[] expanded = Arrays.copyOf(map, this.jutsuList.size());
            stack.getOrCreateTag().putIntArray(EXPERIENCE_MAP_TAG, expanded);
            return expanded;
        }
        return map;
    }

    private int getDefinitionIndex(JutsuDefinition definition) {
        for (int i = 0; i < this.jutsuList.size(); i++) {
            JutsuDefinition candidate = this.jutsuList.get(i);
            if (candidate.index() == definition.index()
                    && candidate.translationKey().equals(definition.translationKey())) {
                return i;
            }
        }
        return -1;
    }

    public record JutsuDefinition(
            int index,
            String translationKey,
            char rank,
            int requiredXp,
            double chakraUsage,
            float basePower,
            float powerUpDelay,
            JutsuType type) {
        public static JutsuDefinition ranked(int index, String translationKey, char rank, double chakraUsage) {
            return new JutsuDefinition(index, translationKey, rank, requiredXpForRank(rank), chakraUsage, 0.0F, 50.0F, null);
        }

        public JutsuDefinition withPower(float basePower, float powerUpDelay) {
            return new JutsuDefinition(index, translationKey, rank, requiredXp, chakraUsage, basePower, powerUpDelay, type);
        }

        private JutsuDefinition withType(JutsuType type) {
            return new JutsuDefinition(index, translationKey, rank, requiredXp, chakraUsage, basePower, powerUpDelay, type);
        }

        private static int requiredXpForRank(char rank) {
            return switch (rank) {
                case 'S' -> 400;
                case 'A' -> 250;
                case 'B' -> 200;
                case 'C' -> 150;
                case 'D' -> 100;
                default -> 900;
            };
        }
    }

    public enum JutsuType {
        NINJUTSU,
        DOTON,
        FUTON,
        KATON,
        RAITON,
        SUITON,
        INTON,
        YOTON,
        JINTON,
        MOKUTON,
        JITON,
        IRYO,
        HYOTON,
        BAKUTON,
        SHAKUTON,
        BYAKUGAN,
        SHARINGAN,
        RINNEGAN,
        RANTON,
        FUTTON,
        YOOTON,
        SHIKOTSUMYAKU,
        KUCHIYOSE,
        TENSEIGAN,
        SENJUTSU,
        SIXPATHSENJUTSU,
        KEKKEIMORA
    }
}
