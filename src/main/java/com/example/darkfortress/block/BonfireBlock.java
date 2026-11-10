package com.example.darkfortress.block;

import com.example.darkfortress.BonfireMenu;
import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.ModItems;
import com.example.darkfortress.Stats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Right-click to rest: sets respawn, heals, refills Estus, respawns the fortress enemies. */
public class BonfireBlock extends Block {
    public BonfireBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld sw && player instanceof ServerPlayerEntity sp) {
            sp.setSpawnPoint(sw.getRegistryKey(), pos.south(), 0.0f, true, false);
            Stats.apply(sp);
            sp.heal(sp.getMaxHealth());
            sp.getHungerManager().add(20, 1.0f);
            sp.clearStatusEffects();
            Fx.repairAll(sp);
            for (int i = 0; i < sp.getInventory().size(); i++) {
                ItemStack s = sp.getInventory().getStack(i);
                if (s.isOf(ModItems.ESTUS_FLASK)) {
                    s.setDamage(0);
                }
            }
            sw.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.3, 0.5, 0.3, 0.05);
            sw.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
            sw.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.7f, 0.6f);
            Fx.title(sp, "BONFIRE LIT", Formatting.GOLD, "Rested. Gear repaired. Enemies have returned.");
            sp.sendMessage(Text.literal("Estus refilled. Respawn point set.").formatted(Formatting.GRAY), true);
            if (sw.getRegistryKey() == World.OVERWORLD) {
                Fortress.respawnMobs(sw);
            }
            BonfireMenu.open(sp);
        }
        return ActionResult.success(world.isClient());
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        world.addParticle(ParticleTypes.FLAME, x, pos.getY() + 1.05, z, 0.0, 0.03, 0.0);
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.LAVA, x, pos.getY() + 1.05, z, 0.0, 0.0, 0.0);
        }
    }
}
