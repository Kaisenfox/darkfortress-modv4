package com.example.darkfortress.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/** Shared boss logic: boss bar, engage-on-first-sight, per-boss fight logic, death hook. */
public abstract class DSBoss extends DSMob {
    protected final ServerBossBar bossBar;
    protected boolean engaged = false;

    protected DSBoss(EntityType<? extends DSMob> type, World world, String title, BossBar.Color color) {
        super(type, world);
        this.bossBar = new ServerBossBar(Text.literal(title), color, BossBar.Style.PROGRESS);
        this.bossBar.setVisible(false);
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
            onEngage(sw);
        }
        fightTick(sw, target);
    }

    protected void onEngage(ServerWorld sw) {}

    protected abstract void fightTick(ServerWorld sw, LivingEntity target);

    protected abstract void onDefeated(ServerWorld sw);

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        bossBar.clearPlayers();
        if (this.getWorld() instanceof ServerWorld sw) {
            onDefeated(sw);
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        // Each boss drops its own loot in onDefeated.
    }
}
