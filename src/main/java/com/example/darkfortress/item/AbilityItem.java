package com.example.darkfortress.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** A right-click item whose behavior is a function; optionally consumed on use. */
public class AbilityItem extends Item {

    @FunctionalInterface
    public interface Ability {
        boolean activate(ServerWorld world, PlayerEntity player);
    }

    private final Ability ability;
    private final int cooldownTicks;
    private final boolean consume;
    private final String[] lines;

    public AbilityItem(Settings settings, int cooldownTicks, boolean consume, Ability ability, String... lines) {
        super(settings);
        this.ability = ability;
        this.cooldownTicks = cooldownTicks;
        this.consume = consume;
        this.lines = lines;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.success(stack, true);
        }
        if (!ability.activate(serverWorld, player)) {
            return TypedActionResult.fail(stack);
        }
        if (cooldownTicks > 0) {
            player.getItemCooldownManager().set(this, cooldownTicks);
        }
        if (consume && !player.isCreative()) {
            stack.decrement(1);
        }
        return TypedActionResult.success(stack, false);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : lines) {
            tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
        }
    }
}
