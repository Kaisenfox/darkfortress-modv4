package com.example.darkfortress.client;

import com.example.darkfortress.DarkFortressMod;
import com.example.darkfortress.entity.HollowArcherEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.util.Identifier;

public class ArcherRenderer extends BipedEntityRenderer<HollowArcherEntity, SkeletonEntityModel<HollowArcherEntity>> {
    private static final Identifier TEXTURE =
            Identifier.of(DarkFortressMod.MOD_ID, "textures/entity/hollow_archer.png");

    public ArcherRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SkeletonEntityModel<>(ctx.getPart(EntityModelLayers.SKELETON)), 0.5f);
    }

    @Override
    public Identifier getTexture(HollowArcherEntity entity) {
        return TEXTURE;
    }
}
