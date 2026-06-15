package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class RantonCloudEntity extends Entity {
    private static final double CHAKRA_BURN_PER_TICK = 1.0D;
    private static final int LIGHTNING_COLOR = 0xC00000FF;
    private static final int SMOKE_COLOR = 0xFF303030;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(RantonCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DAMAGE_XP = SynchedEntityData.defineId(RantonCloudEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public RantonCloudEntity(EntityType<? extends RantonCloudEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, int damageXp) {
        setOwner(owner);
        setDamageXp(damageXp);
        moveToOwner(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, int damageXp) {
        RantonCloudEntity entity = ModEntityTypes.RANTONCLOUD.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, damageXp);
        return owner.level().addFreshEntity(entity);
    }

    @Nullable
    public static RantonCloudEntity findActive(ServerLevel level, LivingEntity owner) {
        UUID ownerId = owner.getUUID();
        return level.getEntitiesOfClass(RantonCloudEntity.class, owner.getBoundingBox().inflate(128.0D),
                        entity -> entity.ownerUuid != null && entity.ownerUuid.equals(ownerId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DAMAGE_XP, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !Chakra.pathway(owner).consume(CHAKRA_BURN_PER_TICK)) {
            discard();
            return;
        }
        moveToOwner(owner);
        playElectricity(owner);
        spawnFeedback((ServerLevel)this.level(), owner);
        strikeNearby(owner);
        strikeLookTarget(owner);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setDamageXp(tag.getInt("DamageXp"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("DamageXp", getDamageXp());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void playElectricity(LivingEntity owner) {
        if (this.random.nextInt(20) == 0) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.1F, this.random.nextFloat() * 0.6F + 0.3F);
        }
    }

    private void spawnFeedback(ServerLevel level, LivingEntity owner) {
        Vec3 center = new Vec3(
                owner.getX() + (this.random.nextDouble() - 0.5D) * 2.0D,
                owner.getY() + this.random.nextDouble() * 1.6D,
                owner.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D);
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc != null) {
            arc.configureRandom(center, 1.2D, Vec3.ZERO, LIGHTNING_COLOR, 1, 0.0F, 0.0F);
            level.addFreshEntity(arc);
        }
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, SMOKE_COLOR, 30, 0, 0, owner.getId(), 0),
                owner.getX(),
                owner.getY() + 0.9D,
                owner.getZ(),
                100,
                0.4D,
                0.6D,
                0.4D,
                0.0D);
    }

    private void strikeNearby(LivingEntity owner) {
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(4.0D),
                target -> canStrike(owner, target))) {
            strike(owner, target);
        }
    }

    private void strikeLookTarget(LivingEntity owner) {
        HitResult result = ProcedureUtils.objectEntityLookingAt(owner, 10.0D, 0.0D, false, false,
                target -> target instanceof LivingEntity living && canStrike(owner, living));
        if (result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity target) {
            strike(owner, target);
        }
    }

    private boolean canStrike(LivingEntity owner, LivingEntity target) {
        return target.isAlive() && target != owner && target.getRootVehicle() != owner.getRootVehicle();
    }

    private void strike(LivingEntity owner, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc == null) {
            return;
        }
        Vec3 from = this.position().add(0.0D, 1.0D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        arc.configureBetween(from, to, LIGHTNING_COLOR, 1, 0.0F, 0.0F, 4);
        float damage = this.random.nextFloat() * 0.05F * getDamageXp();
        if (damage > 0.0F) {
            arc.setDamage(damage, owner);
        }
        level.addFreshEntity(arc);
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
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

    private int getDamageXp() {
        return this.entityData.get(DAMAGE_XP);
    }

    private void setDamageXp(int damageXp) {
        this.entityData.set(DAMAGE_XP, Math.max(damageXp, 0));
    }
}
