package com.example.darkfortress.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

/** Renders a fullbright overlay texture (glowing eyes, runes, molten cracks). */
public class GlowFeature<T extends LivingEntity, M extends EntityModel<T>> extends EyesFeatureRenderer<T, M> {
    private final RenderLayer layer;

    public GlowFeature(FeatureRendererContext<T, M> context, Identifier texture) {
        super(context);
        this.layer = RenderLayer.getEyes(texture);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return layer;
    }
}
