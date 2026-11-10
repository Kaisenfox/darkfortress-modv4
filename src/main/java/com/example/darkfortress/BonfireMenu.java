package com.example.darkfortress;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The bonfire menu: a vanilla 9x3 chest GUI driven entirely by the server.
 * Click a stat to spend XP levels on it. Items can never be taken out.
 */
public class BonfireMenu extends GenericContainerScreenHandler {
    private static final int[] STAT_SLOTS = {10, 11, 12, 13, 14};
    private static final Item[] ICONS = {Items.GOLDEN_APPLE, Items.BLAZE_POWDER, Items.FEATHER, Items.SHIELD, Items.SUGAR};
    private static final Formatting[] COLORS = {Formatting.RED, Formatting.GOLD, Formatting.AQUA, Formatting.GRAY, Formatting.GREEN};

    private final SimpleInventory menuInv;
    private final ServerPlayerEntity owner;

    private BonfireMenu(int syncId, PlayerInventory playerInv, SimpleInventory inv, ServerPlayerEntity owner) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3);
        this.menuInv = inv;
        this.owner = owner;
        refresh();
    }

    public static void open(ServerPlayerEntity p) {
        SimpleInventory inv = new SimpleInventory(27);
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, player) -> new BonfireMenu(syncId, playerInv, inv, p),
                Text.literal("Bonfire - Strengthen Yourself")));
    }

    private static Text t(String s, Formatting f) {
        return Text.literal(s).styled(st -> st.withItalic(false).withColor(f));
    }

    private static ItemStack icon(Item item, Text name, List<Text> lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, name);
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return s;
    }

    private void refresh() {
        for (int i = 0; i < 27; i++) {
            menuInv.setStack(i, icon(Items.GRAY_STAINED_GLASS_PANE, t(" ", Formatting.GRAY), new ArrayList<>()));
        }
        List<Text> info = new ArrayList<>();
        info.add(t("Soul Level: " + Stats.soulLevel(owner), Formatting.AQUA));
        info.add(t("XP levels you can spend: " + owner.experienceLevel, Formatting.GREEN));
        info.add(t("Slay enemies and absorb their souls for XP.", Formatting.DARK_GRAY));
        menuInv.setStack(4, icon(Items.NETHER_STAR, t("Your Soul", Formatting.AQUA), info));

        for (int i = 0; i < Stats.KEYS.length; i++) {
            String key = Stats.KEYS[i];
            int lvl = Stats.get(owner, key);
            int max = Stats.max(key);
            List<Text> lore = new ArrayList<>();
            lore.add(t(Stats.EFFECTS[i], Formatting.GRAY));
            lore.add(t("Level " + lvl + " / " + max, Formatting.WHITE));
            if (lvl >= max) {
                lore.add(t("MAXIMUM", Formatting.GOLD));
            } else {
                int cost = Stats.cost(lvl);
                boolean ok = owner.experienceLevel >= cost;
                lore.add(t("Cost: " + cost + " XP levels", ok ? Formatting.GREEN : Formatting.RED));
                lore.add(t(ok ? "Click to upgrade" : "Not enough XP", ok ? Formatting.YELLOW : Formatting.DARK_RED));
            }
            menuInv.setStack(STAT_SLOTS[i], icon(ICONS[i], t(Stats.NAMES[i], COLORS[i]), lore));
        }

        List<Text> rest = new ArrayList<>();
        rest.add(t("Respawn point set here.", Formatting.GRAY));
        rest.add(t("Health restored, gear repaired,", Formatting.GRAY));
        rest.add(t("Estus refilled. Enemies have returned.", Formatting.GRAY));
        menuInv.setStack(16, icon(Items.CAMPFIRE, t("Rested", Formatting.GOLD), rest));

        menuInv.setStack(22, icon(Items.BARRIER, t("Close", Formatting.RED), new ArrayList<>()));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Never let anything be picked up or moved; just treat clicks as button presses.
        if (slotIndex >= 0 && slotIndex < 27
                && (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE)) {
            if (slotIndex == 22) {
                owner.closeHandledScreen();
                return;
            }
            for (int i = 0; i < STAT_SLOTS.length; i++) {
                if (STAT_SLOTS[i] == slotIndex) {
                    upgrade(i);
                    return;
                }
            }
        }
    }

    private void upgrade(int idx) {
        String key = Stats.KEYS[idx];
        int lvl = Stats.get(owner, key);
        if (lvl >= Stats.max(key)) {
            owner.sendMessage(Text.literal(Stats.NAMES[idx] + " is already at maximum.").formatted(Formatting.GOLD), true);
            return;
        }
        int cost = Stats.cost(lvl);
        if (owner.experienceLevel < cost) {
            owner.sendMessage(Text.literal("You need " + cost + " XP levels (you have " + owner.experienceLevel + ").")
                    .formatted(Formatting.RED), true);
            return;
        }
        owner.addExperienceLevels(-cost);
        Stats.set(owner, key, lvl + 1);
        Stats.apply(owner);
        if (key.equals("vit")) owner.heal(2.0f);
        if (owner.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, owner.getX(), owner.getY() + 1.0, owner.getZ(), 12, 0.4, 0.6, 0.4, 0.05);
            sw.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
        owner.sendMessage(Text.literal(Stats.NAMES[idx] + " raised to " + (lvl + 1)).formatted(Formatting.GREEN), true);
        refresh();
    }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
