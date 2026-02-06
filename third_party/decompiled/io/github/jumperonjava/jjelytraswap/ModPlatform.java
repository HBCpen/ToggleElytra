/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 */
package io.github.jumperonjava.jjelytraswap;

import java.util.function.Consumer;
import net.minecraft.class_304;
import net.minecraft.class_310;

public interface ModPlatform {
    public String getModloader();

    public boolean isModLoaded(String var1);

    public void registerClientTickEvent(Consumer<class_310> var1);

    public class_304 registerKeyBind(String var1, int var2);
}

