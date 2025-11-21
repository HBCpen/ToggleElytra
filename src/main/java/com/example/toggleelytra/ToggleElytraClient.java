package com.example.toggleelytra;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class ToggleElytraClient implements ClientModInitializer {

    private boolean wasJumpPressed = false;
    private boolean wasOnGroundLastTick = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null)
                return;

            boolean isJumpPressed = client.options.jumpKey.isPressed();
            boolean jumpJustPressed = isJumpPressed && !wasJumpPressed;
            wasJumpPressed = isJumpPressed;

            ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);

            if (player.isOnGround()) {
                // Grounded: Ensure Chestplate is equipped
                if (isElytra(chestItem)) {
                    equipChestplate(client, player);
                }
            } else {
                // Airborne: Toggle on Jump Press
                // Only toggle if we were NOT on the ground last tick (prevents toggle on jump
                // takeoff)
                if (jumpJustPressed && !wasOnGroundLastTick) {
                    if (isElytra(chestItem)) {
                        equipChestplate(client, player);
                    } else if (isChestplate(chestItem, player)) {
                        equipElytra(client, player);
                    } else {
                        // If wearing nothing or something else, maybe try to equip Elytra first?
                        // Requirement says "toggle", implies switching between the two.
                        // If neither is worn, let's prioritize Elytra for flight.
                        equipElytra(client, player);
                    }
                }
            }
            wasOnGroundLastTick = player.isOnGround();
        });
    }

    private boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    private boolean isChestplate(ItemStack stack, net.minecraft.entity.LivingEntity player) {
        if (stack.isEmpty())
            return false;
        return player.getPreferredEquipmentSlot(stack) == EquipmentSlot.CHEST
                && !isElytra(stack);
    }

    private void equipChestplate(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findChestplate(player);
        if (slot != -1) {
            swapChestplate(client, player, slot);
        }
    }

    private void equipElytra(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findElytra(player);
        if (slot != -1) {
            swapChestplate(client, player, slot);
            // Auto-glide
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                        player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
    }

    private int findChestplate(ClientPlayerEntity player) {

        // Better approach: Iterate ScreenHandler slots to get the correct ID for
        // clickSlot
        // PlayerScreenHandler:
        // 0-4: Crafting
        // 5: Helm, 6: Chest, 7: Legs, 8: Boots
        // 9-35: Main Inventory
        // 36-44: Hotbar
        // 45: Offhand

        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isChestplate(stack, player)) {
                return i;
            }
        }
        return -1;
    }

    private int findElytra(ClientPlayerEntity player) {
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (isElytra(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void swapChestplate(MinecraftClient client, ClientPlayerEntity player, int sourceSlot) {
        if (client.interactionManager == null)
            return;

        // 6 is the Chestplate slot in PlayerScreenHandler
        int chestSlot = 6;

        // Pick up the item from source
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP,
                player);

        // Place it in chest slot (swaps with current)
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, chestSlot, 0, SlotActionType.PICKUP,
                player);

        // Place the swapped item back in source
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, sourceSlot, 0, SlotActionType.PICKUP,
                player);
    }
}
