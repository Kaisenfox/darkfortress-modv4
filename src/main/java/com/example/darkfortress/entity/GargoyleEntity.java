package com.example.darkfortress.entity;

import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Belfry mini-boss: telegraphed charge and ground stomp. */
public class GargoyleEntity extends DSBoss {
    private int chargeTelegraph = -1;
    private int charging = 0;
    private boolean chargeHit = false;
    private Vec3d chargeDir = Vec3d.ZERO;
    private int stompTimer = -1;
    private int chargeCd = 100;
    private int stompCd = 70;

    public GargoyleEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world, "Stone Gargoyle", BossBar.Color.WHITE);
        this.experiencePoints = 200;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 260.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void onEngage(ServerWorld sw) {
        Fortress.setSeal(sw, Fortress.SEAL_GARGOYLE, true);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.5f, 0.6f);
    }

    @Override
    protected void fightTick(ServerWorld sw, LivingEntity target) {
        double distSq = this.squaredDistanceTo(target);

        if (charging > 0) {
            charging--;
            sw.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 6, 0.4, 0.2, 0.4, 0.05);
            if (!chargeHit) {
                for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(1.2),
                        pl -> pl.isAlive() && !pl.isSpectator())) {
                    p.damage(sw.getDamageSources().mobAttack(this), 20.0f);
                    Vec3d k = p.getPos().subtract(this.getPos());
                    k = new Vec3d(k.x, 0, k.z);
                    if (k.lengthSquared() > 1.0E-4) {
                        k = k.normalize().multiply(1.6);
                        p.addVelocity(k.x, 0.5, k.z);
                        p.velocityModified = true;
                    }
                    chargeHit = true;
                }
            }
            return;
        }

        if (chargeTelegraph > 0) {
            chargeTelegraph--;
            this.getNavigation().stop();
            for (int i = 1; i <= 10; i++) {
                Vec3d p = this.getPos().add(chargeDir.multiply(i));
                sw.spawnParticles(ParticleTypes.CRIT, p.x, p.y + 0.2, p.z, 1, 0.1, 0.0, 0.1, 0.0);
            }
            if (chargeTelegraph == 0) {
                this.setVelocity(chargeDir.x * 1.9, 0.15, chargeDir.z * 1.9);
                this.velocityModified = true;
                charging = 12;
                chargeHit = false;
            }
            return;
        }

        if (stompTimer > 0) {
            stompTimer--;
            this.getNavigation().stop();
            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + getHeight() + 0.2, getZ(), 6, 0.6, 0.3, 0.6, 0.1);
            if (stompTimer == 0) {
                Fx.slam(sw, this, 6.0, 14.0f, 1.2);
                stompTimer = -1;
            }
            return;
        }

        if (chargeCd > 0) chargeCd--;
        if (stompCd > 0) stompCd--;

        if (stompCd <= 0 && distSq < 36.0) {
            stompTimer = 22;
            stompCd = 110;
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5f, 0.9f);
        } else if (chargeCd <= 0 && distSq > 25.0 && distSq < 400.0) {
            Vec3d d = target.getPos().subtract(this.getPos());
            chargeDir = new Vec3d(d.x, 0, d.z).normalize();
            chargeTelegraph = 25;
            chargeCd = 140;
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5f, 1.2f);
        }
    }

    @Override
    protected void onDefeated(ServerWorld sw) {
        Fortress.onMiniBossDefeated(sw, this, Fortress.FLAG_GARGOYLE, Fortress.SEAL_GARGOYLE, "GARGOYLE SLAIN");
        this.dropStack(new ItemStack(ModItems.SOUL_LOST_UNDEAD, 15));
        this.dropStack(new ItemStack(ModItems.HALBERD));
        this.dropStack(new ItemStack(ModItems.DIVINE_BLESSING, 2));
        this.dropStack(new ItemStack(ModItems.FIREBOMB, 4));
    }
}
