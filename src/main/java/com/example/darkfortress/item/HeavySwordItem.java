package com.example.darkfortress.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Slow, heavy hitter that shoves enemies back. */
public class HeavySwordItem extends SwordItem {
    public HeavySwordItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Vec3d push = target.getPos().subtract(attacker.getPos());
        push = new Vec3d(push.x, 0, push.z);
        if (push.lengthSquared() > 1.0E-4) {
            push = push.normalize().multiply(0.9);
            target.addVelocity(push.x, 0.25, push.z);
            target.velocityModified = true;
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Heavy. Hits knock enemies back.").formatted(Formatting.GRAY));
    }
}
