package com.example.darkfortress;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

/**
 * Souls-style character stats, upgraded at bonfires by spending XP levels.
 * Levels are stored as player command tags ("df_vit_3"), which persist across death and logout.
 */
public final class Stats {
    public static final String[] KEYS = {"vit", "str", "dex", "end", "agi"};
    public static final String[] NAMES = {"Vitality", "Strength", "Dexterity", "Endurance", "Agility"};
    public static final String[] EFFECTS = {
            "+1 heart per level", "+0.6 attack damage per level", "+0.08 attack speed per level",
            "+0.5 armor per level", "+1.5% movement speed per level"};

    private Stats() {}

    public static int max(String key) {
        return key.equals("agi") ? 10 : 20;
    }

    /** XP levels needed to raise a stat from the given level. */
    public static int cost(int level) {
        return 2 + level + level / 3;
    }

    public static int get(PlayerEntity p, String key) {
        String pre = "df_" + key + "_";
        for (String t : p.getCommandTags()) {
            if (t.startsWith(pre)) {
                try {
                    return Integer.parseInt(t.substring(pre.length()));
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        return 0;
    }

    public static void set(PlayerEntity p, String key, int value) {
        String pre = "df_" + key + "_";
        for (String t : new ArrayList<>(p.getCommandTags())) {
            if (t.startsWith(pre)) p.removeCommandTag(t);
        }
        if (value > 0) p.addCommandTag(pre + value);
    }

    public static int soulLevel(PlayerEntity p) {
        int sum = 0;
        for (String k : KEYS) sum += get(p, k);
        return sum;
    }

    /** (Re)applies every stat as an attribute modifier. Safe to call any time. */
    public static void apply(PlayerEntity p) {
        mod(p, EntityAttributes.GENERIC_MAX_HEALTH, "stat_vit", get(p, "vit") * 2.0, EntityAttributeModifier.Operation.ADD_VALUE);
        mod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, "stat_str", get(p, "str") * 0.6, EntityAttributeModifier.Operation.ADD_VALUE);
        mod(p, EntityAttributes.GENERIC_ATTACK_SPEED, "stat_dex", get(p, "dex") * 0.08, EntityAttributeModifier.Operation.ADD_VALUE);
        mod(p, EntityAttributes.GENERIC_ARMOR, "stat_end", get(p, "end") * 0.5, EntityAttributeModifier.Operation.ADD_VALUE);
        mod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, "stat_agi", get(p, "agi") * 0.015, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void mod(PlayerEntity p, RegistryEntry<EntityAttribute> attr, String id, double value,
                            EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        Identifier mid = Identifier.of(DarkFortressMod.MOD_ID, id);
        inst.removeModifier(mid);
        if (value != 0) {
            inst.addPersistentModifier(new EntityAttributeModifier(mid, value, op));
        }
    }
}
