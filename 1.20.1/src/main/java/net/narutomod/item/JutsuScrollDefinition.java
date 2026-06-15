package net.narutomod.item;

import java.util.Arrays;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.narutomod.NarutomodMod;
import net.narutomod.registry.ModItems;

public enum JutsuScrollDefinition {
    RASENGAN(
            "rasengan",
            "item.narutomod.scroll_rasengan",
            "entity.narutomod.rasengan",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.RASENGAN),
    BODY_REPLACEMENT(
            "body_replacement",
            "item.narutomod.scroll_body_replacement",
            "entity.narutomod.replacementclone",
            "D-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.REPLACEMENT),
    KAGE_BUNSHIN(
            "kage_bunshin",
            "item.narutomod.scroll_kage_bunshin",
            "entity.narutomod.kage_bunshin",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.KAGE_BUNSHIN),
    HIDING_IN_CAMOUFLAGE(
            "hiding_in_camouflage",
            "item.narutomod.scroll_hiding_in_camouflage",
            "tooltip.ninjutsu.hidingincamouflage",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.INVISIBILITY),
    TRANSFORMATION(
            "transformation",
            "item.narutomod.scroll_transformation",
            "entity.narutomod.transformation_jutsu",
            "D-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.TRANSFORM),
    BUG_SWARM(
            "bug_swarm",
            "item.narutomod.scroll_kikaichu_sphere",
            "entity.narutomod.bugball",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/ninjutsu.png"),
            () -> ModItems.NINJUTSU.get(),
            () -> NinjutsuItem.BUG_SWARM),
    GENJUTSU(
            "genjutsu",
            "item.narutomod.scroll_genjutsu",
            "entity.narutomod.genjutsu",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/inton.png"),
            () -> ModItems.INTON.get(),
            () -> IntonItem.GENJUTSU),
    MIND_TRANSFER(
            "mind_transfer",
            "item.narutomod.scroll_mind_transfer",
            "entity.narutomod.mind_transfer",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/inton.png"),
            () -> ModItems.INTON.get(),
            () -> IntonItem.MIND_TRANSFER),
    SHADOW_IMITATION(
            "shadow_imitation",
            "item.narutomod.scroll_shadow_imitation",
            "entity.narutomod.shadow_imitation",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/inton.png"),
            () -> ModItems.INTON.get(),
            () -> IntonItem.SHADOW_IMITATION),
    HEALING(
            "healing",
            "item.narutomod.scroll_healing",
            "entity.narutomod.healingjutsu",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/medical-ninja-symbol.png"),
            () -> ModItems.IRYO_JUTSU.get(),
            () -> IryoJutsuItem.HEALING),
    POISON_MIST(
            "poison_mist",
            "item.narutomod.scroll_poison_mist",
            "entity.narutomod.poison_mist",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/medical-ninja-symbol.png"),
            () -> ModItems.IRYO_JUTSU.get(),
            () -> IryoJutsuItem.POISON_MIST),
    CELLULAR_ACTIVATION(
            "cellular_activation",
            "item.narutomod.scroll_cellular_activation",
            "entity.narutomod.cellular_activation",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/medical-ninja-symbol.png"),
            () -> ModItems.IRYO_JUTSU.get(),
            () -> IryoJutsuItem.CELLULAR_ACTIVATION),
    ENHANCED_STRENGTH(
            "enhanced_strength",
            "item.narutomod.scroll_enhanced_strength",
            "entity.narutomod.enhanced_strength",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/medical-ninja-symbol.png"),
            () -> ModItems.IRYO_JUTSU.get(),
            () -> IryoJutsuItem.ENHANCED_STRENGTH),
    CHIDORI(
            "chidori",
            "item.narutomod.scroll_chidori",
            "entity.narutomod.chidori",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/raiton.png"),
            () -> ModItems.RAITON.get(),
            () -> RaitonItem.CHIDORI),
    LIGHTNING_CHAKRA_MODE(
            "lightning_chakra_mode",
            "item.narutomod.scroll_lightning_chakra_mode",
            "entity.narutomod.raitonchakramode",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/raiton.png"),
            () -> ModItems.RAITON.get(),
            () -> RaitonItem.CHAKRA_MODE),
    LIGHTNING_BEAST(
            "lightning_beast",
            "item.narutomod.scroll_lightning_beast",
            "entity.narutomod.lightning_beast",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/raiton.png"),
            () -> ModItems.RAITON.get(),
            () -> RaitonItem.CHASING_DOG),
    FALSE_DARKNESS(
            "false_darkness",
            "item.narutomod.scroll_false_darkness",
            "entity.narutomod.false_darkness",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/raiton.png"),
            () -> ModItems.RAITON.get(),
            () -> RaitonItem.FALSE_DARKNESS),
    GREAT_FIREBALL(
            "great_fireball",
            "item.narutomod.scroll_great_fireball",
            "entity.narutomod.katonfireball",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/katon.png"),
            () -> ModItems.KATON.get(),
            () -> KatonItem.GREAT_FIREBALL),
    FIRE_ANNIHILATION(
            "fire_annihilation",
            "item.narutomod.scroll_fire_annihilation",
            "tooltip.katon.annihilation",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/katon.png"),
            () -> ModItems.KATON.get(),
            () -> KatonItem.FIRE_ANNIHILATION),
    HIDING_IN_ASH(
            "hiding_in_ash",
            "item.narutomod.scroll_hiding_in_ash",
            "entity.narutomod.hiding_in_ash",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/katon.png"),
            () -> ModItems.KATON.get(),
            () -> KatonItem.HIDING_IN_ASH),
    GREAT_FLAME(
            "great_flame",
            "item.narutomod.scroll_fire_stream",
            "entity.narutomod.katonfirestream",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/katon.png"),
            () -> ModItems.KATON.get(),
            () -> KatonItem.GREAT_FLAME),
    HIDING_IN_MIST(
            "hiding_in_mist",
            "item.narutomod.scroll_hiding_in_mist",
            "entity.narutomod.suitonmist",
            "D-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.HIDING_IN_MIST),
    WATER_BULLET(
            "water_bullet",
            "item.narutomod.scroll_water_stream",
            "entity.narutomod.suitonstream",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.WATER_BULLET),
    WATER_DRAGON(
            "water_dragon",
            "item.narutomod.scroll_water_dragon",
            "entity.narutomod.water_dragon",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.WATER_DRAGON),
    WATER_PRISON(
            "water_prison",
            "item.narutomod.scroll_water_prison",
            "entity.narutomod.water_prison",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.WATER_PRISON),
    WATER_SHARK(
            "water_shark",
            "item.narutomod.scroll_water_shark",
            "entity.narutomod.suiton_shark",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.WATER_SHARK),
    WATER_SHOCKWAVE(
            "water_shockwave",
            "item.narutomod.scroll_water_shockwave",
            "entity.narutomod.water_shockwave",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/suiton.png"),
            () -> ModItems.SUITON.get(),
            () -> SuitonItem.WATER_SHOCKWAVE),
    HIDING_IN_ROCK(
            "hiding_in_rock",
            "item.narutomod.scroll_hiding_in_rock",
            "entity.narutomod.entityhidinginrock",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/doton.png"),
            () -> ModItems.DOTON.get(),
            () -> DotonItem.HIDING_IN_ROCK),
    EARTH_SPEARS(
            "earth_spears",
            "item.narutomod.scroll_earth_spears",
            "entity.narutomod.earth_spears",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/doton.png"),
            () -> ModItems.DOTON.get(),
            () -> DotonItem.EARTH_SPEARS),
    EARTH_WALL(
            "earth_wall",
            "item.narutomod.scroll_earth_wall",
            "entity.narutomod.entityearthwall",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/doton.png"),
            () -> ModItems.DOTON.get(),
            () -> DotonItem.EARTH_WALL),
    SWAMP_PIT(
            "swamp_pit",
            "item.narutomod.scroll_swamp_pit",
            "entity.narutomod.swamp_pit",
            "A-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/doton.png"),
            () -> ModItems.DOTON.get(),
            () -> DotonItem.SWAMP_PIT),
    EARTH_SANDWICH(
            "earth_sandwich",
            "item.narutomod.scroll_earth_sandwich",
            "entity.narutomod.earth_sandwich",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/doton.png"),
            () -> ModItems.DOTON.get(),
            () -> DotonItem.EARTH_SANDWICH),
    FUTON_VACUUM(
            "futon_vacuum",
            "item.narutomod.scroll_futon_vacuum",
            "entity.narutomod.futon_vacuum",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/futon.png"),
            () -> ModItems.FUTON.get(),
            () -> FutonItem.VACUUM),
    RASENSHURIKEN(
            "rasenshuriken",
            "item.narutomod.scroll_rasenshuriken",
            "entity.narutomod.rasenshuriken",
            "S-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/futon.png"),
            () -> ModItems.FUTON.get(),
            () -> FutonItem.RASENSHURIKEN),
    BIG_BLOW(
            "big_blow",
            "item.narutomod.scroll_big_blow",
            "entity.narutomod.futon_great_breakthrough",
            "C-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/futon.png"),
            () -> ModItems.FUTON.get(),
            () -> FutonItem.BIG_BLOW),
    CHAKRA_FLOW(
            "futon_chakra_flow",
            "item.narutomod.scroll_futon_chakra_flow",
            "entity.narutomod.futonchakraflow",
            "D-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/futon.png"),
            () -> ModItems.FUTON.get(),
            () -> FutonItem.CHAKRA_FLOW),
    MULTI_SIZE(
            "multi_size",
            "item.narutomod.scroll_multi_size",
            "biggerme",
            "B-rank jutsu scroll",
            NarutomodMod.location("textures/blocks/yoton.png"),
            () -> ModItems.YOTON.get(),
            () -> AdvancedNatureJutsuItem.AdvancedNatureKind.YOTON.definitionByIndex(0));

    private final String id;
    private final String titleKey;
    private final String jutsuNameKey;
    private final String rankTooltip;
    private final ResourceLocation iconTexture;
    private final Supplier<Item> targetItem;
    private final Supplier<JutsuItem.JutsuDefinition> jutsuDefinition;

    JutsuScrollDefinition(
            String id,
            String titleKey,
            String jutsuNameKey,
            String rankTooltip,
            ResourceLocation iconTexture,
            Supplier<Item> targetItem,
            Supplier<JutsuItem.JutsuDefinition> jutsuDefinition) {
        this.id = id;
        this.titleKey = titleKey;
        this.jutsuNameKey = jutsuNameKey;
        this.rankTooltip = rankTooltip;
        this.iconTexture = iconTexture;
        this.targetItem = targetItem;
        this.jutsuDefinition = jutsuDefinition;
    }

    public String id() {
        return id;
    }

    public String titleKey() {
        return titleKey;
    }

    public String jutsuNameKey() {
        return jutsuNameKey;
    }

    public String rankTooltip() {
        return rankTooltip;
    }

    public ResourceLocation iconTexture() {
        return iconTexture;
    }

    public Item targetItem() {
        return targetItem.get();
    }

    public JutsuItem.JutsuDefinition jutsuDefinition() {
        return jutsuDefinition.get();
    }

    public static JutsuScrollDefinition byId(String id) {
        return Arrays.stream(values())
                .filter(definition -> definition.id.equals(id))
                .findFirst()
                .orElse(RASENGAN);
    }
}
