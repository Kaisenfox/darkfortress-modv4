package com.example.darkfortress.block;

import com.example.darkfortress.Ashen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Step on it to travel between the fortress and the Ashen Realm. */
public class AshenPortalBlock extends Block {
    public AshenPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world instanceof ServerWorld sw && entity instanceof ServerPlayerEntity p) {
            Ashen.usePad(sw, p);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 2; i++) {
            world.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble(), pos.getZ() + random.nextDouble(),
                    0.0, 0.3, 0.0);
        }
    }
}
