package com.example.darkfortress;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Ashen Realm: a flat, endless, twilit dimension (defined in data/darkfortress/dimension).
 * Structures are generated deterministically per chunk as chunks load:
 * labyrinths (with a boss), ruined chapels, watchtowers, graveyards and bonfire obelisks.
 */
public final class Ashen {
    public static final RegistryKey<World> WORLD =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(DarkFortressMod.MOD_ID, "ashen_realm"));

    private static final int FLOOR = 63;      // top block of the flat world; you stand at FLOOR + 1
    private static final int REGION = 128;
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final Map<UUID, Long> LAST_PAD = new HashMap<>();

    private Ashen() {}

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register(Ashen::onChunkLoad);
        ServerTickEvents.END_SERVER_TICK.register(Ashen::drain);
    }

    // ------------------------------------------------------------------ travel

    public static void usePad(ServerWorld world, ServerPlayerEntity p) {
        MinecraftServer server = world.getServer();
        long now = server.getTicks();
        Long last = LAST_PAD.get(p.getUuid());
        if (last != null && now - last < 100) return;
        LAST_PAD.put(p.getUuid(), now);
        if (world.getRegistryKey().equals(WORLD)) {
            sendHome(server, p);
        } else {
            sendToRealm(server, p);
        }
    }

    public static void sendToRealm(MinecraftServer server, ServerPlayerEntity p) {
        ServerWorld realm = server.getWorld(WORLD);
        if (realm == null) {
            DarkFortressMod.LOGGER.error("Ashen Realm dimension {} was not found", WORLD.getValue());
            p.sendMessage(Text.literal("The Ashen Realm dimension is not loaded in this world. "
                    + "Make a NEW world with the mod installed, and check logs/latest.log for a data pack error.")
                    .formatted(Formatting.RED));
            return;
        }
        p.sendMessage(Text.literal("Entering the Ashen Realm...").formatted(Formatting.GRAY));
        try {
            ensureHub(realm);
        } catch (Throwable t) {
            DarkFortressMod.LOGGER.error("Ashen Realm hub generation failed", t);
            p.sendMessage(Text.literal("Hub generation failed: " + t).formatted(Formatting.RED));
        }
        LAST_PAD.put(p.getUuid(), (long) server.getTicks());
        p.setSpawnPoint(WORLD, new BlockPos(0, FLOOR + 1, -2), 0.0f, true, false);
        Fx.title(p, "THE ASHEN REALM", Formatting.DARK_GRAY, "Ruins, labyrinths and things that wait");
        tp(server, p, "darkfortress:ashen_realm", 0.5, FLOOR + 1, -4.5);
        Scheduler.later(40, () -> {
            if (!p.getWorld().getRegistryKey().equals(WORLD)) {
                p.sendMessage(Text.literal("The teleport did not happen. Look for a command error in chat above, "
                        + "and check logs/latest.log.").formatted(Formatting.RED));
            }
        });
    }

    public static void sendHome(MinecraftServer server, ServerPlayerEntity p) {
        ServerWorld ow = server.getOverworld();
        BlockPos o = Fortress.origin(ow);
        LAST_PAD.put(p.getUuid(), (long) server.getTicks());
        tp(server, p, "minecraft:overworld", o.getX() + 0.5, o.getY() + 9, o.getZ() - 19.5);
    }

    static void tp(MinecraftServer server, ServerPlayerEntity p, String dim, double x, double y, double z) {
        String cmd = String.format(Locale.ROOT, "execute in %s run tp %s %.2f %.2f %.2f", dim, p.getUuidAsString(), x, y, z);
        server.getCommandManager().executeWithPrefix(p.getCommandSource().withLevel(4).withSilent(), cmd);
    }

    // ------------------------------------------------------------------ hashing

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static int hash3(int a, int b, int c) {
        return hash(hash(a, b), c);
    }

    private static int pct(int a, int b, int c) {
        return Math.floorMod(hash3(a, b, c), 100);
    }

    private static BlockState darkBrick(int x, int y, int z) {
        int r = pct(x, y, z);
        if (r < 55) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
        if (r < 80) return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        if (r < 92) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        return Blocks.BLACKSTONE.getDefaultState();
    }

    private static BlockState tile(int x, int z) {
        int r = pct(x, z, 3);
        if (r < 60) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        if (r < 85) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        return Blocks.COBBLED_DEEPSLATE.getDefaultState();
    }

    // ------------------------------------------------------------------ canvas (writes only inside one chunk)

    @FunctionalInterface
    private interface PosFn {
        BlockState at(int x, int y, int z);
    }

    private static final class Canvas {
        final ServerWorld w;
        final int minX, maxX, minZ, maxZ;
        final BlockPos.Mutable m = new BlockPos.Mutable();

        Canvas(ServerWorld w, int minX, int minZ, int maxX, int maxZ) {
            this.w = w;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        boolean in(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        void set(int x, int y, int z, BlockState s) {
            if (in(x, z)) w.setBlockState(m.set(x, y, z), s, 2);
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
            fillWith(x1, y1, z1, x2, y2, z2, (x, y, z) -> s);
        }

        void fillWith(int x1, int y1, int z1, int x2, int y2, int z2, PosFn f) {
            int ax = Math.max(Math.min(x1, x2), minX), bx = Math.min(Math.max(x1, x2), maxX);
            int az = Math.max(Math.min(z1, z2), minZ), bz = Math.min(Math.max(z1, z2), maxZ);
            int ay = Math.min(y1, y2), by = Math.max(y1, y2);
            for (int x = ax; x <= bx; x++)
                for (int z = az; z <= bz; z++)
                    for (int y = ay; y <= by; y++)
                        w.setBlockState(m.set(x, y, z), f.at(x, y, z), 2);
        }

        void lantern(int x, int y, int z) {
            set(x, y, z, Blocks.SOUL_LANTERN.getDefaultState());
        }

        void hangingLantern(int x, int y, int z) {
            set(x, y, z, Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        }

        void bonfire(int x, int y, int z) {
            set(x, y, z, ModBlocks.BONFIRE.getDefaultState());
            set(x, y + 1, z, Blocks.END_ROD.getDefaultState());
        }

        void chest(int x, int y, int z, int tier) {
            if (!in(x, z)) return;
            if (pct(x, y, z) >= (tier <= 1 ? 50 : (tier == 2 ? 65 : 80))) return;
            set(x, y, z, Blocks.CHEST.getDefaultState());
            BlockEntity be = w.getBlockEntity(new BlockPos(x, y, z));
            if (be instanceof ChestBlockEntity c) {
                Loot.fill(c, new BlockPos(x, y, z).asLong(), tier);
            }
        }

        /** kind: 0 soldier, 1 skeleton, 2 knight, 3 mage, 4 ghoul, 5 archer */
        void spawn(int kind, double x, double y, double z) {
            if (!in((int) Math.floor(x), (int) Math.floor(z))) return;
            Scheduler.later(2, () -> spawnKind(w, kind, x, y, z));
        }

        void spawnBoss(double x, double y, double z) {
            if (!in((int) Math.floor(x), (int) Math.floor(z))) return;
            Scheduler.later(2, () -> Fortress.spawnMob(w, ModEntities.LABYRINTH_WARDEN, x, y, z, ItemStack.EMPTY));
        }
    }

    private static void spawnKind(ServerWorld w, int kind, double x, double y, double z) {
        switch (kind) {
            case 0 -> Fortress.spawnMob(w, ModEntities.HOLLOW_SOLDIER, x, y, z, new ItemStack(Items.IRON_SWORD));
            case 1 -> Fortress.spawnMob(w, ModEntities.SKELETON_WARRIOR, x, y, z, new ItemStack(Items.STONE_SWORD));
            case 2 -> Fortress.spawnMob(w, ModEntities.HOLLOW_KNIGHT, x, y, z, new ItemStack(ModItems.CLAYMORE));
            case 3 -> Fortress.spawnMob(w, ModEntities.HOLLOW_MAGE, x, y, z, new ItemStack(ModItems.SOUL_STAFF));
            case 4 -> Fortress.spawnMob(w, ModEntities.ASH_GHOUL, x, y, z, ItemStack.EMPTY);
            default -> Fortress.spawnMob(w, ModEntities.HOLLOW_ARCHER, x, y, z, new ItemStack(Items.BOW));
        }
    }

    // ------------------------------------------------------------------ hub (arrival shrine)

    public static void ensureHub(ServerWorld w) {
        BlockPos marker = new BlockPos(1, 2, 1);
        if (w.getBlockState(marker).isOf(Blocks.RED_NETHER_BRICKS)) return;
        // Generate the surrounding citadel first, so the shrine is built on top of it.
        for (int cx = -32; cx <= 16; cx += 16) {
            for (int cz = -32; cz <= 16; cz += 16) {
                w.getChunk(cx >> 4, cz >> 4);
                generateChunk(w, cx, cz);
            }
        }
        Canvas c = new Canvas(w, -30, -30, 30, 30);
        for (int x = -14; x <= 14; x++) {
            for (int z = -14; z <= 14; z++) {
                if (x * x + z * z <= 13 * 13) {
                    c.set(x, FLOOR, z, tile(x, z));
                }
            }
        }
        // A ring of tall pillars (cathedral colonnade) with braziers at their feet.
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int px = (int) Math.round(Math.cos(a) * 10);
            int pz = (int) Math.round(Math.sin(a) * 10);
            c.fillWith(px, FLOOR + 1, pz, px, FLOOR + 21, pz, Ashen::darkBrick);
            int bx = (int) Math.round(Math.cos(a) * 8.5);
            int bz = (int) Math.round(Math.sin(a) * 8.5);
            c.set(bx, FLOOR + 1, bz, Blocks.SOUL_CAMPFIRE.getDefaultState());
        }
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            c.lantern((int) Math.round(Math.cos(a) * 17), FLOOR + 1, (int) Math.round(Math.sin(a) * 17));
        }
        c.bonfire(0, FLOOR + 1, 0);
        for (int x = -1; x <= 1; x++)
            for (int z = 7; z <= 9; z++)
                c.set(x, FLOOR, z, ModBlocks.ASHEN_PORTAL.getDefaultState());
        c.lantern(-2, FLOOR + 1, 2);
        c.lantern(2, FLOOR + 1, 2);
        w.setBlockState(marker, Blocks.RED_NETHER_BRICKS.getDefaultState(), 2);
    }

    // ------------------------------------------------------------------ chunk generation

    /** -1 = none (hub area), else 0..4 structure type or 9 = empty. */
    private static int regionType(int rx, int rz) {
        if ((rx == 0 || rx == -1) && (rz == 0 || rz == -1)) return -1;
        int h = Math.floorMod(hash(rx, rz), 100);
        if (h < 24) return 0;   // labyrinth
        if (h < 40) return 1;   // chapel ruin
        if (h < 56) return 2;   // watchtower
        if (h < 70) return 3;   // graveyard
        if (h < 84) return 4;   // bonfire obelisk
        return 9;
    }

    private static final java.util.ArrayDeque<long[]> PENDING = new java.util.ArrayDeque<>();

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (!world.getRegistryKey().equals(WORLD)) return;
        // Never touch the world inside this callback (the chunk is mid-load). Queue it.
        PENDING.add(new long[]{chunk.getPos().getStartX(), chunk.getPos().getStartZ()});
    }

    /** Generates at most 2 queued chunks per tick, always the ones players are standing in first. */
    private static void drain(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        ServerWorld w = server.getWorld(WORLD);
        if (w == null) {
            PENDING.clear();
            return;
        }
        int budget = 2;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (budget <= 0) break;
            long px = p.getBlockX() & ~15, pz = p.getBlockZ() & ~15;
            java.util.Iterator<long[]> it = PENDING.iterator();
            while (it.hasNext()) {
                long[] c = it.next();
                if (c[0] == px && c[1] == pz) {
                    it.remove();
                    safeGenerate(w, (int) c[0], (int) c[1]);
                    budget--;
                    break;
                }
            }
        }
        while (budget-- > 0 && !PENDING.isEmpty()) {
            long[] c = PENDING.poll();
            safeGenerate(w, (int) c[0], (int) c[1]);
        }
    }

    private static void safeGenerate(ServerWorld w, int cx0, int cz0) {
        try {
            generateChunk(w, cx0, cz0);
        } catch (Throwable t) {
            DarkFortressMod.LOGGER.error("Ashen Realm chunk generation failed at " + cx0 + "," + cz0, t);
        }
    }

    private static void generateChunk(ServerWorld world, int cx0, int cz0) {
        if (!world.getChunkManager().isChunkLoaded(cx0 >> 4, cz0 >> 4)) return;
        BlockPos marker = new BlockPos(cx0, 1, cz0);
        BlockState ms = world.getBlockState(marker);
        if (ms.isAir() || ms.isOf(Blocks.CRYING_OBSIDIAN)) return;
        world.setBlockState(marker, Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);

        Canvas c = new Canvas(world, cx0, cz0, cx0 + 15, cz0 + 15);
        terrain(c, cx0, cz0);

        int rx = Math.floorDiv(cx0, REGION);
        int rz = Math.floorDiv(cz0, REGION);
        int cx = rx * REGION + REGION / 2;
        int cz = rz * REGION + REGION / 2;
        switch (regionType(rx, rz)) {
            case 0 -> labyrinth(c, cx, cz);
            case 1 -> chapelRuin(c, cx, cz);
            case 2 -> watchtower(c, cx, cz);
            case 3 -> graveyard(c, cx, cz);
            case 4 -> obelisk(c, cx, cz);
            default -> {}
        }
        decor(c, cx0, cz0);

        // Never entomb a player who was standing here when the walls went up.
        for (ServerPlayerEntity p : world.getPlayers()) {
            int px = p.getBlockX(), pz = p.getBlockZ();
            if (c.in(px, pz)) c.fill(px - 1, p.getBlockY(), pz - 1, px + 1, p.getBlockY() + 2, pz + 1, AIR);
        }
    }

    // ------------------------------------------------------------------ the citadel (enclosed layout)

    private static final int SOLID = 0, CORRIDOR = 1, ROOM = 2, AVENUE = 3, PLAZA = 4, HUBAREA = 5;
    /** Open space is sealed by a ceiling plane above this height. */
    private static final int CEIL = FLOOR + 31;

    private static double h01(int x, int z, int seed) {
        return (hash3(x, z, seed) & 0xffff) / 65535.0;
    }

    /** Smooth value noise in 0..1. */
    private static double vnoise(int x, int z, double scale, int seed) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = h01(x0, z0, seed), b = h01(x0 + 1, z0, seed), c = h01(x0, z0 + 1, seed), d = h01(x0 + 1, z0 + 1, seed);
        double top = a + (b - a) * tx, bot = c + (d - c) * tx;
        return top + (bot - top) * tz;
    }

    private static int zone(int x, int z) {
        if (Math.abs(x) < 48 && Math.abs(z) < 48) return 2;
        return Math.floorMod(hash3(Math.floorDiv(x, 96), Math.floorDiv(z, 96), 7), 4);
    }

    /** What kind of space is this column: solid masonry, a corridor, a hall, an avenue, a set-piece plaza... */
    private static int kind(int x, int z) {
        if (Math.hypot(x + 0.5, z + 0.5) <= 21) return HUBAREA;
        if ((Math.abs(x) <= 4 && Math.abs(z) <= 70) || (Math.abs(z) <= 4 && Math.abs(x) <= 70)) return AVENUE;
        int rx = Math.floorDiv(x, REGION), rz = Math.floorDiv(z, REGION);
        int dx = Math.floorMod(x, REGION) - REGION / 2, dz = Math.floorMod(z, REGION) - REGION / 2;
        int t = regionType(rx, rz);
        if (t >= 0 && t != 9) {
            int r = t == 0 ? 44 : 22;
            if (Math.abs(dx) <= r && Math.abs(dz) <= r) return PLAZA;
        }
        if (Math.abs(dx) <= 4 || Math.abs(dz) <= 4) return AVENUE;
        double n1 = vnoise(x, z, 38, 11) * 2 - 1;
        double n2 = vnoise(x, z, 27, 23) * 2 - 1;
        if (vnoise(x, z, 22, 37) > 0.76) return ROOM;
        if (Math.abs(n1) < 0.15 || Math.abs(n2) < 0.12) return CORRIDOR;
        return SOLID;
    }

    /** Open height above the floor for a non-solid column. */
    private static int ceilH(int x, int z, int k) {
        if (k == HUBAREA) return 22;
        if (k == PLAZA) return 24;
        if (k == AVENUE) return 13;
        if (k == ROOM) return 14 + (int) Math.round(vnoise(x, z, 16, 51) * 10);
        return 8 + (int) Math.round(vnoise(x, z, 14, 61) * 5);
    }

    private static boolean nearWall(int x, int z) {
        return kind(x + 1, z) == SOLID || kind(x - 1, z) == SOLID || kind(x, z + 1) == SOLID || kind(x, z - 1) == SOLID;
    }

    // ---- palettes: Burg ruins (0), Catacombs (1), Cathedral (2), Ember depths (3)

    private static BlockState wall(int zone, int x, int y, int z) {
        int r = pct(x, y, z);
        if (zone == 0) {
            if (r < 40) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 62) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            if (r < 82) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            if (r < 94) return Blocks.COBBLESTONE.getDefaultState();
            return Blocks.ANDESITE.getDefaultState();
        }
        if (zone == 1) {
            if (r < 35) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
            if (r < 55) return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            if (r < 75) return Blocks.TUFF_BRICKS.getDefaultState();
            if (r < 90) return Blocks.COBBLED_DEEPSLATE.getDefaultState();
            return Blocks.DEEPSLATE_TILES.getDefaultState();
        }
        if (zone == 2) {
            if (r < 40) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            if (r < 60) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            if (r < 82) return Blocks.BLACKSTONE.getDefaultState();
            if (r < 93) return Blocks.DEEPSLATE_TILES.getDefaultState();
            if (r < 99) return Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
            return Blocks.GILDED_BLACKSTONE.getDefaultState();
        }
        if (r < 32) return Blocks.NETHER_BRICKS.getDefaultState();
        if (r < 52) return Blocks.CRACKED_NETHER_BRICKS.getDefaultState();
        if (r < 66) return Blocks.RED_NETHER_BRICKS.getDefaultState();
        if (r < 82) return Blocks.BLACKSTONE.getDefaultState();
        if (r < 94) return Blocks.BASALT.getDefaultState();
        return Blocks.MAGMA_BLOCK.getDefaultState();
    }

    private static BlockState floorBlock(int zone, int x, int z, int k) {
        int r = pct(x, z, 17);
        boolean road = k == AVENUE || k == HUBAREA;
        if (zone == 0) {
            if (road) return r < 70 ? Blocks.STONE_BRICKS.getDefaultState() : (r < 88 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.MOSSY_STONE_BRICKS.getDefaultState());
            if (r < 40) return Blocks.COBBLESTONE.getDefaultState();
            if (r < 70) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 86) return Blocks.GRAVEL.getDefaultState();
            return Blocks.MOSSY_COBBLESTONE.getDefaultState();
        }
        if (zone == 1) {
            if (r < 45) return Blocks.DEEPSLATE_TILES.getDefaultState();
            if (r < 70) return Blocks.COBBLED_DEEPSLATE.getDefaultState();
            if (r < 90) return Blocks.TUFF_BRICKS.getDefaultState();
            return road ? Blocks.DEEPSLATE_TILES.getDefaultState() : Blocks.SOUL_SOIL.getDefaultState();
        }
        if (zone == 2) {
            if (road) return ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            if (r < 55) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            if (r < 85) return Blocks.POLISHED_BLACKSTONE.getDefaultState();
            return Blocks.SMOOTH_BASALT.getDefaultState();
        }
        if (r < 45) return Blocks.BLACKSTONE.getDefaultState();
        if (r < 70) return Blocks.BASALT.getDefaultState();
        if (r < 88) return Blocks.NETHER_BRICKS.getDefaultState();
        if (r < 99) return road ? Blocks.BLACKSTONE.getDefaultState() : Blocks.SOUL_SOIL.getDefaultState();
        return Blocks.MAGMA_BLOCK.getDefaultState();
    }

    private static void terrain(Canvas c, int cx0, int cz0) {
        final int top = CEIL + 3;
        final BlockState lantern = Blocks.SOUL_LANTERN.getDefaultState();
        final BlockState hanging = lantern.with(LanternBlock.HANGING, true);
        for (int x = cx0; x <= cx0 + 15; x++) {
            for (int z = cz0; z <= cz0 + 15; z++) {
                int k = kind(x, z);
                int zone = zone(x, z);
                boolean pillar = false;
                if (k == ROOM) {
                    int m = zone == 2 ? 6 : 9;
                    pillar = Math.floorMod(x, m) == 0 && Math.floorMod(z, m) == 0;
                }
                c.set(x, FLOOR, z, floorBlock(zone, x, z, k));
                if (k == SOLID || pillar) {
                    for (int y = FLOOR + 1; y <= top; y++) c.set(x, y, z, wall(zone, x, y, z));
                } else {
                    int h = ceilH(x, z, k);
                    for (int y = FLOOR + h + 1; y <= top; y++) c.set(x, y, z, wall(zone, x, y, z));
                    int r = pct(x, z, 99);
                    boolean grand = k == AVENUE || k == HUBAREA || k == PLAZA;
                    if (r < (grand ? 3 : 2) && nearWall(x, z)) {
                        c.set(x, FLOOR + 1, z, lantern);
                    } else if (r >= 3 && r < (grand ? 6 : 4)) {
                        int len = 2 + (r % 2);
                        for (int i = 0; i < len; i++) c.set(x, FLOOR + h - i, z, Blocks.CHAIN.getDefaultState());
                        c.set(x, FLOOR + h - len, z, hanging);
                    } else if (zone == 1 && r >= 6 && r < 8) {
                        c.set(x, FLOOR + 1, z, Blocks.BONE_BLOCK.getDefaultState());
                        if (r == 7) c.set(x, FLOOR + 2, z, Blocks.SKELETON_SKULL.getDefaultState());
                    } else if (zone == 1 && r == 8) {
                        c.set(x, FLOOR + h, z, Blocks.COBWEB.getDefaultState());
                    } else if (zone == 3 && r >= 6 && r < 8) {
                        c.set(x, FLOOR + h + 1, z, Blocks.SHROOMLIGHT.getDefaultState());
                    }
                }
                c.set(x, top + 1, z, Blocks.BEDROCK.getDefaultState());
            }
        }
    }

    // ------------------------------------------------------------------ set dressing in halls

    private static boolean roomy(int x, int z) {
        if (kind(x, z) != ROOM) return false;
        for (int dx = -5; dx <= 5; dx += 5)
            for (int dz = -5; dz <= 5; dz += 5)
                if (kind(x + dx, z + dz) == SOLID) return false;
        return ceilH(x, z, ROOM) >= 18;
    }

    private static void decor(Canvas c, int cx0, int cz0) {
        for (int i = 0; i < 3; i++) {
            int px = cx0 + 5 + Math.floorMod(hash3(cx0, cz0, 100 + i), 6);
            int pz = cz0 + 5 + Math.floorMod(hash3(cx0, cz0, 120 + i), 6);
            if (!roomy(px, pz)) continue;
            int zone = zone(px, pz);
            int roll = pct(cx0, cz0, 130 + i);
            int seed = hash3(cx0, cz0, 140 + i);
            if (roll < 28) {
                deadTree(c, px, FLOOR + 1, pz, seed);
            } else if (roll < 45) {
                glowTree(c, px, FLOOR + 1, pz, seed, zone != 3);
            } else if (roll < 57) {
                if (zone == 3) {
                    for (int dx = -5; dx <= 5; dx++) {
                        for (int dz = -5; dz <= 5; dz++) {
                            int d2 = dx * dx + dz * dz;
                            if (d2 <= 9) c.set(px + dx, FLOOR, pz + dz, Blocks.LAVA.getDefaultState());
                            else if (d2 <= 20) c.set(px + dx, FLOOR, pz + dz, (hash3(px + dx, pz + dz, 9) & 1) == 0
                                    ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.BLACKSTONE.getDefaultState());
                        }
                    }
                } else {
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx * dx + dz * dz <= 5) {
                                c.set(px + dx, FLOOR, pz + dz, Blocks.SOUL_SOIL.getDefaultState());
                                if (pct(px + dx, pz + dz, 311) < 50) c.set(px + dx, FLOOR + 1, pz + dz, Blocks.SOUL_FIRE.getDefaultState());
                            }
                        }
                    }
                }
            } else if (roll < 68) {
                for (int s = 0; s < 7; s++) {
                    int bx = px - 2 + Math.floorMod(hash3(px, pz, 341 + s), 5);
                    int bz = pz - 2 + Math.floorMod(hash3(px, pz, 351 + s), 5);
                    c.set(bx, FLOOR + 1, bz, Blocks.BONE_BLOCK.getDefaultState());
                    if (s % 3 == 0) c.set(bx, FLOOR + 2, bz, Blocks.SKELETON_SKULL.getDefaultState());
                }
            } else if (roll < 78) {
                c.fill(px, FLOOR + 1, pz, px, FLOOR + 2, pz, Blocks.STONE_BRICK_WALL.getDefaultState());
                c.set(px, FLOOR + 3, pz, Blocks.SKELETON_SKULL.getDefaultState());
            } else if (roll < 88) {
                c.fillWith(px - 3, FLOOR + 1, pz, px - 3, FLOOR + 6, pz, Ashen::darkBrick);
                c.fillWith(px + 3, FLOOR + 1, pz, px + 3, FLOOR + 5, pz, Ashen::darkBrick);
                for (int x = px - 3; x <= px + 3; x++) if (pct(x, pz, 322) < 75) c.set(x, FLOOR + 6, pz, darkBrick(x, FLOOR + 6, pz));
                c.lantern(px, FLOOR + 1, pz);
            }
        }
        // Enemies: the deeper, the nastier. Never near the arrival hub.
        int[][] pools = {{0, 0, 2, 5}, {1, 1, 4, 3}, {2, 3, 3, 5}, {4, 4, 2, 3}};
        for (int i = 0; i < 2; i++) {
            if (pct(cx0, cz0, 400 + i) >= 14) continue;
            int ex = cx0 + 2 + Math.floorMod(hash3(cx0, cz0, 410 + i), 12);
            int ez = cz0 + 2 + Math.floorMod(hash3(cx0, cz0, 430 + i), 12);
            if (kind(ex, ez) == SOLID || Math.hypot(ex, ez) < 34) continue;
            int[] pool = pools[zone(ex, ez)];
            int mob = pool[Math.floorMod(hash3(cx0, cz0, 420 + i), 4)];
            int n = 1 + Math.floorMod(hash3(cx0, cz0, 440 + i), 2);
            for (int k = 0; k < n; k++) c.spawn(mob, ex + 0.5 + k, FLOOR + 1, ez + 0.5);
        }
    }

    private static void deadTree(Canvas c, int x, int y0, int z, int seed) {
        int ht = 6 + Math.floorMod(hash(seed, 1), 4);
        BlockState log = Blocks.DARK_OAK_LOG.getDefaultState();
        for (int y = 0; y < ht; y++) c.set(x, y0 + y, z, log);
        for (int k = 0; k < 3; k++) {
            int by = 3 + Math.floorMod(hash(seed, 10 + k), ht - 3);
            Direction d = Direction.fromHorizontal(Math.floorMod(hash(seed, 20 + k), 4));
            BlockState bl = log.with(PillarBlock.AXIS, d.getAxis());
            int len = 2 + Math.floorMod(hash(seed, 30 + k), 2);
            for (int i = 1; i <= len; i++) {
                c.set(x + d.getOffsetX() * i, y0 + by + i / 2, z + d.getOffsetZ() * i, bl);
            }
            c.set(x + d.getOffsetX() * (len + 1), y0 + by + len / 2, z + d.getOffsetZ() * (len + 1), Blocks.COBWEB.getDefaultState());
        }
    }

    /** Glowing alien trees: teal "soul" trees or red "ember" trees with shroomlight fruit. */
    private static void glowTree(Canvas c, int x, int y0, int z, int seed, boolean teal) {
        int ht = 6 + Math.floorMod(hash(seed, 2), 3);
        BlockState stem = (teal ? Blocks.WARPED_STEM : Blocks.CRIMSON_STEM).getDefaultState();
        BlockState wart = (teal ? Blocks.WARPED_WART_BLOCK : Blocks.NETHER_WART_BLOCK).getDefaultState();
        for (int y = 0; y < ht; y++) c.set(x, y0 + y, z, stem);
        int top = y0 + ht;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int d2 = dx * dx + dy * dy * 2 + dz * dz;
                    if (d2 > 10) continue;
                    int r = pct(x + dx, top + dy, z + dz);
                    if (r < 14) continue;
                    c.set(x + dx, top + dy, z + dz, r > 92 ? Blocks.SHROOMLIGHT.getDefaultState() : wart);
                }
            }
        }
        c.set(x, y0 + ht - 1, z, stem);
    }

    // ------------------------------------------------------------------ labyrinth

    private static boolean openN(int cx, int cz, int i, int j, int n) {
        if (j == 0) return false;
        if (i == n - 1) return true;
        return (hash3(i + cx * 31, j + cz * 17, 91) & 1) == 0;
    }

    private static boolean openE(int cx, int cz, int i, int j, int n) {
        if (i == n - 1) return false;
        if (j == 0) return true;
        return (hash3(i + cx * 31, j + cz * 17, 91) & 1) == 1;
    }

    private static boolean isWall(int dx, int dz, int n, int p, int cx, int cz) {
        if (dx >= 37 && dx <= 47 && dz >= 37 && dz <= 47) return false;   // boss room interior
        int mx = dx % p, mz = dz % p;
        int i = dx / p, j = dz / p;
        if (mx == 0 && mz == 0) return true;
        if (mx == 0) {
            if (i == 0 || i == n) return true;
            return !openE(cx, cz, i - 1, j, n);
        }
        if (mz == 0) {
            if (j == 0 || j == n) return true;
            return !openN(cx, cz, i, j, n);
        }
        return false;
    }

    private static void labyrinth(Canvas c, int cx, int cz) {
        final int n = 21, p = 4, s = n * p + 1;
        final int ox = cx - 42, oz = cz - 42;
        int xa = Math.max(c.minX, ox), xb = Math.min(c.maxX, ox + s - 1);
        int za = Math.max(c.minZ, oz), zb = Math.min(c.maxZ, oz + s - 1);
        if (xa > xb || za > zb) return;

        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                boolean wall = isWall(x - ox, z - oz, n, p, cx, cz);
                c.set(x, FLOOR, z, tile(x, z));
                for (int y = FLOOR + 1; y <= FLOOR + 7; y++) {
                    c.set(x, y, z, wall ? darkBrick(x, y, z) : AIR);
                }
            }
        }
        // Four entrances, one on each avenue.
        c.fill(ox + 41, FLOOR + 1, oz + 84, ox + 43, FLOOR + 7, oz + 84, AIR);
        c.fill(ox + 41, FLOOR + 1, oz, ox + 43, FLOOR + 7, oz, AIR);
        c.fill(ox, FLOOR + 1, oz + 41, ox, FLOOR + 7, oz + 43, AIR);
        c.fill(ox + 84, FLOOR + 1, oz + 41, ox + 84, FLOOR + 7, oz + 43, AIR);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i >= 9 && i <= 11 && j >= 9 && j <= 11) continue;
                if ((i == 10 && (j <= 1 || j >= n - 2)) || (j == 10 && (i <= 1 || i >= n - 2))) continue;
                int x = ox + p * i + 2;
                int z = oz + p * j + 2;
                if (!c.in(x, z)) continue;

                int degree = 0;
                if (openN(cx, cz, i, j, n)) degree++;
                if (j < n - 1 && openN(cx, cz, i, j + 1, n)) degree++;
                if (openE(cx, cz, i, j, n)) degree++;
                if (i > 0 && openE(cx, cz, i - 1, j, n)) degree++;

                int h = pct(i, j, cx ^ cz);
                if (degree == 1 && Math.floorMod(hash3(i, j, 7), 5) == 0) {
                    c.chest(x, FLOOR + 1, z, Math.floorMod(hash3(i, j, 8), 5) == 0 ? 3 : 2);
                    continue;
                }
                if (h < 4) c.spawn(0, x + 0.5, FLOOR + 1, z + 0.5);
                else if (h < 7) c.spawn(1, x + 0.5, FLOOR + 1, z + 0.5);
                else if (h < 9) c.spawn(2, x + 0.5, FLOOR + 1, z + 0.5);
                else if (h < 10) c.spawn(3, x + 0.5, FLOOR + 1, z + 0.5);
                else if (h < 11) c.spawn(4, x + 0.5, FLOOR + 1, z + 0.5);
                else if (h < 14) c.lantern(x, FLOOR + 1, z);
            }
        }
        // Boss room.
        c.spawnBoss(ox + 42 + 0.5, FLOOR + 1, oz + 42 + 0.5);
        c.chest(ox + 38, FLOOR + 1, oz + 38, 3);
        c.chest(ox + 46, FLOOR + 1, oz + 46, 3);
        c.lantern(ox + 38, FLOOR + 1, oz + 46);
        c.lantern(ox + 46, FLOOR + 1, oz + 38);
    }

    // ------------------------------------------------------------------ chapel ruin

    private static void chapelRuin(Canvas c, int cx, int cz) {
        final int r = 8;
        c.fillWith(cx - r, FLOOR, cz - r, cx + r, FLOOR, cz + r, (x, y, z) -> tile(x, z));
        c.fill(cx - r, FLOOR + 1, cz - r, cx + r, FLOOR + 9, cz + r, AIR);
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = x == cx - r || x == cx + r || z == cz - r || z == cz + r;
                if (!edge) continue;
                if (z == cz + r && Math.abs(x - cx) <= 1) continue;   // doorway
                int h = 1 + pct(x, z, 11) % 6;
                c.fillWith(x, FLOOR + 1, z, x, FLOOR + h, z, Ashen::darkBrick);
            }
        }
        for (int px : new int[]{cx - 4, cx + 4}) {
            for (int pz : new int[]{cz - 4, cz, cz + 4}) {
                int h = 3 + pct(px, pz, 12) % 4;
                c.fillWith(px, FLOOR + 1, pz, px, FLOOR + h, pz, Ashen::darkBrick);
            }
        }
        c.fill(cx - 1, FLOOR + 1, cz - r + 2, cx + 1, FLOOR + 1, cz - r + 2, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        c.chest(cx, FLOOR + 2, cz - r + 2, 3);
        c.lantern(cx - 1, FLOOR + 2, cz - r + 2);
        c.lantern(cx + 1, FLOOR + 2, cz - r + 2);
        c.spawn(1, cx - 3 + 0.5, FLOOR + 1, cz + 0.5);
        c.spawn(1, cx + 3 + 0.5, FLOOR + 1, cz + 0.5);
        c.spawn(3, cx + 0.5, FLOOR + 1, cz - 3 + 0.5);
        c.spawn(2, cx + 0.5, FLOOR + 1, cz + 2 + 0.5);
    }

    // ------------------------------------------------------------------ watchtower

    private static void watchtower(Canvas c, int cx, int cz) {
        final int r = 4;
        c.fillWith(cx - r, FLOOR, cz - r, cx + r, FLOOR, cz + r, (x, y, z) -> tile(x, z));
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = x == cx - r || x == cx + r || z == cz - r || z == cz + r;
                for (int y = FLOOR + 1; y <= FLOOR + 17; y++) {
                    c.set(x, y, z, edge ? darkBrick(x, y, z) : AIR);
                }
            }
        }
        c.fill(cx - 1, FLOOR + 1, cz + r, cx, FLOOR + 3, cz + r, AIR);              // door
        // Upper platform with a hole for the ladder.
        c.fillWith(cx - 3, FLOOR + 15, cz - 3, cx + 3, FLOOR + 15, cz + 3, (x, y, z) -> darkBrick(x, y, z));
        c.fill(cx, FLOOR + 15, cz - 3, cx, FLOOR + 15, cz - 3, AIR);
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
        for (int y = FLOOR + 1; y <= FLOOR + 16; y++) c.set(cx, y, cz - 3, ladder);
        // Crenellations.
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = x == cx - r || x == cx + r || z == cz - r || z == cz + r;
                if (edge && Math.floorMod(x + z, 2) == 0) c.set(x, FLOOR + 18, z, darkBrick(x, FLOOR + 18, z));
            }
        }
        c.chest(cx + 2, FLOOR + 1, cz + 2, 2);
        c.chest(cx + 2, FLOOR + 16, cz + 2, 3);
        c.hangingLantern(cx, FLOOR + 14, cz);
        c.spawn(0, cx - 2 + 0.5, FLOOR + 1, cz + 0.5);
        c.spawn(1, cx + 0.5, FLOOR + 1, cz + 2 + 0.5);
        c.spawn(5, cx - 2 + 0.5, FLOOR + 16, cz + 2 + 0.5);
        c.spawn(5, cx - 2 + 0.5, FLOOR + 16, cz - 1 + 0.5);
    }

    // ------------------------------------------------------------------ graveyard

    private static void graveyard(Canvas c, int cx, int cz) {
        for (int x = cx - 18; x <= cx + 18; x += 3) {
            for (int z = cz - 18; z <= cz + 18; z += 3) {
                if (Math.abs(x - cx) <= 6 && Math.abs(z - cz) <= 6) continue;
                if (pct(x, z, 21) < 55) {
                    c.set(x, FLOOR + 1, z, Blocks.COBBLESTONE_WALL.getDefaultState());
                    c.set(x, FLOOR + 2, z, Blocks.STONE_BRICK_WALL.getDefaultState());
                    c.set(x, FLOOR + 1, z + 1, Blocks.MOSSY_COBBLESTONE_SLAB.getDefaultState());
                    if (pct(x, z, 22) < 18) c.spawn(1, x + 0.5, FLOOR + 1, z - 1 + 0.5);
                }
            }
        }
        // Mausoleum.
        final int r = 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                boolean edge = x == cx - r || x == cx + r || z == cz - r || z == cz + r;
                c.set(x, FLOOR, z, tile(x, z));
                for (int y = FLOOR + 1; y <= FLOOR + 5; y++) {
                    c.set(x, y, z, edge ? darkBrick(x, y, z) : AIR);
                }
                c.set(x, FLOOR + 6, z, darkBrick(x, FLOOR + 6, z));
            }
        }
        c.fill(cx - 1, FLOOR + 1, cz + r, cx, FLOOR + 3, cz + r, AIR);
        c.chest(cx, FLOOR + 1, cz - 2, 3);
        c.hangingLantern(cx, FLOOR + 5, cz);
        c.spawn(1, cx - 2 + 0.5, FLOOR + 1, cz + 0.5);
        c.spawn(1, cx + 2 + 0.5, FLOOR + 1, cz + 0.5);
        c.spawn(2, cx + 0.5, FLOOR + 1, cz + 2 + 0.5);
        c.spawn(3, cx + 0.5, FLOOR + 1, cz + 0.5);
    }

    // ------------------------------------------------------------------ obelisk (rest stop)

    private static void obelisk(Canvas c, int cx, int cz) {
        c.fillWith(cx - 5, FLOOR, cz - 5, cx + 5, FLOOR, cz + 5, (x, y, z) -> tile(x, z));
        c.fill(cx - 5, FLOOR + 1, cz - 5, cx + 5, FLOOR + 8, cz + 5, AIR);
        for (int px : new int[]{cx - 5, cx + 5}) {
            for (int pz : new int[]{cz - 5, cz + 5}) {
                c.fillWith(px, FLOOR + 1, pz, px, FLOOR + 6, pz, Ashen::darkBrick);
                c.lantern(px, FLOOR + 7, pz);
            }
        }
        c.bonfire(cx, FLOOR + 1, cz);
        c.lantern(cx - 2, FLOOR + 1, cz);
        c.lantern(cx + 2, FLOOR + 1, cz);
    }
}
