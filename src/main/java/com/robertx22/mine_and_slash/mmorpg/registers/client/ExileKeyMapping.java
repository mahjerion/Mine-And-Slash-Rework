package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

/**
 * A KeyMapping that keeps its KeyModifier in sync with the key it is actually bound to.
 * <p>
 * Vanilla Options.setKey only assigns the key and leaves the modifier alone. Forge fixes that up in
 * KeyBindsScreen.keyPressed by calling setKeyModifierAndCode first, but it never patched
 * KeyBindsScreen.mouseClicked. So a bind created with KeyModifier.SHIFT that gets rebound to a mouse
 * button kept requiring shift forever, and that state got persisted to options.txt.
 */
public class ExileKeyMapping extends KeyMapping {

    public ExileKeyMapping(String name, int keyCode, String category) {
        super(name, keyCode, category);
    }

    public ExileKeyMapping(String name, IKeyConflictContext conflictContext, InputConstants.Type type, int keyCode, String category) {
        super(name, conflictContext, type, keyCode, category);
    }

    public ExileKeyMapping(String name, IKeyConflictContext conflictContext, KeyModifier modifier, InputConstants.Type type, int keyCode, String category) {
        super(name, conflictContext, modifier, type, keyCode, category);
    }

    @Override
    public void setKey(InputConstants.Key key) {
        if (key.equals(this.getKey())) {
            // Forge's keyboard path already ran setKeyModifierAndCode, so the keys match here and we
            // leave the modifier it just resolved alone.
            super.setKey(key);
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            // The controls screen offers no way to type a modifier for a mouse button, so a mouse bind
            // is always a plain one.
            this.setKeyModifierAndCode(KeyModifier.NONE, key);
        } else {
            // Any other path that skipped setKeyModifierAndCode (third party controls screens) arrives
            // here with a new key and a stale modifier.
            this.setKeyModifierAndCode(null, key);
        }
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier modifier, InputConstants.Key keyCode) {
        if (modifier == null && KeyModifier.isKeyCodeModifier(this.getKey())) {
            // Forge derives the new modifier from the old key, which is right for the deliberate
            // "hold shift, then press Z" combo flow. But KeyBindsScreen also parks a bind on a bare
            // modifier key when the player taps one and lets go, and then the next rebind inherits a
            // modifier nobody asked for. Only honour it while the key is actually held down.
            KeyModifier old = KeyModifier.getModifier(this.getKey());
            modifier = old != null && old.isActive(null) ? old : KeyModifier.NONE;
        }
        super.setKeyModifierAndCode(modifier, keyCode);
    }
}
