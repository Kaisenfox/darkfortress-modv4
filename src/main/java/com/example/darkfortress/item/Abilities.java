package com.example.darkfortress.item;

import com.example.darkfortress.Fx;
import com.example.darkfortress.Scheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class Abilities {
    private Abilities() {}

    /** Moonlight Greatsword: piercing beam of moonlight. */
    public static boolean moonbeam(ServerWorld world, PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f).normalize();
        Vec3d maxEnd = start.add(dir.multiply(40));
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, maxEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d end = hit.getType() == HitResult.Type.MISS ? maxEnd : hit.getPos();

        Box searchBox = new Box(start, end).expand(1.5);
        for (Entity e : world.getOtherEntities(player, searchBox, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            if (e.getBoundingBox().expand(0.4).raycast(start, end).isPresent()) {
                e.timeUntilRegen = 0;
                e.damage(world.getDamageSources().playerAttack(player), 18.0f);
            }
        }
        double len = start.distanceTo(end);
        for (double d = 1.0; d < len; d += 0.4) {
            Vec3d p = start.add(dir.multiply(d));
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.01);
        }
        world.spawnParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.6f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 2.0f);
        return true;
    }

    /** Lightning Spear: three bolts where you look. */
    public static boolean lightning(ServerWorld world, PlayerEntity player) {
        Vec3d t = Fx.rayTarget(world, player, 48);
        if (t == null) {
            player.sendMessage(Text.literal("No target in range"), true);
            return false;
        }
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Scheduler.later(i * 4, () -> {
                double ox = idx == 0 ? 0 : (world.random.nextDouble() - 0.5) * 4;
                double oz = idx == 0 ? 0 : (world.random.nextDouble() - 0.5) * 4;
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
                if (bolt != null) {
                    bolt.refreshPositionAfterTeleport(t.x + ox, t.y, t.z + oz);
                    world.spawnEntity(bolt);
                }
            });
        }
        Fx.ring(world, t, 4, 5, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD);
        return true;
    }

    /** Pyromancy Flame: cone of fire. */
    public static boolean pyromancy(ServerWorld world, PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d mouth = player.getEyePos().add(look.multiply(0.8)).add(0, -0.3, 0);

        Box box = player.getBoundingBox().expand(10);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            Vec3d rel = e.getPos().add(0, e.getHeight() / 2, 0).subtract(mouth);
            double d = rel.length();
            if (d < 0.1 || d > 9.0) continue;
            if (rel.normalize().dotProduct(look) < 0.65) continue;
            e.timeUntilRegen = 0;
            e.damage(world.getDamageSources().playerAttack(player), 7.0f);
            e.setOnFireFor(6);
        }
        for (int i = 0; i < 60; i++) {
            double d = 0.5 + world.random.nextDouble() * 8.5;
            Vec3d p = mouth.add(look.multiply(d)).add(
                    (world.random.nextDouble() - 0.5) * d * 0.35,
                    (world.random.nextDouble() - 0.5) * d * 0.35,
                    (world.random.nextDouble() - 0.5) * d * 0.35);
            world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
            if (i % 4 == 0) world.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    /** Divine Blessing: full heal, cleanse, absorption. */
    public static boolean blessing(ServerWorld world, PlayerEntity player) {
        if (player.getHealth() >= player.getMaxHealth()) {
            player.sendMessage(Text.literal("Already at full health"), true);
            return false;
        }
        player.heal(player.getMaxHealth());
        player.removeStatusEffect(StatusEffects.POISON);
        player.removeStatusEffect(StatusEffects.WITHER);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.NAUSEA);
        player.removeStatusEffect(StatusEffects.HUNGER);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 2));
        world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 15, 0.5, 0.6, 0.5, 0.1);
        Fx.ring(world, player.getPos(), 3.5, 6, ParticleTypes.END_ROD, ParticleTypes.HAPPY_VILLAGER);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.3f);
        return true;
    }
}
