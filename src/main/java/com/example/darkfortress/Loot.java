package com.example.darkfortress;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Random chest loot by tier: 1 = early, 2 = mid, 3 = boss rooms and secrets. */
public final class Loot {
    private Loot() {}

    public static void fill(ChestBlockEntity chest, long seed, int tier) {
        Random r = new Random(seed);
        int n = 3 + r.nextInt(3) + (tier >= 3 ? 2 : 0);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, r);
        for (int i = 0; i < n; i++) {
            chest.setStack(slots.get(i), roll(r, tier));
        }
    }

    private static ItemStack st(Item item, Random r, int min, int max) {
        return new ItemStack(item, min + r.nextInt(max - min + 1));
    }

    private static ItemStack roll(Random r, int tier) {
        int x = r.nextInt(100);
        if (tier <= 1) {
            if (x < 18) return st(ModItems.SOUL_LOST_UNDEAD, r, 3, 6);
            if (x < 28) return st(ModItems.GREEN_BLOSSOM, r, 1, 2);
            if (x < 36) return st(ModItems.FIREBOMB, r, 2, 4);
            if (x < 44) return new ItemStack(ModItems.HOLLOW_DAGGER);
            if (x < 51) return new ItemStack(ModItems.HATCHET);
            if (x < 58) return new ItemStack(ModItems.WINGED_SPEAR);
            if (x < 65) return new ItemStack(ModItems.LONGSWORD);
            if (x < 73) return st(Items.BREAD, r, 4, 8);
            if (x < 81) return st(Items.ARROW, r, 8, 16);
            if (x < 87) return new ItemStack(Items.BOW);
            if (x < 93) return st(Items.IRON_INGOT, r, 2, 4);
            if (x < 97) return new ItemStack(ModItems.KNIGHT_HELM);
            return new ItemStack(ModItems.KNIGHT_BOOTS);
        }
        if (tier == 2) {
            if (x < 14) return st(ModItems.SOUL_LOST_UNDEAD, r, 6, 10);
            if (x < 24) return st(ModItems.GREEN_BLOSSOM, r, 2, 3);
            if (x < 34) return st(ModItems.FIREBOMB, r, 3, 5);
            if (x < 42) return new ItemStack(ModItems.UCHIGATANA);
            if (x < 49) return new ItemStack(ModItems.HALBERD);
            if (x < 56) return new ItemStack(ModItems.CLAYMORE);
            if (x < 62) return new ItemStack(ModItems.WARHAMMER);
            if (x < 68) return new ItemStack(ModItems.DIVINE_BLESSING);
            if (x < 74) return new ItemStack(ModItems.KNIGHT_ARMOR);
            if (x < 80) return new ItemStack(ModItems.KNIGHT_LEGGINGS);
            if (x < 85) return new ItemStack(ModItems.KNIGHT_HELM);
            if (x < 89) return new ItemStack(ModItems.KNIGHT_BOOTS);
            if (x < 94) return new ItemStack(Items.CROSSBOW);
            return new ItemStack(Items.GOLDEN_APPLE);
        }
        if (x < 12) return st(ModItems.SOUL_LOST_UNDEAD, r, 10, 16);
        if (x < 22) return st(ModItems.DIVINE_BLESSING, r, 1, 2);
        if (x < 32) return st(ModItems.FIREBOMB, r, 4, 6);
        if (x < 40) return new ItemStack(ModItems.UCHIGATANA);
        if (x < 48) return new ItemStack(ModItems.HALBERD);
        if (x < 56) return new ItemStack(ModItems.WARHAMMER);
        if (x < 62) return new ItemStack(ModItems.CLAYMORE);
        if (x < 68) return new ItemStack(ModItems.SOUL_STAFF);
        if (x < 74) return new ItemStack(ModItems.KNIGHT_ARMOR);
        if (x < 80) return st(Items.GOLDEN_APPLE, r, 1, 2);
        if (x < 86) return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        if (x < 93) return st(Items.DIAMOND, r, 2, 4);
        return st(ModItems.GREEN_BLOSSOM, r, 3, 4);
    }
}
