#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AUDIT_ROOT = ROOT / "audit"
LEGACY_POTIONS = ROOT / "1.12.2" / "src" / "main" / "java" / "net" / "narutomod" / "potion"
LEGACY_PARTICLES = ROOT / "1.12.2" / "src" / "main" / "java" / "net" / "narutomod" / "Particles.java"
TARGET_JAVA = ROOT / "1.20.1" / "src" / "main" / "java" / "net" / "narutomod"
TARGET_RESOURCES = ROOT / "1.20.1" / "src" / "main" / "resources"
REGISTRY_PACKAGE = TARGET_JAVA / "registry"


def java_identifier(name: str, suffix: str = "") -> str:
    value = re.sub(r"[^A-Za-z0-9_]", "_", name).upper()
    value = re.sub(r"_+", "_", value).strip("_")
    if not value:
        value = "ENTRY"
    if value[0].isdigit():
        value = f"ENTRY_{value}"
    if suffix:
        value = f"{value}_{suffix}"
    return value


def read_registry_rows() -> list[dict[str, str]]:
    with (AUDIT_ROOT / "legacy_registry_names.csv").open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def unique_names(rows: list[dict[str, str]], registry: str) -> list[str]:
    names = []
    for row in rows:
        if row["registry"] == registry and row["name"] not in names:
            names.append(row["name"])
    return sorted(names)


def potion_names() -> list[str]:
    names = []
    for path in sorted(LEGACY_POTIONS.glob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for match in re.finditer(r"\bsetRegistryName\s*\(\s*\"([^\"]+)\"\s*\)", text):
            name = match.group(1)
            if name not in names:
                names.append(name)
    return sorted(names)


def particle_names() -> list[str]:
    text = LEGACY_PARTICLES.read_text(encoding="utf-8", errors="replace")
    match = re.search(r"public\s+enum\s+Types\s*\{(?P<body>.*?);", text, flags=re.S)
    if not match:
        raise RuntimeError("Could not find Particles.Types enum")
    return sorted(re.findall(r'\b[A-Z0-9_]+\s*\(\s*"([^"]+)"', match.group("body")))


def data_resource_item_references() -> list[str]:
    names: set[str] = set()
    roots = [
        TARGET_RESOURCES / "data" / "narutomod" / "recipes",
        TARGET_RESOURCES / "data" / "narutomod" / "loot_tables",
        TARGET_RESOURCES / "data" / "narutomod" / "advancements",
    ]

    def add_item_id(value: object) -> None:
        if isinstance(value, str) and value.startswith("narutomod:"):
            names.add(value.split(":", 1)[1])

    def walk(value: object) -> None:
        if isinstance(value, dict):
            add_item_id(value.get("item"))
            add_item_id(value.get("name"))
            items = value.get("items")
            if isinstance(items, list):
                for item in items:
                    add_item_id(item)
            for child in value.values():
                walk(child)
        elif isinstance(value, list):
            for child in value:
                walk(child)

    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*.json"):
            walk(json.loads(path.read_text(encoding="utf-8")))
    return sorted(names)


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def write_generated_registry_csv(
    item_names: list[str],
    block_names: list[str],
    entity_names: list[str],
    effect_names: list[str],
    particle_names_: list[str],
) -> None:
    path = AUDIT_ROOT / "generated_registry_names.csv"
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["registry", "name"])
        writer.writeheader()
        for name in block_names:
            writer.writerow({"registry": "block", "name": name})
            writer.writerow({"registry": "item", "name": name})
        for name in item_names:
            writer.writerow({"registry": "item", "name": name})
        for name in entity_names:
            writer.writerow({"registry": "entity_type", "name": name})
        for name in effect_names:
            writer.writerow({"registry": "mob_effect", "name": name})
        for name in particle_names_:
            writer.writerow({"registry": "particle_type", "name": name})
        writer.writerow({"registry": "item", "name": "porting_marker"})
        writer.writerow({"registry": "entity_type", "name": "porting_dummy"})


