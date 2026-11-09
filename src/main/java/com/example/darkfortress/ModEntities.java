package com.example.darkfortress;

import com.example.darkfortress.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static EntityType<HollowSoldierEntity> HOLLOW_SOLDIER;
    public static EntityType<HollowArcherEntity> HOLLOW_ARCHER;
    public static EntityType<FolkEntity> SKELETON_WARRIOR;
    public static EntityType<FolkEntity> HOLLOW_KNIGHT;
    public static EntityType<FolkEntity> ASH_GHOUL;
    public static EntityType<HollowMageEntity> HOLLOW_MAGE;
    public static EntityType<BlackKnightEntity> BLACK_KNIGHT;
    public static EntityType<GargoyleEntity> GARGOYLE;
    public static EntityType<WitchOfAshEntity> WITCH_OF_ASH;
    public static EntityType<DemonLordEntity> DEMON_LORD;
    public static EntityType<LabyrinthWardenEntity> LABYRINTH_WARDEN;

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> reg(String id, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(DarkFortressMod.MOD_ID, id), builder.build(id));
    }

    public static void init() {
        HOLLOW_SOLDIER = reg("hollow_soldier", EntityType.Builder.create(HollowSoldierEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.95f).maxTrackingRange(8));
        HOLLOW_ARCHER = reg("hollow_archer", EntityType.Builder.create(HollowArcherEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.99f).maxTrackingRange(8));
        SKELETON_WARRIOR = reg("skeleton_warrior", EntityType.Builder.create(FolkEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.95f).maxTrackingRange(8));
        HOLLOW_KNIGHT = reg("hollow_knight", EntityType.Builder.create(FolkEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.68f, 2.15f).maxTrackingRange(8));
        ASH_GHOUL = reg("ash_ghoul", EntityType.Builder.create(FolkEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.85f).maxTrackingRange(8));
        HOLLOW_MAGE = reg("hollow_mage", EntityType.Builder.create(HollowMageEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.95f).maxTrackingRange(8));
        BLACK_KNIGHT = reg("black_knight", EntityType.Builder.create(BlackKnightEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.72f, 2.35f).maxTrackingRange(10).makeFireImmune());
        GARGOYLE = reg("gargoyle", EntityType.Builder.create(GargoyleEntity::new, SpawnGroup.MONSTER)
                .dimensions(1.0f, 3.3f).maxTrackingRange(10).makeFireImmune());
        WITCH_OF_ASH = reg("witch_of_ash", EntityType.Builder.create(WitchOfAshEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.7f, 2.15f).maxTrackingRange(10).makeFireImmune());
        DEMON_LORD = reg("demon_lord", EntityType.Builder.create(DemonLordEntity::new, SpawnGroup.MONSTER)
                .dimensions(1.35f, 4.3f).maxTrackingRange(10).makeFireImmune());
        LABYRINTH_WARDEN = reg("labyrinth_warden", EntityType.Builder.create(LabyrinthWardenEntity::new, SpawnGroup.MONSTER)
                .dimensions(1.2f, 3.9f).maxTrackingRange(10).makeFireImmune());

        FabricDefaultAttributeRegistry.register(HOLLOW_SOLDIER, HollowSoldierEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HOLLOW_ARCHER, HollowArcherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SKELETON_WARRIOR, FolkEntity.skeleton());
        FabricDefaultAttributeRegistry.register(HOLLOW_KNIGHT, FolkEntity.knight());
        FabricDefaultAttributeRegistry.register(ASH_GHOUL, FolkEntity.ghoul());
        FabricDefaultAttributeRegistry.register(HOLLOW_MAGE, HollowMageEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BLACK_KNIGHT, BlackKnightEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(GARGOYLE, GargoyleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WITCH_OF_ASH, WitchOfAshEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DEMON_LORD, DemonLordEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LABYRINTH_WARDEN, LabyrinthWardenEntity.createAttributes());
    }
}
