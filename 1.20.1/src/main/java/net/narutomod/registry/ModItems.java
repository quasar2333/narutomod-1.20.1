package net.narutomod.registry;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.item.AdvancedNatureJutsuItem;
import net.narutomod.item.AkatsukiRobeItem;
import net.narutomod.item.AsuraCannonItem;
import net.narutomod.item.AsuraPathArmorItem;
import net.narutomod.item.BasicMeleeWeaponItem;
import net.narutomod.item.BijuCloakItem;
import net.narutomod.item.BoneArmorItem;
import net.narutomod.item.ByakuganHelmetItem;
import net.narutomod.item.DojutsuHelmetItem;
import net.narutomod.item.DotonItem;
import net.narutomod.item.EightGatesItem;
import net.narutomod.item.EternalMangekyoHelmetItem;
import net.narutomod.item.FoldingFanItem;
import net.narutomod.item.FutonItem;
import net.narutomod.item.HiramekareiSwordItem;
import net.narutomod.item.IceSenbonItem;
import net.narutomod.item.IryoJutsuItem;
import net.narutomod.item.IntonItem;
import net.narutomod.item.ItemOnBody;
import net.narutomod.item.JutsuScrollDefinition;
import net.narutomod.item.JutsuScrollItem;
import net.narutomod.item.KagutsuchiSwordItem;
import net.narutomod.item.KamuiDimensionItem;
import net.narutomod.item.KamuiShurikenItem;
import net.narutomod.item.KatonItem;
import net.narutomod.item.KibaBladesItem;
import net.narutomod.item.KusanagiSwordItem;
import net.narutomod.item.MedicalScrollItem;
import net.narutomod.item.NinjaArmorItem;
import net.narutomod.item.NinjaToolItem;
import net.narutomod.item.NinjutsuItem;
import net.narutomod.item.NuibariSwordItem;
import net.narutomod.item.NuibariThrownItem;
import net.narutomod.item.ObitoMangekyoHelmetItem;
import net.narutomod.item.NarutoConsumableItem;
import net.narutomod.item.PuppetScrollItem;
import net.narutomod.item.RaitonItem;
import net.narutomod.item.RinneganHelmetItem;
import net.narutomod.item.RinneganRobeItem;
import net.narutomod.item.RyoItem;
import net.narutomod.item.ScrollRasenganItem;
import net.narutomod.item.SenjutsuItem;
import net.narutomod.item.SixPathSenjutsuItem;
import net.narutomod.item.SpecialProjectileWeaponItem;
import net.narutomod.item.SteamArmorItem;
import net.narutomod.item.SuitonItem;
import net.narutomod.item.SummoningContractItem;
import net.narutomod.item.TeamScrollItem;
import net.narutomod.item.TenseiganChakraModeItem;
import net.narutomod.item.UchihaArmorItem;
import net.narutomod.item.ZzzItem;

