/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.lunaluna.fabric.elytrarecast.config.ElytraRecastConfig
 *  net.minecraft.class_10192
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_1799
 *  net.minecraft.class_1887
 *  net.minecraft.class_1890
 *  net.minecraft.class_1893
 *  net.minecraft.class_2378
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_2848
 *  net.minecraft.class_2848$class_2849
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_338
 *  net.minecraft.class_5134
 *  net.minecraft.class_5321
 *  net.minecraft.class_6880
 *  net.minecraft.class_7924
 *  net.minecraft.class_9285
 *  net.minecraft.class_9285$class_9287
 *  net.minecraft.class_9331
 *  net.minecraft.class_9334
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package io.github.jumperonjava.jjelytraswap;

import io.github.jumperonjava.jjelytraswap.ModPlatform;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import me.lunaluna.fabric.elytrarecast.config.ElytraRecastConfig;
import net.minecraft.class_10192;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1657;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_2378;
import net.minecraft.class_2561;
import net.minecraft.class_2596;
import net.minecraft.class_2848;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_338;
import net.minecraft.class_5134;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_7924;
import net.minecraft.class_9285;
import net.minecraft.class_9331;
import net.minecraft.class_9334;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JJElytraSwapInit {
    public static final String MODID = "jjelytraswap";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"JJElytraSwap");
    public static ModPlatform PLATFORM = null;
    public static boolean enabled = true;
    public static boolean shouldWearChestplatePrevTick = true;

    public static void entrypoint(ModPlatform platform) {
        PLATFORM = platform;
        JJElytraSwapInit.onInitializeClient();
    }

    public static boolean stackHasComponent(class_1799 stack, class_9331<?> type) {
        return stack.method_57826(type);
    }

    public static void tryWearChestplate(class_310 client) {
        if (client.field_1687 == null || client.field_1724 == null) {
            return;
        }
        if (client.field_1724.method_6118(class_1304.field_6174).method_7960()) {
            return;
        }
        List chestplateSlots = JJElytraSwapInit.getChestplateSlots();
        chestplateSlots = chestplateSlots.stream().filter(slot -> (float)JJElytraSwapInit.getChestplateStat(client.field_1724.method_31548().method_5438(slot.intValue())) > 0.0f).sorted(Comparator.comparingInt(slot -> JJElytraSwapInit.getChestplateStat(client.field_1724.method_31548().method_5438(slot.intValue())))).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(chestplateSlots);
        if (PLATFORM.isModLoaded("elytra-recast")) {
            try {
                if (client.field_1690.field_1903.method_1434() && JJElytraSwapInit.elytraRecastEnabled()) {
                    return;
                }
            }
            catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        if (!chestplateSlots.isEmpty()) {
            int bestSlot = (Integer)chestplateSlots.get(0);
            JJElytraSwapInit.swap(bestSlot, client);
        }
    }

    private static boolean elytraRecastEnabled() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return ElytraRecastConfig.enabled && ElytraRecastConfig.jumpEnabled;
    }

    public static void tryWearElytra() {
        if (class_310.method_1551().field_1687 == null || class_310.method_1551().field_1724 == null) {
            return;
        }
        if (JJElytraSwapInit.stackHasComponent(class_310.method_1551().field_1724.method_31548().method_5438(38), class_9334.field_54197)) {
            return;
        }
        List<Integer> elytraSlots = JJElytraSwapInit.getElytraSlots();
        elytraSlots.sort(Comparator.comparingInt(slot -> JJElytraSwapInit.getElytraStat(class_310.method_1551().field_1724.method_31548().method_5438(slot.intValue()))));
        if (!elytraSlots.isEmpty()) {
            int bestSlot = elytraSlots.get(elytraSlots.size() - 1);
            JJElytraSwapInit.wearElytra(bestSlot);
        }
    }

    public static List<Integer> getElytraSlots() {
        ArrayList<Integer> elytraSlots = new ArrayList<Integer>();
        for (int slot : JJElytraSwapInit.slotArray()) {
            if (!JJElytraSwapInit.stackHasComponent(class_310.method_1551().field_1724.method_31548().method_5438(slot), class_9334.field_54197)) continue;
            elytraSlots.add(slot);
        }
        return elytraSlots;
    }

    public static List<Integer> getChestplateSlots() {
        ArrayList<Integer> chestplateSlots = new ArrayList<Integer>();
        class_310 client = class_310.method_1551();
        for (int slot : JJElytraSwapInit.slotArray()) {
            if (!JJElytraSwapInit.isSlotChestplate(slot)) continue;
            chestplateSlots.add(slot);
        }
        return chestplateSlots;
    }

    private static class_2378<class_1887> getEnchantmentRegistry() {
        return class_310.method_1551().field_1687.method_30349().method_30530(class_7924.field_41265);
    }

    private static int getLevel(class_5321<class_1887> key, class_1799 stack) {
        class_1887 enchant = (class_1887)JJElytraSwapInit.getEnchantmentRegistry().method_29107(key);
        class_6880 enchantEntry = JJElytraSwapInit.getEnchantmentRegistry().method_47983((Object)enchant);
        return class_1890.method_8225((class_6880)enchantEntry, (class_1799)stack);
    }

    private static int getElytraStat(class_1799 elytraItem) {
        int stat = JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9101, elytraItem) * 3 + 1 + JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9119, elytraItem);
        return stat;
    }

    private static int getChestplateStat(class_1799 chestplateItem) {
        float score = 1.0f;
        class_1799 holder = chestplateItem;
        if (JJElytraSwapInit.stackHasComponent(chestplateItem, class_9334.field_54196) && ((class_10192)chestplateItem.method_58694(class_9334.field_54196)).comp_3174() == class_1304.field_6174) {
            class_9285 component = (class_9285)chestplateItem.method_58694(class_9334.field_49636);
            for (class_9285.class_9287 entry : component.comp_2393()) {
                class_6880 attribute = entry.comp_2395();
                if (attribute == class_5134.field_23724) {
                    score = (float)((double)score + entry.comp_2396().comp_2449());
                }
                if (attribute != class_5134.field_23725) continue;
                score = (float)((double)score + entry.comp_2396().comp_2449());
            }
            score += (float)(JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9111, chestplateItem) * 2);
            score = (float)((double)score + (double)JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9101, chestplateItem) * 0.5);
            score = (float)((double)score + (JJElytraSwapInit.stackHasComponent(chestplateItem, class_9334.field_49631) ? 0.25 : 0.0));
            score = (float)((double)score + (double)JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9119, chestplateItem) * 0.24 / 3.0);
        }
        return (int)(score * 1000.0f);
    }

    private static void wearElytra(int slotId) {
        JJElytraSwapInit.swap(slotId, class_310.method_1551());
        try {
            class_310.method_1551().method_1562().method_52787((class_2596)new class_2848((class_1297)class_310.method_1551().field_1724, class_2848.class_2849.field_12982));
            class_310.method_1551().field_1724.method_23669();
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private static void swap(int slot, class_310 client) {
        int slot2 = slot;
        if (slot2 == 40) {
            slot2 = 45;
        }
        if (slot2 < 9) {
            slot2 += 36;
        }
        try {
            client.field_1761.method_2906(0, slot2, 0, class_1713.field_7790, (class_1657)client.field_1724);
            client.field_1761.method_2906(0, 6, 0, class_1713.field_7790, (class_1657)client.field_1724);
            client.field_1761.method_2906(0, slot2, 0, class_1713.field_7790, (class_1657)client.field_1724);
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isSlotChestplate(int slotId) {
        if (class_310.method_1551().field_1724 == null) {
            return false;
        }
        class_1799 chestSlot = class_310.method_1551().field_1724.method_31548().method_5438(slotId);
        return !chestSlot.method_7960() && JJElytraSwapInit.stackHasComponent(chestSlot, class_9334.field_54196) && ((class_10192)chestSlot.method_58694(class_9334.field_54196)).comp_3174() == class_1304.field_6174 && JJElytraSwapInit.getLevel((class_5321<class_1887>)class_1893.field_9113, chestSlot) == 0;
    }

    private static int[] slotArray() {
        int i;
        int[] range = new int[37];
        for (i = 0; i < 9; ++i) {
            range[i] = 8 - i;
        }
        for (i = 9; i < 36; ++i) {
            range[i] = 35 - (i - 9);
        }
        range[36] = 40;
        return range;
    }

    public static void onInitializeClient() {
        class_304 bind = PLATFORM.registerKeyBind("jjelytraswap.keybind", -1);
        PLATFORM.registerClientTickEvent(client -> {
            boolean shouldWearChestplate;
            if (client.field_1687 == null || client.field_1724 == null) {
                return;
            }
            if (bind.method_1436()) {
                enabled = !enabled;
                String ts = "jjelytraswap." + (enabled ? "enabled" : "disabled");
                client.field_1705.method_1743().method_1812((class_2561)class_2561.method_43471((String)ts));
            }
            if (!enabled) {
                return;
            }
            boolean isInAir = !client.field_1724.method_24828() && !client.field_1724.method_52535();
            boolean bl = shouldWearChestplate = !isInAir;
            if (shouldWearChestplate && !shouldWearChestplatePrevTick && JJElytraSwapInit.stackHasComponent(class_310.method_1551().field_1724.method_6118(class_1304.field_6174), class_9334.field_54197)) {
                JJElytraSwapInit.tryWearChestplate(client);
            }
            shouldWearChestplatePrevTick = shouldWearChestplate;
        });
    }

    private static void debugLogInChat(Object ... objects) {
        class_338 chat = class_310.method_1551().field_1705.method_1743();
        if (chat == null) {
            return;
        }
        StringBuilder msg = new StringBuilder();
        for (Object object : objects) {
            if (object instanceof class_2561) {
                class_2561 text = (class_2561)object;
                msg.append(text.method_10858(100000));
                continue;
            }
            msg.append(object);
        }
        chat.method_1812(class_2561.method_30163((String)msg.toString()));
    }
}

