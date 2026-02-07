package com.example.toggleelytra;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class ToggleElytraClient implements ClientModInitializer {

    private boolean wasJumpPressed = false;
    private boolean shouldWearChestplatePrevTick = true;

    // Flag set when user presses jump to request elytra equip.
    // Consumed by SwapCheckMixin in tickMovement() at the correct timing.
    // The Mixin checks airborne/valid state before actually equipping.
    public static boolean jumpToggleRequested = false;

    // Counter for retrying START_FALL_FLYING after elytra equip.
    // In singleplayer, the swap and flight packet may need to happen on
    // separate ticks because the integrated server processes them synchronously.
    // This counter allows the Mixin to retry sending the flight packet for
    // a few ticks after the swap, similar to how JJElytraSwap retries every tick.
    public static int flyRetryTicksRemaining = 0;
    private static final int FLY_RETRY_MAX_TICKS = 5;

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
                // Cancel any pending elytra requests on landing
                jumpToggleRequested = false;
                flyRetryTicksRemaining = 0;
            }
            shouldWearChestplatePrevTick = shouldWearChestplate;

            // --- Manual toggle request via jump key ---
            // Set the flag on jump press regardless of current ground state.
            // The Mixin will check airborne state before acting, so if the player
            // is still on ground when tickMovement runs, the flag persists until
            // the player becomes airborne.
            if (jumpJustPressed && !player.isTouchingWater()) {
                ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
                if (isElytra(chestItem)) {
                    // Elytra -> Chestplate: immediate swap (no flight packet needed)
                    equipChestplate(client, player);
                } else {
                    // Chestplate/empty -> Elytra: defer to tickMovement() Mixin
                    jumpToggleRequested = true;
                }
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
     * Attempt to start gliding. Returns true if successful.
     * Should ONLY be called from within tickMovement() (via Mixin)
     * when the player already has an elytra equipped.
     */
    public static boolean tryStartGliding(MinecraftClient client, ClientPlayerEntity player) {
        if (!isElytra(player.getEquippedStack(EquipmentSlot.CHEST))) return false;
        if (player.isGliding()) return true; // already gliding

        client.getNetworkHandler().sendPacket(
                new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        player.startGliding();
        return true;
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