public final class ModItems {
    public static final RegistryObject<Item> AKATSUKI_ROBEBODY = register("akatsuki_robebody",
            () -> new AkatsukiRobeItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> AKATSUKI_ROBEHELMET = register("akatsuki_robehelmet",
            () -> new AkatsukiRobeItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> ANBU_MASK_1HELMET = register("anbu_mask_1helmet",
            () -> new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> ANBU_MASK_2HELMET = register("anbu_mask_2helmet",
            () -> new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> ANBU_MASK_3HELMET = register("anbu_mask_3helmet",
            () -> new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> ANBU_SWORD = register("anbu_sword",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.ANBU_SWORD));
    public static final RegistryObject<Item> ASHBONES = register("ashbones",
            () -> new SpecialProjectileWeaponItem(
                    SpecialProjectileWeaponItem.WeaponKind.ASH_BONES,
                    ModEntityTypes.ENTITYBULLETASHBONES,
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ASURACANON = register("asuracanon", AsuraCannonItem::new);
    public static final RegistryObject<Item> ASURAPATHARMORBODY = register("asurapatharmorbody",
            AsuraPathArmorItem::new);
    public static final RegistryObject<Item> BAKUTON = register("bakuton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.BAKUTON));
    public static final RegistryObject<Item> BIJU_CLOAKBODY = register("biju_cloakbody",
            () -> new BijuCloakItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> BIJU_CLOAKHELMET = register("biju_cloakhelmet",
            () -> new BijuCloakItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> BIJU_CLOAKLEGS = register("biju_cloaklegs",
            () -> new BijuCloakItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> BLACK_RECEIVER = register("black_receiver",
            () -> new SpecialProjectileWeaponItem(
                    SpecialProjectileWeaponItem.WeaponKind.BLACK_RECEIVER,
                    ModEntityTypes.ENTITYBULLETBLACK_RECEIVER,
                    new Item.Properties().durability(50)));
    public static final RegistryObject<Item> BONE_ARMORBODY = register("bone_armorbody",
            BoneArmorItem::new);
    public static final RegistryObject<Item> BONE_DRILL = register("bone_drill",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.BONE_DRILL));
    public static final RegistryObject<Item> BONE_SWORD = register("bone_sword",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.BONE_SWORD));
    public static final RegistryObject<Item> BYAKUGANHELMET = register("byakuganhelmet", ByakuganHelmetItem::new);
    public static final RegistryObject<Item> BYAKURINNESHARINGANHELMET = register("byakurinnesharinganhelmet",
            () -> new ByakuganHelmetItem(true));
    public static final RegistryObject<Item> CHAKRAFRUIT = register("chakrafruit",
            () -> new NarutoConsumableItem(NarutoConsumableItem.ConsumableKind.CHAKRA_FRUIT));
    public static final RegistryObject<Item> CHOKUTO = register("chokuto",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.CHOKUTO));
    public static final RegistryObject<Item> DOTON = register("doton", DotonItem::new);
    public static final RegistryObject<Item> EIGHTGATES = register("eightgates", EightGatesItem::new);
    public static final RegistryObject<Item> FOLDING_FAN = register("folding_fan", FoldingFanItem::new);
    public static final RegistryObject<Item> FUTON = register("futon", FutonItem::new);
    public static final RegistryObject<Item> FUTTON = register("futton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.FUTTON));
    public static final RegistryObject<Item> GOURDBODY = register("gourdbody",
            () -> new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HIRAMEKAREI_SWORD = register("hiramekarei_sword", HiramekareiSwordItem::new);
    public static final RegistryObject<Item> HYOTON = register("hyoton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.HYOTON));
    public static final RegistryObject<Item> ICE_SENBON = register("ice_senbon", IceSenbonItem::new);
    public static final RegistryObject<Item> INTON = register("inton", IntonItem::new);
    public static final RegistryObject<Item> IRYO_JUTSU = register("iryo_jutsu", IryoJutsuItem::new);
    public static final RegistryObject<Item> JINTON = register("jinton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.JINTON));
    public static final RegistryObject<Item> JITON = register("jiton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.JITON));
    public static final RegistryObject<Item> KABUTOWARI = register("kabutowari",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.KABUTOWARI));
    public static final RegistryObject<Item> KABUTOWARI_AXE = register("kabutowari_axe",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.KABUTOWARI_AXE));
    public static final RegistryObject<Item> KABUTOWARI_HAMMER = register("kabutowari_hammer",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.KABUTOWARI_HAMMER));
    public static final RegistryObject<Item> KAGUTSUCHISWORDRANGED = register("kagutsuchiswordranged", KagutsuchiSwordItem::new);
    public static final RegistryObject<Item> KAMUIDIMENSION = register("kamuidimension", KamuiDimensionItem::new);
    public static final RegistryObject<Item> KAMUISHURIKEN = register("kamuishuriken", KamuiShurikenItem::new);
    public static final RegistryObject<Item> KATON = register("katon", KatonItem::new);
    public static final RegistryObject<Item> KEKKEI_MORA = register("kekkei_mora",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.KEKKEI_MORA));
    public static final RegistryObject<Item> KIBA_BLADES = register("kiba_blades", KibaBladesItem::new);
    public static final RegistryObject<Item> KUNAI = register("kunai",
            () -> new NinjaToolItem(3, ModEntityTypes.ENTITYBULLETKUNAI, true, 2.0F, true, true,
                    NinjaToolItem.SoundEventsRef.ARROW_SHOOT, 4.0D, ItemOnBody.BodyPart.RIGHT_LEG));
    public static final RegistryObject<Item> KUNAI_EXPLOSIVE = register("kunai_explosive",
            () -> new NinjaToolItem(3, ModEntityTypes.ENTITYBULLETKUNAI_EXPLOSIVE, true, 2.0F, false, true,
                    NinjaToolItem.SoundEventsRef.ARROW_SHOOT, 4.0D, ItemOnBody.BodyPart.RIGHT_LEG));
    public static final RegistryObject<Item> KUSANAGI_SWORD = register("kusanagi_sword", KusanagiSwordItem::new);
    public static final RegistryObject<Item> MANGEKYOSHARINGANETERNALHELMET = register("mangekyosharinganeternalhelmet",
            EternalMangekyoHelmetItem::new);
    public static final RegistryObject<Item> MANGEKYOSHARINGANHELMET = register("mangekyosharinganhelmet",
            () -> new DojutsuHelmetItem("narutomod:textures/mangekyosharinganhelmet_sasuke.png"));
    public static final RegistryObject<Item> MANGEKYOSHARINGANOBITOHELMET = register("mangekyosharinganobitohelmet",
            ObitoMangekyoHelmetItem::new);
    public static final RegistryObject<Item> MEDICAL_SCROLL = register("medical_scroll", MedicalScrollItem::new);
    public static final RegistryObject<Item> MILITARY_RATIONS_PILL = register("military_rations_pill",
            () -> new NarutoConsumableItem(NarutoConsumableItem.ConsumableKind.MILITARY_RATIONS_PILL));
    public static final RegistryObject<Item> MILITARY_RATIONS_PILL_GOLD = register("military_rations_pill_gold",
            () -> new NarutoConsumableItem(NarutoConsumableItem.ConsumableKind.MILITARY_RATIONS_PILL_GOLD));
    public static final RegistryObject<Item> MOKUTON = register("mokuton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.MOKUTON));
    public static final RegistryObject<Item> NINJA_ARMOR_AMEHELMET = register("ninja_armor_amehelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.AME, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_ANBUBODY = register("ninja_armor_anbubody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.ANBU, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_ANBULEGS = register("ninja_armor_anbulegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.ANBU, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_FISHNETSLEGS = register("ninja_armor_fishnetslegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.FISHNET, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_IWABODY = register("ninja_armor_iwabody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.IWA, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_IWAHELMET = register("ninja_armor_iwahelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.IWA, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_IWALEGS = register("ninja_armor_iwalegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.IWA, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_JUMPSUITLEGS = register("ninja_armor_jumpsuitlegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.JUMPSUIT, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_KIRIBODY = register("ninja_armor_kiribody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KIRI, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_KIRIHELMET = register("ninja_armor_kirihelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KIRI, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_KIRILEGS = register("ninja_armor_kirilegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KIRI, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_KONOHABODY = register("ninja_armor_konohabody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KONOHA, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_KONOHAHELMET = register("ninja_armor_konohahelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KONOHA, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_KONOHALEGS = register("ninja_armor_konohalegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KONOHA, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_KUMOBODY = register("ninja_armor_kumobody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KUMO, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_KUMOHELMET = register("ninja_armor_kumohelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KUMO, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_KUMOLEGS = register("ninja_armor_kumolegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.KUMO, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_SUNABODY = register("ninja_armor_sunabody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SUNA, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_SUNAHELMET = register("ninja_armor_sunahelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SUNA, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJA_ARMOR_SUNALEGS = register("ninja_armor_sunalegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SUNA, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> NINJA_ARMOR_WAR_1BODY = register("ninja_armor_war_1body",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.WAR1, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> NINJA_ARMOR_WAR_1HELMET = register("ninja_armor_war_1helmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.WAR1, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> NINJUTSU = register("ninjutsu", NinjutsuItem::new);
    public static final RegistryObject<Item> NUIBARI_SWORD = register("nuibari_sword", NuibariSwordItem::new);
    public static final RegistryObject<Item> NUIBARI_THROWN = register("nuibari_thrown", NuibariThrownItem::new);
    public static final RegistryObject<Item> RAITON = register("raiton", RaitonItem::new);
    public static final RegistryObject<Item> RANTON = register("ranton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.RANTON));
    public static final RegistryObject<Item> RINNEGANBODY = register("rinneganbody",
            () -> new RinneganRobeItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> RINNEGANHELMET = register("rinneganhelmet",
            () -> new RinneganHelmetItem(false));
    public static final RegistryObject<Item> RINNEGANLEGS = register("rinneganlegs",
            () -> new RinneganRobeItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> RYO_100 = register("ryo_100", () -> new RyoItem("100 Ryo"));
    public static final RegistryObject<Item> RYO_1000 = register("ryo_1000", () -> new RyoItem("1000 Ryo"));
    public static final RegistryObject<Item> RYO_10000 = register("ryo_10000", () -> new RyoItem("10,000 Ryo"));
    public static final RegistryObject<Item> RYO_1_M = register("ryo_1_m", () -> new RyoItem("1,000,000 Ryo"));
    public static final RegistryObject<Item> SAGE_MODE_ARMORHELMET = register("sage_mode_armorhelmet",
            () -> new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SAMEHADA = register("samehada",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.SAMEHADA));
    public static final RegistryObject<Item> SAMURAI_ARMORBODY = register("samurai_armorbody",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SAMURAI, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> SAMURAI_ARMORHELMET = register("samurai_armorhelmet",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SAMURAI, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> SAMURAI_ARMORLEGS = register("samurai_armorlegs",
            () -> new NinjaArmorItem(NinjaArmorItem.Style.SAMURAI, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> SCROLL_BIG_BLOW = register("scroll_big_blow",
            () -> new JutsuScrollItem(JutsuScrollDefinition.BIG_BLOW));
    public static final RegistryObject<Item> SCROLL_BODY_REPLACEMENT = register("scroll_body_replacement",
            () -> new JutsuScrollItem(JutsuScrollDefinition.BODY_REPLACEMENT));
    public static final RegistryObject<Item> SCROLL_CELLULAR_ACTIVATION = register("scroll_cellular_activation",
            () -> new JutsuScrollItem(JutsuScrollDefinition.CELLULAR_ACTIVATION));
    public static final RegistryObject<Item> SCROLL_CHIDORI = register("scroll_chidori", () -> new JutsuScrollItem(JutsuScrollDefinition.CHIDORI));
    public static final RegistryObject<Item> SCROLL_EARTH_SANDWICH = register("scroll_earth_sandwich",
            () -> new JutsuScrollItem(JutsuScrollDefinition.EARTH_SANDWICH));
    public static final RegistryObject<Item> SCROLL_EARTH_SPEARS = register("scroll_earth_spears",
            () -> new JutsuScrollItem(JutsuScrollDefinition.EARTH_SPEARS));
    public static final RegistryObject<Item> SCROLL_EARTH_WALL = register("scroll_earth_wall",
            () -> new JutsuScrollItem(JutsuScrollDefinition.EARTH_WALL));
    public static final RegistryObject<Item> SCROLL_ENHANCED_STRENGTH = register("scroll_enhanced_strength",
            () -> new JutsuScrollItem(JutsuScrollDefinition.ENHANCED_STRENGTH));
    public static final RegistryObject<Item> SCROLL_FALSE_DARKNESS = register("scroll_false_darkness",
            () -> new JutsuScrollItem(JutsuScrollDefinition.FALSE_DARKNESS));
    public static final RegistryObject<Item> SCROLL_FIRE_ANNIHILATION = register("scroll_fire_annihilation",
            () -> new JutsuScrollItem(JutsuScrollDefinition.FIRE_ANNIHILATION));
    public static final RegistryObject<Item> SCROLL_FIRE_STREAM = register("scroll_fire_stream",
            () -> new JutsuScrollItem(JutsuScrollDefinition.GREAT_FLAME));
    public static final RegistryObject<Item> SCROLL_FUTON_CHAKRA_FLOW = register("scroll_futon_chakra_flow",
            () -> new JutsuScrollItem(JutsuScrollDefinition.CHAKRA_FLOW));
    public static final RegistryObject<Item> SCROLL_FUTON_VACUUM = register("scroll_futon_vacuum",
            () -> new JutsuScrollItem(JutsuScrollDefinition.FUTON_VACUUM));
    public static final RegistryObject<Item> SCROLL_GENJUTSU = register("scroll_genjutsu",
            () -> new JutsuScrollItem(JutsuScrollDefinition.GENJUTSU));
    public static final RegistryObject<Item> SCROLL_GREAT_FIREBALL = register("scroll_great_fireball",
            () -> new JutsuScrollItem(JutsuScrollDefinition.GREAT_FIREBALL));
    public static final RegistryObject<Item> SCROLL_HEALING = register("scroll_healing",
            () -> new JutsuScrollItem(JutsuScrollDefinition.HEALING));
    public static final RegistryObject<Item> SCROLL_HIDING_IN_ASH = register("scroll_hiding_in_ash",
            () -> new JutsuScrollItem(JutsuScrollDefinition.HIDING_IN_ASH));
    public static final RegistryObject<Item> SCROLL_HIDING_IN_CAMOUFLAGE = register("scroll_hiding_in_camouflage",
            () -> new JutsuScrollItem(JutsuScrollDefinition.HIDING_IN_CAMOUFLAGE));
    public static final RegistryObject<Item> SCROLL_HIDING_IN_MIST = register("scroll_hiding_in_mist",
            () -> new JutsuScrollItem(JutsuScrollDefinition.HIDING_IN_MIST));
    public static final RegistryObject<Item> SCROLL_HIDING_IN_ROCK = register("scroll_hiding_in_rock",
            () -> new JutsuScrollItem(JutsuScrollDefinition.HIDING_IN_ROCK));
    public static final RegistryObject<Item> SCROLL_KAGE_BUNSHIN = register("scroll_kage_bunshin",
            () -> new JutsuScrollItem(JutsuScrollDefinition.KAGE_BUNSHIN));
    public static final RegistryObject<Item> SCROLL_KARASU = register("scroll_karasu",
            () -> new PuppetScrollItem(PuppetScrollItem.PuppetScrollKind.KARASU));
    public static final RegistryObject<Item> SCROLL_KIKAICHU_SPHERE = register("scroll_kikaichu_sphere",
            () -> new JutsuScrollItem(JutsuScrollDefinition.BUG_SWARM));
    public static final RegistryObject<Item> SCROLL_LIGHTNING_BEAST = register("scroll_lightning_beast",
            () -> new JutsuScrollItem(JutsuScrollDefinition.LIGHTNING_BEAST));
    public static final RegistryObject<Item> SCROLL_LIGHTNING_CHAKRA_MODE = register("scroll_lightning_chakra_mode",
            () -> new JutsuScrollItem(JutsuScrollDefinition.LIGHTNING_CHAKRA_MODE));
    public static final RegistryObject<Item> SCROLL_MIND_TRANSFER = register("scroll_mind_transfer",
            () -> new JutsuScrollItem(JutsuScrollDefinition.MIND_TRANSFER));
    public static final RegistryObject<Item> SCROLL_MULTI_SIZE = register("scroll_multi_size",
            () -> new JutsuScrollItem(JutsuScrollDefinition.MULTI_SIZE));
    public static final RegistryObject<Item> SCROLL_POISON_MIST = register("scroll_poison_mist",
            () -> new JutsuScrollItem(JutsuScrollDefinition.POISON_MIST));
    public static final RegistryObject<Item> SCROLL_RASENGAN = register("scroll_rasengan", ScrollRasenganItem::new);
    public static final RegistryObject<Item> SCROLL_RASENSHURIKEN = register("scroll_rasenshuriken",
            () -> new JutsuScrollItem(JutsuScrollDefinition.RASENSHURIKEN));
    public static final RegistryObject<Item> SCROLL_SANSHOUO = register("scroll_sanshouo",
            () -> new PuppetScrollItem(PuppetScrollItem.PuppetScrollKind.SANSHOUO));
    public static final RegistryObject<Item> SCROLL_SHADOW_IMITATION = register("scroll_shadow_imitation",
            () -> new JutsuScrollItem(JutsuScrollDefinition.SHADOW_IMITATION));
    public static final RegistryObject<Item> SCROLL_SWAMP_PIT = register("scroll_swamp_pit",
            () -> new JutsuScrollItem(JutsuScrollDefinition.SWAMP_PIT));
    public static final RegistryObject<Item> SCROLL_TRANSFORMATION = register("scroll_transformation",
            () -> new JutsuScrollItem(JutsuScrollDefinition.TRANSFORMATION));
    public static final RegistryObject<Item> SCROLL_WATER_DRAGON = register("scroll_water_dragon",
            () -> new JutsuScrollItem(JutsuScrollDefinition.WATER_DRAGON));
    public static final RegistryObject<Item> SCROLL_WATER_PRISON = register("scroll_water_prison",
            () -> new JutsuScrollItem(JutsuScrollDefinition.WATER_PRISON));
    public static final RegistryObject<Item> SCROLL_WATER_SHARK = register("scroll_water_shark",
            () -> new JutsuScrollItem(JutsuScrollDefinition.WATER_SHARK));
    public static final RegistryObject<Item> SCROLL_WATER_SHOCKWAVE = register("scroll_water_shockwave",
            () -> new JutsuScrollItem(JutsuScrollDefinition.WATER_SHOCKWAVE));
    public static final RegistryObject<Item> SCROLL_WATER_STREAM = register("scroll_water_stream",
            () -> new JutsuScrollItem(JutsuScrollDefinition.WATER_BULLET));
    public static final RegistryObject<Item> SENJUTSU = register("senjutsu", SenjutsuItem::new);
    public static final RegistryObject<Item> SHAKUTON = register("shakuton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.SHAKUTON));
    public static final RegistryObject<Item> SHARINGANHELMET = register("sharinganhelmet",
            () -> new DojutsuHelmetItem("narutomod:textures/sharinganhelmet.png"));
    public static final RegistryObject<Item> SHIBUKI_SWORD = register("shibuki_sword",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.SHIBUKI_SWORD));
    public static final RegistryObject<Item> SHIKOTSUMYAKU = register("shikotsumyaku",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.SHIKOTSUMYAKU));
    public static final RegistryObject<Item> SHURIKEN = register("shuriken",
            () -> new NinjaToolItem(64, ModEntityTypes.ENTITYBULLETSHURIKEN, false, 1.4F, true, true,
                    NinjaToolItem.SoundEventsRef.ARROW_SHOOT, 0.0D, ItemOnBody.BodyPart.LEFT_LEG));
    public static final RegistryObject<Item> SIX_PATH_SENJUTSU = register("six_path_senjutsu", SixPathSenjutsuItem::new);
    public static final RegistryObject<Item> SMOKE_BOMB = register("smoke_bomb",
            () -> new NinjaToolItem(16, ModEntityTypes.ENTITYBULLETSMOKE_BOMB, false, 1.0F, false, false,
                    NinjaToolItem.SoundEventsRef.SNOWBALL_THROW, 0.0D));
    public static final RegistryObject<Item> STEAM_ARMORBODY = register("steam_armorbody",
            () -> new SteamArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> STEAM_ARMORHELMET = register("steam_armorhelmet",
            () -> new SteamArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> STEAM_ARMORLEGS = register("steam_armorlegs",
            () -> new SteamArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> SUITON = register("suiton", SuitonItem::new);
    public static final RegistryObject<Item> SUMMONING_CONTRACT = register("summoning_contract", SummoningContractItem::new);
    public static final RegistryObject<Item> TEAM_SCROLL = register("team_scroll", TeamScrollItem::new);
    public static final RegistryObject<Item> TENSEIGAN_CHAKRA_MODE = register("tenseigan_chakra_mode", TenseiganChakraModeItem::new);
    public static final RegistryObject<Item> TENSEIGANBODY = register("tenseiganbody",
            () -> new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> TENSEIGANHELMET = register("tenseiganhelmet",
            () -> new RinneganHelmetItem(true));
    public static final RegistryObject<Item> TENSEIGANLEGS = register("tenseiganlegs",
            () -> new ArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> TOTSUKA_SWORD = register("totsuka_sword",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.TOTSUKA_SWORD));
    public static final RegistryObject<Item> UCHIHABODY = register("uchihabody",
            () -> new UchihaArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> UCHIHABOOTS = register("uchihaboots",
            () -> new UchihaArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> UCHIHALEGS = register("uchihalegs",
            () -> new UchihaArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WHITEZETSUFLESH = register("whitezetsuflesh",
            () -> new NarutoConsumableItem(NarutoConsumableItem.ConsumableKind.WHITE_ZETSU_FLESH));
    public static final RegistryObject<Item> YOOTON = register("yooton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.YOOTON));
    public static final RegistryObject<Item> YOTON = register("yoton",
            () -> new AdvancedNatureJutsuItem(AdvancedNatureJutsuItem.AdvancedNatureKind.YOTON));
    public static final RegistryObject<Item> ZABUZA_SWORD = register("zabuza_sword",
            () -> new BasicMeleeWeaponItem(BasicMeleeWeaponItem.WeaponKind.ZABUZA_SWORD));
    public static final RegistryObject<Item> ZZZ = register("zzz", ZzzItem::new);

    private ModItems() {
    }

    private static RegistryObject<Item> register(String name) {
        return ModRegistries.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> register(String name, Supplier<? extends Item> supplier) {
        return ModRegistries.ITEMS.register(name, supplier);
    }

    public static List<RegistryObject<Item>> all() {
        return List.of(
                AKATSUKI_ROBEBODY,
                AKATSUKI_ROBEHELMET,
                ANBU_MASK_1HELMET,
                ANBU_MASK_2HELMET,
                ANBU_MASK_3HELMET,
                ANBU_SWORD,
                ASHBONES,
                ASURACANON,
                ASURAPATHARMORBODY,
                BAKUTON,
                BIJU_CLOAKBODY,
                BIJU_CLOAKHELMET,
                BIJU_CLOAKLEGS,
                BLACK_RECEIVER,
                BONE_ARMORBODY,
                BONE_DRILL,
                BONE_SWORD,
                BYAKUGANHELMET,
                BYAKURINNESHARINGANHELMET,
                CHAKRAFRUIT,
                CHOKUTO,
                DOTON,
                EIGHTGATES,
                FOLDING_FAN,
                FUTON,
                FUTTON,
                GOURDBODY,
                HIRAMEKAREI_SWORD,
                HYOTON,
                ICE_SENBON,
                INTON,
                IRYO_JUTSU,
                JINTON,
                JITON,
                KABUTOWARI,
                KABUTOWARI_AXE,
                KABUTOWARI_HAMMER,
                KAGUTSUCHISWORDRANGED,
                KAMUIDIMENSION,
                KAMUISHURIKEN,
                KATON,
                KEKKEI_MORA,
                KIBA_BLADES,
                KUNAI,
                KUNAI_EXPLOSIVE,
                KUSANAGI_SWORD,
                MANGEKYOSHARINGANETERNALHELMET,
                MANGEKYOSHARINGANHELMET,
                MANGEKYOSHARINGANOBITOHELMET,
                MEDICAL_SCROLL,
                MILITARY_RATIONS_PILL,
                MILITARY_RATIONS_PILL_GOLD,
                MOKUTON,
                NINJA_ARMOR_AMEHELMET,
                NINJA_ARMOR_ANBUBODY,
                NINJA_ARMOR_ANBULEGS,
                NINJA_ARMOR_FISHNETSLEGS,
                NINJA_ARMOR_IWABODY,
                NINJA_ARMOR_IWAHELMET,
                NINJA_ARMOR_IWALEGS,
                NINJA_ARMOR_JUMPSUITLEGS,
                NINJA_ARMOR_KIRIBODY,
                NINJA_ARMOR_KIRIHELMET,
                NINJA_ARMOR_KIRILEGS,
                NINJA_ARMOR_KONOHABODY,
                NINJA_ARMOR_KONOHAHELMET,
                NINJA_ARMOR_KONOHALEGS,
                NINJA_ARMOR_KUMOBODY,
                NINJA_ARMOR_KUMOHELMET,
                NINJA_ARMOR_KUMOLEGS,
                NINJA_ARMOR_SUNABODY,
                NINJA_ARMOR_SUNAHELMET,
                NINJA_ARMOR_SUNALEGS,
                NINJA_ARMOR_WAR_1BODY,
                NINJA_ARMOR_WAR_1HELMET,
                NINJUTSU,
                NUIBARI_SWORD,
                NUIBARI_THROWN,
                RAITON,
                RANTON,
                RINNEGANBODY,
                RINNEGANHELMET,
                RINNEGANLEGS,
                RYO_100,
                RYO_1000,
                RYO_10000,
                RYO_1_M,
                SAGE_MODE_ARMORHELMET,
                SAMEHADA,
                SAMURAI_ARMORBODY,
                SAMURAI_ARMORHELMET,
                SAMURAI_ARMORLEGS,
                SCROLL_BIG_BLOW,
                SCROLL_BODY_REPLACEMENT,
                SCROLL_CELLULAR_ACTIVATION,
                SCROLL_CHIDORI,
                SCROLL_EARTH_SANDWICH,
                SCROLL_EARTH_SPEARS,
                SCROLL_EARTH_WALL,
                SCROLL_ENHANCED_STRENGTH,
                SCROLL_FALSE_DARKNESS,
                SCROLL_FIRE_ANNIHILATION,
                SCROLL_FIRE_STREAM,
                SCROLL_FUTON_CHAKRA_FLOW,
                SCROLL_FUTON_VACUUM,
                SCROLL_GENJUTSU,
                SCROLL_GREAT_FIREBALL,
                SCROLL_HEALING,
                SCROLL_HIDING_IN_ASH,
                SCROLL_HIDING_IN_CAMOUFLAGE,
                SCROLL_HIDING_IN_MIST,
                SCROLL_HIDING_IN_ROCK,
                SCROLL_KAGE_BUNSHIN,
                SCROLL_KARASU,
                SCROLL_KIKAICHU_SPHERE,
                SCROLL_LIGHTNING_BEAST,
                SCROLL_LIGHTNING_CHAKRA_MODE,
                SCROLL_MIND_TRANSFER,
                SCROLL_MULTI_SIZE,
                SCROLL_POISON_MIST,
                SCROLL_RASENGAN,
                SCROLL_RASENSHURIKEN,
                SCROLL_SANSHOUO,
                SCROLL_SHADOW_IMITATION,
                SCROLL_SWAMP_PIT,
                SCROLL_TRANSFORMATION,
                SCROLL_WATER_DRAGON,
                SCROLL_WATER_PRISON,
                SCROLL_WATER_SHARK,
                SCROLL_WATER_SHOCKWAVE,
                SCROLL_WATER_STREAM,
                SENJUTSU,
                SHAKUTON,
                SHARINGANHELMET,
                SHIBUKI_SWORD,
                SHIKOTSUMYAKU,
                SHURIKEN,
                SIX_PATH_SENJUTSU,
                SMOKE_BOMB,
                STEAM_ARMORBODY,
                STEAM_ARMORHELMET,
                STEAM_ARMORLEGS,
                SUITON,
                SUMMONING_CONTRACT,
                TEAM_SCROLL,
                TENSEIGAN_CHAKRA_MODE,
                TENSEIGANBODY,
                TENSEIGANHELMET,
                TENSEIGANLEGS,
                TOTSUKA_SWORD,
                UCHIHABODY,
                UCHIHABOOTS,
                UCHIHALEGS,
                WHITEZETSUFLESH,
                YOOTON,
                YOTON,
                ZABUZA_SWORD,
                ZZZ
        );
    }

    public static void touch() {
    }
}
