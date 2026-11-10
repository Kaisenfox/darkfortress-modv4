package com.example.darkfortress;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarkFortressMod implements ModInitializer {
    public static final String MOD_ID = "darkfortress";
    public static final String VERSION = "2.2.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ---- Tweakables ----
    /** Keep your items when you die (so you never lose the key or loot). */
    public static final boolean KEEP_INVENTORY = true;
    /** Stop natural monster spawns so only the fortress enemies exist. */
    public static final boolean DISABLE_MOB_SPAWNING = true;
    /** Freeze the overworld at dusk for a gloomier mood. */
    public static final boolean ETERNAL_DUSK = true;
    /** Permanent drizzle for a gloomier courtyard. */
    public static final boolean GLOOMY_WEATHER = true;

    @Override
    public void onInitialize() {
        Scheduler.init();
        ModArmor.init();
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        Ashen.init();
        Atmosphere.init();
        registerCreativeTab();
        CommandRegistrationCallback.EVENT.register(FortressCommands::register);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GameRules rules = server.getGameRules();
            rules.get(GameRules.SPAWN_RADIUS).set(0, server);
            if (KEEP_INVENTORY) rules.get(GameRules.KEEP_INVENTORY).set(true, server);
            if (DISABLE_MOB_SPAWNING) rules.get(GameRules.DO_MOB_SPAWNING).set(false, server);
            if (ETERNAL_DUSK) {
                rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                server.getOverworld().setTimeOfDay(14000L);
            }
            if (GLOOMY_WEATHER) {
                rules.get(GameRules.DO_WEATHER_CYCLE).set(false, server);
                server.getOverworld().setWeather(0, 1000000, true, false);
            }
            Fortress.loadOrigin(server);
            Fortress.ensureBuilt(server.getOverworld());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            p.sendMessage(Text.literal("Dark Fortress " + VERSION + " loaded. Type /fortress status to check it.").formatted(Formatting.DARK_GRAY));
            Fortress.ensureBuilt(server.getOverworld());
            Stats.apply(p);
            if (p.getCommandTags().contains("df_kit")) return;
            p.addCommandTag("df_kit");
            p.giveItemStack(new ItemStack(ModItems.ESTUS_FLASK));
            p.giveItemStack(new ItemStack(ModItems.BROKEN_SWORD));
            p.giveItemStack(new ItemStack(Items.SHIELD));
            p.giveItemStack(new ItemStack(ModItems.GREEN_BLOSSOM, 2));
            ServerWorld w = server.getOverworld();
            BlockPos o = Fortress.origin(w);
            p.setSpawnPoint(w.getRegistryKey(), o.add(0, 0, 4), 0.0f, true, false);
            Fx.title(p, "YOU ARE TRAPPED", Formatting.DARK_RED, "The gate is sealed. Something must fall before it opens.");
            p.sendMessage(Text.literal("You are Undead. Light bonfires (right-click) to rest, repair your gear and refill your Estus Flask.").formatted(Formatting.GOLD));
            p.sendMessage(Text.literal("Right-click a bonfire to rest and spend XP levels on your stats. Slay the Demon of the Keep to break the seal.").formatted(Formatting.GRAY));
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                Fx.title(p, "YOU DIED", Formatting.DARK_RED, null);
            }
        });

        // Respawning repairs all your gear and refills Estus; the fortress resets.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Fx.repairAll(newPlayer);
            Stats.apply(newPlayer);
            if (newPlayer.getServer() != null) {
                Fortress.respawnMobs(newPlayer.getServer().getOverworld());
            }
        });
    }

    private void registerCreativeTab() {
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.ESTUS_FLASK))
                .displayName(Text.translatable("itemGroup.darkfortress.main"))
                .entries((context, entries) -> {
                    entries.add(ModItems.ESTUS_FLASK);
                    entries.add(ModItems.GREEN_BLOSSOM);
                    entries.add(ModItems.FIREBOMB);
                    entries.add(ModItems.DIVINE_BLESSING);
                    entries.add(ModItems.BROKEN_SWORD);
                    entries.add(ModItems.LONGSWORD);
                    entries.add(ModItems.HOLLOW_DAGGER);
                    entries.add(ModItems.HATCHET);
                    entries.add(ModItems.WINGED_SPEAR);
                    entries.add(ModItems.UCHIGATANA);
                    entries.add(ModItems.HALBERD);
                    entries.add(ModItems.WARHAMMER);
                    entries.add(ModItems.CLAYMORE);
                    entries.add(ModItems.BLACK_KNIGHT_GREATSWORD);
                    entries.add(ModItems.MOONLIGHT_GREATSWORD);
                    entries.add(ModItems.TEST_BLADE);
                    entries.add(ModItems.SOUL_STAFF);
                    entries.add(ModItems.PYROMANCY_FLAME);
                    entries.add(ModItems.LIGHTNING_SPEAR);
                    entries.add(ModItems.RING_OF_FAVOR);
                    entries.add(ModItems.KNIGHT_HELM);
                    entries.add(ModItems.KNIGHT_ARMOR);
                    entries.add(ModItems.KNIGHT_LEGGINGS);
                    entries.add(ModItems.KNIGHT_BOOTS);
                    entries.add(ModItems.BLACK_KNIGHT_HELM);
                    entries.add(ModItems.BLACK_KNIGHT_ARMOR);
                    entries.add(ModItems.BLACK_KNIGHT_LEGGINGS);
                    entries.add(ModItems.BLACK_KNIGHT_BOOTS);
                    entries.add(ModItems.SOUL_LOST_UNDEAD);
                    entries.add(ModItems.SOUL_GREAT_DEMON);
                    entries.add(ModItems.FORTRESS_KEY);
                    entries.add(ModBlocks.BONFIRE);
                    entries.add(ModBlocks.KEY_GATE);
                    entries.add(ModBlocks.ILLUSORY_WALL);
                    entries.add(ModBlocks.ASHEN_PORTAL);
                    entries.add(ModBlocks.SEALED_GATE);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), group);
    }
}
