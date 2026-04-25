package com.example.toggleelytra.mixin;

import com.example.toggleelytra.ToggleElytraClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class SwapCheckMixin {

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) return;
        if (client.currentScreen != null) {
            ToggleElytraClient.clearPendingSwap();
            ToggleElytraClient.clearPendingFireworkUse();
            return;
        }

        if (ToggleElytraClient.pendingSwapAction != null) {
            if (ToggleElytraClient.pendingSwapAction == ToggleElytraClient.PendingSwapAction.EQUIP_ELYTRA
                    && !ToggleElytraClient.isValidAirborneSwapState(player)) {
                ToggleElytraClient.clearPendingSwap();
            } else if ((player.isTouchingWater() || player.isInLava())
                    && ToggleElytraClient.pendingSwapAction == ToggleElytraClient.PendingSwapAction.EQUIP_CHESTPLATE) {
                ToggleElytraClient.clearPendingSwap();
            } else {
                ToggleElytraClient.consumePendingSwap(client, player);
            }
        }
        ToggleElytraClient.consumePendingFireworkUse(client, player);
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovementTail(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) return;
        if (client.currentScreen != null) {
            return;
        }

        if (!ToggleElytraClient.isValidAirborneSwapState(player)) {
            return;
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
