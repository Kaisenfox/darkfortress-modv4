package com.example.darkfortress.block;

import com.example.darkfortress.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Locked gate. Right-click with a Fortress Key to open the whole connected gate. */
public class KeyGateBlock extends Block {
    public KeyGateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack held = player.getMainHandStack();
        if (!held.isOf(ModItems.FORTRESS_KEY)) {
            if (!world.isClient) {
                player.sendMessage(Text.literal("The gate is locked. A key is required.").formatted(Formatting.GRAY), true);
            }
            return ActionResult.success(world.isClient());
        }
        if (world instanceof ServerWorld sw) {
            if (!player.isCreative()) held.decrement(1);
            sw.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.5f, 0.6f);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            queue.add(pos);
            seen.add(pos);
            while (!queue.isEmpty() && seen.size() < 400) {
                BlockPos p = queue.poll();
                for (Direction d : Direction.values()) {
                    BlockPos n = p.offset(d);
                    if (!seen.contains(n) && sw.getBlockState(n).isOf(this)) {
                        seen.add(n);
                        queue.add(n);
                    }
                }
                sw.breakBlock(p, false);
            }
            player.sendMessage(Text.literal("The gate swings open.").formatted(Formatting.GOLD), true);
        }
        return ActionResult.success(world.isClient());
    }
}
