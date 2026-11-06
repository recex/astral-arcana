package com.astralarcana.item;

import com.astralarcana.client.ClientFx;
import com.astralarcana.spell.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CosmicStaffItem extends Item {
    public static final int MAX_CHARGE = 60;
    public static final int MIN_CHARGE = 15;

    public CosmicStaffItem(Properties props) {
        super(props);
    }

    public static int getSpell(ItemStack stack) {
        return stack.hasTag() ? Math.floorMod(stack.getTag().getInt("Spell"), Spells.COUNT) : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int next = (getSpell(stack) + 1) % Spells.COUNT;
            stack.getOrCreateTag().putInt("Spell", next);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("spell.astralarcana." + next).withStyle(Spells.COLOR[next]), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f + next * 0.25f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int charge = getUseDuration(stack) - remaining;
        int spell = getSpell(stack);
        float ratio = Math.min(charge, MAX_CHARGE) / (float) MAX_CHARGE;
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.chargeTick(user, spell, charge, ratio));
        } else {
            if (charge == 1) {
                level.playSound(null, user.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.5f, 0.8f);
            } else if (charge == MAX_CHARGE) {
                level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.4f);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player)) return;
        int charge = getUseDuration(stack) - timeLeft;
        if (charge < MIN_CHARGE) return;
        float power = Math.min(charge, MAX_CHARGE) / (float) MAX_CHARGE;
        int spell = getSpell(stack);
        Spells.cast((ServerLevel) level, player, spell, power);
        player.getCooldowns().addCooldown(this, Spells.COOLDOWN[spell]);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.astralarcana.spell", Component.translatable("spell.astralarcana." + getSpell(stack))));
        tip.add(Component.translatable("tooltip.astralarcana.hold"));
        tip.add(Component.translatable("tooltip.astralarcana.switch"));
    }
}
