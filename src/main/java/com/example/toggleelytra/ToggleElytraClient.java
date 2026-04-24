package com.example.toggleelytra;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

public class ToggleElytraClient implements ClientModInitializer {

    public enum PendingSwapAction {
        EQUIP_ELYTRA,
        EQUIP_CHESTPLATE
    }

    private static boolean wasJumpPressed = false;
    private static boolean wasUsePressed = false;

    // Debounce counter for ground detection.
    // isOnGround() can flicker when walking off a block edge, creating false
    // "landing" events. We only treat it as a real landing after the player
    // has been consistently on-ground for GROUND_DEBOUNCE_TICKS ticks.
    private int groundTickCounter = 0;
    private static final int GROUND_DEBOUNCE_TICKS = 2;
    private boolean landingHandled = false;

    public static PendingSwapAction pendingSwapAction = null;
    public static boolean swapSuppressionArmed = false;
    private static boolean pendingGlideAfterSwap = false;
    private static int pendingFireworkSlot = -1;
    private static int pendingHotbarSlot = -1;
    private static boolean fireworkSuppressionArmed = false;

    // Counter for retrying START_FALL_FLYING after elytra equip.
    // In singleplayer, the swap and flight packet may need to happen on
    // separate ticks because the integrated server processes them synchronously.
    // This counter allows the Mixin to retry sending the flight packet for
    // a few ticks after the swap, similar to how JJElytraSwap retries every tick.
    public static int flyRetryTicksRemaining = 0;
    private static final int FLY_RETRY_MAX_TICKS = 5;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) return;

            boolean isJumpPressed = client.options.jumpKey.isPressed();
            boolean jumpJustPressed = isJumpPressed && !wasJumpPressed;
            wasJumpPressed = isJumpPressed;

            if (!jumpJustPressed) return;

            boolean isInFluid = player.isTouchingWater() || player.isInLava();
            if (isInFluid || !isValidAirborneSwapState(player)) return;

            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isElytra(chestItem)) {
                if (player.isGliding()) {
                    requestSwap(PendingSwapAction.EQUIP_CHESTPLATE, false);
                }
                return;
            }

            requestSwap(PendingSwapAction.EQUIP_ELYTRA, true);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (client.options.useKey.isPressed()) {
                // Keep the latch set while the use key is held.
            } else {
                wasUsePressed = false;
            }

            if (player == null || client.world == null) {
                clearPendingSwap();
                clearPendingFireworkUse();
                return;
            }

            // --- Fluid detection ---
            // When in fluid (water/lava), the mod is completely disabled to
            // allow elytra usage underwater and in lava. The ground counter
            // is frozen so no auto-swap or state changes occur.
            boolean isInFluid = player.isTouchingWater() || player.isInLava();

            // --- Ground detection with debounce ---
            // isOnGround() can flicker (true->false->true) when walking off
            // a block edge. To avoid false landing events, we require the
            // player to be on-ground for GROUND_DEBOUNCE_TICKS consecutive
            // ticks before treating it as a real landing.
            if (!isInFluid && player.isOnGround()) {
                groundTickCounter++;
            } else if (!isInFluid) {
                groundTickCounter = 0;
                landingHandled = false;
            }
            // When in fluid: freeze the ground counter (don't increment or
            // reset), so the mod is fully inert.

            // --- Ground -> Chestplate swap (debounced landing) ---
            // Skip entirely when in fluid.
            if (!isInFluid && groundTickCounter >= GROUND_DEBOUNCE_TICKS && !landingHandled) {
                landingHandled = true;
                ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
                if (isElytra(chestItem)) {
                    clearPendingSwap();
                    requestSwap(PendingSwapAction.EQUIP_CHESTPLATE, false);
                }
                flyRetryTicksRemaining = 0;
            }

        });
    }

    // --- Public utility methods used by both this class and the Mixin ---

    public static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    public static boolean isChestplate(ItemStack stack, net.minecraft.entity.LivingEntity player) {
        if (stack.isEmpty()) return false;
        return player.getPreferredEquipmentSlot(stack) == EquipmentSlot.CHEST
                && !isElytra(stack);
    }

    public static void equipChestplate(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findChestplate(player);
        if (slot != -1) {
            swapChestSlot(client, player, slot);
        }
    }

    /**
     * Swap elytra into the chest slot from inventory.
     * Does NOT start gliding - that is handled separately by tryStartGliding().
     */
    public static boolean equipElytra(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findElytra(player);
        if (slot != -1) {
            swapChestSlot(client, player, slot);
            return true;
        }
        return false;
    }

    /**
     * Attempt to start gliding. Returns true if the player has, or can start,
     * gliding with the currently equipped elytra.
     */
    public static boolean tryStartGliding(MinecraftClient client, ClientPlayerEntity player) {
        if (!isElytra(player.getEquippedStack(EquipmentSlot.CHEST))) return false;
        if (player.isGliding()) return true; // already gliding

        client.getNetworkHandler().sendPacket(
                new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        player.startGliding();
        return true;
    }

    public static int getFlyRetryMaxTicks() {
        return FLY_RETRY_MAX_TICKS;
    }

    public static boolean isValidAirborneSwapState(ClientPlayerEntity player) {
        return !player.isOnGround()
                && !player.isClimbing()
                && !player.isSleeping()
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && !player.isTouchingWater()
                && !player.isInLava();
    }

    public static void requestSwap(PendingSwapAction action, boolean glideAfterSwap) {
        if (pendingSwapAction != null) return;

        pendingSwapAction = action;
        swapSuppressionArmed = true;
        pendingGlideAfterSwap = glideAfterSwap;
    }

    public static boolean shouldSuppressMovementThisTick() {
        return (swapSuppressionArmed && pendingSwapAction != null)
                || (fireworkSuppressionArmed && pendingFireworkSlot != -1);
    }

    public static void clearPendingSwap() {
        pendingSwapAction = null;
        swapSuppressionArmed = false;
        pendingGlideAfterSwap = false;
    }

    public static void handleRightClick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !player.isGliding()) {
            return;
        }

        if (wasUsePressed) {
            return;
        }
        wasUsePressed = true;

        if (player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET
                || player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            return;
        }

        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return;
        }

        if (isUsableItem(player.getMainHandStack(), player) || isUsableItem(player.getOffHandStack(), player)) {
            return;
        }

        if (player.isUsingItem()) {
            return;
        }

        int fireworkSlot = findFireworkRocket(player.getInventory());
        if (fireworkSlot == -1) {
            return;
        }

        requestFireworkUse(player, fireworkSlot);
    }

    public static void consumePendingSwap(MinecraftClient client, ClientPlayerEntity player) {
        if (pendingSwapAction == null) return;

        PendingSwapAction action = pendingSwapAction;
        boolean swapSucceeded = switch (pendingSwapAction) {
            case EQUIP_ELYTRA -> equipElytra(client, player);
            case EQUIP_CHESTPLATE -> {
                int slot = findChestplate(player);
                if (slot == -1) {
                    yield false;
                }
                swapChestSlot(client, player, slot);
                yield true;
            }
        };

        boolean shouldRetryGlide = swapSucceeded
                && action == PendingSwapAction.EQUIP_ELYTRA
                && pendingGlideAfterSwap;

        clearPendingSwap();
        if (shouldRetryGlide) {
            if (tryStartGliding(client, player) && player.isGliding()) {
                flyRetryTicksRemaining = 0;
            } else {
                flyRetryTicksRemaining = getFlyRetryMaxTicks();
            }
        }
    }

    public static void clearPendingFireworkUse() {
        pendingFireworkSlot = -1;
        pendingHotbarSlot = -1;
        fireworkSuppressionArmed = false;
    }

    public static void consumePendingFireworkUse(MinecraftClient client, ClientPlayerEntity player) {
        if (pendingFireworkSlot == -1 || pendingHotbarSlot == -1) {
            return;
        }

        if (client.interactionManager == null || !player.isGliding()) {
            clearPendingFireworkUse();
            return;
        }

        PlayerInventory inventory = player.getInventory();
        if (pendingFireworkSlot < 0 || pendingFireworkSlot >= inventory.size()) {
            clearPendingFireworkUse();
            return;
        }

        if (pendingHotbarSlot < 0 || pendingHotbarSlot > 8) {
            clearPendingFireworkUse();
            return;
        }

        ItemStack stack = inventory.getStack(pendingFireworkSlot);
        if (stack.getItem() != Items.FIREWORK_ROCKET) {
            clearPendingFireworkUse();
            return;
        }

        int sourceSlotId = convertInventoryIndexToScreenSlot(pendingFireworkSlot);
        if (sourceSlotId == -1) {
            clearPendingFireworkUse();
            return;
        }

        int syncId = player.playerScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, sourceSlotId, pendingHotbarSlot, SlotActionType.SWAP, player);
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        client.interactionManager.clickSlot(syncId, sourceSlotId, pendingHotbarSlot, SlotActionType.SWAP, player);
        clearPendingFireworkUse();
    }

    public static int findChestplate(ClientPlayerEntity player) {
        // PlayerScreenHandler slots:
        // 0-4: Crafting, 5: Helm, 6: Chest, 7: Legs, 8: Boots
        // 9-35: Main Inventory, 36-44: Hotbar, 45: Offhand
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isChestplate(stack, player)) {
                return i;
            }
        }
        return -1;
    }

    public static int findElytra(ClientPlayerEntity player) {
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isElytra(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isUsableItem(ItemStack stack, ClientPlayerEntity player) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof BlockItem) {
            return true;
        }

        if (stack.getMaxUseTime(player) > 0) {
            return true;
        }

        return stack.getItem() == Items.ENDER_PEARL
                || stack.getItem() == Items.SNOWBALL
                || stack.getItem() == Items.EGG
                || stack.getItem() == Items.WIND_CHARGE
                || stack.getItem() == Items.SPLASH_POTION
                || stack.getItem() == Items.LINGERING_POTION;
    }

    private static int findFireworkRocket(PlayerInventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }

    private static void requestFireworkUse(ClientPlayerEntity player, int slotIndex) {
        int currentSlotIndex = player.getInventory().getSelectedSlot();
        if (slotIndex == currentSlotIndex || pendingFireworkSlot != -1) {
            return;
        }

        pendingFireworkSlot = slotIndex;
        pendingHotbarSlot = currentSlotIndex;
        fireworkSuppressionArmed = true;
    }

    private static int convertInventoryIndexToScreenSlot(int playerInventoryIndex) {
        if (playerInventoryIndex >= 0 && playerInventoryIndex <= 8) {
            return 36 + playerInventoryIndex;
        }

        if (playerInventoryIndex >= 9 && playerInventoryIndex <= 35) {
            return playerInventoryIndex;
        }

        if (playerInventoryIndex == 40) {
            return 45;
        }

        return -1;
    }

    private static void swapChestSlot(MinecraftClient client, ClientPlayerEntity player, int sourceSlot) {
        if (client.interactionManager == null) return;

        int chestSlot = 6;

        // Pick up item from source slot
        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP, player);
        // Place in chest slot (swaps with current)
        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId, chestSlot, 0, SlotActionType.PICKUP, player);
        // Place swapped item back in source slot
        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP, player);
    }
}
