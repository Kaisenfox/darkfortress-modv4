package com.example.darkfortress.entity;

import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.ModEntities;
import com.example.darkfortress.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Boss of the labyrinth. Bull charge (stuns itself on walls), stomp, roar that summons skeletons. */
public class LabyrinthWardenEntity extends DSBoss {
    private int chargeTelegraph = -1;
    private int charging = 0;
    private int chargeElapsed = 0;
    private boolean chargeHit = false;
    private Vec3d chargeDir = Vec3d.ZERO;
    private int stunTicks = 0;
    private int stompTimer = -1;
    private int chargeCd = 90;
    private int stompCd = 60;
    private int roarCd = 300;
    private boolean phase2 = false;

    public LabyrinthWardenEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world, "Labyrinth Warden", BossBar.Color.RED);
        this.experiencePoints = 600;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 700.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 24.0)
                .add(EntityAttributes.GENERIC_ARMOR, 14.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void onEngage(ServerWorld sw) {
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.5f);
    }

    @Override
    protected void fightTick(ServerWorld sw, LivingEntity target) {
        if (!phase2 && this.getHealth() < this.getMaxHealth() * 0.5f) {
            phase2 = true;
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.36);
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 4.0f, 0.4f);
            Fx.ring(sw, this.getPos(), 8.0, 8, ParticleTypes.FLAME, ParticleTypes.POOF);
        }

        double distSq = this.squaredDistanceTo(target);

        if (stunTicks > 0) {
            stunTicks--;
            this.getNavigation().stop();
            sw.spawnParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + getHeight() + 0.3, getZ(), 6, 0.6, 0.2, 0.6, 0.1);
            return;
        }

        if (charging > 0) {
            charging--;
            chargeElapsed++;
            this.setVelocity(chargeDir.x * 1.5, this.getVelocity().y, chargeDir.z * 1.5);
            this.velocityModified = true;
            sw.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 8, 0.5, 0.2, 0.5, 0.05);
            if (!chargeHit) {
                for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(0.8),
                        pl -> pl.isAlive() && !pl.isSpectator())) {
                    p.damage(sw.getDamageSources().mobAttack(this), 28.0f);
                    Vec3d k = new Vec3d(chargeDir.x, 0, chargeDir.z).multiply(2.0);
                    p.addVelocity(k.x, 0.6, k.z);
                    p.velocityModified = true;
                    chargeHit = true;
                }
            }
            if (chargeElapsed > 3 && this.horizontalCollision) {
                stunTicks = 70;
                charging = 0;
                this.setVelocity(Vec3d.ZERO);
                Fx.slam(sw, this, 4.0, 10.0f, 0.8);
                sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 2.0f, 0.5f);
            }
            return;
        }

        if (chargeTelegraph > 0) {
            chargeTelegraph--;
            this.getNavigation().stop();
            for (int i = 1; i <= 14; i++) {
                Vec3d p = this.getPos().add(chargeDir.multiply(i));
                sw.spawnParticles(ParticleTypes.FLAME, p.x, p.y + 0.15, p.z, 1, 0.1, 0.0, 0.1, 0.0);
            }
            if (chargeTelegraph == 0) {
                charging = 18;
                chargeElapsed = 0;
                chargeHit = false;
            }
            return;
        }

        if (stompTimer > 0) {
            stompTimer--;
            this.getNavigation().stop();
            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + getHeight() + 0.2, getZ(), 8, 0.8, 0.3, 0.8, 0.1);
            if (stompTimer == 0) {
                Fx.slam(sw, this, 6.5, 22.0f, 1.3);
                stompTimer = -1;
            }
            return;
        }

        if (chargeCd > 0) chargeCd--;
        if (stompCd > 0) stompCd--;
        if (roarCd > 0) roarCd--;

        if (roarCd <= 0) {
            int adds = sw.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(30),
                    e -> e.getCommandTags().contains(Fortress.ADD_TAG)).size();
            if (adds < 4) {
                for (int i = 0; i < 2; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    Fortress.spawnAddOf(sw, ModEntities.SKELETON_WARRIOR, getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4,
                            new ItemStack(Items.STONE_SWORD));
                }
            }
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 3.0f, 0.7f);
            roarCd = 500;
        }

        if (stompCd <= 0 && distSq < 49.0) {
            stompTimer = 24;
            stompCd = phase2 ? 70 : 100;
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0f, 0.8f);
        } else if (chargeCd <= 0 && distSq > 16.0 && distSq < 625.0) {
            Vec3d d = target.getPos().subtract(this.getPos());
            chargeDir = new Vec3d(d.x, 0, d.z).normalize();
            chargeTelegraph = 30;
            chargeCd = phase2 ? 80 : 130;
            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0f, 1.2f);
        }
    }

    @Override
    protected void onDefeated(ServerWorld sw) {
        this.dropStack(new ItemStack(ModItems.SOUL_GREAT_DEMON, 2));
        this.dropStack(new ItemStack(ModItems.DIVINE_BLESSING, 3));
        this.dropStack(new ItemStack(ModItems.UCHIGATANA));
        this.dropStack(new ItemStack(ModItems.WARHAMMER));
        this.dropStack(new ItemStack(Items.DIAMOND, 5));
        for (var p : sw.getPlayers(pl -> pl.squaredDistanceTo(this) < 4096.0)) {
            Fx.title(p, "WARDEN SLAIN", net.minecraft.util.Formatting.GOLD, null);
        }
    }
}
