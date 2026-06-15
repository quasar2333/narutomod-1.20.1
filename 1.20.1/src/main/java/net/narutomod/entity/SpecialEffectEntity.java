package net.narutomod.entity;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class SpecialEffectEntity extends Entity {
    public static final int DEFAULT_DURATION = 600;
    private static final EntityDataAccessor<Integer> TYPE_ID = SynchedEntityData.defineId(SpecialEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(SpecialEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SpecialEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFESPAN = SynchedEntityData.defineId(SpecialEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SpecialEffectEntity.class, EntityDataSerializers.INT);

    public SpecialEffectEntity(EntityType<? extends SpecialEffectEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    @Nullable
    public static SpecialEffectEntity spawn(Level level, EffectType type, int color, float radius, int lifespan, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        SpecialEffectEntity entity = ModEntityTypes.SPECIALEFFECTENTITY.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(type, color, radius, lifespan);
        entity.moveTo(x, y, z, 0.0F, 0.0F);
        serverLevel.addFreshEntity(entity);
        return entity;
    }

    public void configure(EffectType type, int color, float radius, int lifespan) {
        setEffectType(type);
        setColor(color);
        setRadius(radius);
        setLifespan(lifespan);
        setAge(0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE_ID, EffectType.ROTATING_LINES_COLOR_END.getId());
        this.entityData.define(AGE, 0);
        this.entityData.define(RADIUS, 200.0F);
        this.entityData.define(LIFESPAN, DEFAULT_DURATION);
        this.entityData.define(COLOR, 0xFF00FF);
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xRotO = getXRot();
        this.yRotO = getYRot();
        setAge(getAge() + 1);
        setYRot(getYRot() + 30.0F);
        if (getAge() >= getLifespan() || getAge() < 0) {
            discard();
        }
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 256.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setEffectType(EffectType.byId(tag.getInt("Type")));
        setAge(tag.getInt("Age"));
        setRadius(tag.contains("Radius") ? tag.getFloat("Radius") : 200.0F);
        setLifespan(tag.contains("Lifespan") ? tag.getInt("Lifespan") : DEFAULT_DURATION);
        setColor(tag.contains("Color") ? tag.getInt("Color") : 0xFF00FF);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Type", getEffectType().getId());
        tag.putInt("Age", getAge());
        tag.putFloat("Radius", getRadius());
        tag.putInt("Lifespan", getLifespan());
        tag.putInt("Color", getColor());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public EffectType getEffectType() {
        return EffectType.byId(this.entityData.get(TYPE_ID));
    }

    private void setEffectType(EffectType type) {
        this.entityData.set(TYPE_ID, type.getId());
    }

    public int getAge() {
        return this.entityData.get(AGE);
    }

    private void setAge(int age) {
        this.entityData.set(AGE, age);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    private void setRadius(float radius) {
        this.entityData.set(RADIUS, Math.max(radius, 0.0F));
    }

    public int getLifespan() {
        return Math.max(this.entityData.get(LIFESPAN), 1);
    }

    private void setLifespan(int lifespan) {
        this.entityData.set(LIFESPAN, Math.max(lifespan, 1));
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    public enum EffectType {
        ROTATING_LINES_COLOR_END(0),
        EXPANDING_SPHERES_FADE_TO_BLACK(1);

        private static final Map<Integer, EffectType> BY_ID = new HashMap<>();

        static {
            for (EffectType type : values()) {
                BY_ID.put(type.id, type);
            }
        }

        private final int id;

        EffectType(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static EffectType byId(int id) {
            return BY_ID.getOrDefault(id, ROTATING_LINES_COLOR_END);
        }
    }
}
