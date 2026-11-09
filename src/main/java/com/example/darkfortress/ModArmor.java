package com.example.darkfortress;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;

/** Custom armor materials. Worn textures: textures/models/armor/<name>_layer_1.png and _layer_2.png */
public final class ModArmor {
    public static RegistryEntry<ArmorMaterial> KNIGHT;
    public static RegistryEntry<ArmorMaterial> BLACK_KNIGHT;

    private ModArmor() {}

    public static void init() {
        KNIGHT = register("knight", 2, 6, 5, 2, 12, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.0f, Items.IRON_INGOT);
        BLACK_KNIGHT = register("black_knight", 3, 8, 6, 3, 15, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, 3.0f, 0.1f, Items.NETHERITE_INGOT);
    }

    private static RegistryEntry<ArmorMaterial> register(String name, int helmet, int chest, int legs, int boots,
                                                         int enchantability, RegistryEntry<SoundEvent> sound,
                                                         float toughness, float knockbackResistance, Item repair) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, legs);
        defense.put(ArmorItem.Type.CHESTPLATE, chest);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chest);
        Identifier id = Identifier.of(DarkFortressMod.MOD_ID, name);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(id));
        ArmorMaterial material = new ArmorMaterial(defense, enchantability, sound,
                () -> Ingredient.ofItems(repair), layers, toughness, knockbackResistance);
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id, material);
    }
}
