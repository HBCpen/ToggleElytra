/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1294
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package io.github.jumperonjava.jjelytraswap.mixin;

import io.github.jumperonjava.jjelytraswap.JJElytraSwapInit;
import net.minecraft.class_1294;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_746.class})
public class SwapCheckMixin {
    @Inject(method={"tickMovement"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;checkGliding()Z", shift=At.Shift.AFTER)})
    public void swapToElytra(CallbackInfo callbackInfo) {
        if (!JJElytraSwapInit.enabled) {
            return;
        }
        class_746 target = (class_746)this;
        if (!(target.method_24828() || target.method_6128() || target.method_5799() || target.method_6059(class_1294.field_5902))) {
            JJElytraSwapInit.tryWearElytra();
        }
    }
}

