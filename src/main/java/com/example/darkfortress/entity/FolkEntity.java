package com.example.darkfortress.entity;

import com.example.darkfortress.ModEntities;
import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** One class, three enemy types: Skeleton Warrior (fast), Hollow Knight (tough), Ash Ghoul (leaps). */
public class FolkEntity extends DSMob {
    private int leapCooldown = 60;

    public FolkEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder skeleton() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.29)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 22.0);
    }

    public static DefaultAttributeContainer.Builder knight() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 55.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 22.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    public static DefaultAttributeContainer.Builder ghoul() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.getType() == ModEntities.ASH_GHOUL) {
            if (leapCooldown > 0) leapCooldown--;
            LivingEntity t = this.getTarget();
            if (t != null && leapCooldown <= 0 && this.isOnGround()) {
                double d = this.squaredDistanceTo(t);
                if (d > 16.0 && d < 64.0) {
                    Vec3d dir = t.getPos().subtract(this.getPos());
                    dir = new Vec3d(dir.x, 0, dir.z).normalize();
                    this.setVelocity(dir.x * 1.0, 0.5, dir.z * 1.0);
                    this.velocityModified = true;
                    leapCooldown = 80;
                }
            }
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        if (this.getWorld().isClient) return;
        if (this.getType() == ModEntities.HOLLOW_KNIGHT) {
            dropSouls(ModItems.SOUL_LOST_UNDEAD, 3, 5);
        } else if (this.getType() == ModEntities.ASH_GHOUL) {
            dropSouls(ModItems.SOUL_LOST_UNDEAD, 1, 2);
        } else {
            dropSouls(ModItems.SOUL_LOST_UNDEAD, 1, 3);
        }
        if (this.random.nextInt(10) == 0) {
            this.dropStack(new ItemStack(ModItems.GREEN_BLOSSOM));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getType() == ModEntities.SKELETON_WARRIOR ? SoundEvents.ENTITY_SKELETON_AMBIENT : SoundEvents.ENTITY_ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.getType() == ModEntities.SKELETON_WARRIOR ? SoundEvents.ENTITY_SKELETON_HURT : SoundEvents.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getType() == ModEntities.SKELETON_WARRIOR ? SoundEvents.ENTITY_SKELETON_DEATH : SoundEvents.ENTITY_ZOMBIE_DEATH;
    }
}
