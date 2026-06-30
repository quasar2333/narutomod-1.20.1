package net.narutomod.registry;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.narutomod.NarutomodMod;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> NINJUTSU = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("ninjutsu_damage"));
    public static final ResourceKey<DamageType> NINJUTSU_FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("ninjutsu_fire_damage"));
    public static final ResourceKey<DamageType> AMATERASU = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("amaterasu_damage"));
    public static final ResourceKey<DamageType> SENJUTSU = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("senjutsu_damage"));
    public static final ResourceKey<DamageType> JINTON = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("jinton_damage"));
    public static final ResourceKey<DamageType> KATON_FIRESTREAM = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("katon_firestream_damage"));
    public static final ResourceKey<DamageType> KUSANAGI = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("kusanagi_damage"));
    public static final ResourceKey<DamageType> HIRUDORA = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("hirudora_damage"));
    public static final ResourceKey<DamageType> NIGHT_GUY = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("night_guy_damage"));
    public static final ResourceKey<DamageType> SHINRATENSEI = ResourceKey.create(Registries.DAMAGE_TYPE, NarutomodMod.location("shinratensei_damage"));

    private ModDamageTypes() {
    }

    public static DamageSource ninjutsu(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, NINJUTSU, directEntity, causingEntity);
    }

    public static DamageSource ninjutsuFire(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, NINJUTSU_FIRE, directEntity, causingEntity);
    }

    public static boolean isNinjutsu(DamageSource source) {
        return source.is(NINJUTSU) || source.is(NINJUTSU_FIRE);
    }

    public static DamageSource amaterasu(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, AMATERASU, directEntity, causingEntity);
    }

    public static DamageSource senjutsu(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, SENJUTSU, directEntity, causingEntity);
    }

    public static DamageSource jinton(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, JINTON, directEntity, causingEntity);
    }

    public static DamageSource katonFireStream(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, KATON_FIRESTREAM, directEntity, causingEntity);
    }

    public static DamageSource kusanagi(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, KUSANAGI, directEntity, causingEntity);
    }

    public static DamageSource hirudora(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, HIRUDORA, directEntity, causingEntity);
    }

    public static DamageSource nightGuy(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, NIGHT_GUY, directEntity, causingEntity);
    }

    public static DamageSource shinratensei(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, SHINRATENSEI, directEntity, causingEntity);
    }

    public static DamageSource fireball(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return source(level, causingEntity == null ? DamageTypes.UNATTRIBUTED_FIREBALL : DamageTypes.FIREBALL,
                directEntity, causingEntity);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder.Reference<DamageType> holder = registry.getHolderOrThrow(key);
        return new DamageSource(holder, directEntity, causingEntity);
    }
}
