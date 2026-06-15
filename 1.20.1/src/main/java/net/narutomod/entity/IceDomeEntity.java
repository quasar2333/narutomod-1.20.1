package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class IceDomeEntity extends Entity {
    public static final float WIDTH = 9.6F;
    public static final float HEIGHT = 6.4F;
    public static final float HALF_WIDTH = WIDTH * 0.5F;
    private static final int TALK_TIME = 26;
    private static final int GROW_AND_TALK_TIME = 56;
    private static final int SHOOT_SPEARS_DURATION = 100;
    private static final int UPKEEP_INTERVAL = 20;
    private static final double UPKEEP_COST = 5.0D;
    private static final float MAX_HEALTH = 400.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IceDomeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(IceDomeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SHOOT_SPEARS_TIME = SynchedEntityData.defineId(IceDomeEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private final Set<UUID> insideUuids = new HashSet<>();
    private boolean brokenFeedbackPlayed;

    public IceDomeEntity(EntityType<? extends IceDomeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        configure(owner, owner.getX(), owner.getY() - 0.1D, owner.getZ());
    }

    public void configure(LivingEntity owner, double x, double y, double z) {
        setOwner(owner);
        setHealth(MAX_HEALTH);
        moveTo(x, y, z, owner.getYRot(), 0.0F);
    }

    public static boolean spawnOrTrigger(LivingEntity owner) {
        IceDomeEntity active = findActiveOwnedDome(owner);
        if (active != null && active.isPositionInside(owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D))) {
            active.shootSpears();
            return true;
        }
        IceDomeEntity dome = ModEntityTypes.ICE_DOME.get().create(owner.level());
        if (dome == null) {
            return false;
        }
        dome.configure(owner);
        owner.level().addFreshEntity(dome);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SOUND_MAKYOHYOSHO.get(),
                SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    @Nullable
    public static IceDomeEntity spawnAt(LivingEntity owner, double x, double y, double z) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        IceDomeEntity dome = ModEntityTypes.ICE_DOME.get().create(level);
        if (dome == null) {
            return null;
        }
        dome.configure(owner, x, y, z);
        level.addFreshEntity(dome);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SOUND_MAKYOHYOSHO.get(),
                SoundSource.NEUTRAL, 1.0F, 0.9F);
        return dome;
    }

    @Nullable
    public static IceDomeEntity findActiveOwnedDome(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntitiesOfClass(IceDomeEntity.class, owner.getBoundingBox().inflate(64.0D),
                        dome -> dome.isAlive() && dome.isOwnedBy(owner))
                .stream()
                .min((left, right) -> Double.compare(left.distanceToSqr(owner), right.distanceToSqr(owner)))
                .orElse(null);
    }

    public boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID()) || getOwner() == entity;
    }

    public float getHealthValue() {
        return this.entityData.get(HEALTH);
    }

    public int getGrowAndTalkTime() {
        return GROW_AND_TALK_TIME;
    }

    public void shootSpears() {
        this.entityData.set(SHOOT_SPEARS_TIME, SHOOT_SPEARS_DURATION);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(HEALTH, MAX_HEALTH);
        this.entityData.define(SHOOT_SPEARS_TIME, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount <= 5) {
            captureInitialInsideEntities();
        }
        if (this.tickCount == TALK_TIME) {
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_ICE_FORMATION.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (this.tickCount % UPKEEP_INTERVAL == 0 && !consumeUpkeep()) {
            breakDome();
            return;
        }
        if (getShootSpearsTime() > 0) {
            fireSpearsAtInsideTargets();
            this.entityData.set(SHOOT_SPEARS_TIME, getShootSpearsTime() - 1);
        }
        applyBarrierCollision();
        if (this.tickCount % 40 == 0) {
            cleanupInsideList();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isIgnoredDamage(source)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker && isInsideMember(attacker)) {
            return false;
        }
        if (this.level().isClientSide || amount <= 0.0F) {
            return false;
        }
        setHealth(getHealthValue() - amount * 0.2F);
        markHurt();
        if (getHealthValue() <= 0.0F) {
            breakDome();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return domeBox().inflate(1.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D;
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        this.entityData.set(SHOOT_SPEARS_TIME, tag.getInt("ShootSpearsTime"));
        this.insideUuids.clear();
        if (tag.contains("InsideEntities", 9)) {
            ListTag list = tag.getList("InsideEntities", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    this.insideUuids.add(entry.getUUID("UUID"));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Health", getHealthValue());
        tag.putInt("ShootSpearsTime", getShootSpearsTime());
        ListTag list = new ListTag();
        for (UUID uuid : this.insideUuids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            list.add(entry);
        }
        tag.put("InsideEntities", list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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

    private void setHealth(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, MAX_HEALTH));
    }

    private int getShootSpearsTime() {
        return this.entityData.get(SHOOT_SPEARS_TIME);
    }

    private boolean consumeUpkeep() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        if (owner instanceof Player player && player.isCreative()) {
            return true;
        }
        if (owner instanceof HakuEntity haku) {
            return haku.consumeLegacyChakra(UPKEEP_COST);
        }
        return Chakra.pathway(owner).consume(UPKEEP_COST);
    }

    private void captureInitialInsideEntities() {
        AABB domeBox = domeBox();
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, domeBox.inflate(0.25D),
                entity -> entity.isAlive() && !isOwnedBy(entity) && fullyInside(entity.getBoundingBox(), domeBox))) {
            this.insideUuids.add(entity.getUUID());
        }
    }

    private void fireSpearsAtInsideTargets() {
        LivingEntity owner = getOwner();
        if (!(this.level() instanceof ServerLevel level) || owner == null || !owner.isAlive()) {
            return;
        }
        cleanupInsideList();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, domeBox().inflate(1.0D), this::shouldShootAt)) {
            Vec3 offset = randomHorizontalOffset(HALF_WIDTH * 0.8D);
            Vec3 from = new Vec3(getX() + offset.x(), getY() + HEIGHT - 1.6D, getZ() + offset.z());
            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            IceSpearEntity spear = ModEntityTypes.ICE_SPEAR.get().create(level);
            if (spear == null) {
                continue;
            }
            spear.configure(owner, from, to, 0.95F, 0.25F);
            level.addFreshEntity(spear);
            level.playSound(null, from.x(), from.y(), from.z(), ModSounds.SOUND_ICE_SHOOT_SMALL.get(),
                    SoundSource.NEUTRAL, 0.7F, level.random.nextFloat() * 0.4F + 0.8F);
        }
    }

    private boolean shouldShootAt(LivingEntity target) {
        LivingEntity owner = getOwner();
        return target.isAlive()
                && isInsideMember(target)
                && !isOwnedBy(target)
                && (owner == null || !owner.isAlliedTo(target));
    }

    private Vec3 randomHorizontalOffset(double radius) {
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        double distance = Math.sqrt(this.random.nextDouble()) * radius;
        return new Vec3(Mth.cos((float) angle) * distance, 0.0D, Mth.sin((float) angle) * distance);
    }

    private void applyBarrierCollision() {
        AABB domeBox = domeBox();
        AABB search = domeBox.inflate(1.0D);
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, search, LivingEntity::isAlive)) {
            if (isOwnedBy(entity) || isInsideMember(entity)) {
                keepInside(entity, domeBox);
            } else if (entity.getBoundingBox().intersects(domeBox)) {
                pushOutside(entity, domeBox);
            }
        }
    }

    private void keepInside(LivingEntity entity, AABB domeBox) {
        if (fullyInside(entity.getBoundingBox(), domeBox)) {
            return;
        }
        double halfWidth = entity.getBbWidth() * 0.5D;
        double x = Mth.clamp(entity.getX(), domeBox.minX + halfWidth, domeBox.maxX - halfWidth);
        double y = Mth.clamp(entity.getY(), domeBox.minY, domeBox.maxY - entity.getBbHeight());
        double z = Mth.clamp(entity.getZ(), domeBox.minZ + halfWidth, domeBox.maxZ - halfWidth);
        entity.teleportTo(x, y, z);
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(-0.2D, 0.0D, -0.2D));
        entity.hurtMarked = true;
    }

    private void pushOutside(LivingEntity entity, AABB domeBox) {
        double halfWidth = entity.getBbWidth() * 0.5D;
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        double x = entity.getX();
        double z = entity.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            x = dx >= 0.0D ? domeBox.maxX + halfWidth + 0.02D : domeBox.minX - halfWidth - 0.02D;
        } else {
            z = dz >= 0.0D ? domeBox.maxZ + halfWidth + 0.02D : domeBox.minZ - halfWidth - 0.02D;
        }
        entity.teleportTo(x, entity.getY(), z);
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(-0.4D, 0.0D, -0.4D));
        entity.hurtMarked = true;
    }

    private void cleanupInsideList() {
        this.insideUuids.removeIf(uuid -> !(this.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(uuid) instanceof LivingEntity entity)
                || !entity.isAlive());
    }

    private boolean isInsideMember(LivingEntity entity) {
        return isOwnedBy(entity) || this.insideUuids.contains(entity.getUUID());
    }

    private boolean isPositionInside(Vec3 position) {
        return domeBox().contains(position);
    }

    private AABB domeBox() {
        return new AABB(
                getX() - HALF_WIDTH,
                getY(),
                getZ() - HALF_WIDTH,
                getX() + HALF_WIDTH,
                getY() + HEIGHT,
                getZ() + HALF_WIDTH);
    }

    private void breakDome() {
        if (!this.level().isClientSide && !this.brokenFeedbackPlayed) {
            this.brokenFeedbackPlayed = true;
            playBreakFeedback();
        }
        discard();
    }

    private void playBreakFeedback() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, getX(), getY() + HEIGHT * 0.5D, getZ(), ModSounds.SOUND_ICE_SHOOT_SMALL.get(),
                SoundSource.NEUTRAL, 1.0F, 0.8F);
        AABB box = domeBox();
        int count = 100 + this.random.nextInt(50);
        for (int i = 0; i < count; i++) {
            IceSpearEntity.spawnShatteredShard(
                    level,
                    Mth.lerp(this.random.nextDouble(), box.minX, box.maxX),
                    Mth.lerp(this.random.nextDouble(), box.minY, box.maxY),
                    Mth.lerp(this.random.nextDouble(), box.minZ, box.maxZ),
                    (this.random.nextDouble() - 0.5D) * 0.05D,
                    0.0D,
                    (this.random.nextDouble() - 0.5D) * 0.05D);
        }
    }

    private boolean isIgnoredDamage(DamageSource source) {
        return source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER)
                || source.getDirectEntity() instanceof IceSpearEntity;
    }

    private static boolean fullyInside(AABB entityBox, AABB domeBox) {
        return entityBox.minX >= domeBox.minX
                && entityBox.maxX <= domeBox.maxX
                && entityBox.minY >= domeBox.minY
                && entityBox.maxY <= domeBox.maxY
                && entityBox.minZ >= domeBox.minZ
                && entityBox.maxZ <= domeBox.maxZ;
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                return;
            }
            Entity attacker = event.getSource().getEntity();
            for (IceDomeEntity dome : target.level().getEntitiesOfClass(IceDomeEntity.class, target.getBoundingBox().inflate(HALF_WIDTH + 3.0D))) {
                if (!dome.isAlive()) {
                    continue;
                }
                boolean targetInside = dome.isInsideMember(target);
                boolean attackerInside = attacker instanceof LivingEntity living && dome.isInsideMember(living);
                boolean blocksOwner = dome.isOwnedBy(target);
                boolean blocksCrossBoundary = attacker instanceof LivingEntity && targetInside != attackerInside && !dome.isOwnedBy(attacker);
                if (blocksOwner || blocksCrossBoundary) {
                    event.setCanceled(true);
                    dome.hurt(event.getSource(), event.getAmount());
                    return;
                }
            }
        }
    }
}
