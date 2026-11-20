package com.example.darkfortress;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Chest loot, tier 1 = early, 2 = mid, 3 = boss rooms and secrets.
 * Chests hold only a few items, and most of what's inside is junk, weak gear or minor healing.
 * Good gear is deliberately rare.
 */
public final class Loot {
    private record E(int weight, Function<Random, ItemStack> make) {}

    private Loot() {}

    private static E e(int w, Function<Random, ItemStack> f) {
        return new E(w, f);
    }

    private static ItemStack n(Item item, Random r, int min, int max) {
        return new ItemStack(item, min + r.nextInt(max - min + 1));
    }

    private static ItemStack of(Item item) {
        return new ItemStack(item);
    }

    private static ItemStack junk(Random r) {
        Item[] a = {ModItems.BONE_DUST, ModItems.RUSTED_TRINKET, ModItems.TATTERED_CLOTH, ModItems.BROKEN_CHARM,
                Items.STICK, Items.BONE, Items.ROTTEN_FLESH, Items.STRING};
        return n(a[r.nextInt(a.length)], r, 1, 2);
    }

    private static ItemStack healing(Random r) {
        Item[] a = {ModItems.MOSS_CLUMP, ModItems.BANDAGE, ModItems.STALE_ALE, ModItems.ROTTEN_BREAD};
        return n(a[r.nextInt(a.length)], r, 1, 2);
    }

    private static ItemStack weak(Random r) {
        Item[] a = {ModItems.RUSTY_SHORTSWORD, ModItems.BENT_CLUB, ModItems.CRACKED_MACE, ModItems.BLUNT_CLEAVER,
                ModItems.SPLINTERED_SPEAR, ModItems.ROTTED_FLAIL, ModItems.JESTERS_SABRE};
        return of(a[r.nextInt(a.length)]);
    }

    private static List<E> tier1() {
        return List.of(
                e(24, Loot::junk), e(16, Loot::weak), e(16, Loot::healing),
                e(8, r -> n(ModItems.SOUL_LOST_UNDEAD, r, 1, 3)),
                e(8, r -> n(Items.ARROW, r, 6, 12)),
                e(5, r -> n(ModItems.FIREBOMB, r, 1, 2)),
                e(4, r -> of(ModItems.GREEN_BLOSSOM)),
                e(4, r -> n(Items.IRON_INGOT, r, 1, 2)),
                e(10, r -> ItemStack.EMPTY),
                e(1, r -> of(ModItems.HOLLOW_DAGGER)), e(1, r -> of(ModItems.HATCHET)),
                e(1, r -> of(ModItems.WINGED_SPEAR)), e(1, r -> of(ModItems.LONGSWORD)),
                e(1, r -> of(ModItems.KNIGHT_HELM)), e(1, r -> of(ModItems.KNIGHT_BOOTS)));
    }

    private static List<E> tier2() {
        return List.of(
                e(14, Loot::junk), e(10, Loot::weak), e(14, Loot::healing),
                e(12, r -> n(ModItems.SOUL_LOST_UNDEAD, r, 3, 6)),
                e(8, r -> n(ModItems.FIREBOMB, r, 2, 3)),
                e(5, r -> n(ModItems.GREEN_BLOSSOM, r, 1, 2)),
                e(4, r -> n(Items.ARROW, r, 8, 16)),
                e(6, r -> ItemStack.EMPTY),
                e(2, r -> of(ModItems.UCHIGATANA)), e(2, r -> of(ModItems.HALBERD)),
                e(2, r -> of(ModItems.CLAYMORE)), e(2, r -> of(ModItems.WARHAMMER)),
                e(2, r -> of(ModItems.KNIGHT_ARMOR)), e(2, r -> of(ModItems.KNIGHT_LEGGINGS)),
                e(1, r -> of(ModItems.KNIGHT_HELM)), e(1, r -> of(ModItems.KNIGHT_BOOTS)),
                e(3, r -> of(ModItems.DIVINE_BLESSING)),
                e(2, r -> of(Items.CROSSBOW)), e(2, r -> of(Items.GOLDEN_APPLE)));
    }

    private static List<E> tier3() {
        return List.of(
                e(9, Loot::junk), e(9, Loot::healing), e(6, Loot::weak),
                e(14, r -> n(ModItems.SOUL_LOST_UNDEAD, r, 6, 12)),
                e(8, r -> n(ModItems.FIREBOMB, r, 3, 4)),
                e(9, r -> of(ModItems.DIVINE_BLESSING)),
                e(5, r -> of(ModItems.SOUL_STAFF)),
                e(4, r -> of(ModItems.UCHIGATANA)), e(4, r -> of(ModItems.HALBERD)),
                e(4, r -> of(ModItems.WARHAMMER)), e(3, r -> of(ModItems.CLAYMORE)),
                e(3, r -> of(ModItems.KNIGHT_ARMOR)),
                e(1, r -> of(Items.ENCHANTED_GOLDEN_APPLE)),
                e(4, r -> n(Items.GOLDEN_APPLE, r, 1, 2)),
                e(3, r -> n(Items.DIAMOND, r, 1, 3)),
                e(6, r -> n(ModItems.GREEN_BLOSSOM, r, 2, 3)),
                e(6, r -> ItemStack.EMPTY));
    }

    private static ItemStack pick(Random r, List<E> pool) {
        int total = 0;
        for (E e : pool) total += e.weight();
        int x = r.nextInt(total);
        for (E e : pool) {
            x -= e.weight();
            if (x < 0) return e.make().apply(r);
        }
        return ItemStack.EMPTY;
    }

    public static void fill(ChestBlockEntity chest, long seed, int tier) {
        Random r = new Random(seed);
        int count = tier <= 1 ? 1 + r.nextInt(3) : (tier == 2 ? 2 + r.nextInt(3) : 3 + r.nextInt(3));
        List<E> pool = tier <= 1 ? tier1() : (tier == 2 ? tier2() : tier3());
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, r);
        for (int i = 0; i < count; i++) {
            ItemStack s = pick(r, pool);
            if (!s.isEmpty()) chest.setStack(slots.get(i), s);
        }
    }
}
