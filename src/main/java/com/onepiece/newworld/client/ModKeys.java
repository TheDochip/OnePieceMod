package com.onepiece.newworld.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода. Меняются в «Настройки → Управление → One Piece: New World». */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.onepiece_newworld";

    public static final KeyMapping ABILITY_1 = key("ability_1", GLFW.GLFW_KEY_Z);
    public static final KeyMapping ABILITY_2 = key("ability_2", GLFW.GLFW_KEY_X);
    public static final KeyMapping ABILITY_3 = key("ability_3", GLFW.GLFW_KEY_C);
    public static final KeyMapping ARMAMENT = key("armament", GLFW.GLFW_KEY_V);
    public static final KeyMapping OBSERVATION = key("observation", GLFW.GLFW_KEY_G);
    public static final KeyMapping CHARACTER = key("character", GLFW.GLFW_KEY_K);

    /** Клавиши слотов способностей по порядку. */
    public static final KeyMapping[] ABILITY_SLOTS = {ABILITY_1, ABILITY_2, ABILITY_3};
    public static final KeyMapping[] ALL = {ABILITY_1, ABILITY_2, ABILITY_3, ARMAMENT, OBSERVATION, CHARACTER};

    private ModKeys() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.onepiece_newworld." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    /** Короткая подпись клавиши для интерфейса, например «Z». */
    public static String label(KeyMapping key) {
        return key.getTranslatedKeyMessage().getString();
    }
}
