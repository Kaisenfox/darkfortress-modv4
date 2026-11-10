package com.example.darkfortress;

import com.example.darkfortress.entity.BlackKnightEntity;
import com.example.darkfortress.entity.DSBoss;
import com.example.darkfortress.entity.DSMob;
import com.example.darkfortress.entity.DemonLordEntity;
import com.example.darkfortress.entity.GargoyleEntity;
import com.example.darkfortress.entity.HollowArcherEntity;
import com.example.darkfortress.entity.WitchOfAshEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Generates and manages the Dark Fortress at world spawn (97x97 blocks).
 * x east, z south, y relative to the courtyard floor, origin = world spawn.
 *
 *  Courtyard hub (bonfire at 0,0,3) with Barracks (east), Chapel + Witch boss (west),
 *  Belfry + Gargoyle boss (north-west), Graveyard (north-east), Market + bonfire (south).
 *  Curtain wall, ramparts, 4 corner towers (NW tower hides a secret).
 *  Catacombs: Black Knight hall (key), Ossuary + bonfire, Prison, Armory, secret alcove.
 *  The Keep: locked gate, great hall + bonfire, stairs to the Demon of the Keep.
 *  Beating the Demon opens a portal to the Ashen Realm dimension.
 */
public final class Fortress {
    public static final int FLAG_BUILT = 0;
    public static final int FLAG_KNIGHT = 1;
    public static final int FLAG_BOSS = 2;
    public static final int FLAG_GARGOYLE = 3;
    public static final int FLAG_WITCH = 4;

    public static final int SEAL_DEMON = 0;
    public static final int SEAL_GARGOYLE = 1;
    public static final int SEAL_WITCH = 2;

    public static final String ADD_TAG = "df_boss_add";

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final int NO_SUPPORT = Integer.MIN_VALUE;

    private static final int SOLDIER = 0, ARCHER = 1, SKELETON = 2, KNIGHT = 3, GHOUL = 4, MAGE = 5;

    // type, x, y, z (relative to origin)
    private static final int[][] SPAWNS = {
            // courtyard
            {SOLDIER, -22, 0, 12}, {SOLDIER, 22, 0, 10}, {SOLDIER, -30, 0, -4}, {SOLDIER, 30, 0, -6},
            {SOLDIER, -8, 0, -10}, {SOLDIER, 8, 0, -11}, {GHOUL, -20, 0, -22}, {GHOUL, 20, 0, -24},
            {KNIGHT, -40, 0, 10}, {KNIGHT, 40, 0, 4},
            // outer ward / market
            {SOLDIER, -24, 0, 36}, {SOLDIER, 24, 0, 34}, {MAGE, -30, 0, 42}, {MAGE, 30, 0, 41},
            {GHOUL, -10, 0, 28}, {GHOUL, 10, 0, 28},
            // ramparts
            {ARCHER, -47, 9, -24}, {ARCHER, -47, 9, -2}, {ARCHER, -47, 9, 26}, {ARCHER, 47, 9, -26},
            {ARCHER, 47, 9, -2}, {ARCHER, 47, 9, 24}, {ARCHER, -24, 9, 47}, {ARCHER, 24, 9, 47},
            {ARCHER, -24, 9, -47}, {ARCHER, 24, 9, -47},
            // towers
            {KNIGHT, 44, 9, 44}, {SOLDIER, -44, 9, 44}, {SOLDIER, 44, 9, -44}, {SKELETON, -44, 9, -42},
            // barracks
            {SOLDIER, 24, 0, 25}, {KNIGHT, 30, 0, 25}, {SOLDIER, 30, 0, 29},
            // chapel
            {MAGE, -27, 0, 22}, {SKELETON, -24, 0, 34}, {GHOUL, -22, 0, 28},
            // graveyard
            {SKELETON, 28, 0, -30}, {SKELETON, 34, 0, -28}, {SKELETON, 40, 0, -34}, {GHOUL, 26, 0, -38},
            {KNIGHT, 34, 0, -36},
            // catacombs: corridor
            {SKELETON, -10, -9, 3}, {SKELETON, 10, -9, 3}, {SOLDIER, -16, -9, 3}, {ARCHER, 18, -9, 3},
            {SKELETON, -30, -9, 3}, {SKELETON, -38, -9, 3}, {SOLDIER, -6, -9, 10}, {SOLDIER, 6, -9, 20},
            // catacombs: ossuary
            {SKELETON, -28, -9, 18}, {SKELETON, -29, -9, 14}, {MAGE, -30, -9, 19}, {GHOUL, -32, -9, 16},
            // catacombs: prison
            {SKELETON, -32, -9, 28}, {KNIGHT, -32, -9, 34}, {GHOUL, -36, -9, 26}, {SKELETON, -28, -9, 38},
            {MAGE, -32, -9, 38},
            // catacombs: armory
            {KNIGHT, 18, -9, 14}, {KNIGHT, 24, -9, 18}, {SOLDIER, 28, -9, 12},
            // keep great hall
            {SOLDIER, 8, 0, -26}, {SOLDIER, -8, 0, -28}, {KNIGHT, 0, 0, -29}, {ARCHER, -12, 0, -18},
            {ARCHER, 12, 0, -29}, {MAGE, 4, 0, -30}, {SOLDIER, -8, 0, -20},
    };

    // bonfire safe zones (relative); no regular enemy spawns within 10 blocks
    private static final int[][] BONFIRES = {
            {0, 0, 3}, {27, -9, 4}, {0, 0, -18}, {0, 0, 38}, {-39, -9, 7}, {0, 9, -21}
    };

    private Fortress() {}

    // ------------------------------------------------------------------ origin / flags

    // ------------------------------------------------------------------ stored origin

    private static BlockPos storedOrigin = null;
    private static boolean building = false;
    private static boolean buildFailed = false;
    private static boolean attempted = false;

    public static BlockPos storedOrigin() {
        return storedOrigin;
    }

    public static boolean isBuilding() {
        return building;
    }

