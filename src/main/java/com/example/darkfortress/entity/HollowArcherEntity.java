package com.example.darkfortress.entity;

import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Skeleton-based archer that doesn't burn in daylight. */
public class HollowArcherEntity extends SkeletonEntity {
    public HollowArcherEntity(EntityType<? extends SkeletonEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractSkeletonEntity.createAbstractSkeletonAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 22.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        if (!this.getWorld().isClient) {
            this.dropStack(new ItemStack(ModItems.SOUL_LOST_UNDEAD, 1 + this.random.nextInt(3)));
        }
    }
}
