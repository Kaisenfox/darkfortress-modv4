package com.example.darkfortress.item;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

/** A melee weapon with its own stats, optional extra reach, and an on-hit effect (its "attack style"). */
public class WeaponItem extends Item {

    @FunctionalInterface
    public interface HitEffect {
        void onHit(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity target);
    }

    private final HitEffect effect;
    private final String[] lines;

    public WeaponItem(Settings settings, HitEffect effect, String... lines) {
        super(settings);
        this.effect = effect;
        this.lines = lines;
    }

    /**
     * @param damage extra damage on top of the 1 base (a stone sword is +4)
     * @param speed  attack speed modifier (sword is -2.4, dagger -1.5, hammer -3.2)
     * @param reach  extra reach in blocks
     */
    public static Item.Settings stats(int durability, double damage, double speed, double reach, Rarity rarity) {
        AttributeModifiersComponent.Builder b = AttributeModifiersComponent.builder();
        b.add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, damage, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
        b.add(EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, speed, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.MAINHAND);
        if (reach != 0) {
            b.add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,
                    new EntityAttributeModifier(Identifier.of("darkfortress", "weapon_reach"), reach, EntityAttributeModifier.Operation.ADD_VALUE),
                    AttributeModifierSlot.MAINHAND);
        }
        return new Item.Settings().maxDamage(durability).rarity(rarity).attributeModifiers(b.build());
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, EquipmentSlot.MAINHAND);
        if (effect != null && attacker.getWorld() instanceof ServerWorld sw) {
            effect.onHit(sw, stack, attacker, target);
        }
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : lines) {
            tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
        }
    }
}
