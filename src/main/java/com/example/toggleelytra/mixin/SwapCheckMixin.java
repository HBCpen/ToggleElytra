package com.example.toggleelytra.mixin;

import com.example.toggleelytra.ToggleElytraClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class SwapCheckMixin {

    @Inject(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isGliding()Z",
            ordinal = 1,
            shift = At.Shift.BEFORE
        )
    )
    private void onAfterCheckGliding(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) return;

        // Must be airborne and in a valid state
        if (player.isOnGround() || player.isClimbing() || player.isSleeping()
                || player.hasStatusEffect(StatusEffects.LEVITATION)) {
            // Don't clear jumpToggleRequested - player may still be
            // on ground during the jump takeoff tick.
            return;
        }

        // Mod is completely inert when in fluid (water/lava).
        // This is a safety guard in case jumpToggleRequested was set
        // on the same tick the player entered fluid.
        if (player.isTouchingWater() || player.isInLava()) {
            return;
        }

        // Process manual jump-key toggle request (both directions)
        if (ToggleElytraClient.jumpToggleRequested) {
            ToggleElytraClient.jumpToggleRequested = false;
            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);

            if (ToggleElytraClient.isElytra(chestItem)) {
                // Elytra -> Chestplate
                ToggleElytraClient.equipChestplate(client, player);
            } else {
                // Chestplate/empty -> Elytra
                if (ToggleElytraClient.equipElytra(client, player)) {
                    ToggleElytraClient.flyRetryTicksRemaining = 5;
                }
            }
        }

        // Retry sending START_FALL_FLYING until gliding starts or retries expire.
        if (ToggleElytraClient.flyRetryTicksRemaining > 0) {
            if (ToggleElytraClient.tryStartGliding(client, player)) {
                if (player.isGliding()) {
                    ToggleElytraClient.flyRetryTicksRemaining = 0;
                } else {
                    ToggleElytraClient.flyRetryTicksRemaining--;
                }
            } else {
                ToggleElytraClient.flyRetryTicksRemaining--;
            }
        }
    }
}
