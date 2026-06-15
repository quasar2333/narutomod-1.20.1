package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class JintonCubeEntity extends Entity {
    private static final int WAIT_TICKS = 60;
    private static final int GROW_TICKS = 30;
    private static final int IDLE_TICKS = 40;
    private static final int SHRINK_TICKS = 10;
    private static final int DUST_COLOR = 0xC0A0A0A0;
    private static final double TARGET_RANGE = 50.0D;
    private static final double BASE_SIDE = 0.5D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(JintonCubeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CUBE_SCALE = SynchedEntityData.defineId(JintonCubeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FULL_SCALE = SynchedEntityData.defineId(JintonCubeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(JintonCubeEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;

    public JintonCubeEntity(EntityType<? extends JintonCubeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float chargedPower) {
        setOwner(owner);
        setFullScale(chargedPower * 2.0F + 2.0F);
        setCubeScale(0.5F);
        updateWaitPosition(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float chargedPower) {
        JintonCubeEntity cube = ModEntityTypes.JINTONCUBE.get().create(owner.level());
        if (cube == null) {
            return false;
        }
        cube.configure(owner, chargedPower);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SOUND_GENKAIHAKURINOJUTSU.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.level().addFreshEntity(cube);
        return true;
    }

    public float getCubeScale() {
        return this.entityData.get(CUBE_SCALE);
    }

    public int getWaitTicks() {
        return WAIT_TICKS;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(CUBE_SCALE, 0.5F);
        this.entityData.define(FULL_SCALE, 1.0F);
        this.entityData.define(ANCHORED, false);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (this.tickCount < WAIT_TICKS) {
            updateWaitPosition(owner);
            return;
        }
        if (this.tickCount == WAIT_TICKS) {
            anchorAtLookTarget(owner);
            return;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            tickServerEffects(serverLevel, owner);
        }
        if (this.tickCount > idleStartTick() + SHRINK_TICKS) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return cubeBox().inflate(1.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D + getSideLength();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.tickCount = tag.getInt("Life");
        setCubeScale(tag.contains("Scale") ? tag.getFloat("Scale") : 0.5F);
        setFullScale(tag.contains("FullScale") ? tag.getFloat("FullScale") : 1.0F);
        this.entityData.set(ANCHORED, tag.getBoolean("Anchored"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Life", this.tickCount);
        tag.putFloat("Scale", getCubeScale());
        tag.putFloat("FullScale", getFullScale());
        tag.putBoolean("Anchored", this.entityData.get(ANCHORED));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void tickServerEffects(ServerLevel level, LivingEntity owner) {
        holdEntities(level);
        int growEnd = WAIT_TICKS + GROW_TICKS;
        if (this.tickCount < growEnd) {
            float progress = (this.tickCount - WAIT_TICKS) / (float) GROW_TICKS;
            setCubeScale(1.0F + (getFullScale() - 1.0F) * progress);
            return;
        }
        spawnDust(level);
        int idleStart = idleStartTick();
        if (this.tickCount > idleStart) {
            destroyBlocks(level, owner);
            damageEntities(level, owner);
            float shrink = 1.0F - (this.tickCount - idleStart) / (float) SHRINK_TICKS;
            setCubeScale(getFullScale() * Mth.clamp(shrink, 0.0F, 1.0F));
        } else {
            setCubeScale(getFullScale());
        }
    }

    private void updateWaitPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 offset = look.normalize().scale(0.5D);
        moveTo(owner.getX() + offset.x(), owner.getY() + owner.getEyeHeight() - 0.6D + offset.y(), owner.getZ() + offset.z(),
                owner.getYRot(), owner.getXRot());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        setCubeScale(0.5F);
    }

    private void anchorAtLookTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, TARGET_RANGE, true);
        Vec3 target = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            Entity targetEntity = entityHit.getEntity();
            target = new Vec3(targetEntity.getX(), targetEntity.getY() + targetEntity.getBbHeight() * 0.5D, targetEntity.getZ());
        }
        moveTo(target.x(), target.y(), target.z(), owner.getYRot(), owner.getXRot());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.entityData.set(ANCHORED, true);
        applyAnchorSlowness();
    }

    private void applyAnchorSlowness() {
        double radius = Math.max(getFullScale() * 0.25D, 0.25D);
        AABB area = new AABB(position(), position()).inflate(radius);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 5, false, false));
        }
    }

    private void holdEntities(ServerLevel level) {
        AABB area = cubeBox();
        for (Entity target : level.getEntities(this, area, entity -> entity.isAlive() && !(entity instanceof JintonCubeEntity))) {
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
        }
    }

    private void spawnDust(ServerLevel level) {
        double side = getSideLength();
        int count = Math.max((int)(getFullScale() * 6.0F), 1);
        level.sendParticles(ModParticleTypes.options(NarutoParticleKind.FALLING_DUST, DUST_COLOR),
                getX(), getY(), getZ(), count, side * 0.2D, side * 0.2D, side * 0.2D, 0.1D);
    }

    private void destroyBlocks(ServerLevel level, LivingEntity owner) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            return;
        }
        AABB area = cubeBox();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(area.minX);
        int maxX = Mth.ceil(area.maxX);
        int minY = Mth.floor(area.minY);
        int maxY = Mth.ceil(area.maxY);
        int minZ = Mth.floor(area.minZ);
        int maxZ = Mth.ceil(area.maxZ);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void damageEntities(ServerLevel level, LivingEntity owner) {
        AABB area = cubeBox();
        DamageSource source = ModDamageTypes.jinton(level, this, owner);
        for (Entity target : level.getEntities(this, area, entity -> entity.isAlive() && !(entity instanceof JintonCubeEntity))) {
            AABB targetBox = target.getBoundingBox();
            double targetVolume = volume(targetBox);
            if (targetVolume <= 0.0D) {
                continue;
            }
            double overlap = volume(area.intersect(targetBox));
            double damageRatio = overlap / targetVolume * 0.5D;
            if (damageRatio <= 0.0D) {
                continue;
            }
            target.invulnerableTime = 0;
            if (target instanceof LivingEntity living) {
                living.hurt(source, Math.max(living.getMaxHealth() * (float) damageRatio, 1.0F));
            } else {
                target.discard();
            }
        }
    }

    private AABB cubeBox() {
        double half = getSideLength() * 0.5D;
        return new AABB(getX() - half, getY() - half, getZ() - half, getX() + half, getY() + half, getZ() + half);
    }

    private double getSideLength() {
        return Math.max(getCubeScale() * BASE_SIDE, 0.05D);
    }

    private float getFullScale() {
        return this.entityData.get(FULL_SCALE);
    }

    private void setFullScale(float scale) {
        this.entityData.set(FULL_SCALE, Mth.clamp(scale, 0.5F, 102.0F));
    }

    private void setCubeScale(float scale) {
        this.entityData.set(CUBE_SCALE, Mth.clamp(scale, 0.05F, 102.0F));
    }

    private int idleStartTick() {
        return WAIT_TICKS + GROW_TICKS + IDLE_TICKS;
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

    private static double volume(AABB box) {
        return Math.max(box.getXsize(), 0.0D) * Math.max(box.getYsize(), 0.0D) * Math.max(box.getZsize(), 0.0D);
    }
}
