package com.example.darkfortress.entity;

import com.example.darkfortress.Fortress;
import com.example.darkfortress.Fx;
import com.example.darkfortress.ModEntities;
import com.example.darkfortress.ModItems;
import com.example.darkfortress.Scheduler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/** Chapel boss: teleports, fires soul-bolt volleys, curses the ground, summons Ash Ghouls. */
public class WitchOfAshEntity extends DSBoss {
    private int tpCd = 100;
    private int volleyCd = 60;
    private int summonCd = 200;
    private int curseCd = 140;

    public WitchOfAshEntity(EntityType<? extends DSMob> type, World world) {
        super(type, world, "Witch of Ash", BossBar.Color.PURPLE);
        this.experiencePoints = 250;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 220.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 30.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void onEngage(ServerWorld sw) {
        Fortress.setSeal(sw, Fortress.SEAL_WITCH, true);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2.0f, 0.6f);
    }

    @Override
    protected void fightTick(ServerWorld sw, LivingEntity target) {
        if (tpCd > 0) tpCd--;
        if (volleyCd > 0) volleyCd--;
        if (summonCd > 0) summonCd--;
        if (curseCd > 0) curseCd--;
        double distSq = this.squaredDistanceTo(target);
        this.getLookControl().lookAt(target, 30.0f, 30.0f);

        if (volleyCd == 15) {
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.0, getZ(), 12, 0.4, 0.3, 0.4, 0.03);
        }
        if (volleyCd <= 0 && this.canSee(target)) {
            volley(sw, target);
            volleyCd = this.getHealth() < this.getMaxHealth() * 0.5f ? 55 : 80;
        }
        if (tpCd <= 0 && distSq < 100.0) {
            teleportNear(sw, target);
            tpCd = 150;
        }
        if (curseCd <= 0) {
            curse(sw, target);
            curseCd = 200;
        }
        if (summonCd <= 0) {
            int adds = sw.getEntitiesByClass(MobEntity.class, this.getBoundingBox().expand(40),
                    e -> e.getCommandTags().contains(Fortress.ADD_TAG)).size();
            if (adds < 3) {
                for (int i = 0; i < 2; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    Fortress.spawnAddOf(sw, ModEntities.ASH_GHOUL, getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, ItemStack.EMPTY);
                }
                sw.spawnParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 40, 1.5, 1.0, 1.5, 0.3);
                sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.5f, 1.0f);
            }
            summonCd = 360;
        }
    }

    private void volley(ServerWorld sw, LivingEntity target) {
        Vec3d from = this.getEyePos();
        Vec3d base = target.getEyePos().subtract(from).normalize();
        for (int i = -1; i <= 1; i++) {
            double a = Math.toRadians(i * 12.0);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            Vec3d dir = new Vec3d(base.x * cos - base.z * sin, base.y, base.x * sin + base.z * cos).normalize();
            Vec3d end = from.add(dir.multiply(26));
            BlockHitResult hit = sw.raycast(new RaycastContext(
                    from, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) end = hit.getPos();
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, new Box(from, end).expand(1.5),
                    pl -> pl.isAlive() && !pl.isSpectator())) {
                if (p.getBoundingBox().expand(0.3).raycast(from, end).isPresent()) {
                    p.damage(sw.getDamageSources().mobAttack(this), 7.0f);
                }
            }
            double len = from.distanceTo(end);
            for (double d = 0.8; d < len; d += 0.45) {
                Vec3d p = from.add(dir.multiply(d));
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 1.0f);
    }

    private void teleportNear(ServerWorld sw, LivingEntity target) {
        for (int i = 0; i < 10; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 5.0 + this.random.nextDouble() * 4.0;
            double x = target.getX() + Math.cos(a) * r;
            double z = target.getZ() + Math.sin(a) * r;
            double y = target.getY();
            Box nb = this.getBoundingBox().offset(x - getX(), y - getY(), z - getZ());
            BlockPos below = BlockPos.ofFloored(x, y - 1, z);
            if (sw.isSpaceEmpty(this, nb) && !sw.getBlockState(below).isAir()) {
                sw.spawnParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                this.refreshPositionAfterTeleport(x, y, z);
                sw.spawnParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 30, 0.4, 0.8, 0.4, 0.3);
                sw.playSound(null, x, y, z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.8f);
                return;
            }
        }
    }

    private void curse(ServerWorld sw, LivingEntity target) {
        final Vec3d c = target.getPos();
        for (int k = 0; k < 20; k++) {
            Scheduler.later(k, () -> {
                for (int s = 0; s < 14; s++) {
                    double a = (Math.PI * 2 * s) / 14;
                    sw.spawnParticles(ParticleTypes.SQUID_INK, c.x + Math.cos(a) * 3.5, c.y + 0.1, c.z + Math.sin(a) * 3.5, 1, 0, 0, 0, 0);
                }
            });
        }
        Scheduler.later(20, () -> {
            sw.spawnParticles(ParticleTypes.PORTAL, c.x, c.y + 1.0, c.z, 60, 1.5, 1.0, 1.5, 0.3);
            Box box = new Box(c.x - 3.5, c.y - 1, c.z - 3.5, c.x + 3.5, c.y + 3, c.z + 3.5);
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, box, pl -> pl.isAlive() && !pl.isSpectator())) {
                if (Math.hypot(p.getX() - c.x, p.getZ() - c.z) <= 3.5) {
                    p.damage(sw.getDamageSources().mobAttack(this), 6.0f);
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
                }
            }
            sw.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 0.6f);
        });
    }

    @Override
    protected void onDefeated(ServerWorld sw) {
        Fortress.onMiniBossDefeated(sw, this, Fortress.FLAG_WITCH, Fortress.SEAL_WITCH, "WITCH SLAIN");
        this.dropStack(new ItemStack(ModItems.SOUL_GREAT_DEMON));
        this.dropStack(new ItemStack(ModItems.SOUL_STAFF));
        this.dropStack(new ItemStack(ModItems.UCHIGATANA));
        this.dropStack(new ItemStack(ModItems.DIVINE_BLESSING, 2));
        this.dropStack(new ItemStack(Items.GOLDEN_APPLE, 2));
    }
}
