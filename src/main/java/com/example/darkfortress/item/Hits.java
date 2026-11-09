package com.example.darkfortress.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** On-hit effects: each weapon class has a different attack style. */
public final class Hits {
    private Hits() {}

    private static DamageSource src(ServerWorld w, LivingEntity a) {
        return a instanceof PlayerEntity p ? w.getDamageSources().playerAttack(p) : w.getDamageSources().mobAttack(a);
    }

    /** Dagger: hitting an enemy from behind deals big bonus damage. */
    public static void backstab(ServerWorld w, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        Vec3d a = attacker.getRotationVec(1.0f);
        Vec3d t = target.getRotationVec(1.0f);
        Vec3d ah = new Vec3d(a.x, 0, a.z);
        Vec3d th = new Vec3d(t.x, 0, t.z);
        if (ah.lengthSquared() < 1.0E-4 || th.lengthSquared() < 1.0E-4) return;
        if (ah.normalize().dotProduct(th.normalize()) > 0.5) {
            target.timeUntilRegen = 0;
            target.damage(src(w, attacker), 8.0f);
            w.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ(), 20, 0.3, 0.4, 0.3, 0.3);
            w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ(), 6, 0.3, 0.3, 0.3, 0.2);
            if (attacker instanceof PlayerEntity p) {
                p.sendMessage(net.minecraft.text.Text.literal("Backstab!").formatted(net.minecraft.util.Formatting.RED), true);
            }
        }
    }

    /** Katana: causes bleeding (damage over time). */
    public static void bleed(ServerWorld w, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
        w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
    }

    /** Halberd: the swing also hits other enemies near the target. */
    public static void cleave(ServerWorld w, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        Box box = target.getBoundingBox().expand(2.5);
        for (Entity e : w.getOtherEntities(attacker, box, ent -> ent instanceof LivingEntity && ent.isAlive() && ent != target)) {
            e.timeUntilRegen = 0;
            e.damage(src(w, attacker), 4.0f);
        }
        w.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
    }

    /** Warhammer: slows and weakens the target. */
    public static void stun(ServerWorld w, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 3));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 50, 1));
        w.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() + 0.2, target.getZ(), 12, 0.3, 0.1, 0.3, 0.1);
    }

    /** Hatchet: breaks a blocking player's shield. */
    public static void shieldBreak(ServerWorld w, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        if (target instanceof PlayerEntity p && p.isBlocking()) {
            p.getItemCooldownManager().set(Items.SHIELD, 100);
            p.clearActiveItem();
            w.spawnParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        }
    }
}
