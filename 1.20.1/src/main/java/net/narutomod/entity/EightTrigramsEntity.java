package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerTracker;
import net.narutomod.registry.ModEntityTypes;

public final class EightTrigramsEntity extends Entity {
    private static final String ACTIVE_KEY = "HakkeRokujuuyonshouActive";
    private static final double EFFECT_RADIUS = 16.0D;
    private static final int EFFECT_DURATION = 240;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(EightTrigramsEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public EightTrigramsEntity(EntityType<? extends EightTrigramsEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        EightTrigramsEntity entity = ModEntityTypes.EIGHTTRIGRAMSENTITY.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        return level.addFreshEntity(entity);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), 0.0F, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        if (this.tickCount > 3 && this.tickCount < 20) {
            applyAreaDebuffs(owner);
        }
        if (owner instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("tooltip.byakugan.jutsu2"), true);
            if (this.tickCount % 40 == 4) {
                int strength = Math.max((int)(PlayerTracker.getNinjaLevel(player) + this.tickCount) / 30, 0);
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, strength, false, false));
                owner.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50, 3, false, false));
            }
        }
        if (this.tickCount > EFFECT_DURATION) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            clearOwnerActive();
        }
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        setActive(owner, true);
    }

    @Nullable
    private LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof LivingEntity living) {
            return living;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            setOwner(living);
            return living;
        }
        return null;
    }

    private void clearOwnerActive() {
        LivingEntity owner = getOwner();
        if (owner != null) {
            setActive(owner, false);
        }
    }

    private static void setActive(Entity entity, boolean active) {
        entity.getPersistentData().putBoolean(ACTIVE_KEY, active);
    }

    private void applyAreaDebuffs(LivingEntity owner) {
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(EFFECT_RADIUS), LivingEntity::isAlive)) {
            if (target == owner || target.getRootVehicle() == owner.getRootVehicle()) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 4, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15, 255, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 15, 5, false, false));
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class HurtHook {
        private HurtHook() {
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            Entity attacker = event.getSource().getEntity();
            if (attacker != null && attacker.getPersistentData().getBoolean(ACTIVE_KEY)) {
                Chakra.pathway(event.getEntity()).consume(0.125F);
            }
        }
    }
}
