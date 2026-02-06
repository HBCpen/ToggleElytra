/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package io.github.jumperonjava.jjelytraswap;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class ConfigScreen
extends class_437 {
    public ConfigScreen(class_437 parent) {
        super((class_2561)class_2561.method_43473());
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        super.method_25394(context, mouseX, mouseY, delta);
        context.method_25300(this.field_22787.field_1772, "Hello, world", this.field_22789 / 2, this.field_22790 / 2, -1);
    }

    public static ConfigScreen createConfigScreen(class_437 parent) {
        return new ConfigScreen(parent);
    }
}

