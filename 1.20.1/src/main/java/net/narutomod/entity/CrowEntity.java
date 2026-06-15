package net.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModSounds;

public final class CrowEntity extends Bat {
    private static final int MIN_LIFE = 200;
    private static final int RANDOM_LIFE = 100;

    private int lifeSpan;
    private boolean real;

    public CrowEntity(EntityType<? extends CrowEntity> entityType, Level level) {
        super(entityType, level);
        this.lifeSpan = MIN_LIFE + this.random.nextInt(RANDOM_LIFE);
        this.real = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes();
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        if (!this.level().isClientSide && --this.lifeSpan <= 0) {
            discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return this.real;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.real && super.hurt(source, amount);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.SOUND_CROW_CALL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : MIN_LIFE;
        this.real = tag.getBoolean("Real");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeSpan", this.lifeSpan);
        tag.putBoolean("Real", this.real);
    }
}
