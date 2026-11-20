package com.example.darkfortress;

import com.example.darkfortress.item.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    // consumables / currency
    public static Item ESTUS_FLASK, SOUL_LOST_UNDEAD, SOUL_GREAT_DEMON, FORTRESS_KEY, DIVINE_BLESSING, GREEN_BLOSSOM, FIREBOMB;
    // swords
    public static Item BROKEN_SWORD, LONGSWORD, CLAYMORE, BLACK_KNIGHT_GREATSWORD, MOONLIGHT_GREATSWORD;
    // other weapon classes
    public static Item HOLLOW_DAGGER, HATCHET, WINGED_SPEAR, UCHIGATANA, HALBERD, WARHAMMER;
    // magic
    public static Item LIGHTNING_SPEAR, PYROMANCY_FLAME, SOUL_STAFF;
    // cheap loot: healing, junk, weak and silly weapons
    public static Item MOSS_CLUMP, BANDAGE, STALE_ALE, ROTTEN_BREAD, BONE_DUST, RUSTED_TRINKET, TATTERED_CLOTH, BROKEN_CHARM;
    public static Item RUSTY_SHORTSWORD, BENT_CLUB, CRACKED_MACE, BLUNT_CLEAVER, SPLINTERED_SPEAR, ROTTED_FLAIL, JESTERS_SABRE;
    // testing
    public static Item TEST_BLADE;
    // accessories
    public static Item RING_OF_FAVOR;
    // armor
    public static Item KNIGHT_HELM, KNIGHT_ARMOR, KNIGHT_LEGGINGS, KNIGHT_BOOTS;
    public static Item BLACK_KNIGHT_HELM, BLACK_KNIGHT_ARMOR, BLACK_KNIGHT_LEGGINGS, BLACK_KNIGHT_BOOTS;

    private ModItems() {}

    private static Item reg(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(DarkFortressMod.MOD_ID, id), item);
    }

    private static Item.Settings gear() {
        return new Item.Settings().maxCount(1).rarity(Rarity.RARE).fireproof();
    }

    private static Item armor(String id, net.minecraft.registry.entry.RegistryEntry<net.minecraft.item.ArmorMaterial> mat,
                              ArmorItem.Type type, int mult, Rarity rarity) {
        return reg(id, new ArmorItem(mat, type, new Item.Settings().maxDamage(type.getMaxDamage(mult)).rarity(rarity)));
    }

    public static void init() {
        ESTUS_FLASK = reg("estus_flask", new EstusFlaskItem(new Item.Settings().maxDamage(5).rarity(Rarity.UNCOMMON)));
        SOUL_LOST_UNDEAD = reg("soul_of_a_lost_undead", new SoulItem(new Item.Settings().maxCount(64), 30, false));
        SOUL_GREAT_DEMON = reg("soul_of_a_great_demon", new SoulItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC), 1500, true));
        FORTRESS_KEY = reg("fortress_key", new Item(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

        DIVINE_BLESSING = reg("divine_blessing", new AbilityItem(
                new Item.Settings().maxCount(4).rarity(Rarity.EPIC), 0, true, Abilities::blessing,
                "Right-click: fully heal and cleanse", "Consumed on use"));
        GREEN_BLOSSOM = reg("green_blossom", new AbilityItem(
                new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON), 0, true, ExtraAbilities::greenBlossom,
                "Right-click: Speed II for 60 seconds", "and brief regeneration"));
        FIREBOMB = reg("firebomb", new AbilityItem(
                new Item.Settings().maxCount(16), 15, true, ExtraAbilities::firebomb,
                "Right-click: hurl a firebomb (9 damage + burning)", "Range: 22 blocks"));

        // ----- swords -----
        BROKEN_SWORD = reg("broken_straight_sword", new SwordItem(ToolMaterials.STONE, new Item.Settings()
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.STONE, 3, -2.4f))));
        LONGSWORD = reg("longsword", new SwordItem(ToolMaterials.IRON, new Item.Settings().rarity(Rarity.UNCOMMON)
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.IRON, 3, -2.4f))));
        CLAYMORE = reg("claymore", new HeavySwordItem(ToolMaterials.IRON, new Item.Settings().rarity(Rarity.UNCOMMON)
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.IRON, 6, -2.9f))));
        BLACK_KNIGHT_GREATSWORD = reg("black_knight_greatsword", new HeavySwordItem(ToolMaterials.NETHERITE, gear()
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 9, -3.0f))));
        MOONLIGHT_GREATSWORD = reg("moonlight_greatsword", new MoonlightGreatswordItem(ToolMaterials.NETHERITE,
                new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()
                        .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 7, -2.8f))));

        // ----- other weapon classes -----
        HOLLOW_DAGGER = reg("hollow_dagger", new WeaponItem(WeaponItem.stats(160, 2.0, -1.5, 0, Rarity.COMMON),
                Hits::backstab, "Dagger: very fast", "Backstab: big bonus damage from behind"));
        HATCHET = reg("hatchet", new WeaponItem(WeaponItem.stats(220, 4.0, -2.7, 0, Rarity.COMMON),
                Hits::shieldBreak, "Axe: breaks shields"));
        WINGED_SPEAR = reg("winged_spear", new WeaponItem(WeaponItem.stats(250, 4.0, -2.6, 1.5, Rarity.UNCOMMON),
                null, "Spear: long reach"));
        UCHIGATANA = reg("uchigatana", new WeaponItem(WeaponItem.stats(300, 4.5, -2.0, 0, Rarity.UNCOMMON),
                Hits::bleed, "Katana: fast", "Hits cause bleeding"));
        HALBERD = reg("halberd", new WeaponItem(WeaponItem.stats(350, 7.0, -3.0, 1.0, Rarity.RARE),
                Hits::cleave, "Polearm: long reach", "Hits cleave nearby enemies"));
        WARHAMMER = reg("warhammer", new WeaponItem(WeaponItem.stats(400, 9.0, -3.2, 0, Rarity.RARE),
                Hits::stun, "Hammer: very heavy", "Hits stun and slow"));

        TEST_BLADE = reg("test_blade", new TestBladeItem(WeaponItem.stats(100000, 1000.0, -1.0, 4.0, Rarity.EPIC)));

        // ----- cheap loot -----
        MOSS_CLUMP = reg("moss_clump", new AbilityItem(new Item.Settings().maxCount(8), 20, true, false, ExtraAbilities::mossClump,
                "Restores 2 hearts", "Tastes of damp earth"));
        BANDAGE = reg("bandage", new AbilityItem(new Item.Settings().maxCount(8), 20, true, false, ExtraAbilities::bandage,
                "Slowly restores health", "Already seen some use"));
        STALE_ALE = reg("stale_ale", new AbilityItem(new Item.Settings().maxCount(4), 20, true, false, ExtraAbilities::staleAle,
                "Restores 1.5 hearts", "You will regret it"));
        ROTTEN_BREAD = reg("rotten_bread", new AbilityItem(new Item.Settings().maxCount(8), 20, true, false, ExtraAbilities::rottenBread,
                "Restores 1 heart", "Makes you hungry"));
        BONE_DUST = reg("bone_dust", new FlavorItem(new Item.Settings().maxCount(64), "Fine white dust.", "Useless."));
        RUSTED_TRINKET = reg("rusted_trinket", new FlavorItem(new Item.Settings().maxCount(16), "Someone once loved this.", "Useless."));
        TATTERED_CLOTH = reg("tattered_cloth", new FlavorItem(new Item.Settings().maxCount(64), "A scrap of something.", "Useless."));
        BROKEN_CHARM = reg("broken_charm", new FlavorItem(new Item.Settings().maxCount(16), "It didn't work.", "Useless."));

        RUSTY_SHORTSWORD = reg("rusty_shortsword", new WeaponItem(WeaponItem.stats(90, 2.5, -2.2, 0, Rarity.COMMON), null,
                "Barely a weapon."));
        BENT_CLUB = reg("bent_club", new WeaponItem(WeaponItem.stats(120, 3.0, -2.8, 0, Rarity.COMMON), null,
                "Hit them until it stops."));
        CRACKED_MACE = reg("cracked_mace", new WeaponItem(WeaponItem.stats(140, 4.5, -3.0, 0, Rarity.COMMON), null,
                "Heavy and about to fall apart."));
        BLUNT_CLEAVER = reg("blunt_cleaver", new WeaponItem(WeaponItem.stats(100, 3.5, -2.4, 0, Rarity.COMMON), null,
                "Meant for meat."));
        SPLINTERED_SPEAR = reg("splintered_spear", new WeaponItem(WeaponItem.stats(80, 2.5, -2.6, 1.0, Rarity.COMMON), null,
                "Reach, at least."));
        ROTTED_FLAIL = reg("rotted_flail", new WeaponItem(WeaponItem.stats(130, 3.5, -2.9, 0, Rarity.COMMON), Hits::rot,
                "Its rot seeps into wounds"));
        JESTERS_SABRE = reg("jesters_sabre", new WeaponItem(WeaponItem.stats(160, 3.0, -1.8, 0, Rarity.UNCOMMON), Hits::jester,
                "Hits cause a random effect", "Sometimes it helps them"));

        // ----- magic -----
        LIGHTNING_SPEAR = reg("lightning_spear", new AbilityItem(
                new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof(), 100, false, Abilities::lightning,
                "Right-click: call lightning on your target", "Range: 48 blocks"));
        PYROMANCY_FLAME = reg("pyromancy_flame", new AbilityItem(
                new Item.Settings().maxCount(1).rarity(Rarity.RARE).fireproof(), 30, false, Abilities::pyromancy,
                "Right-click: cone of fire"));
        SOUL_STAFF = reg("soul_staff", new AbilityItem(
                new Item.Settings().maxCount(1).rarity(Rarity.RARE), 25, false, ExtraAbilities::soulArrow,
                "Right-click: fire a soul arrow (14 damage)", "Range: 32 blocks"));

        RING_OF_FAVOR = reg("ring_of_favor", new RingOfFavorItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

        // ----- armor (real worn textures) -----
        KNIGHT_HELM = armor("knight_helm", ModArmor.KNIGHT, ArmorItem.Type.HELMET, 15, Rarity.UNCOMMON);
        KNIGHT_ARMOR = armor("knight_armor", ModArmor.KNIGHT, ArmorItem.Type.CHESTPLATE, 15, Rarity.UNCOMMON);
        KNIGHT_LEGGINGS = armor("knight_leggings", ModArmor.KNIGHT, ArmorItem.Type.LEGGINGS, 15, Rarity.UNCOMMON);
        KNIGHT_BOOTS = armor("knight_boots", ModArmor.KNIGHT, ArmorItem.Type.BOOTS, 15, Rarity.UNCOMMON);
        BLACK_KNIGHT_HELM = armor("black_knight_helm", ModArmor.BLACK_KNIGHT, ArmorItem.Type.HELMET, 37, Rarity.RARE);
        BLACK_KNIGHT_ARMOR = armor("black_knight_armor", ModArmor.BLACK_KNIGHT, ArmorItem.Type.CHESTPLATE, 37, Rarity.RARE);
        BLACK_KNIGHT_LEGGINGS = armor("black_knight_leggings", ModArmor.BLACK_KNIGHT, ArmorItem.Type.LEGGINGS, 37, Rarity.RARE);
        BLACK_KNIGHT_BOOTS = armor("black_knight_boots", ModArmor.BLACK_KNIGHT, ArmorItem.Type.BOOTS, 37, Rarity.RARE);
    }
}