    /** Reads the saved fortress location (kept in the world folder) at server start. */
    public static void loadOrigin(MinecraftServer server) {
        storedOrigin = null;
        building = false;
        attempted = false;
        try {
            Path f = server.getSavePath(WorldSavePath.ROOT).resolve("darkfortress_origin.txt");
            if (Files.exists(f)) {
                String[] p = Files.readString(f).trim().split("\\s+");
                storedOrigin = new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                DarkFortressMod.LOGGER.info("Dark Fortress origin loaded: {}", storedOrigin);
            }
        } catch (Exception e) {
            DarkFortressMod.LOGGER.warn("Could not read Dark Fortress origin file", e);
        }
    }

    private static void saveOrigin(MinecraftServer server, BlockPos o) {
        storedOrigin = o;
        try {
            Path f = server.getSavePath(WorldSavePath.ROOT).resolve("darkfortress_origin.txt");
            Files.writeString(f, o.getX() + " " + o.getY() + " " + o.getZ());
        } catch (IOException e) {
            DarkFortressMod.LOGGER.warn("Could not save Dark Fortress origin file", e);
        }
    }

    public static BlockPos computeOrigin(ServerWorld world, int x, int z) {
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        return new BlockPos(x, Math.max(top, world.getSeaLevel() + 1), z);
    }

    public static BlockPos origin(ServerWorld world) {
        if (storedOrigin != null) return storedOrigin;
        BlockPos sp = world.getSpawnPos();
        return computeOrigin(world, sp.getX(), sp.getZ());
    }

    private static BlockPos flagPos(ServerWorld world, int idx) {
        BlockPos o = origin(world);
        return new BlockPos(o.getX() - 44 + idx, o.getY() - 8, o.getZ() - 44);
    }

    public static boolean hasFlag(ServerWorld world, int idx) {
        return world.getBlockState(flagPos(world, idx)).isOf(Blocks.RED_NETHER_BRICKS);
    }

    public static void setFlag(ServerWorld world, int idx) {
        world.setBlockState(flagPos(world, idx), Blocks.RED_NETHER_BRICKS.getDefaultState(), 2);
    }

    // ------------------------------------------------------------------ public API

    public static void ensureBuilt(ServerWorld world) {
        if (building || attempted) return;
        if (hasFlag(world, FLAG_BUILT)) {
            if (storedOrigin == null) saveOrigin(world.getServer(), origin(world));
            return;
        }
        BlockPos o = origin(world);
        DarkFortressMod.LOGGER.info("Building the Dark Fortress at {}", o);
        startBuild(world, o);
    }

    private static void broadcast(ServerWorld world, String msg, boolean overlay) {
        DarkFortressMod.LOGGER.info(msg);
        world.getServer().getPlayerManager().broadcast(Text.literal(msg), overlay);
    }

    /** Builds the fortress in stages over several ticks so the game never freezes, reporting errors. */
    public static boolean startBuild(ServerWorld world, BlockPos o) {
        if (building) return false;
        building = true;
        attempted = true;
        buildFailed = false;
        saveOrigin(world.getServer(), o);
        final B b = new B(world, o.getX(), o.getY(), o.getZ());
        final String[] names = {"clearing the land", "clearing the sky", "laying the foundation", "walls and paving",
                "corner towers", "the Keep", "the catacombs", "courtyard buildings", "gloom and decay",
                "sealing the fortress", "bonfires"};
        final Runnable[] steps = {
                () -> clearLow(b), () -> clearHigh(b), () -> foundation(b), () -> wallsAndPaving(b),
                () -> buildTowers(b), () -> buildKeep(b), () -> buildCatacombs(b), () -> buildCourtyard(b),
                () -> atmosphere(b), () -> sealRing(b),
                () -> {
                    b.bonfire(0, 0, 3);
                    b.bonfire(0, 0, 38);
                }
        };
        for (int i = 0; i < steps.length; i++) {
            final int idx = i;
            Scheduler.later(5 + i * 4, () -> {
                broadcast(world, "Dark Fortress: " + names[idx] + " (" + (idx + 1) + "/" + (steps.length + 1) + ")", true);
                try {
                    steps[idx].run();
                } catch (Throwable t) {
                    buildFailed = true;
                    DarkFortressMod.LOGGER.error("Dark Fortress build step failed: " + names[idx], t);
                    broadcast(world, "Dark Fortress: step '" + names[idx] + "' FAILED: " + t, false);
                }
            });
        }
        Scheduler.later(5 + steps.length * 4, () -> finishBuild(world, o));
        return true;
    }

    private static void finishBuild(ServerWorld world, BlockPos o) {
        try {
            if (!buildFailed) {
                setFlag(world, FLAG_BUILT);
                respawnMobs(world);
            }
            for (ServerPlayerEntity p : world.getPlayers(pl ->
                    Math.abs(pl.getX() - o.getX()) < 52 && Math.abs(pl.getZ() - o.getZ()) < 52)) {
                Ashen.tp(world.getServer(), p, "minecraft:overworld", o.getX() + 0.5, o.getY(), o.getZ() + 4.5);
            }
            if (buildFailed) {
                broadcast(world, "Dark Fortress: finished WITH ERRORS. Check logs/latest.log and run /fortress status.", false);
            } else {
                broadcast(world, "The Dark Fortress is ready! (use /fortress tp to return to it)", false);
            }
        } catch (Throwable t) {
            DarkFortressMod.LOGGER.error("Dark Fortress finish failed", t);
            broadcast(world, "Dark Fortress: finishing failed: " + t, false);
        } finally {
            building = false;
        }
    }

