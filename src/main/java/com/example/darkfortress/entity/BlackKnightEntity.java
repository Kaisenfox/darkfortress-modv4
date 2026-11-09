package com.example.darkfortress.entity;

import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Catacomb mini-boss. Telegraphed overhead smash. Drops the Fortress Key. */
public class BlackKnightEntity extends DSMob {
    private int smashTimer = -1;
    private int smashCooldown = 80;

    public BlackKnightEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world);
        this.experiencePoints = 60;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 160.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 11.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        LivingEntity target = this.getTarget();

        if (smashTimer > 0) {
            smashTimer--;
            this.getNavigation().stop();
            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + getHeight() + 0.3, getZ(), 5, 0.5, 0.3, 0.5, 0.1);
            if (smashTimer == 0) {
                Fx.slam(sw, this, 5.0, 16.0f, 1.1);
                smashTimer = -1;
            }
        } else if (target != null && --smashCooldown <= 0 && this.squaredDistanceTo(target) < 25.0) {
            smashTimer = 22;
            smashCooldown = 90 + this.random.nextInt(40);
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.HOSTILE, 1.5f, 0.5f);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (this.getWorld() instanceof ServerWorld sw) {
            Fortress.setFlag(sw, Fortress.FLAG_KNIGHT);
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        if (this.getWorld().isClient) return;
        dropSouls(ModItems.SOUL_LOST_UNDEAD, 8, 12);
        this.dropStack(new ItemStack(ModItems.FORTRESS_KEY));
        this.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_GREATSWORD));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
    }
}
