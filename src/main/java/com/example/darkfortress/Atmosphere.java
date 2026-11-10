package com.example.darkfortress;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Drifting fog and ash around players inside the fortress and in the Ashen Realm. */
public final class Atmosphere {
    private Atmosphere() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 8 != 0) return;
            BlockPos o = Fortress.storedOrigin();
            for (ServerWorld w : server.getWorlds()) {
                boolean overworld = w.getRegistryKey() == World.OVERWORLD;
                boolean realm = w.getRegistryKey().equals(Ashen.WORLD);
                if (!overworld && !realm) continue;
                for (ServerPlayerEntity p : w.getPlayers()) {
                    if (overworld) {
                        if (o == null || Math.abs(p.getX() - o.getX()) > 58 || Math.abs(p.getZ() - o.getZ()) > 58) continue;
                        fog(w, p, 1.0);
                    } else {
                        fog(w, p, 0.7);
                        w.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 2, 12, 2, 12, 0.01);
                    }
                }
            }
        });
    }

    private static void fog(ServerWorld w, ServerPlayerEntity p, double density) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        w.spawnParticles(ParticleTypes.CLOUD, x, y + 0.4, z, (int) (18 * density), 13, 0.5, 13, 0.0);
        w.spawnParticles(ParticleTypes.CLOUD, x, y + 2.0, z, (int) (8 * density), 13, 1.0, 13, 0.0);
        w.spawnParticles(ParticleTypes.WHITE_ASH, x, y + 4.0, z, (int) (24 * density), 14, 5, 14, 0.0);
    }
}
