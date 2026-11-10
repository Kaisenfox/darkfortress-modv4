package com.example.darkfortress;

import com.example.darkfortress.block.AshenPortalBlock;
import com.example.darkfortress.block.BonfireBlock;
import com.example.darkfortress.block.IllusoryWallBlock;
import com.example.darkfortress.block.KeyGateBlock;
import com.example.darkfortress.block.SealedGateBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static Block BONFIRE;
    public static Block KEY_GATE;
    public static Block ILLUSORY_WALL;
    public static Block ASHEN_PORTAL;
    public static Block SEALED_GATE;

    private ModBlocks() {}

    public static void init() {
        BONFIRE = register("bonfire", new BonfireBlock(AbstractBlock.Settings.create()
                .strength(-1.0f, 3600000.0f).dropsNothing().luminance(s -> 15).sounds(BlockSoundGroup.STONE)));
        KEY_GATE = register("key_gate", new KeyGateBlock(AbstractBlock.Settings.create()
                .strength(-1.0f, 3600000.0f).dropsNothing().sounds(BlockSoundGroup.METAL)));
        ILLUSORY_WALL = register("illusory_wall", new IllusoryWallBlock(AbstractBlock.Settings.create()
                .strength(1.5f, 6.0f).dropsNothing().sounds(BlockSoundGroup.STONE)));
        ASHEN_PORTAL = register("ashen_portal", new AshenPortalBlock(AbstractBlock.Settings.create()
                .strength(-1.0f, 3600000.0f).dropsNothing().luminance(s -> 12).sounds(BlockSoundGroup.STONE)));
        SEALED_GATE = register("sealed_gate", new SealedGateBlock(AbstractBlock.Settings.create()
                .strength(-1.0f, 3600000.0f).dropsNothing().sounds(BlockSoundGroup.METAL)));
    }

    private static Block register(String id, Block block) {
        Identifier ident = Identifier.of(DarkFortressMod.MOD_ID, id);
        Block registered = Registry.register(Registries.BLOCK, ident, block);
        Registry.register(Registries.ITEM, ident, new BlockItem(registered, new Item.Settings()));
        return registered;
    }
}
