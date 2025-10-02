package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.ArrayList;
import java.util.List;

public class SpellKeybind {

    public static boolean IS_ON_SECONd_HOTBAR = false;

    public static List<SpellKeybind> ALL = new ArrayList<>();
    public static List<SpellKeybind> FIRST_HOTBAR_KEYS = new ArrayList<>();

    public KeyMapping key;
    public int num = 0;
    boolean firstbar;

    public SpellKeybind(int num, int key, KeyModifier mod, boolean firstbar) {
        if (mod != null) {
            this.key = new KeyMapping(
                    KeybindsRegister.prefix + "spell_" + num,
                    KeyConflictContext.IN_GAME,
                    mod,
                    InputConstants.Type.KEYSYM,
                    key,
                    KeybindsRegister.CATEGORY
            );
        } else {
            this.key = new KeyMapping(
                    KeybindsRegister.prefix + "spell_" + num,
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    key,
                    KeybindsRegister.CATEGORY
            );
        }
        this.firstbar = firstbar;
        this.num = num;

        ALL.add(this);
        if (firstbar) {
            FIRST_HOTBAR_KEYS.add(this);
        }
    }

    public int getIndex() {
        int n = num - 1;

/*
        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            if (IS_ON_SECONd_HOTBAR) {
                if (!firstbar) {
                    n -= 4;

                }
            }
        }

 */

        return n;
    }

}