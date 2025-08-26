package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class KeybindsRegister {

    static String CATEGORY = "key." + SlashRef.MODID + ".keybind";
    static String prefix = SlashRef.MODID + ".key.";

    public static KeyMapping HUB_SCREEN_KEY = new KeyMapping(prefix + "hub_screen", GLFW.GLFW_KEY_H, CATEGORY);

    public static KeyMapping UNSUMMON = new KeyMapping(prefix + "unsummon", GLFW.GLFW_KEY_MINUS, CATEGORY);

    public static KeyMapping HOTBAR_SWAP = new KeyMapping(prefix + "hotbar_swap", GLFW.GLFW_KEY_F1, CATEGORY);

    public static KeyMapping QUICK_DRINK_POTION = new KeyMapping(prefix + "quick_drink_potion", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);

    public static KeyMapping SHOW_ENTITY_STATS = new KeyMapping(prefix + "show_entity_stats", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);

    public static KeyMapping OPEN_MASTER_BACKPACK = new KeyMapping(prefix + "open_master_backpack", GLFW.GLFW_KEY_B, CATEGORY);

    public static SpellKeybind SPELL_HOTBAR_1 = new SpellKeybind(1, GLFW.GLFW_KEY_R, null, true);
    public static SpellKeybind SPELL_HOTBAR_2 = new SpellKeybind(2, GLFW.GLFW_KEY_V, null, true);
    public static SpellKeybind SPELL_HOTBAR_3 = new SpellKeybind(3, GLFW.GLFW_KEY_C, null, true);
    public static SpellKeybind SPELL_HOTBAR_4 = new SpellKeybind(4, GLFW.GLFW_KEY_G, null, true);
    public static SpellKeybind SPELL_HOTBAR_5 = new SpellKeybind(5, GLFW.GLFW_KEY_R, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_6 = new SpellKeybind(6, GLFW.GLFW_KEY_V, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_7 = new SpellKeybind(7, GLFW.GLFW_KEY_C, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_8 = new SpellKeybind(8, GLFW.GLFW_KEY_G, KeyModifier.SHIFT, false);

    public static SpellKeybind getSpellHotbar(int num) {
        return SpellKeybind.ALL.stream().filter(x -> x.getIndex() == num).findAny().orElseThrow(() -> new RuntimeException(num + " isn't a valid hotbar number"));
    }

    public static void register(RegisterKeyMappingsEvent x) {

        x.register(HUB_SCREEN_KEY);
        x.register(UNSUMMON);
        x.register(HOTBAR_SWAP);
        x.register(QUICK_DRINK_POTION);
        x.register(SHOW_ENTITY_STATS);
        x.register(OPEN_MASTER_BACKPACK);
        for (SpellKeybind k : SpellKeybind.ALL) {
            x.register(k.key);
        }

    }

}
