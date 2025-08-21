package com.robertx22.mine_and_slash.event_hooks.player;

import java.util.List;
import java.util.Stack;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.gui.screens.stat_gui.StatScreen;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ChatUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LookUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.OpenEntityStatsRequestPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.QuickUsePotionPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.UnsummonPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.spells.TellServerToCastSpellPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.settings.KeyModifier;

public class OnKeyPress {

    public static int cooldown = 0;

    // Store what order spell keys are pressed in to prioritize most recently pressed
    private static Stack<SpellKeybind> spellKeysPressed = new Stack<>();

    // Number of last spell sent to the server
    private static int lastSpellNumber = -1;
    // Timer to resend packet so the server knows we want to keep casting
    private static int spellPacketResendTimer = 0;

    public static void onEndTick(Minecraft mc) {

        if (mc.player == null) {
            return;
        }

        if (ChatUtils.wasChatOpenRecently()) {
            return;
        }

        updateSpellInputs();

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (KeybindsRegister.UNSUMMON.isDown()) {
            Packets.sendToServer(new UnsummonPacket());
            cooldown = 3;
        } else if (KeybindsRegister.HUB_SCREEN_KEY.isDown()) {
            mc.setScreen(new MainHubScreen());
            cooldown = 10;
        } else if (KeybindsRegister.HOTBAR_SWAP.isDown()) {
            SpellKeybind.IS_ON_SECONd_HOTBAR = !SpellKeybind.IS_ON_SECONd_HOTBAR;
            cooldown = 5;
        } else if (KeybindsRegister.QUICK_DRINK_POTION.consumeClick()) {
            Packets.sendToServer(new QuickUsePotionPacket());
        } else if (KeybindsRegister.SHOW_ENTITY_STATS.isDown()) {
            if (showEntityStats(mc)) {
                cooldown = 10;
            }
        }
    }

    private static boolean showEntityStats(Minecraft mc) {
        LivingEntity pickedEntity = pickEntity(mc);
        if (pickedEntity == null) {
            return false;
        }
        if (pickedEntity instanceof Player) {
            // we already have stats for other players
            mc.setScreen(new StatScreen(pickedEntity));
        } else {
            // mob stats need to be requested from the server
            Packets.sendToServer(new OpenEntityStatsRequestPacket(pickedEntity));
        }
        return true;
    }

    private static LivingEntity pickEntity(Minecraft mc) {
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) {
            return null;
        }
        List<LivingEntity> results = LookUtils.getLivingEntityLookedAt(cameraEntity, 100.0, true);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    private static boolean checkToAddSpellKeyPress(SpellKeybind key) {
        if (key.key.consumeClick()) {
            spellKeysPressed.add(key);
            // Consume any remaining clicks
            while (key.key.consumeClick()) {
            }
            return true;
        }
        return false;
    }

    private static void updateSpellInputs() {
        var keys = SpellKeybind.ALL;

        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            keys = SpellKeybind.FIRST_HOTBAR_KEYS;
        }

        spellKeysPressed.removeIf(key -> !key.key.isDown());

        // Prioritize binds with modifiers in case the same key is reused but with a modifier
        for (SpellKeybind key : keys) {
            if (key.key.getKeyModifier() == KeyModifier.NONE) {
                if (checkToAddSpellKeyPress(key)) {
                    break;
                }
            }
        }

        for (SpellKeybind key : keys) {
            if (key.key.getKeyModifier() != KeyModifier.NONE) {
                if (checkToAddSpellKeyPress(key)) {
                    break;
                }
            }
        }

        int number;

        if (!spellKeysPressed.empty()) {
            number = spellKeysPressed.lastElement().getIndex();
            if (SpellKeybind.IS_ON_SECONd_HOTBAR) {
                number += 4;
            }
        } else {
            number = -1;
        }

        if (number == lastSpellNumber) {
            if (number == -1) {
                return;
            }
            if (spellPacketResendTimer > 0) {
                spellPacketResendTimer--;
                return;
            }
        }

        Packets.sendToServer(new TellServerToCastSpellPacket(number));
        lastSpellNumber = number;
        spellPacketResendTimer = 2;
    }
}
