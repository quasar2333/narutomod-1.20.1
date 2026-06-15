package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodMod;
import net.narutomod.registry.ModItems;

public final class NuibariSwordEntity extends ThrowableItemProjectile {
    private static final String SKEWERED_TIME_TAG = "NuibariSkeweredTime";
    private static final String DISABLE_KNOCKBACK_TAG = "TempData_disableKnockback";
    private static final int OWNER_PICKUP_TICKS = 15;
    private static final int MAX_SKEWERED_TIME = 300;
    private static final double SKEWER_CHAIN_DISTANCE = 2.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(NuibariSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GROUNDED =
            SynchedEntityData.defineId(NuibariSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SKEWERED_ENTITY_IDS =
            SynchedEntityData.defineId(NuibariSwordEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private UUID ownerUuid;
    private final List<UUID> skeweredEntities = new ArrayList<>();
    private double damage = 16.0D;
    private float groundedHealth = 15.0F;

    public NuibariSwordEntity(EntityType<? extends NuibariSwordEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(LivingEntity owner, double damage) {
        setLivingOwner(owner);
        this.damage = damage;
        setItem(new ItemStack(ModItems.NUIBARI_SWORD.get()));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(GROUNDED, false);
        this.entityData.define(SKEWERED_ENTITY_IDS, "");
    }

    @Override
    public void tick() {
        if (isGrounded()) {
            super.tick();
            tickGrounded();
            return;
        }
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (owner != null && !owner.isAlive()) {
            clearLivingOwner();
        } else if (owner != null && distanceTo(owner) > 50.0D) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x() * -0.4D, motion.y(), motion.z() * -0.4D);
        }
        updateSkeweredEntities();
        tryPickup();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (target == getLivingOwner()) {
            return;
        }
        target.getPersistentData().putBoolean(DISABLE_KNOCKBACK_TAG, true);
        float amount = (float)(getDeltaMovement().length() * this.damage);
        DamageSource source = this.damageSources().thrown(this, getLivingOwner());
        boolean hurt = target.hurt(source, amount);
        if (target instanceof LivingEntity living && (hurt || isLastSkewered(living))) {
            if (living.isAlive() && averageSize(living) < 2.0D && !isLastSkewered(living)) {
                addSkewered(living);
            }
            playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            setDeltaMovement(getDeltaMovement().scale(0.85D));
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.1D));
            setYRot(getYRot() + 180.0F);
            yRotO += 180.0F;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        setGrounded(true);
        setNoGravity(true);
        this.noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        Vec3 location = result.getLocation();
        moveTo(location.x(), location.y(), location.z(), getYRot(), getXRot());
        this.groundedHealth = 15.0F;
        playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity living)) {
            return false;
        }
        if (!isGrounded()) {
            Vec3 look = living.getLookAngle();
            setDeltaMovement(look.x(), look.y(), look.z());
            hurtMarked = true;
            return true;
        }
        this.groundedHealth -= amount;
        if (this.groundedHealth <= 0.0F) {
            clearSkeweredEntities();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != getLivingOwner();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.NUIBARI_SWORD.get();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            clearSkeweredEntities();
        }
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.damage = tag.contains("Damage") ? tag.getDouble("Damage") : 16.0D;
        this.groundedHealth = tag.contains("GroundedHealth") ? tag.getFloat("GroundedHealth") : 15.0F;
        setGrounded(tag.getBoolean("Grounded"));
        this.skeweredEntities.clear();
        ListTag skewered = tag.getList("SkeweredEntities", Tag.TAG_COMPOUND);
        for (int index = 0; index < skewered.size(); index++) {
            CompoundTag entry = skewered.getCompound(index);
            if (entry.hasUUID("UUID")) {
                this.skeweredEntities.add(entry.getUUID("UUID"));
            }
        }
        if (isGrounded()) {
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            this.noPhysics = true;
        }
        syncSkeweredEntityIds();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putDouble("Damage", this.damage);
        tag.putFloat("GroundedHealth", this.groundedHealth);
        tag.putBoolean("Grounded", isGrounded());
        ListTag skewered = new ListTag();
        for (UUID uuid : this.skeweredEntities) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            skewered.add(entry);
        }
        tag.put("SkeweredEntities", skewered);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void retrieveToward(Player player) {
        double dx = player.getX() - getX();
        double dy = player.getBoundingBox().minY + player.getBbHeight() / 3.0D - getY();
        double dz = player.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        setGrounded(false);
        setNoGravity(false);
        this.noPhysics = false;
        shoot(dx, dy + horizontal * 0.3D, dz, (float)Math.sqrt(horizontal) * 0.3F, 0.0F);
    }

    public boolean isOwner(Player player) {
        return getLivingOwner() == player;
    }

    @Nullable
    public LivingEntity getOwnerForRender() {
        return getLivingOwner();
    }

    public List<LivingEntity> getSkeweredEntitiesForRender() {
        String ids = this.entityData.get(SKEWERED_ENTITY_IDS);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<LivingEntity> entities = new ArrayList<>();
        for (String token : ids.split(",")) {
            try {
                Entity entity = this.level().getEntity(Integer.parseInt(token));
                if (entity instanceof LivingEntity living) {
                    entities.add(living);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed synced ids instead of breaking rendering.
            }
        }
        return entities;
    }

    public void clearLivingOwner() {
        this.ownerUuid = null;
        this.entityData.set(OWNER_ID, -1);
        super.setOwner(null);
    }

    private void setLivingOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        super.setOwner(owner);
    }

    @Nullable
    private LivingEntity getLivingOwner() {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living) {
            return living;
        }
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof LivingEntity living) {
            super.setOwner(living);
            return living;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            setLivingOwner(living);
            return living;
        }
        return null;
    }

    private boolean isGrounded() {
        return this.entityData.get(GROUNDED);
    }

    private void setGrounded(boolean grounded) {
        this.entityData.set(GROUNDED, grounded);
    }

    private void tickGrounded() {
        setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            updateSkeweredEntities();
            tryPickup();
        }
    }

    private void tryPickup() {
        LivingEntity owner = getLivingOwner();
        if (owner instanceof Player player && this.tickCount > OWNER_PICKUP_TICKS && distanceToSqr(player) < 4.0D) {
            returnToOwner(player);
            return;
        }
        if (owner == null && isGrounded()) {
            for (Player player : this.level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.0D))) {
                giveSwordTo(player);
                return;
            }
        }
    }

    private void returnToOwner(Player player) {
        ItemStack sword = new ItemStack(ModItems.NUIBARI_SWORD.get());
        if (!net.narutomod.item.NuibariThrownItem.replaceBoundStack(player, getId(), sword)
                && !player.getInventory().add(sword)) {
            dropSword(sword);
        }
        discard();
    }

    private void giveSwordTo(Player player) {
        ItemStack sword = new ItemStack(ModItems.NUIBARI_SWORD.get());
        if (!player.getInventory().add(sword)) {
            dropSword(sword);
        }
        discard();
    }

    private void dropSword(ItemStack sword) {
        ItemEntity drop = new ItemEntity(this.level(), getX(), getY(), getZ(), sword);
        drop.setPickUpDelay(0);
        this.level().addFreshEntity(drop);
    }

    private void addSkewered(LivingEntity entity) {
        if (!this.skeweredEntities.contains(entity.getUUID())) {
            this.skeweredEntities.add(entity.getUUID());
            entity.getPersistentData().putInt(SKEWERED_TIME_TAG, 0);
            syncSkeweredEntityIds();
        }
    }

    private boolean isLastSkewered(LivingEntity entity) {
        return !this.skeweredEntities.isEmpty()
                && this.skeweredEntities.get(this.skeweredEntities.size() - 1).equals(entity.getUUID());
    }

    private void updateSkeweredEntities() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity previous = this;
        Iterator<UUID> iterator = this.skeweredEntities.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity found = serverLevel.getEntity(uuid);
            if (!(found instanceof LivingEntity living) || !isTargetable(living)) {
                clearSkewered(livingOrNull(found));
                iterator.remove();
                continue;
            }
            int skeweredTime = living.getPersistentData().getInt(SKEWERED_TIME_TAG);
            if (skeweredTime >= MAX_SKEWERED_TIME) {
                clearSkewered(living);
                iterator.remove();
                continue;
            }
            if (!(living instanceof Player)) {
                living.getPersistentData().putInt(SKEWERED_TIME_TAG, skeweredTime + 1);
            }
            double distance = previous.distanceTo(living);
            if (distance > SKEWER_CHAIN_DISTANCE) {
                Vec3 pull = previous.position().subtract(living.position()).normalize()
                        .scale(0.2D * distance / SKEWER_CHAIN_DISTANCE);
                living.setDeltaMovement(living.getDeltaMovement().add(pull));
                living.hurtMarked = true;
            }
            previous = living;
        }
        syncSkeweredEntityIds();
    }

    private static LivingEntity livingOrNull(@Nullable Entity entity) {
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean isTargetable(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator();
    }

    private void clearSkeweredEntities() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (UUID uuid : this.skeweredEntities) {
                clearSkewered(livingOrNull(serverLevel.getEntity(uuid)));
            }
        }
        this.skeweredEntities.clear();
        syncSkeweredEntityIds();
    }

    private void syncSkeweredEntityIds() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.skeweredEntities.isEmpty() || !(this.level() instanceof ServerLevel serverLevel)) {
            this.entityData.set(SKEWERED_ENTITY_IDS, "");
            return;
        }
        StringBuilder ids = new StringBuilder();
        for (UUID uuid : this.skeweredEntities) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            if (!ids.isEmpty()) {
                ids.append(',');
            }
            ids.append(entity.getId());
        }
        this.entityData.set(SKEWERED_ENTITY_IDS, ids.toString());
    }

    private static void clearSkewered(@Nullable LivingEntity entity) {
        if (entity != null) {
            entity.getPersistentData().remove(SKEWERED_TIME_TAG);
        }
    }

    private static double averageSize(Entity entity) {
        return (entity.getBbWidth() + entity.getBbWidth() + entity.getBbHeight()) / 3.0D;
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onKnockback(LivingKnockBackEvent event) {
            if (event.getEntity().getPersistentData().getBoolean(DISABLE_KNOCKBACK_TAG)) {
                event.setCanceled(true);
                event.getEntity().getPersistentData().remove(DISABLE_KNOCKBACK_TAG);
            }
        }
    }
}
