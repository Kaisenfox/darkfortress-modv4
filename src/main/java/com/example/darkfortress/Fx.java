package com.example.darkfortress;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class Fx {
    private Fx() {}

    /** Point on a block the player is looking at, or null. */
    public static Vec3d rayTarget(ServerWorld world, PlayerEntity player, double range) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(range));
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getPos();
    }

    /** Expanding ground ring animated over several ticks. */
    public static void ring(ServerWorld world, Vec3d origin, double maxRadius, int ticks,
                            ParticleEffect main, ParticleEffect accent) {
        for (int i = 1; i <= ticks; i++) {
            final double r = maxRadius * i / ticks;
            Scheduler.later(i - 1, () -> {
                int points = (int) (r * 8) + 8;
                for (int k = 0; k < points; k++) {
                    double a = (Math.PI * 2 * k) / points;
                    double x = origin.x + Math.cos(a) * r;
                    double z = origin.z + Math.sin(a) * r;
                    world.spawnParticles(main, x, origin.y + 0.2, z, 1, 0, 0.02, 0, 0.02);
                    if (k % 3 == 0) {
                        world.spawnParticles(accent, x, origin.y + 0.1, z, 1, 0, 0.05, 0, 0.02);
                    }
                }
            });
        }
    }

    /** Ground-pound: hurts and launches players around the attacker. */
    public static void slam(ServerWorld world, LivingEntity attacker, double radius, float damage, double knock) {
        Vec3d c = attacker.getPos();
        Box box = new Box(c.x - radius, c.y - 1, c.z - radius, c.x + radius, c.y + 3, c.z + radius);
        for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, box, pl -> pl.isAlive() && !pl.isSpectator())) {
            double dx = p.getX() - c.x;
            double dz = p.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;
            p.damage(world.getDamageSources().mobAttack(attacker), damage);
            if (dist > 0.01) {
                p.addVelocity(dx / dist * knock, 0.5, dz / dist * knock);
                p.velocityModified = true;
            }
        }
        ring(world, c, radius, 6, ParticleTypes.CRIT, ParticleTypes.POOF);
        world.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 0.8, 0.1, 0.8, 0.0);
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 1.5f, 0.7f);
    }

    /** Big centered title text, like "YOU DIED". */
    public static void title(ServerPlayerEntity p, String text, Formatting color, String subtitle) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 25));
        if (subtitle != null) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(Formatting.GRAY)));
        }
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(text).formatted(color, Formatting.BOLD)));
    }

    /** Resets the durability of every damageable item the player carries (incl. armor, offhand, Estus). */
    public static void repairAll(PlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++) {
            net.minecraft.item.ItemStack s = p.getInventory().getStack(i);
            if (s.isDamageable() && s.getDamage() > 0) {
                s.setDamage(0);
            }
        }
    }
}
