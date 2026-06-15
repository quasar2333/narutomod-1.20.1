package net.narutomod.entity;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModItems;

public final class WhiteZetsuEntity extends NinjaMobEntity {
    private static final EntityDataAccessor<Integer> DISGUISED_PLAYER_ID =
            SynchedEntityData.defineId(WhiteZetsuEntity.class, EntityDataSerializers.INT);
    private static final double DISGUISE_RANGE = 50.0D;

    public WhiteZetsuEntity(EntityType<? extends WhiteZetsuEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 25;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KUNAI.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(25.0D, 0.5D, 0.6D, 5.0D, 48.0D);
    }

    public int getDisguisedPlayerId() {
        return this.entityData.get(DISGUISED_PLAYER_ID);
    }

    @Nullable
    public LivingEntity getDisguisedPlayer() {
        int playerId = getDisguisedPlayerId();
        return playerId >= 0 && this.level().getEntity(playerId) instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DISGUISED_PLAYER_ID, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, (float) DISGUISE_RANGE));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && getDisguisedPlayerId() < 0) {
            acquireLegacyDisguise();
        }
    }

    private void acquireLegacyDisguise() {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(DISGUISE_RANGE),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        players.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresent(this::setDisguisedPlayer);
    }

    private void setDisguisedPlayer(Player player) {
        this.entityData.set(DISGUISED_PLAYER_ID, player.getId());
        this.setCustomName(player.getName());
    }
}
