package com.example.toggleelytra.mixin;

import com.example.toggleelytra.ToggleElytraClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MovementSuppressMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        suppressMovementIfNeeded();
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        suppressMovementIfNeeded();
    }

    @Inject(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/input/Input;tick()V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterInputTick(CallbackInfo ci) {
        suppressMovementIfNeeded();
    }

    private void suppressMovementIfNeeded() {
        if (!ToggleElytraClient.shouldSuppressMovementThisTick()) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        player.setSprinting(false);
        player.input.playerInput = PlayerInput.DEFAULT;
        ((InputAccessor) player.input).toggleelytra$setMovementVector(Vec2f.ZERO);
    }
}
