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
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;checkGliding()Z",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterCheckGliding(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) return;

        // Must be airborne and in a valid state for elytra flight
        if (player.isOnGround() || player.isClimbing() || player.isSleeping()
                || player.hasStatusEffect(StatusEffects.LEVITATION)) {
            // Don't clear jumpToggleRequested here - the player may still be
            // on ground during the jump takeoff tick.
            return;
        }

        // Step 1: Process manual jump-key toggle request - swap inventory
        if (ToggleElytraClient.jumpToggleRequested) {
            ToggleElytraClient.jumpToggleRequested = false;
            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
            if (!ToggleElytraClient.isElytra(chestItem)) {
                if (ToggleElytraClient.equipElytra(client, player)) {
                    // Swap initiated - start retry counter for flight packet
                    ToggleElytraClient.flyRetryTicksRemaining = 5;
                }
            }
        }

        // Step 2: Retry sending START_FALL_FLYING until gliding starts or retries expire.
        // In singleplayer, the server processes clickSlot synchronously but
        // checkGliding() has already run this tick, so the flight packet may
        // only succeed on the next tick. Retrying for a few ticks handles this.
        if (ToggleElytraClient.flyRetryTicksRemaining > 0) {
            if (ToggleElytraClient.tryStartGliding(client, player)) {
                if (player.isGliding()) {
                    // Successfully gliding - stop retrying
                    ToggleElytraClient.flyRetryTicksRemaining = 0;
                } else {
                    ToggleElytraClient.flyRetryTicksRemaining--;
                }
            } else {
                // Elytra not equipped (swap may not have taken effect yet)
                ToggleElytraClient.flyRetryTicksRemaining--;
            }
        }
    }
}
