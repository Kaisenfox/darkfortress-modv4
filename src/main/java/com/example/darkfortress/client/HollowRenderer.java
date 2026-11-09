package com.example.darkfortress.client;

import com.example.darkfortress.DarkFortressMod;
import com.example.darkfortress.entity.DSMob;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Humanoid with a custom skin and optional scale. */
public class HollowRenderer extends BipedEntityRenderer<DSMob, BipedEntityModel<DSMob>> {
    private final Identifier texture;
    private final float scale;

    public HollowRenderer(EntityRendererFactory.Context ctx, String name, float scale) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f * scale);
        this.texture = Identifier.of(DarkFortressMod.MOD_ID, "textures/entity/" + name + ".png");
        this.scale = scale;
    }

    @Override
    public Identifier getTexture(DSMob entity) {
        return texture;
    }

    @Override
    protected void scale(DSMob entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