    public static String statusReport(ServerWorld w) {
        StringBuilder s = new StringBuilder();
        s.append("Dark Fortress ").append(DarkFortressMod.VERSION).append(" is loaded.\n");
        s.append("Origin: ").append(storedOrigin == null ? "not set (fortress not built yet)" : storedOrigin.toShortString()).append("\n");
        s.append("Building now: ").append(building).append("\n");
        if (storedOrigin != null) {
            BlockPos o = storedOrigin;
            s.append("Built flag: ").append(hasFlag(w, FLAG_BUILT)).append("\n");
            s.append("Start bonfire present: ").append(w.getBlockState(o.add(0, 0, 3)).isOf(ModBlocks.BONFIRE)).append("\n");
            s.append("Defeated: Black Knight=").append(hasFlag(w, FLAG_KNIGHT))
                    .append(", Gargoyle=").append(hasFlag(w, FLAG_GARGOYLE))
                    .append(", Witch=").append(hasFlag(w, FLAG_WITCH))
                    .append(", Demon=").append(hasFlag(w, FLAG_BOSS)).append("\n");
            Box box = new Box(o.getX() - 55, o.getY() - 45, o.getZ() - 55, o.getX() + 55, o.getY() + 45, o.getZ() + 55);
            s.append("Loaded enemies near fortress: ").append(w.getEntitiesByClass(MobEntity.class, box, Fortress::isRegular).size()).append("\n");
        }
        s.append("Ashen Realm dimension available: ").append(w.getServer().getWorld(Ashen.WORLD) != null);
        return s.toString();
    }

