package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KeybindsRegister {

    static String CATEGORY = "key." + SlashRef.MODID + ".keybind";
    static String prefix = SlashRef.MODID + ".key.";

    public static KeyMapping HUB_SCREEN_KEY = new ExileKeyMapping(prefix + "hub_screen", GLFW.GLFW_KEY_H, CATEGORY);

    public static KeyMapping UNSUMMON = new ExileKeyMapping(prefix + "unsummon", GLFW.GLFW_KEY_MINUS, CATEGORY);

    public static KeyMapping HOTBAR_SWAP = new ExileKeyMapping(prefix + "hotbar_swap", GLFW.GLFW_KEY_F1, CATEGORY);

    public static KeyMapping QUICK_DRINK_POTION = new ExileKeyMapping(prefix + "quick_drink_potion", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);

    public static KeyMapping SHOW_ENTITY_STATS = new ExileKeyMapping(prefix + "show_entity_stats", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);

    public static KeyMapping OPEN_MASTER_BACKPACK = new ExileKeyMapping(
            prefix + "open_master_backpack",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static SpellKeybind SPELL_HOTBAR_1 = new SpellKeybind(1, GLFW.GLFW_KEY_R, null, true);
    public static SpellKeybind SPELL_HOTBAR_2 = new SpellKeybind(2, GLFW.GLFW_KEY_F, null, true);
    public static SpellKeybind SPELL_HOTBAR_3 = new SpellKeybind(3, GLFW.GLFW_KEY_C, null, true);
    public static SpellKeybind SPELL_HOTBAR_4 = new SpellKeybind(4, GLFW.GLFW_KEY_V, null, true);
    public static SpellKeybind SPELL_HOTBAR_5 = new SpellKeybind(5, GLFW.GLFW_KEY_R, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_6 = new SpellKeybind(6, GLFW.GLFW_KEY_F, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_7 = new SpellKeybind(7, GLFW.GLFW_KEY_C, KeyModifier.SHIFT, false);
    public static SpellKeybind SPELL_HOTBAR_8 = new SpellKeybind(8, GLFW.GLFW_KEY_V, KeyModifier.SHIFT, false);

    public static SpellKeybind getSpellHotbar(int num) {
        return SpellKeybind.ALL.stream().filter(x -> x.getIndex() == num).findAny().orElseThrow(() -> new RuntimeException(num + " isn't a valid hotbar number"));
    }

    public static List<KeyMapping> allKeys() {
        List<KeyMapping> list = new ArrayList<>(List.of(
                HUB_SCREEN_KEY,
                UNSUMMON,
                HOTBAR_SWAP,
                QUICK_DRINK_POTION,
                SHOW_ENTITY_STATS,
                OPEN_MASTER_BACKPACK
        ));
        for (SpellKeybind k : SpellKeybind.ALL) {
            list.add(k.key);
        }
        return list;
    }

    /**
     * Cleans up bind states the vanilla controls screen can leave behind, which would otherwise make a
     * bind silently keep a modifier it shouldn't have.
     */
    public static void sanitizeBinds() {
        boolean changed = false;

        for (KeyMapping k : allKeys()) {
            if (KeyModifier.isKeyCodeModifier(k.getKey())) {
                // A bind resting on a bare SHIFT/CTRL/ALT is dead anyway, and Forge derives the NEXT
                // rebind's modifier from the old key - which is how a spell ends up needing shift again.
                k.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
                changed = true;
            } else if (k.getKey().getType() == InputConstants.Type.MOUSE && k.getKeyModifier() != KeyModifier.NONE) {
                // Forge's controls screen can't produce a modifier + mouse button bind, so one in that
                // state is the unpatched mouseClicked leaving a stale modifier behind.
                k.setKeyModifierAndCode(KeyModifier.NONE, k.getKey());
                changed = true;
            }
        }

        if (changed) {
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        }
    }

    public static void register(RegisterKeyMappingsEvent x) {
        for (KeyMapping k : allKeys()) {
            x.register(k);
        }
    }

}
