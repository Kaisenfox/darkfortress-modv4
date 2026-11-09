package com.example.darkfortress.entity;

import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Backs away from you and fires soul bolts from range. */
public class HollowMageEntity extends DSMob {
    private int cast = 70;

    public HollowMageEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 26.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 7.0f, 1.0, 1.35));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) return;
        if (this.squaredDistanceTo(t) > 24.0 * 24.0 || !this.canSee(t)) return;

        this.getLookControl().lookAt(t, 30.0f, 30.0f);
        cast--;
        if (cast > 0 && cast < 15) {
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.6, getZ(), 3, 0.3, 0.2, 0.3, 0.02);
        }
        if (cast <= 0) {
            cast = 60 + this.random.nextInt(25);
            Vec3d from = this.getEyePos();
            Vec3d to = t.getEyePos();
            Vec3d dir = to.subtract(from);
            double len = dir.length();
            dir = dir.normalize();
            for (double d = 0.5; d < len; d += 0.4) {
                Vec3d p = from.add(dir.multiply(d));
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            t.damage(sw.getDamageSources().mobAttack(this), 7.0f);
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.0f, 1.3f);
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        if (this.getWorld().isClient) return;
        dropSouls(ModItems.SOUL_LOST_UNDEAD, 2, 4);
        if (this.random.nextInt(25) == 0) this.dropStack(new ItemStack(ModItems.SOUL_STAFF));
        if (this.random.nextInt(8) == 0) this.dropStack(new ItemStack(ModItems.GREEN_BLOSSOM));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_EVOKER_DEATH;
    }
}
