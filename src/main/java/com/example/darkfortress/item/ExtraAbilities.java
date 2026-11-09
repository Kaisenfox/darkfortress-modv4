package com.example.darkfortress.item;

import com.example.darkfortress.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class ExtraAbilities {
    private ExtraAbilities() {}

    /** Soul Staff: a fast sorcery bolt that hits the first enemy in line. */
    public static boolean soulArrow(ServerWorld world, PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f).normalize();
        Vec3d maxEnd = start.add(dir.multiply(32));
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, maxEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d end = hit.getType() == HitResult.Type.MISS ? maxEnd : hit.getPos();

        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        Box sb = new Box(start, end).expand(1.0);
        for (Entity e : world.getOtherEntities(player, sb, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            var hp = e.getBoundingBox().expand(0.3).raycast(start, end);
            if (hp.isPresent()) {
                double d = hp.get().distanceTo(start);
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        if (best != null) {
            best.timeUntilRegen = 0;
            best.damage(world.getDamageSources().playerAttack(player), 14.0f);
            end = start.add(dir.multiply(bestDist));
        }
        double len = start.distanceTo(end);
        for (double d = 1.0; d < len; d += 0.35) {
            Vec3d p = start.add(dir.multiply(d));
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }

    /** Firebomb: thrown at where you look; burns everything near the impact. */
    public static boolean firebomb(ServerWorld world, PlayerEntity player) {
        Vec3d t = Fx.rayTarget(world, player, 22);
        if (t == null) {
            t = player.getEyePos().add(player.getRotationVec(1.0f).multiply(14));
        }
        final Vec3d c = t;
        world.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 0.8, 0.3, 0.8, 0.0);
        world.spawnParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 60, 1.2, 0.5, 1.2, 0.08);
        world.spawnParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 15, 1.0, 0.4, 1.0, 0.1);
        Box box = new Box(c.x - 3.5, c.y - 2, c.z - 3.5, c.x + 3.5, c.y + 3, c.z + 3.5);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            if (e.getPos().distanceTo(c) <= 3.5) {
                e.timeUntilRegen = 0;
                e.damage(world.getDamageSources().playerAttack(player), 9.0f);
                e.setOnFireFor(5);
            }
        }
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.0f, 1.6f);
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5f, 0.7f);
        return true;
    }

    /** Green Blossom: a burst of speed and a little regeneration. */
    public static boolean greenBlossom(ServerWorld world, PlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}
