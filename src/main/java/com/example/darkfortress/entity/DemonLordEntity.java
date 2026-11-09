package com.example.darkfortress.entity;

import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.Scheduler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Final boss. Phase 1: melee, ground slam, fire breath, leap.
 * Phase 2 (below 50% HP): faster, harder, summons hollows, rains fire pillars.
 */
public class DemonLordEntity extends DSMob {
    private final ServerBossBar bossBar = new ServerBossBar(
            Text.literal("Demon of the Keep"), BossBar.Color.RED, BossBar.Style.PROGRESS);

    private boolean engaged = false;
    private boolean phase2 = false;
    private boolean airborne = false;
    private int airTicks = 0;
    private int slamTimer = -1;
    private int fireTicks = 0;
    private int slamCooldown = 70;
    private int fireCooldown = 150;
    private int leapCooldown = 100;
    private int pillarCooldown = 60;

    public DemonLordEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world);
        this.experiencePoints = 500;
        this.bossBar.setVisible(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 450.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 18.0)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    public void clearBar() {
        bossBar.clearPlayers();
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        if (!engaged) {
            engaged = true;
            bossBar.setVisible(true);
            Fortress.sealArena(sw);
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 3.0f, 0.5f);
        }
        if (!phase2 && this.getHealth() < this.getMaxHealth() * 0.5f) {
            enterPhase2(sw);
        }

        // Mid-leap: wait for landing, then slam.
        if (airborne) {
            airTicks++;
            if (airTicks > 5 && this.isOnGround()) {
                airborne = false;
                airTicks = 0;
                Fx.slam(sw, this, 6.5, phase2 ? 22.0f : 16.0f, 1.3);
            } else if (airTicks > 80) {
                airborne = false;
                airTicks = 0;
            }
            return;
        }

        if (slamCooldown > 0) slamCooldown--;
        if (fireCooldown > 0) fireCooldown--;
        if (leapCooldown > 0) leapCooldown--;
        if (pillarCooldown > 0) pillarCooldown--;

        double distSq = this.squaredDistanceTo(target);

        // --- Ground slam (telegraphed) ---
        if (slamTimer > 0) {
            slamTimer--;
            this.getNavigation().stop();
            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + getHeight() + 0.2, getZ(), 8, 0.9, 0.3, 0.9, 0.1);
            sw.spawnParticles(ParticleTypes.FLAME, getX(), getY() + getHeight() * 0.9, getZ(), 4, 0.6, 0.3, 0.6, 0.02);
            if (slamTimer == 0) {
                Fx.slam(sw, this, 7.0, phase2 ? 24.0f : 18.0f, 1.4);
                slamTimer = -1;
            }
            return;
        }

        // --- Fire breath ---
        if (fireTicks > 0) {
            fireTicks--;
            this.getNavigation().stop();
            this.getLookControl().lookAt(target, 40.0f, 40.0f);
            Vec3d toTarget = target.getPos().subtract(this.getPos());
            Vec3d dir = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
            Vec3d mouth = this.getPos().add(0, this.getHeight() * 0.75, 0).add(dir.multiply(1.8));
            for (int i = 0; i < 8; i++) {
                double d = 1.0 + sw.random.nextDouble() * 11.0;
                Vec3d p = mouth.add(dir.multiply(d)).add(
                        (sw.random.nextDouble() - 0.5) * d * 0.3,
                        (sw.random.nextDouble() - 0.5) * d * 0.2 - d * 0.08,
                        (sw.random.nextDouble() - 0.5) * d * 0.3);
                sw.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
                if (i % 3 == 0) sw.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
            }
            if (fireTicks < 30 && fireTicks % 5 == 0) {
                Box box = new Box(mouth, mouth.add(dir.multiply(13))).expand(3);
                for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, box, pl -> pl.isAlive() && !pl.isSpectator())) {
                    Vec3d rel = p.getPos().add(0, 1, 0).subtract(mouth);
                    if (rel.length() < 13 && rel.normalize().dotProduct(dir) > 0.8) {
                        p.damage(sw.getDamageSources().mobAttack(this), 5.0f);
                        p.setOnFireFor(4);
                    }
                }
                sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.6f);
            }
            return;
        }

        // --- Choose next move ---
        if (slamCooldown <= 0 && distSq < 81.0) {
            slamTimer = 25;
            slamCooldown = phase2 ? 80 : 130;
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0f, 0.8f);
        } else if (fireCooldown <= 0 && distSq > 36.0 && distSq < 256.0) {
            fireTicks = 40;
            fireCooldown = phase2 ? 140 : 200;
        } else if (leapCooldown <= 0 && distSq > 144.0 && this.isOnGround()) {
            Vec3d d = target.getPos().subtract(this.getPos());
            d = new Vec3d(d.x, 0, d.z).normalize();
            this.setVelocity(d.x * 1.5, 0.95, d.z * 1.5);
            this.velocityModified = true;
            airborne = true;
            airTicks = 0;
            leapCooldown = phase2 ? 90 : 140;
        }

        if (phase2 && pillarCooldown <= 0) {
            firePillars(sw, target);
            pillarCooldown = 120;
        }
    }

    private void enterPhase2(ServerWorld sw) {
        phase2 = true;
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(28.0);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.34);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.4f);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.5f, 0.6f);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        Fx.ring(sw, this.getPos(), 9.0, 8, ParticleTypes.FLAME, ParticleTypes.LAVA);
        for (int i = 0; i < 3; i++) {
            double a = i * (Math.PI * 2 / 3);
            Fortress.spawnAdd(sw, getX() + Math.cos(a) * 6, getY(), getZ() + Math.sin(a) * 6);
        }
    }

    /** Five delayed fire pillars around the target, each with a ground telegraph. */
    private void firePillars(ServerWorld sw, LivingEntity target) {
        for (int i = 0; i < 5; i++) {
            double ox = i == 0 ? 0 : (sw.random.nextDouble() - 0.5) * 10;
            double oz = i == 0 ? 0 : (sw.random.nextDouble() - 0.5) * 10;
            final Vec3d c = new Vec3d(target.getX() + ox, target.getY(), target.getZ() + oz);
            final int delay = i * 6;
            for (int k = 0; k < 20; k++) {
                Scheduler.later(delay + k, () -> {
                    for (int s = 0; s < 12; s++) {
                        double a = (Math.PI * 2 * s) / 12;
                        sw.spawnParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * 2.5, c.y + 0.1, c.z + Math.sin(a) * 2.5, 1, 0, 0, 0, 0);
                    }
                });
            }
            Scheduler.later(delay + 20, () -> {
                sw.spawnParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 25, 0.8, 1.5, 0.8, 0.2);
                sw.spawnParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 60, 0.8, 2.0, 0.8, 0.1);
                sw.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 2, 0.5, 0.2, 0.5, 0);
                sw.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.5f);
                Box box = new Box(c.x - 2.8, c.y - 1, c.z - 2.8, c.x + 2.8, c.y + 4, c.z + 2.8);
                for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, box, pl -> pl.isAlive() && !pl.isSpectator())) {
                    if (Math.hypot(p.getX() - c.x, p.getZ() - c.z) <= 2.8) {
                        p.damage(sw.getDamageSources().mobAttack(this), 14.0f);
                        p.setOnFireFor(5);
                    }
                }
            });
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        bossBar.clearPlayers();
        if (this.getWorld() instanceof ServerWorld sw) {
            Fortress.onBossDefeated(sw, this);
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        // Loot is handled in Fortress.onBossDefeated.
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }
}
