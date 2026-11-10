package com.example.darkfortress.item;

import com.example.darkfortress.Fx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/** Testing tool: one-hit kill with extra reach, and right-click smites everything nearby. */
public class TestBladeItem extends WeaponItem {
    public TestBladeItem(Settings settings) {
        super(settings, Hits::annihilate,
                "TEST WEAPON: kills anything in one hit, bosses included",
                "Right-click: smite every enemy within 20 blocks");
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (world instanceof ServerWorld sw) {
            Box box = player.getBoundingBox().expand(20);
            int n = 0;
            for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class, box,
                    x -> x != player && x.isAlive() && !(x instanceof PlayerEntity))) {
                e.timeUntilRegen = 0;
                e.damage(sw.getDamageSources().playerAttack(player), 100000.0f);
                n++;
            }
            Fx.ring(sw, player.getPos(), 20.0, 8, ParticleTypes.END_ROD, ParticleTypes.SOUL_FIRE_FLAME);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.5f);
            player.sendMessage(Text.literal("Smote " + n + " enemies").formatted(Formatting.LIGHT_PURPLE), true);
        }
        player.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(stack, world.isClient());
    }
}