def render_mod_items(item_names: list[str]) -> str:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.world.item.Item;",
        "import net.minecraftforge.registries.RegistryObject;",
        "",
        "public final class ModItems {",
    ]
    for name in item_names:
        lines.append(f"    public static final RegistryObject<Item> {java_identifier(name)} = register(\"{name}\");")

    lines.extend([
        "",
        "    private ModItems() {",
        "    }",
        "",
        "    private static RegistryObject<Item> register(String name) {",
        "        return ModRegistries.ITEMS.register(name, () -> new Item(new Item.Properties()));",
        "    }",
        "",
        "    public static List<RegistryObject<Item>> all() {",
        "        return List.of(",
    ])
    for index, name in enumerate(item_names):
        comma = "," if index < len(item_names) - 1 else ""
        lines.append(f"                {java_identifier(name)}{comma}")
    lines.extend([
        "        );",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    return "\n".join(lines)


def render_mod_blocks(block_names: list[str]) -> str:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.world.item.BlockItem;",
        "import net.minecraft.world.item.Item;",
        "import net.minecraft.world.level.block.Block;",
        "import net.minecraft.world.level.block.state.BlockBehaviour;",
        "import net.minecraft.world.level.material.MapColor;",
        "import net.minecraftforge.registries.RegistryObject;",
        "",
        "public final class ModBlocks {",
    ]
    for name in block_names:
        ident = java_identifier(name)
        lines.append(f"    public static final RegistryObject<Block> {ident} = registerBlock(\"{name}\");")
        lines.append(f"    public static final RegistryObject<Item> {java_identifier(name, 'ITEM')} = registerBlockItem(\"{name}\", {ident});")

    lines.extend([
        "",
        "    private ModBlocks() {",
        "    }",
        "",
        "    private static RegistryObject<Block> registerBlock(String name) {",
        "        return ModRegistries.BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()",
        "                .mapColor(MapColor.STONE)",
        "                .strength(1.0F, 6.0F)",
        "                .noOcclusion()));",
        "    }",
        "",
        "    private static RegistryObject<Item> registerBlockItem(String name, RegistryObject<Block> block) {",
        "        return ModRegistries.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));",
        "    }",
        "",
        "    public static List<RegistryObject<Item>> blockItems() {",
        "        return List.of(",
    ])
    for index, name in enumerate(block_names):
        comma = "," if index < len(block_names) - 1 else ""
        lines.append(f"                {java_identifier(name, 'ITEM')}{comma}")
    lines.extend([
        "        );",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    return "\n".join(lines)


def render_mod_entities(entity_names: list[str]) -> str:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.world.entity.EntityType;",
        "import net.minecraft.world.entity.MobCategory;",
        "import net.minecraftforge.registries.RegistryObject;",
        "import net.narutomod.entity.PortingDummyEntity;",
        "",
        "public final class ModEntityTypes {",
    ]
    for name in entity_names:
        lines.append(f"    public static final RegistryObject<EntityType<PortingDummyEntity>> {java_identifier(name)} = register(\"{name}\");")

    lines.extend([
        "",
        "    private ModEntityTypes() {",
        "    }",
        "",
        "    private static RegistryObject<EntityType<PortingDummyEntity>> register(String name) {",
        "        return ModRegistries.ENTITY_TYPES.register(name, () -> EntityType.Builder.<PortingDummyEntity>of(PortingDummyEntity::new, MobCategory.MISC)",
        "                .sized(0.6F, 1.8F)",
        "                .clientTrackingRange(8)",
        "                .updateInterval(3)",
        "                .build(name));",
        "    }",
        "",
        "    public static List<RegistryObject<EntityType<PortingDummyEntity>>> all() {",
        "        return List.of(",
    ])
    for index, name in enumerate(entity_names):
        comma = "," if index < len(entity_names) - 1 else ""
        lines.append(f"                {java_identifier(name)}{comma}")
    lines.extend([
        "        );",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    return "\n".join(lines)


def render_mod_effects(effect_names: list[str]) -> str:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.world.effect.MobEffect;",
        "import net.minecraft.world.effect.MobEffectCategory;",
        "import net.minecraftforge.registries.RegistryObject;",
        "import net.narutomod.effect.PortingPlaceholderMobEffect;",
        "",
        "public final class ModEffects {",
    ]
    for name in effect_names:
        lines.append(f"    public static final RegistryObject<MobEffect> {java_identifier(name)} = register(\"{name}\");")

    lines.extend([
        "",
        "    private ModEffects() {",
        "    }",
        "",
        "    private static RegistryObject<MobEffect> register(String name) {",
        "        return ModRegistries.MOB_EFFECTS.register(name, () -> new PortingPlaceholderMobEffect(MobEffectCategory.NEUTRAL, 0x7F7F7F));",
        "    }",
        "",
        "    public static List<RegistryObject<MobEffect>> all() {",
        "        return List.of(",
    ])
    for index, name in enumerate(effect_names):
        comma = "," if index < len(effect_names) - 1 else ""
        lines.append(f"                {java_identifier(name)}{comma}")
    lines.extend([
        "        );",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    return "\n".join(lines)


def render_mod_particle_types(particle_names_: list[str]) -> str:
    lines = [
        "package net.narutomod.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.core.particles.ParticleType;",
        "import net.minecraft.core.particles.SimpleParticleType;",
        "import net.minecraftforge.registries.RegistryObject;",
        "",
        "public final class ModParticleTypes {",
    ]
    for name in particle_names_:
        lines.append(f"    public static final RegistryObject<SimpleParticleType> {java_identifier(name)} = register(\"{name}\");")

    lines.extend([
        "",
        "    private ModParticleTypes() {",
        "    }",
        "",
        "    private static RegistryObject<SimpleParticleType> register(String name) {",
        "        return ModRegistries.PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));",
        "    }",
        "",
        "    public static List<RegistryObject<SimpleParticleType>> all() {",
        "        return List.of(",
    ])
    for index, name in enumerate(particle_names_):
        comma = "," if index < len(particle_names_) - 1 else ""
        lines.append(f"                {java_identifier(name)}{comma}")
    lines.extend([
        "        );",
        "    }",
        "",
        "    public static void touch() {",
        "    }",
        "}",
        "",
    ])
    return "\n".join(lines)


def main() -> None:
    rows = read_registry_rows()
    block_names = unique_names(rows, "block_or_block_item")
    item_names = sorted((set(unique_names(rows, "item")) | set(data_resource_item_references())) - set(block_names))
    entity_names = unique_names(rows, "entity_type")
    effect_names = potion_names()
    particle_names_ = particle_names()

    write(REGISTRY_PACKAGE / "ModItems.java", render_mod_items(item_names))
    write(REGISTRY_PACKAGE / "ModBlocks.java", render_mod_blocks(block_names))
    write(REGISTRY_PACKAGE / "ModEntityTypes.java", render_mod_entities(entity_names))
    write(REGISTRY_PACKAGE / "ModEffects.java", render_mod_effects(effect_names))
    write(REGISTRY_PACKAGE / "ModParticleTypes.java", render_mod_particle_types(particle_names_))
    write_generated_registry_csv(item_names, block_names, entity_names, effect_names, particle_names_)

    summary = {
        "items": len(item_names),
        "blocks": len(block_names),
        "entity_types": len(entity_names),
        "mob_effects": len(effect_names),
        "particle_types": len(particle_names_),
        "outputs": [
            "1.20.1/src/main/java/net/narutomod/registry/ModItems.java",
            "1.20.1/src/main/java/net/narutomod/registry/ModBlocks.java",
            "1.20.1/src/main/java/net/narutomod/registry/ModEntityTypes.java",
            "1.20.1/src/main/java/net/narutomod/registry/ModEffects.java",
            "1.20.1/src/main/java/net/narutomod/registry/ModParticleTypes.java",
            "audit/generated_registry_names.csv",
        ],
    }
    (AUDIT_ROOT / "generated_registry_scaffold_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
