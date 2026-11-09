package com.example.darkfortress.client;

import com.example.darkfortress.ModEntities;
import com.example.darkfortress.entity.DSMob;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DarkFortressClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.<DSMob>register(ModEntities.HOLLOW_SOLDIER, ctx -> new HollowRenderer(ctx, "hollow_soldier", 1.0f));
        EntityRendererRegistry.<DSMob>register(ModEntities.SKELETON_WARRIOR, ctx -> new HollowRenderer(ctx, "skeleton_warrior", 1.0f));
        EntityRendererRegistry.<DSMob>register(ModEntities.HOLLOW_KNIGHT, ctx -> new HollowRenderer(ctx, "hollow_knight", 1.1f));
        EntityRendererRegistry.<DSMob>register(ModEntities.ASH_GHOUL, ctx -> new HollowRenderer(ctx, "ash_ghoul", 0.95f));
        EntityRendererRegistry.<DSMob>register(ModEntities.HOLLOW_MAGE, ctx -> new HollowRenderer(ctx, "hollow_mage", 1.0f));
        EntityRendererRegistry.<DSMob>register(ModEntities.BLACK_KNIGHT, ctx -> new HollowRenderer(ctx, "black_knight", 1.2f));
        EntityRendererRegistry.<DSMob>register(ModEntities.GARGOYLE, ctx -> new HollowRenderer(ctx, "gargoyle", 1.7f));
        EntityRendererRegistry.<DSMob>register(ModEntities.WITCH_OF_ASH, ctx -> new HollowRenderer(ctx, "witch_of_ash", 1.1f));
        EntityRendererRegistry.<DSMob>register(ModEntities.DEMON_LORD, ctx -> new HollowRenderer(ctx, "demon_lord", 2.2f));
        EntityRendererRegistry.<DSMob>register(ModEntities.LABYRINTH_WARDEN, ctx -> new HollowRenderer(ctx, "labyrinth_warden", 2.0f));
        EntityRendererRegistry.register(ModEntities.HOLLOW_ARCHER, ArcherRenderer::new);
    }
}
