package com.example.toggleelytra.mixin;

import com.example.toggleelytra.ToggleElytraClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void onDoItemUse(CallbackInfo ci) {
        ToggleElytraClient.handleRightClick((MinecraftClient) (Object) this);
    }
}
