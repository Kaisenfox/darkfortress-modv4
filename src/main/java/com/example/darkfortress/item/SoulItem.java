package com.example.darkfortress.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Right-click to absorb into experience. Sneak + right-click absorbs the whole stack. */
public class SoulItem extends Item {
    private final int xp;
    private final boolean glint;

    public SoulItem(Settings settings, int xp, boolean glint) {
        super(settings);
        this.xp = xp;
        this.glint = glint;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            int n = player.isSneaking() ? stack.getCount() : 1;
            player.addExperience(xp * n);
            stack.decrement(n);
            player.sendMessage(Text.literal("+" + (xp * n) + " XP").formatted(Formatting.AQUA), true);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.7f);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return glint;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Right-click: absorb (" + xp + " XP)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Sneak: absorb the whole stack").formatted(Formatting.DARK_GRAY));
    }
}
