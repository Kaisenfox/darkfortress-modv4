package com.example.darkfortress.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** 5 charges (shown as the durability bar). Heals 8 hearts. Refilled at bonfires. */
public class EstusFlaskItem extends Item {
    public EstusFlaskItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getDamage() >= stack.getMaxDamage()) {
            if (!world.isClient) {
                player.sendMessage(Text.literal("The flask is empty. Rest at a bonfire.").formatted(Formatting.GRAY), true);
            }
            return TypedActionResult.fail(stack);
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return TypedActionResult.fail(stack);
        }
        player.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world instanceof ServerWorld sw) {
            user.heal(16.0f);
            stack.setDamage(stack.getDamage() + 1);
            sw.spawnParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
            sw.spawnParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 1.0, user.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            sw.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 0.9f);
        }
        return stack;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int left = stack.getMaxDamage() - stack.getDamage();
        tooltip.add(Text.literal("Charges: " + left + "/" + stack.getMaxDamage()).formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Restores 8 hearts. Refills at bonfires.").formatted(Formatting.GRAY));
    }
}
