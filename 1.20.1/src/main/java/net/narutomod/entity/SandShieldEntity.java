package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerTracker;
import net.narutomod.item.JutsuItem;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SandShieldEntity extends Entity {
    public static final String ENTITY_ID_KEY = "JitonSandShieldEntityIdKey";
    public static final int DEFAULT_COLOR = SandBulletEntity.DEFAULT_COLOR;
    public static final double CHAKRA_COST_PER_TICK = 0.5D;
    private static final int RETURN_TIME = 40;
    private static final int SHIELD_COOLDOWN_TICKS = 2400;
    private static final double INTERCEPT_RANGE = 4.0D;
    private static final double COLLISION_SPEED_THRESHOLD = 0.22D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SandShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SandShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(SandShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_HEALTH = SynchedEntityData.defineId(SandShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(SandShieldEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private int returnTicks;
    private boolean cooldownApplied;
    private final List<SandTrail> trails = new ArrayList<>();

    public SandShieldEntity(EntityType<? extends SandShieldEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static boolean spawnFrom(Player owner, int color) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SandShieldEntity active = findActive(serverLevel, owner);
        if (active != null) {
            if (!active.isReturning() && owner.getVehicle() != active) {
                owner.startRiding(active, true);
            }
            return false;
        }
        SandShieldEntity entity = ModEntityTypes.ENTITYJITONSHIELD.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, color);
        boolean added = serverLevel.addFreshEntity(entity);
        if (added) {
            owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
            owner.startRiding(entity, true);
            serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.9F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        }
        return added;
    }

    @Nullable
    public static SandShieldEntity findActive(ServerLevel level, Player owner) {
        int id = owner.getPersistentData().getInt(ENTITY_ID_KEY);
        if (id > 0 && level.getEntity(id) instanceof SandShieldEntity entity && entity.isOwnedBy(owner) && entity.isAlive()) {
            return entity;
        }
        for (SandShieldEntity entity : level.getEntitiesOfClass(SandShieldEntity.class, owner.getBoundingBox().inflate(96.0D))) {
            if (entity.isOwnedBy(owner) && entity.isAlive()) {
                owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
                return entity;
            }
        }
        owner.getPersistentData().remove(ENTITY_ID_KEY);
        return null;
    }

    public static boolean interceptAttack(LivingEntity target, DamageSource source, float amount) {
        if (!(target instanceof Player player) || !(target.level() instanceof ServerLevel serverLevel)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        SandShieldEntity shield = findActive(serverLevel, player);
        if (shield == null || shield.isReturning() || !shield.canBlock(source, target)) {
            return false;
        }
        if (target.distanceToSqr(shield) > INTERCEPT_RANGE * INTERCEPT_RANGE
                && !shield.getBoundingBox().inflate(0.5D).intersects(target.getBoundingBox())) {
            return false;
        }
        shield.absorbAttack(source, amount);
        return true;
    }

    public void configure(Player owner, int color) {
        setOwner(owner);
        setColor(color);
        float maxHealth = (float)Math.max(PlayerTracker.getNinjaLevel(owner) * 5.0D, 1.0D);
        setMaxHealthValue(maxHealth);
        setHealthValue(maxHealth);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isOwnedBy(Entity owner) {
        if (owner.getUUID().equals(this.ownerUuid)) {
            return true;
        }
        int ownerId = this.entityData.get(OWNER_ID);
        return ownerId >= 0 && ownerId == owner.getId();
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(COLOR, DEFAULT_COLOR);
        this.entityData.define(HEALTH, 1.0F);
        this.entityData.define(MAX_HEALTH, 1.0F);
        this.entityData.define(RETURNING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        tickTrails(owner);
        if (isReturning()) {
            tickReturning(owner);
            return;
        }
        if (this.tickCount > 5 && owner.getVehicle() != this) {
            startReturning(owner);
            return;
        }
        if (!owner.isCreative() && !Chakra.pathway(owner).consume(CHAKRA_COST_PER_TICK)) {
            startReturning(owner);
            return;
        }
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        yRotO = getYRot();
        xRotO = getXRot();
        spawnShieldParticles(owner);
        pushFastColliders(owner);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isIgnoredDamage(source) || isReturning() || amount <= 0.0F) {
            return false;
        }
        if (this.level().isClientSide) {
            return false;
        }
        absorbAttack(source, amount);
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player) && !isReturning()) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return !isReturning();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isReturning();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && isOwnedBy(passenger) && !isReturning();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.35D;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setColor(tag.contains("Color") ? tag.getInt("Color") : DEFAULT_COLOR);
        setMaxHealthValue(tag.contains("MaxHealth") ? tag.getFloat("MaxHealth") : 1.0F);
        setHealthValue(tag.contains("Health") ? tag.getFloat("Health") : getMaxHealthValue());
        setReturning(tag.getBoolean("Returning"));
        this.returnTicks = tag.getInt("ReturnTicks");
        this.cooldownApplied = tag.getBoolean("CooldownApplied");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Color", getColor());
        tag.putFloat("Health", getHealthValue());
        tag.putFloat("MaxHealth", getMaxHealthValue());
        tag.putBoolean("Returning", isReturning());
        tag.putInt("ReturnTicks", this.returnTicks);
        tag.putBoolean("CooldownApplied", this.cooldownApplied);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        Player owner = getOwner();
        if (owner != null && owner.getPersistentData().getInt(ENTITY_ID_KEY) == getId()) {
            owner.getPersistentData().remove(ENTITY_ID_KEY);
        }
        super.remove(reason);
    }

    private void absorbAttack(DamageSource source, float amount) {
        Player owner = getOwner();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        Entity feedbackEntity = direct != null ? direct : attacker;
        if (feedbackEntity != null && owner != null && feedbackEntity != owner) {
            ProcedureUtils.pushEntity(owner, feedbackEntity, 5.0D, 1.5F);
            if (!(feedbackEntity instanceof LivingEntity)) {
                feedbackEntity.discard();
            }
        }
        Vec3 hit = feedbackEntity != null
                ? feedbackEntity.position().add(0.0D, feedbackEntity.getBbHeight() * 0.5D, 0.0D)
                : position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        addTrail(hit);
        spawnImpactParticles(hit, feedbackEntity == null ? 40 : 80);
        this.level().playSound(null, hit.x(), hit.y(), hit.z(),
                ModSounds.SOUND_BULLET_IMPACT.get(), SoundSource.NEUTRAL, 0.9F, 0.6F + this.random.nextFloat() * 0.4F);
        setHealthValue(getHealthValue() - amount);
        markHurt();
        if (getHealthValue() <= 0.0F) {
            startReturning(owner);
        }
    }

    private boolean canBlock(DamageSource source, LivingEntity target) {
        return !isIgnoredDamage(source)
                && source.getDirectEntity() != this
                && source.getEntity() != target;
    }

    private boolean isIgnoredDamage(DamageSource source) {
        return source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER);
    }

    private void pushFastColliders(Player owner) {
        AABB box = getBoundingBox().inflate(0.1D);
        for (Entity entity : this.level().getEntities(this, box, entity -> entity.isAlive()
                && entity != owner
                && entity.getVehicle() != this
                && entity.getRootVehicle() != getRootVehicle())) {
            if (ProcedureUtils.getVelocity(entity) <= COLLISION_SPEED_THRESHOLD) {
                continue;
            }
            ProcedureUtils.pushEntity(owner, entity, 5.0D, 1.5F);
            Vec3 hit = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            addTrail(hit);
            spawnImpactParticles(hit, 40);
        }
    }

    private void tickReturning(Player owner) {
        ejectPassengers();
        this.returnTicks++;
        Vec3 mouth = gourdMouthPosition(owner);
        Vec3 delta = mouth.subtract(position());
        if (delta.lengthSqr() > 0.04D) {
            Vec3 step = delta.normalize().scale(Math.min(0.45D, delta.length()));
            setDeltaMovement(step);
            move(MoverType.SELF, step);
        }
        spawnPathParticles(position().add(0.0D, getBbHeight() * 0.5D, 0.0D), mouth, 6);
        if (this.returnTicks >= RETURN_TIME || position().distanceToSqr(mouth) < 0.36D) {
            discard();
        }
    }

    private void startReturning(@Nullable Player owner) {
        if (!isReturning()) {
            setReturning(true);
            this.returnTicks = 0;
            setDeltaMovement(Vec3.ZERO);
            ejectPassengers();
            if (owner != null) {
                applyCooldown(owner);
            }
        }
    }

    private void applyCooldown(Player owner) {
        if (this.cooldownApplied || owner.isCreative()) {
            return;
        }
        ItemStack stack = ProcedureUtils.getMatchingItemStack(owner, ModItems.JITON.get());
        if (stack != null && !stack.isEmpty()) {
            stack.getOrCreateTag().putLong(JutsuItem.COOLDOWN_TAG_PREFIX + 0, this.level().getGameTime() + SHIELD_COOLDOWN_TICKS);
            this.cooldownApplied = true;
        }
    }

    private void spawnShieldParticles(Player owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 20, 3),
                getX(),
                getY() + 1.45D,
                getZ(),
                140,
                1.3D,
                1.3D,
                1.3D,
                0.0D);
        if (this.tickCount <= 20) {
            spawnPathParticles(gourdMouthPosition(owner), position().add(0.0D, 1.45D, 0.0D), 5);
        }
    }

    private void spawnImpactParticles(Vec3 pos, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 12, 3),
                pos.x(),
                pos.y(),
                pos.z(),
                count,
                0.18D,
                0.18D,
                0.18D,
                0.03D);
    }

    private void spawnPathParticles(Vec3 from, Vec3 to, int particlesPerStep) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 delta = to.subtract(from);
        int steps = Mth.clamp((int)(delta.length() * 3.0D), 3, 18);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double)steps));
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 12, 3),
                    point.x(),
                    point.y(),
                    point.z(),
                    particlesPerStep,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.0D);
        }
    }

    private void addTrail(Vec3 start) {
        this.trails.add(new SandTrail(start));
    }

    private void tickTrails(Player owner) {
        Iterator<SandTrail> iterator = this.trails.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(owner)) {
                iterator.remove();
            }
        }
    }

    private Vec3 gourdMouthPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z(), 0.0D, look.x());
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 safeLook = look.lengthSqr() <= 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        return owner.position()
                .add(right.scale(-0.35D))
                .add(0.0D, owner.getBbHeight() * 0.75D, 0.0D)
                .subtract(safeLook.scale(0.25D));
    }

    private void setOwner(Player owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private Player getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof Player player) {
            return player;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof Player player) {
            setOwner(player);
            return player;
        }
        return null;
    }

    private int getColor() {
        return this.entityData.get(COLOR);
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private float getHealthValue() {
        return this.entityData.get(HEALTH);
    }

    private void setHealthValue(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, getMaxHealthValue()));
    }

    private float getMaxHealthValue() {
        return this.entityData.get(MAX_HEALTH);
    }

    private void setMaxHealthValue(float maxHealth) {
        this.entityData.set(MAX_HEALTH, Math.max(maxHealth, 1.0F));
    }

    private void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
    }

    private final class SandTrail {
        private static final int MAX_AGE = 20;
        private final Vec3 start;
        private int age;

        private SandTrail(Vec3 start) {
            this.start = start;
        }

        private boolean tick(LivingEntity owner) {
            this.age++;
            double progress = Mth.clamp(this.age / (double)MAX_AGE, 0.0D, 1.0D);
            Vec3 mouth = gourdMouthPosition(owner);
            Vec3 point = this.start.lerp(mouth, progress);
            spawnPathParticles(point, mouth, 2);
            return this.age >= MAX_AGE;
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide) {
                return;
            }
            if (interceptAttack(target, event.getSource(), event.getAmount())) {
                event.setCanceled(true);
            }
        }
    }
}
