package com.example.darkfortress.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
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

/** Looks like stone brick. Right-click (or just mine it) and it dissolves. */
public class IllusoryWallBlock extends Block {
    public IllusoryWallBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld sw) {
            sw.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.5f, 0.5f);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            queue.add(pos);
            seen.add(pos);
            while (!queue.isEmpty() && seen.size() < 200) {
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
            player.sendMessage(Text.literal("The wall was an illusion.").formatted(Formatting.AQUA), true);
        }
        return ActionResult.success(world.isClient());
    }
}
