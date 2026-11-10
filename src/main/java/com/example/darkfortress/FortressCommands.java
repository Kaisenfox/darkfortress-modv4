package com.example.darkfortress;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * /fortress status   - shows whether the mod loaded and the fortress exists (anyone)
 * /fortress tp       - teleports you to the fortress courtyard (anyone)
 * /fortress build    - (re)builds the fortress where you stand (ops, or always in singleplayer)
 * /fortress respawn  - respawns the fortress enemies
 * /fortress kit      - gives you one of every mod item
 * /fortress realm    - teleports you to the Ashen Realm
 * /fortress opengate - opens the sealed main gate (escape hatch)
 */
public final class FortressCommands {
    private FortressCommands() {}

    private static boolean allowed(ServerCommandSource s) {
        return s.hasPermissionLevel(2) || s.getServer().isSingleplayer();
    }

    public static void register(CommandDispatcher<ServerCommandSource> d, CommandRegistryAccess reg,
                                CommandManager.RegistrationEnvironment env) {
        d.register(CommandManager.literal("fortress")
                .then(CommandManager.literal("status").executes(c -> status(c.getSource())))
                .then(CommandManager.literal("tp").executes(c -> tp(c.getSource())))
                .then(CommandManager.literal("build").requires(FortressCommands::allowed).executes(c -> build(c.getSource())))
                .then(CommandManager.literal("respawn").requires(FortressCommands::allowed).executes(c -> respawn(c.getSource())))
                .then(CommandManager.literal("kit").requires(FortressCommands::allowed).executes(c -> kit(c.getSource())))
                .then(CommandManager.literal("opengate").requires(FortressCommands::allowed).executes(c -> openGate(c.getSource())))
                .then(CommandManager.literal("realm").requires(FortressCommands::allowed).executes(c -> realm(c.getSource()))));
    }

    private static int status(ServerCommandSource src) {
        String report = Fortress.statusReport(src.getServer().getOverworld());
        for (String line : report.split("\n")) {
            src.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    private static int tp(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        MinecraftServer server = src.getServer();
        BlockPos o = Fortress.origin(server.getOverworld());
        Ashen.tp(server, p, "minecraft:overworld", o.getX() + 0.5, o.getY(), o.getZ() + 4.5);
        return 1;
    }

    private static int build(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        MinecraftServer server = src.getServer();
        ServerWorld ow = server.getOverworld();
        if (p.getWorld().getRegistryKey() != World.OVERWORLD) {
            src.sendFeedback(() -> Text.literal("Run this in the Overworld."), false);
            return 0;
        }
        if (Fortress.isBuilding()) {
            src.sendFeedback(() -> Text.literal("The fortress is already being built."), false);
            return 0;
        }
        BlockPos here = p.getBlockPos();
        BlockPos o = Fortress.computeOrigin(ow, here.getX(), here.getZ());
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
                "execute in minecraft:overworld run setworldspawn " + o.getX() + " " + o.getY() + " " + o.getZ());
        Fortress.startBuild(ow, o);
        src.sendFeedback(() -> Text.literal("Building the Dark Fortress at " + o.toShortString() + " ..."), true);
        return 1;
    }

    private static int respawn(ServerCommandSource src) {
        Fortress.respawnMobs(src.getServer().getOverworld());
        src.sendFeedback(() -> Text.literal("Fortress enemies respawned."), true);
        return 1;
    }

    private static int kit(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        int n = 0;
        for (Identifier id : Registries.ITEM.getIds()) {
            if (id.getNamespace().equals(DarkFortressMod.MOD_ID)) {
                Item item = Registries.ITEM.get(id);
                p.giveItemStack(new ItemStack(item));
                n++;
            }
        }
        final int count = n;
        src.sendFeedback(() -> Text.literal("Gave you " + count + " Dark Fortress items."), false);
        return 1;
    }

    private static int openGate(ServerCommandSource src) {
        Fortress.openMainGate(src.getServer().getOverworld());
        src.sendFeedback(() -> Text.literal("The main gate has been opened."), true);
        return 1;
    }

    private static int realm(ServerCommandSource src) throws CommandSyntaxException {
        Ashen.sendToRealm(src.getServer(), src.getPlayerOrThrow());
        return 1;
    }
}
