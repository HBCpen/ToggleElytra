/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2960
 *  net.minecraft.class_304
 *  net.minecraft.class_304$class_11900
 *  net.minecraft.class_310
 */
package io.github.jumperonjava.jjelytraswap.platforms.fabric;

import io.github.jumperonjava.jjelytraswap.JJElytraSwapInit;
import io.github.jumperonjava.jjelytraswap.ModPlatform;
import java.util.function.Consumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;

public class JJElytraSwapFabric
implements ClientModInitializer {
    public void onInitializeClient() {
        JJElytraSwapInit.entrypoint(new FabricPlatform());
    }

    public static class FabricPlatform
    implements ModPlatform {
        @Override
        public String getModloader() {
            return "Fabric";
        }

        @Override
        public boolean isModLoaded(String modloader) {
            return FabricLoader.getInstance().isModLoaded(modloader);
        }

        @Override
        public void registerClientTickEvent(Consumer<class_310> o) {
            ClientTickEvents.END_CLIENT_TICK.register(o::accept);
        }

        @Override
        public class_304 registerKeyBind(String translationKeyName, int defaultKeyId) {
            class_304.class_11900 kbCategory = new class_304.class_11900(class_2960.method_60655((String)"jjelytraswap", (String)"generic"));
            class_304 bind = new class_304(translationKeyName, defaultKeyId, kbCategory);
            KeyBindingHelper.registerKeyBinding((class_304)bind);
            return bind;
        }
    }
}

