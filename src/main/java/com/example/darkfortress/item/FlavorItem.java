package com.example.darkfortress.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A junk item: no function, just a bit of grim flavour text. */
public class FlavorItem extends Item {
    private final String[] lines;

    public FlavorItem(Settings settings, String... lines) {
        super(settings);
        this.lines = lines;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : lines) {
            tooltip.add(Text.literal(line).formatted(Formatting.DARK_GRAY));
        }
    }
}
