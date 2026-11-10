package com.example.darkfortress.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The fortress gate: unbreakable until the Demon of the Keep falls. */
public class SealedGateBlock extends Block {
    public SealedGateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.literal("The gate is sealed by a dark power. Something must fall before it opens.")
                    .formatted(Formatting.DARK_RED), true);
        }
        return ActionResult.success(world.isClient());
    }
}