    public static void setSeal(ServerWorld world, int which, boolean closed) {
        BlockPos o = origin(world);
        int x1, y1, z1, x2, y2, z2;
        if (which == SEAL_DEMON) {
            x1 = 12; y1 = 9; z1 = -25; x2 = 13; y2 = 11; z2 = -22;
        } else if (which == SEAL_GARGOYLE) {
            x1 = -31; y1 = 0; z1 = -28; x2 = -30; y2 = 3; z2 = -28;
        } else {
            x1 = -18; y1 = 0; z1 = 27; x2 = -18; y2 = 3; z2 = 28;
        }
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) {
                    BlockPos p = o.add(x, y, z);
                    if (closed) {
                        world.setBlockState(p, Blocks.IRON_BARS.getDefaultState(), 2);
                    } else if (world.getBlockState(p).isOf(Blocks.IRON_BARS)) {
                        world.setBlockState(p, AIR, 2);
                    }
                }
    }

    public static void sealArena(ServerWorld world) {
        setSeal(world, SEAL_DEMON, true);
    }

    public static void openArena(ServerWorld world) {
        setSeal(world, SEAL_DEMON, false);
    }

    private static boolean isRegular(MobEntity e) {
        if (e instanceof DemonLordEntity || e instanceof BlackKnightEntity || e instanceof DSBoss) return false;
        return e instanceof DSMob || e instanceof HollowArcherEntity;
    }

    private static boolean nearBonfire(int x, int y, int z) {
        for (int[] b : BONFIRES) {
            int dx = x - b[0], dy = y - b[1], dz = z - b[2];
            if (dx * dx + dy * dy + dz * dz <= 100) return true;
        }
        return false;
    }

    public static void respawnMobs(ServerWorld world) {
        BlockPos o = origin(world);
        Box box = new Box(o.getX() - 55, o.getY() - 45, o.getZ() - 55, o.getX() + 55, o.getY() + 45, o.getZ() + 55);

        for (MobEntity m : world.getEntitiesByClass(MobEntity.class, box, Fortress::isRegular)) {
            m.discard();
        }

        ItemStack greatsword = new ItemStack(ModItems.BLACK_KNIGHT_GREATSWORD);
        respawnBoss(world, box, FLAG_BOSS, DemonLordEntity.class, ModEntities.DEMON_LORD, 0.5, 9, -23.5, greatsword.copy(), SEAL_DEMON);
        respawnBoss(world, box, FLAG_KNIGHT, BlackKnightEntity.class, ModEntities.BLACK_KNIGHT, 0.5, -9, 15.5, greatsword.copy(), -1);
        respawnBoss(world, box, FLAG_GARGOYLE, GargoyleEntity.class, ModEntities.GARGOYLE, -29.5, 0, -35.5, ItemStack.EMPTY, SEAL_GARGOYLE);
        respawnBoss(world, box, FLAG_WITCH, WitchOfAshEntity.class, ModEntities.WITCH_OF_ASH, -30.5, 0, 28.5, ItemStack.EMPTY, SEAL_WITCH);

        for (int[] s : SPAWNS) {
            if (nearBonfire(s[1], s[2], s[3])) continue;
            spawnTyped(world, s[0], o.getX() + s[1] + 0.5, o.getY() + s[2], o.getZ() + s[3] + 0.5);
        }
    }

    private static void respawnBoss(ServerWorld world, Box box, int flag, Class<? extends MobEntity> cls,
                                    EntityType<? extends MobEntity> type, double rx, double ry, double rz,
                                    ItemStack weapon, int seal) {
        if (hasFlag(world, flag)) return;
        BlockPos o = origin(world);
        for (MobEntity e : world.getEntitiesByClass(cls, box, x -> true)) {
            if (e instanceof DemonLordEntity d) {
                d.clearBar();
            } else if (e instanceof DSBoss b) {
                b.clearBar();
            }
            e.discard();
        }
        if (seal >= 0) setSeal(world, seal, false);
        spawnMob(world, type, o.getX() + rx, o.getY() + ry, o.getZ() + rz, weapon);
    }

    private static void spawnTyped(ServerWorld w, int code, double x, double y, double z) {
        switch (code) {
            case SOLDIER -> spawnMob(w, ModEntities.HOLLOW_SOLDIER, x, y, z, new ItemStack(Items.IRON_SWORD));
            case ARCHER -> spawnMob(w, ModEntities.HOLLOW_ARCHER, x, y, z, new ItemStack(Items.BOW));
            case SKELETON -> spawnMob(w, ModEntities.SKELETON_WARRIOR, x, y, z, new ItemStack(Items.STONE_SWORD));
            case KNIGHT -> spawnMob(w, ModEntities.HOLLOW_KNIGHT, x, y, z, new ItemStack(ModItems.CLAYMORE));
            case GHOUL -> spawnMob(w, ModEntities.ASH_GHOUL, x, y, z, ItemStack.EMPTY);
            default -> spawnMob(w, ModEntities.HOLLOW_MAGE, x, y, z, new ItemStack(ModItems.SOUL_STAFF));
        }
    }

    public static MobEntity spawnMob(ServerWorld world, EntityType<? extends MobEntity> type,
                                     double x, double y, double z, ItemStack weapon) {
        MobEntity mob = type.create(world);
        if (mob == null) return null;
        mob.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0f, 0.0f);
        if (!weapon.isEmpty()) {
            mob.equipStack(EquipmentSlot.MAINHAND, weapon);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        }
        mob.setPersistent();
        world.spawnEntity(mob);
        return mob;
    }

    public static MobEntity spawnAddOf(ServerWorld world, EntityType<? extends MobEntity> type,
                                       double x, double y, double z, ItemStack weapon) {
        MobEntity m = spawnMob(world, type, x, y, z, weapon);
        if (m != null) m.addCommandTag(ADD_TAG);
        return m;
    }

    public static void spawnAdd(ServerWorld world, double x, double y, double z) {
        spawnAddOf(world, ModEntities.HOLLOW_SOLDIER, x, y, z, new ItemStack(Items.IRON_SWORD));
    }

    public static void onMiniBossDefeated(ServerWorld world, MobEntity boss, int flag, int seal, String title) {
        setFlag(world, flag);
        setSeal(world, seal, false);
        for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(boss) < 4096.0)) {
            Fx.title(p, title, Formatting.GOLD, null);
        }
    }

    public static void onBossDefeated(ServerWorld world, DemonLordEntity boss) {
        setFlag(world, FLAG_BOSS);
        openArena(world);
        openMainGate(world);

        BlockPos o = origin(world);
        BlockPos bp = o.add(0, 9, -21);
        world.setBlockState(bp, ModBlocks.BONFIRE.getDefaultState(), 3);
        world.setBlockState(bp.up(), Blocks.END_ROD.getDefaultState(), 3);

        // Portal pad to the Ashen Realm.
        for (int x = -1; x <= 1; x++)
            for (int z = -25; z <= -23; z++)
                world.setBlockState(o.add(x, 8, z), ModBlocks.ASHEN_PORTAL.getDefaultState(), 3);

        for (MobEntity m : world.getEntitiesByClass(MobEntity.class, boss.getBoundingBox().expand(60),
                e -> e.getCommandTags().contains(ADD_TAG))) {
            m.discard();
        }

        boss.dropStack(new ItemStack(ModItems.MOONLIGHT_GREATSWORD));
        boss.dropStack(new ItemStack(ModItems.SOUL_GREAT_DEMON));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_HELM));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_ARMOR));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_LEGGINGS));
        boss.dropStack(new ItemStack(ModItems.BLACK_KNIGHT_BOOTS));

        for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(boss) < 4096.0)) {
            Fx.title(p, "DEMON SLAIN", Formatting.GOLD, "A portal has opened...");
            p.sendMessage(Text.literal("A glowing pad has appeared. Step on it to enter the Ashen Realm, or wait: you'll be drawn in shortly.").formatted(Formatting.GRAY));
        }

        final Vec3dHolder center = new Vec3dHolder(o.getX() + 0.5, o.getY() + 9, o.getZ() - 22.5);
        Scheduler.later(300, () -> {
            for (ServerPlayerEntity p : world.getPlayers(pl -> pl.squaredDistanceTo(center.x, center.y, center.z) < 1600.0)) {
                Ashen.sendToRealm(world.getServer(), p);
            }
        });
    }

    private record Vec3dHolder(double x, double y, double z) {}

    // ------------------------------------------------------------------ builder helper

    private static final class B {
        final ServerWorld w;
        final int ox, oy, oz;
        final Random rnd = new Random(20260607L);
        final BlockPos.Mutable m = new BlockPos.Mutable();

        B(ServerWorld w, int ox, int oy, int oz) {
            this.w = w;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }

        BlockState get(int x, int y, int z) {
            return w.getBlockState(m.set(ox + x, oy + y, oz + z));
        }

        void set(int x, int y, int z, BlockState s) {
            w.setBlockState(m.set(ox + x, oy + y, oz + z), s, 2);
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
            fillWith(x1, y1, z1, x2, y2, z2, () -> s);
        }

        void fillWith(int x1, int y1, int z1, int x2, int y2, int z2, Supplier<BlockState> s) {
            int ax = Math.min(x1, x2), bx = Math.max(x1, x2);
            int ay = Math.min(y1, y2), by = Math.max(y1, y2);
            int az = Math.min(z1, z2), bz = Math.max(z1, z2);
            for (int x = ax; x <= bx; x++)
                for (int y = ay; y <= by; y++)
                    for (int z = az; z <= bz; z++)
                        set(x, y, z, s.get());
        }

        int hash(int x, int z) {
            return new Random(x * 341873128712L + z * 132897987541L).nextInt(100);
        }

        BlockState brick() {
            int r = rnd.nextInt(100);
            if (r < 62) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 80) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            if (r < 95) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            return Blocks.COBBLESTONE.getDefaultState();
        }

        BlockState dark() {
            int r = rnd.nextInt(100);
            if (r < 55) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
            if (r < 80) return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            if (r < 92) return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            return Blocks.BLACKSTONE.getDefaultState();
        }

        BlockState pave() {
            int r = rnd.nextInt(100);
            if (r < 55) return Blocks.STONE_BRICKS.getDefaultState();
            if (r < 75) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            if (r < 90) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            if (r < 96) return Blocks.COBBLESTONE.getDefaultState();
            return Blocks.COARSE_DIRT.getDefaultState();
        }

        void stairs(int x1, int x2, int y0, int z0, int dy, int dz, int steps, Direction facing,
                    Block stair, int supportFrom, Supplier<BlockState> support) {
            BlockState st = stair.getDefaultState().with(StairsBlock.FACING, facing);
            for (int i = 0; i < steps; i++) {
                int y = y0 + dy * i;
                int z = z0 + dz * i;
                fill(x1, y + 1, z, x2, y + 3, z, AIR);
                if (supportFrom != NO_SUPPORT && y - 1 >= supportFrom) {
                    fillWith(x1, supportFrom, z, x2, y - 1, z, support);
                }
                fill(x1, y, z, x2, y, z, st);
            }
        }

        void lantern(int x, int y, int z, boolean hanging) {
            BlockState s = Blocks.SOUL_LANTERN.getDefaultState();
            if (hanging) s = s.with(LanternBlock.HANGING, true);
            set(x, y, z, s);
        }

        void bonfire(int x, int y, int z) {
            set(x, y, z, ModBlocks.BONFIRE.getDefaultState());
            set(x, y + 1, z, Blocks.END_ROD.getDefaultState());
        }

        ChestBlockEntity placeChest(int x, int y, int z) {
            set(x, y, z, Blocks.CHEST.getDefaultState());
            BlockEntity be = w.getBlockEntity(new BlockPos(ox + x, oy + y, oz + z));
            return be instanceof ChestBlockEntity c ? c : null;
        }

        void chest(int x, int y, int z, int tier) {
            ChestBlockEntity c = placeChest(x, y, z);
            if (c != null) Loot.fill(c, new BlockPos(ox + x, oy + y, oz + z).asLong(), tier);
        }

        void chestItems(int x, int y, int z, ItemStack... stacks) {
            ChestBlockEntity c = placeChest(x, y, z);
            if (c != null) {
                for (int i = 0; i < stacks.length; i++) c.setStack(i, stacks[i]);
            }
        }
    }

    // ------------------------------------------------------------------ the build

    /** An invisible barrier wall all around the fortress so nobody can climb or build their way out. */
    private static void sealRing(B b) {
        BlockState bar = Blocks.BARRIER.getDefaultState();
        b.fill(-49, -14, -49, 49, 60, -49, bar);
        b.fill(-49, -14, 49, 49, 60, 49, bar);
        b.fill(-49, -14, -48, -49, 60, 48, bar);
        b.fill(49, -14, -48, 49, 60, 48, bar);
    }

    /** Called when the Demon of the Keep dies: the gate opens and the barrier lifts. */
    public static void openMainGate(ServerWorld world) {
        BlockPos o = origin(world);
        for (int x = -4; x <= 4; x++)
            for (int y = 0; y <= 6; y++)
                for (int z = 46; z <= 48; z++) {
                    BlockPos p = o.add(x, y, z);
                    if (world.getBlockState(p).isOf(ModBlocks.SEALED_GATE)) world.breakBlock(p, false);
                }
        for (int x = -7; x <= 7; x++)
            for (int y = -2; y <= 40; y++) {
                BlockPos p = o.add(x, y, 49);
                if (world.getBlockState(p).isOf(Blocks.BARRIER)) world.setBlockState(p, AIR, 2);
            }
        for (ServerPlayerEntity pl : world.getPlayers(q -> q.squaredDistanceTo(o.getX(), o.getY(), o.getZ()) < 6400.0)) {
            Fx.title(pl, "THE GATE OPENS", Formatting.GOLD, "You are no longer trapped");
        }
    }

    private static void deadTree(B b, int x, int z, int seed) {
        Random r = new Random(seed * 7919L + x * 31L + z);
        int h = 6 + r.nextInt(4);
        BlockState log = Blocks.DARK_OAK_LOG.getDefaultState();
        for (int y = 0; y < h; y++) b.set(x, y, z, log);
        for (int k = 0; k < 3; k++) {
            int by = 3 + r.nextInt(h - 3);
            Direction d = Direction.fromHorizontal(r.nextInt(4));
            BlockState bl = log.with(PillarBlock.AXIS, d.getAxis());
            int len = 2 + r.nextInt(2);
            for (int i = 1; i <= len; i++) {
                b.set(x + d.getOffsetX() * i, by + i / 2, z + d.getOffsetZ() * i, bl);
            }
            b.set(x + d.getOffsetX() * (len + 1), by + len / 2, z + d.getOffsetZ() * (len + 1), Blocks.COBWEB.getDefaultState());
        }
    }

    /** Gloom: cages, skulls on spikes, barricades, bone piles and dead trees. */
    private static void atmosphere(B b) {
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        BlockState skull = Blocks.SKELETON_SKULL.getDefaultState();
        int[][] cages = {{-26, -8}, {26, -15}, {-40, 12}, {40, 14}, {-12, 22}, {16, 10}, {-28, 43}, {28, 43}};
        for (int[] c : cages) {
            b.fill(c[0] - 1, 0, c[1] - 1, c[0] + 1, 3, c[1] + 1, bars);
            b.fill(c[0], 0, c[1], c[0], 2, c[1], AIR);
            b.set(c[0], 0, c[1], skull);
        }
        // Skulls on spikes flanking the sealed gate.
        for (int dx : new int[]{-8, -7, 7, 8}) {
            b.fill(dx, 0, 44, dx, 1, 44, Blocks.STONE_BRICK_WALL.getDefaultState());
            b.set(dx, 2, 44, skull);
        }
        // Barricades and cobwebs.
        BlockState fence = Blocks.SPRUCE_FENCE.getDefaultState();
        b.fill(-12, 0, 42, -9, 1, 42, fence);
        b.fill(9, 0, 42, 12, 1, 42, fence);
        b.set(-10, 2, 42, Blocks.COBWEB.getDefaultState());
        b.set(10, 2, 42, Blocks.COBWEB.getDefaultState());
        // Bone piles near the gate.
        for (int i = 0; i < 40; i++) {
            int x = -14 + b.rnd.nextInt(29);
            int z = 40 + b.rnd.nextInt(6);
            if (b.get(x, 0, z).isAir()) {
                b.set(x, 0, z, Blocks.BONE_BLOCK.getDefaultState());
                if (b.rnd.nextInt(4) == 0) b.set(x, 1, z, skull);
            }
        }
        // Dead trees.
        int[][] trees = {{-42, -12}, {42, -14}, {-41, 40}, {42, 40}, {-8, -38}, {9, -39}, {-42, 26}, {42, -8}};
        int seed = 1;
        for (int[] t : trees) deadTree(b, t[0], t[1], seed++);
    }

    private static void clearLow(B b) {
        b.fill(-49, 0, -49, 49, 12, 49, AIR);
    }

    private static void clearHigh(B b) {
        b.fill(-49, 13, -49, 49, 34, 49, AIR);
    }

    private static void foundation(B b) {
        BlockState stone = Blocks.STONE.getDefaultState();
        b.fill(-48, -14, -48, 48, -2, 48, stone);
        BlockState bed = Blocks.BEDROCK.getDefaultState();
        b.fill(-48, -14, -48, 48, -14, 48, bed);
        b.fill(-48, -13, -48, 48, -2, -47, bed);
        b.fill(-48, -13, 47, 48, -2, 48, bed);
        b.fill(-48, -13, -46, -47, -2, 46, bed);
        b.fill(47, -13, -46, 48, -2, 46, bed);
        for (int x = -48; x <= 48; x++) {
            for (int z = -48; z <= 48; z++) {
                if (Math.abs(x) < 45 && Math.abs(z) < 45) continue;
                for (int y = -15; y >= -40; y--) {
                    BlockState s = b.get(x, y, z);
                    if (!(s.isAir() || !s.getFluidState().isEmpty())) break;
                    b.set(x, y, z, stone);
                }
            }
        }
    }

    private static void wallsAndPaving(B b) {
        b.fillWith(-45, -1, -45, 45, -1, 45, b::pave);

        // Curtain wall.
        b.fillWith(-48, -1, -48, 48, 8, -46, b::brick);
        b.fillWith(-48, -1, 46, 48, 8, 48, b::brick);
        b.fillWith(-48, -1, -45, -46, 8, 45, b::brick);
        b.fillWith(46, -1, -45, 48, 8, 45, b::brick);
        for (int i = -48; i <= 48; i += 2) {
            for (int y = 9; y <= 10; y++) {
                b.set(i, y, -48, b.brick());
                b.set(i, y, 48, b.brick());
                b.set(-48, y, i, b.brick());
                b.set(48, y, i, b.brick());
            }
        }
        // South gate: sealed until the Demon of the Keep falls.
        b.fill(-4, 0, 46, 4, 6, 48, ModBlocks.SEALED_GATE.getDefaultState());

        // Rampart stairs: east and west walls, south and north runs.
        for (int side : new int[]{-1, 1}) {
            int x1 = side == -1 ? -45 : 44;
            int x2 = side == -1 ? -44 : 45;
            b.stairs(x1, x2, 0, 8, 1, 1, 9, Direction.SOUTH, Blocks.STONE_BRICK_STAIRS, -1, b::brick);
            b.stairs(x1, x2, 0, -8, 1, -1, 9, Direction.NORTH, Blocks.STONE_BRICK_STAIRS, -1, b::brick);
        }
    }

    private static void buildTowers(B b) {
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int xa = sx * 40, xb = sx * 48, za = sz * 40, zb = sz * 48;
                b.fillWith(xa, -1, za, xb, 15, zb, b::brick);
                b.fill(sx * 41, 9, sz * 41, sx * 47, 14, sz * 47, AIR);
                int minX = Math.min(xa, xb), maxX = Math.max(xa, xb);
                int minZ = Math.min(za, zb), maxZ = Math.max(za, zb);
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                        if (edge && Math.floorMod(x + z, 2) == 0) b.set(x, 16, z, b.brick());
                    }
                }
                if (!(sx == -1 && sz == -1)) {
                    b.fill(sx * 40, 9, sz * 47, sx * 40, 10, sz * 47, AIR);
                }
                b.fill(sx * 47, 9, sz * 40, sx * 47, 10, sz * 40, AIR);
                b.lantern(sx * 47, 9, sz * 47, false);
            }
        }
        // NW tower secret: an illusory wall splits the tower; the chamber behind holds a chest.
        b.fill(-47, 9, -44, -41, 14, -44, ModBlocks.ILLUSORY_WALL.getDefaultState());
        b.chestItems(-44, 9, -47,
                new ItemStack(ModItems.RING_OF_FAVOR),
                new ItemStack(ModItems.PYROMANCY_FLAME),
                new ItemStack(ModItems.SOUL_LOST_UNDEAD, 6));
        b.chest(46, 9, 46, 2);
        b.chest(46, 9, -46, 1);
        b.chest(-46, 9, 46, 1);
    }

    private static void buildKeep(B b) {
        b.fillWith(-15, -1, -32, 15, 24, -14, b::dark);
        b.fill(-13, 0, -30, 13, 7, -16, AIR);     // great hall
        b.fill(-13, 9, -30, 13, 21, -16, AIR);    // boss arena
        for (int x = -15; x <= 15; x += 2) {
            b.set(x, 25, -32, b.dark());
            b.set(x, 25, -14, b.dark());
        }
        for (int z = -32; z <= -14; z += 2) {
            b.set(-15, 25, z, b.dark());
            b.set(15, 25, z, b.dark());
        }
        b.fill(-2, 0, -15, 2, 4, -14, ModBlocks.KEY_GATE.getDefaultState());
        for (int px : new int[]{-8, 8}) {
            for (int pz : new int[]{-19, -23, -27}) {
                b.fillWith(px, 0, pz, px, 7, pz, b::dark);
            }
        }
        for (int lx : new int[]{-4, 4}) {
            for (int lz : new int[]{-18, -24, -28}) {
                b.lantern(lx, 7, lz, true);
            }
        }
        b.bonfire(0, 0, -18);
        b.chest(-12, 0, -30, 2);
        b.stairs(12, 13, 0, -17, 1, -1, 9, Direction.NORTH, Blocks.DEEPSLATE_BRICK_STAIRS, 0, b::dark);
        for (int px : new int[]{-9, 9}) {
            for (int pz : new int[]{-20, -26}) {
                b.fillWith(px, 9, pz, px, 20, pz, b::dark);
            }
        }
        for (int lx : new int[]{-5, 5}) {
            for (int lz : new int[]{-20, -28}) {
                b.lantern(lx, 21, lz, true);
            }
        }
        b.set(-6, 9, -23, Blocks.SOUL_CAMPFIRE.getDefaultState());
        b.set(6, 9, -23, Blocks.SOUL_CAMPFIRE.getDefaultState());
    }

    private static void buildCatacombs(B b) {
        // Carve the rooms.
        b.fill(-23, -9, 1, -19, -5, 5, AIR);       // landing
        b.fill(-40, -9, 2, 24, -5, 4, AIR);        // main corridor (long)
        b.fill(-9, -9, 6, 9, -3, 24, AIR);         // Black Knight hall
        b.fill(-2, -9, 5, 2, -6, 5, AIR);          // hall doorway
        b.fill(24, -9, 0, 30, -5, 7, AIR);         // east bonfire room
        b.fill(-14, -9, 6, -11, -6, 10, AIR);      // secret alcove
        b.fill(-12, -9, 5, -12, -6, 5, ModBlocks.ILLUSORY_WALL.getDefaultState());
        // Ossuary (west) and prison below it.
        b.fill(-39, -9, 6, -27, -5, 20, AIR);
        b.fill(-33, -9, 5, -31, -6, 5, AIR);
        b.fill(-33, -9, 21, -31, -6, 23, AIR);
        b.fill(-33, -9, 24, -31, -5, 40, AIR);
        for (int cz : new int[]{25, 29, 33, 37}) {
            b.fill(-37, -9, cz, -35, -6, cz + 2, AIR);
            b.fill(-29, -9, cz, -27, -6, cz + 2, AIR);
            b.fill(-34, -9, cz + 1, -34, -7, cz + 1, AIR);
            b.fill(-30, -9, cz + 1, -30, -7, cz + 1, AIR);
            b.fill(-34, -9, cz, -34, -6, cz, Blocks.IRON_BARS.getDefaultState());
            b.fill(-34, -9, cz + 2, -34, -6, cz + 2, Blocks.IRON_BARS.getDefaultState());
            b.fill(-30, -9, cz, -30, -6, cz, Blocks.IRON_BARS.getDefaultState());
            b.fill(-30, -9, cz + 2, -30, -6, cz + 2, Blocks.IRON_BARS.getDefaultState());
        }
        // Armory (east).
        b.fill(15, -9, 5, 17, -6, 8, AIR);
        b.fill(12, -9, 9, 30, -5, 22, AIR);

        // Stairs: courtyard -> landing, bonfire room -> courtyard.
        b.stairs(-22, -20, -1, -9, -1, 1, 10, Direction.NORTH, Blocks.STONE_BRICK_STAIRS, NO_SUPPORT, b::brick);
        b.stairs(26, 28, -10, -1, 1, -1, 10, Direction.NORTH, Blocks.STONE_BRICK_STAIRS, NO_SUPPORT, b::brick);

        // Dressing and light.
        for (int px : new int[]{-5, 5}) {
            for (int pz : new int[]{10, 18}) {
                b.fillWith(px, -9, pz, px, -3, pz, b::dark);
            }
        }
        for (int lx = -36; lx <= 16; lx += 8) b.lantern(lx, -5, 3, true);
        for (int lx : new int[]{-7, 7}) {
            for (int lz : new int[]{8, 16, 22}) b.lantern(lx, -3, lz, true);
        }
        b.lantern(-21, -5, 3, true);
        b.lantern(27, -5, 5, true);
        b.lantern(-12, -6, 8, true);
        b.lantern(-33, -5, 10, true);
        b.lantern(-33, -5, 16, true);
        b.lantern(-32, -5, 28, true);
        b.lantern(-32, -5, 36, true);
        for (int lx : new int[]{16, 24}) {
            for (int lz : new int[]{12, 20}) b.lantern(lx, -5, lz, true);
        }
        b.bonfire(27, -9, 4);
        b.bonfire(-39, -9, 7);

        // Loot.
        b.chestItems(-12, -9, 9,
                new ItemStack(ModItems.LIGHTNING_SPEAR),
                new ItemStack(ModItems.DIVINE_BLESSING, 2),
                new ItemStack(ModItems.SOUL_LOST_UNDEAD, 6));
        b.chest(-28, -9, 8, 2);
        b.chest(-38, -9, 19, 2);
        b.chest(-37, -9, 26, 2);
        b.chest(-27, -9, 30, 2);
        b.chest(-37, -9, 38, 3);
        b.chest(13, -9, 10, 2);
        b.chest(13, -9, 21, 2);
        b.chest(29, -9, 21, 3);
        b.chest(-8, -9, 23, 2);
        b.set(20, -9, 15, Blocks.ANVIL.getDefaultState());
        b.set(21, -9, 15, Blocks.GRINDSTONE.getDefaultState());

        // Ossuary bones and cobwebs.
        for (int x = -39; x <= -27; x++) {
            for (int z = 6; z <= 20; z++) {
                int h = b.hash(x, z);
                if (h < 10 && !(x == -39 && z == 7)) {
                    b.set(x, -9, z, Blocks.BONE_BLOCK.getDefaultState());
                    if (h % 3 == 0) b.set(x, -8, z, Blocks.SKELETON_SKULL.getDefaultState());
                }
            }
        }
        for (int i = 0; i < 70; i++) {
            int x = -40 + b.rnd.nextInt(65);
            int z = 2 + b.rnd.nextInt(3);
            if (b.get(x, -5, z).isAir()) b.set(x, -5, z, Blocks.COBWEB.getDefaultState());
        }
    }

    private static void buildCourtyard(B b) {
        // ---- Barracks (east) ----
        b.fillWith(18, -1, 18, 36, 7, 32, b::brick);
        b.fill(19, 0, 19, 35, 5, 31, AIR);
        b.fill(18, 0, 24, 18, 2, 25, AIR);
        for (int x : new int[]{22, 28, 34}) for (int z : new int[]{22, 28}) b.lantern(x, 5, z, true);
        b.chest(21, 0, 20, 1);
        b.chest(27, 0, 20, 1);
        b.chest(33, 0, 20, 2);
        b.set(34, 0, 30, Blocks.BARREL.getDefaultState());
        b.set(33, 0, 30, Blocks.BARREL.getDefaultState());

        // ---- Chapel (west): Witch of Ash ----
        b.fillWith(-36, -1, 18, -18, 11, 38, b::dark);
        b.fill(-35, 0, 19, -19, 9, 37, AIR);
        b.fill(-18, 0, 27, -18, 3, 28, AIR);
        for (int z = 22; z <= 34; z += 4) {
            b.fill(-36, 3, z, -36, 7, z + 1, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        }
        for (int x = -32; x <= -22; x += 5) {
            b.fill(x, 3, 18, x + 1, 7, 18, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
            b.fill(x, 3, 38, x + 1, 7, 38, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
        }
        for (int z : new int[]{22, 26, 30, 34}) {
            for (int x : new int[]{-31, -23}) b.fillWith(x, 0, z, x, 9, z, b::dark);
        }
        b.fill(-35, 0, 26, -34, 1, 30, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        b.set(-35, 2, 28, Blocks.SOUL_CAMPFIRE.getDefaultState());
        for (int[] l : new int[][]{{-27, 24}, {-27, 32}, {-33, 28}, {-21, 28}}) b.lantern(l[0], 9, l[1], true);
        b.chest(-34, 0, 20, 2);
        b.chest(-34, 0, 36, 2);

        // ---- Belfry (north-west): Gargoyle ----
        b.fillWith(-38, -1, -44, -22, 13, -28, b::dark);
        b.fill(-37, 0, -43, -23, 11, -29, AIR);
        b.fill(-31, 0, -28, -30, 3, -28, AIR);
        b.fillWith(-33, 14, -39, -27, 26, -33, b::dark);
        for (int x = -33; x <= -27; x++) {
            for (int z = -39; z <= -33; z++) {
                boolean edge = x == -33 || x == -27 || z == -39 || z == -33;
                if (edge && Math.floorMod(x + z, 2) == 0) b.set(x, 27, z, b.dark());
            }
        }
        b.set(-30, 27, -36, Blocks.BELL.getDefaultState());
        for (int px : new int[]{-34, -26}) for (int pz : new int[]{-40, -32}) b.fillWith(px, 0, pz, px, 11, pz, b::dark);
        for (int[] l : new int[][]{{-30, -36}, {-34, -36}, {-26, -36}, {-30, -40}}) b.lantern(l[0], 11, l[1], true);
        b.chest(-36, 0, -42, 2);
        b.chest(-24, 0, -42, 2);

        // ---- Graveyard and mausoleum (north-east) ----
        b.fillWith(30, -1, -40, 38, 6, -32, b::dark);
        b.fill(31, 0, -39, 37, 4, -33, AIR);
        b.fill(30, 0, -36, 30, 2, -36, AIR);
        b.chest(37, 0, -36, 2);
        b.lantern(34, 4, -36, true);
        for (int x = 24; x <= 44; x += 3) {
            for (int z = -44; z <= -24; z += 3) {
                if (x >= 29 && x <= 39 && z >= -41 && z <= -31) continue;
                if (x >= 39 && z <= -39) continue;
                if (b.hash(x, z) < 55) {
                    b.set(x, 0, z, Blocks.COBBLESTONE_WALL.getDefaultState());
                    b.set(x, 1, z, Blocks.STONE_BRICK_WALL.getDefaultState());
                    b.set(x, 0, z + 1, Blocks.MOSSY_COBBLESTONE_SLAB.getDefaultState());
                }
            }
        }
        b.chest(26, 0, -26, 1);

        // ---- Market stalls (south) ----
        for (int sx : new int[]{-30, -14, 14, 30}) {
            int z = 38;
            for (int dx = -2; dx <= 2; dx += 4) {
                for (int dz = -2; dz <= 2; dz += 4) {
                    b.fill(sx + dx, 0, z + dz, sx + dx, 2, z + dz, Blocks.COBBLESTONE_WALL.getDefaultState());
                }
            }
            b.fill(sx - 2, 3, z - 2, sx + 2, 3, z + 2, Blocks.COBBLESTONE_SLAB.getDefaultState());
            if (sx == -30 || sx == 14) {
                b.chest(sx, 0, z, 1);
            } else {
                b.set(sx, 0, z, Blocks.BARREL.getDefaultState());
            }
        }

        // ---- Pillars, rubble, lanterns (never touch the spawn column x=0,z=0) ----
        int[][] pillars = {{-12, 6}, {12, 6}, {-12, -6}, {12, -6}, {-24, -10}, {24, -10}, {-40, 28}, {40, 36}};
        for (int[] p : pillars) {
            int h = 3 + b.rnd.nextInt(5);
            b.fillWith(p[0], 0, p[1], p[0], h, p[1], b::brick);
        }
        for (int i = 0; i < 90; i++) {
            int x = -44 + b.rnd.nextInt(89);
            int z = -12 + b.rnd.nextInt(56);
            if (Math.abs(x) <= 4 && z >= -4 && z <= 8) continue;
            if (x >= -23 && x <= -19 && z >= -9 && z <= 0) continue;
            if (x >= 25 && x <= 29 && z >= -11 && z <= -1) continue;
            if (Math.abs(x) >= 43 && ((z >= 7 && z <= 17) || (z >= -17 && z <= -7))) continue;
            if (x >= 17 && x <= 37 && z >= 17 && z <= 33) continue;
            if (x >= -37 && x <= -17 && z >= 17 && z <= 39) continue;
            if (x >= -4 && x <= 4 && z >= 34 && z <= 42) continue;
            if (b.get(x, 0, z).isAir() && !b.get(x, -1, z).isAir()) {
                b.set(x, 0, z, b.rnd.nextBoolean()
                        ? Blocks.COBBLESTONE_SLAB.getDefaultState()
                        : Blocks.MOSSY_COBBLESTONE.getDefaultState());
            }
        }
        b.set(-5, 0, 6, Blocks.SOUL_CAMPFIRE.getDefaultState());
        b.set(5, 0, 6, Blocks.SOUL_CAMPFIRE.getDefaultState());
        for (int t : new int[]{-40, -28, 0, 28, 40}) {
            b.lantern(-44, 0, t, false);
            b.lantern(44, 0, t, false);
        }
        for (int x : new int[]{-36, -24, -12, 12, 24, 36}) b.lantern(x, 0, 44, false);
    }
}
