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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.item.RaitonItem;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class RaitonChakraModeEntity extends Entity {
    public static final String ENTITY_ID_KEY = "EntityChakraModeIdKey";
    private static final double CHAKRA_BURN_PER_SECOND = RaitonItem.CHAKRA_MODE.chakraUsage();
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(RaitonChakraModeEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private ItemStack sourceStack = ItemStack.EMPTY;
    private int strengthAmplifier = 9;
    private boolean cooldownWritten;

    public RaitonChakraModeEntity(EntityType<? extends RaitonChakraModeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, ItemStack stack) {
        setOwner(owner);
        this.sourceStack = stack.copyWithCount(1);
        if (owner.hasEffect(MobEffects.DAMAGE_BOOST)) {
            this.strengthAmplifier += owner.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
        }
        moveToOwner(owner);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public void stopChakraMode() {
        discardMode();
    }

    @Nullable
    public LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0) {
            Entity entity = this.level().getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity living) {
                setOwner(living);
                return living;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide) {
            if (owner == null || !owner.isAlive()) {
                discardMode();
                return;
            }
            moveToOwner(owner);
            if (this.tickCount % 20 == 2 && !applyBuffsAndBurnChakra(owner)) {
                discardMode();
                return;
            }
            playElectricity(owner);
            spawnFeedback(owner);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        writeCooldown();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("SourceStack")) {
            this.sourceStack = ItemStack.of(tag.getCompound("SourceStack"));
        }
        this.strengthAmplifier = tag.contains("StrengthAmplifier") ? tag.getInt("StrengthAmplifier") : 9;
        this.cooldownWritten = tag.getBoolean("CooldownWritten");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (!this.sourceStack.isEmpty()) {
            tag.put("SourceStack", this.sourceStack.save(new CompoundTag()));
        }
        tag.putInt("StrengthAmplifier", this.strengthAmplifier);
        tag.putBoolean("CooldownWritten", this.cooldownWritten);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private boolean applyBuffsAndBurnChakra(LivingEntity owner) {
        if (!Chakra.pathway(owner).consume(CHAKRA_BURN_PER_SECOND)) {
            return false;
        }
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 21, 3, false, false));
        owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 21, 32, false, false));
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 21, this.strengthAmplifier, false, false));
        owner.addEffect(new MobEffectInstance(MobEffects.JUMP, 21, 6, false, false));
        return true;
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void playElectricity(LivingEntity owner) {
        if (this.random.nextInt(8) == 0) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.1F, this.random.nextFloat() * 0.6F + 0.3F);
        }
    }

    private void spawnFeedback(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = new Vec3(
                owner.getX() + this.random.nextGaussian() * 0.3D,
                owner.getY() + this.random.nextDouble() * 1.3D,
                owner.getZ() + this.random.nextGaussian() * 0.3D);
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (arc != null) {
            arc.configureRandom(center, 0.3D, new Vec3(0.0D, 0.15D, 0.0D), 0xC00000FF, 0, 0.0F, 0.1F);
            serverLevel.addFreshEntity(arc);
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x2080D0FF, 5, 50, 0xF0, owner.getId(), 4),
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                20,
                0.3D,
                0.0D,
                0.3D,
                0.5D);
    }

    private void discardMode() {
        writeCooldown();
        discard();
    }

    private void writeCooldown() {
        if (this.cooldownWritten || this.level().isClientSide) {
            return;
        }
        this.cooldownWritten = true;
        LivingEntity owner = getOwner();
        if (owner == null) {
            return;
        }
        if (owner instanceof Player player) {
            ItemStack requested = this.sourceStack.isEmpty() ? new ItemStack(ModItems.RAITON.get()) : this.sourceStack;
            ItemStack stack = ProcedureUtils.getItemStackIgnoreDurability(player.getInventory(), requested);
            if (stack != null && stack.getItem() instanceof RaitonItem raitonItem) {
                raitonItem.setChakraModeCooldown(stack, this.level(), owner, this.tickCount);
            }
            player.getPersistentData().remove(ENTITY_ID_KEY);
        } else if (!this.sourceStack.isEmpty() && this.sourceStack.getItem() instanceof RaitonItem raitonItem) {
            raitonItem.setChakraModeCooldown(this.sourceStack, this.level(), owner, this.tickCount);
        }
    }
}
