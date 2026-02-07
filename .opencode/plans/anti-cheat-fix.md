# Anti-Cheat Fix Plan for ToggleElytra

## Problem
ToggleElytra is detected by anti-cheat systems, while the reference mod JJElytraSwap is not.

## Root Causes (from comparative analysis)

### HIGH RISK
1. **Direct Entity Flag Manipulation**: `Entity.setFlag(7, true)` via Mixin Invoker bypasses vanilla code path. JJElytraSwap uses `player.startFallFlying()` (vanilla method).
2. **Wrong Tick Timing**: All swap logic runs from `END_CLIENT_TICK` (after tick completes). JJElytraSwap injects elytra-equip into `tickMovement()` right after `checkGliding()` - the natural point where equipment changes matter.
3. **No Ground-Swap Edge Detection**: Current code tries to swap elytra→chestplate EVERY tick on ground. JJElytraSwap only swaps once on the air→ground transition.

### MEDIUM RISK
4. **Missing State Checks**: No checks for `isClimbing()`, `isSleeping()`, `hasStatusEffect(LEVITATION)` before elytra equip.
5. **No Fluid Check for Ground State**: Missing `isSubmergedInWater()` in ground detection.

## Implementation Plan

### File Changes Overview

| File | Action |
|------|--------|
| `ToggleElytraClient.java` | Rewrite |
| `mixin/SwapCheckMixin.java` | **Create** (new file) |
| `mixin/EntityInvoker.java` | **Delete** |
| `toggleelytra.mixins.json` | Update |

### Step 1: Rewrite `ToggleElytraClient.java`

The main class keeps:
- `END_CLIENT_TICK` handler for: jump key edge detection, ground→chestplate edge-detected swap
- Static utility methods for inventory operations (used by both this class and Mixin)
- A `jumpToggleRequested` flag that the Mixin consumes

Remove:
- `predictionEnabled` and prediction command
- `EntityInvoker` usage / `setFlag(7, true)`
- Elytra equip logic from END_CLIENT_TICK (moves to Mixin)

```java
package com.example.toggleelytra;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class ToggleElytraClient implements ClientModInitializer {

    private boolean wasJumpPressed = false;
    private boolean shouldWearChestplatePrevTick = true;

    // Flag set by END_CLIENT_TICK when user requests manual toggle via jump key.
    // Consumed by SwapCheckMixin in tickMovement() at the correct timing.
    public static boolean jumpToggleRequested = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) return;

            // --- Jump key edge detection ---
            boolean isJumpPressed = client.options.jumpKey.isPressed();
            boolean jumpJustPressed = isJumpPressed && !wasJumpPressed;
            wasJumpPressed = isJumpPressed;

            // --- Ground/Fluid detection ---
            boolean isOnGroundOrInFluid = player.isOnGround() || player.isSubmergedInWater();
            boolean shouldWearChestplate = isOnGroundOrInFluid;

            // --- Ground -> Chestplate swap (edge: only on air-to-ground transition) ---
            if (shouldWearChestplate && !shouldWearChestplatePrevTick) {
                ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
                if (isElytra(chestItem)) {
                    equipChestplate(client, player);
                }
            }
            shouldWearChestplatePrevTick = shouldWearChestplate;

            // --- Manual toggle request via jump key ---
            if (jumpJustPressed && !isOnGroundOrInFluid
                    && !player.isTouchingWater()
                    && !player.isClimbing()
                    && !player.isSleeping()
                    && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
                ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
                if (isElytra(chestItem)) {
                    // Chestplate equip (no special timing needed)
                    equipChestplate(client, player);
                } else {
                    // Request elytra equip - processed in tickMovement() Mixin
                    jumpToggleRequested = true;
                }
            }
        });
    }

    // --- Public utility methods ---

    public static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    public static boolean isChestplate(ItemStack stack, net.minecraft.entity.LivingEntity player) {
        if (stack.isEmpty()) return false;
        return player.getPreferredEquipmentSlot(stack) == EquipmentSlot.CHEST && !isElytra(stack);
    }

    public static void equipChestplate(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findChestplate(player);
        if (slot != -1) {
            swapChestSlot(client, player, slot);
        }
    }

    /** Equip elytra and start fall flying. ONLY call from tickMovement() Mixin. */
    public static void equipElytraAndFly(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findElytra(player);
        if (slot != -1) {
            swapChestSlot(client, player, slot);
            client.getNetworkHandler().sendPacket(
                new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            player.startFallFlying(); // vanilla method, NOT setFlag(7, true)
        }
    }

    public static int findChestplate(ClientPlayerEntity player) {
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isChestplate(stack, player)) return i;
        }
        return -1;
    }

    public static int findElytra(ClientPlayerEntity player) {
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isElytra(stack)) return i;
        }
        return -1;
    }

    private static void swapChestSlot(MinecraftClient client, ClientPlayerEntity player, int sourceSlot) {
        if (client.interactionManager == null) return;
        int chestSlot = 6;
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, chestSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP, player);
    }
}
```

### Step 2: Create `mixin/SwapCheckMixin.java`

New Mixin that injects into `ClientPlayerEntity.tickMovement()` right after `checkGliding()`.
Handles both automatic elytra equip (airborne without elytra) and manual jump-key toggle requests.

```java
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

        // State checks: only proceed if airborne and in valid state
        if (player.isOnGround() || player.isClimbing() || player.isSleeping()
                || player.hasStatusEffect(StatusEffects.LEVITATION)) {
            // Clear any pending toggle request if we're no longer in a valid state
            ToggleElytraClient.jumpToggleRequested = false;
            return;
        }

        // Process manual jump-key toggle request (elytra equip direction only)
        if (ToggleElytraClient.jumpToggleRequested) {
            ToggleElytraClient.jumpToggleRequested = false;
            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
            if (!ToggleElytraClient.isElytra(chestItem)) {
                ToggleElytraClient.equipElytraAndFly(client, player);
            }
        }
    }
}
```

### Step 3: Delete `mixin/EntityInvoker.java`

This file is no longer needed since we use `player.startFallFlying()` instead of `setFlag(7, true)`.

### Step 4: Update `toggleelytra.mixins.json`

Replace `EntityInvoker` with `SwapCheckMixin`:

```json
{
    "required": true,
    "package": "com.example.toggleelytra.mixin",
    "compatibilityLevel": "JAVA_21",
    "client": [
        "SwapCheckMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

Note: Changed `"mixins"` to `"client"` since this is a client-only mixin.

## Summary of All Anti-Cheat Fixes

| Issue | Before | After |
|-------|--------|-------|
| Entity flag manipulation | `setFlag(7, true)` via Invoker | `player.startFallFlying()` (vanilla) |
| Elytra equip timing | `END_CLIENT_TICK` (wrong timing) | `tickMovement()` after `checkGliding()` (correct timing) |
| Ground swap frequency | Every tick while grounded | Once on air→ground transition (edge detection) |
| State checks before elytra | None | `isClimbing`, `isSleeping`, `LEVITATION` |
| Ground detection | `isOnGround \|\| hasVehicle` | `isOnGround \|\| isSubmergedInWater` |
| Mixin type | `EntityInvoker` (accessor) | `SwapCheckMixin` (inject into tickMovement) |
| Jump key manual toggle | Processed at wrong timing | Flag set in END_CLIENT_TICK, consumed in tickMovement() Mixin |
