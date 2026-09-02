package com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The small mercenary screen interactions that aren't an item pick: cycling the combat mode, choosing
 * a class, clearing a slot, moving a skill up or down the cast priority, and slotting a learned skill.
 * One packet rather than five, because they all do the same thing - mutate the active mercenary and
 * sync back.
 */
public class MercenaryActionPacket extends MyPacket<MercenaryActionPacket> {

    public enum Action {
        CYCLE_MODE,
        SET_CLASS,
        CLEAR_SLOT,
        SET_SKILL,
        MOVE_PRIORITY_UP,
        MOVE_PRIORITY_DOWN,
        TOGGLE_DISABLED,
    }

    private Action action = Action.CYCLE_MODE;
    private String str = "";
    private int index = 0;

    public MercenaryActionPacket() {
    }

    private MercenaryActionPacket(Action action, String str, int index) {
        this.action = action;
        this.str = str;
        this.index = index;
    }

    public static MercenaryActionPacket cycleMode() {
        return new MercenaryActionPacket(Action.CYCLE_MODE, "", 0);
    }

    public static MercenaryActionPacket toggleDisabled() {
        return new MercenaryActionPacket(Action.TOGGLE_DISABLED, "", 0);
    }

    public static MercenaryActionPacket setClass(String classId) {
        return new MercenaryActionPacket(Action.SET_CLASS, classId, 0);
    }

    public static MercenaryActionPacket clearSlot(MercenarySlotType type, int index) {
        return new MercenaryActionPacket(Action.CLEAR_SLOT, type.name(), index);
    }

    public static MercenaryActionPacket setSkill(int slot, String spellId) {
        return new MercenaryActionPacket(Action.SET_SKILL, spellId, slot);
    }

    public static MercenaryActionPacket movePriority(int slot, boolean up) {
        return new MercenaryActionPacket(up ? Action.MOVE_PRIORITY_UP : Action.MOVE_PRIORITY_DOWN, "", slot);
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "mercenary_action");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        action = tag.readEnum(Action.class);
        str = tag.readUtf(100);
        index = tag.readVarInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeEnum(action);
        tag.writeUtf(str, 100);
        tag.writeVarInt(index);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) {
            return;
        }
        // hiding the hub button is presentation only - this is where the level gate is actually
        // enforced, so a packet sent before the character has unlocked mercenaries does nothing.
        if (!MercenaryManager.isUnlocked(p)) {
            return;
        }
        MercenaryData data = Load.player(p).mercs.getActive();

        switch (action) {
            case CYCLE_MODE -> {
                data.mode = data.mode.next();
                Load.player(p).playerDataSync.setDirtyAndSync(p);
                // announced from here rather than from the keybind handler for two reasons: this side
                // holds the authoritative new mode - the client copy is still a sync behind at the
                // moment of the press - and the screen's own mode button gets the same feedback free.
                // both lang keys already exist, the mode tooltip is built from this exact pair.
                p.displayClientMessage(Words.MercenaryCombatMode.locName()
                        .append(": ")
                        .append(data.mode.locName().withStyle(data.mode.format)), true);
            }
            case TOGGLE_DISABLED -> {
                // on the storage rather than on `data`, which is per mercenary class - switching
                // mercenaries off has to put every one of them away, not just the active one.
                // MercenaryManager.onPlayerTick does the actual dismissing on its next pass.
                var mercs = Load.player(p).mercs;
                mercs.disabled = !mercs.disabled;
                Load.player(p).playerDataSync.setDirtyAndSync(p);
            }
            case SET_CLASS -> {
                // never trust a client supplied registry id
                if (ExileDB.Mercenaries().isRegistered(str)) {
                    MercenaryManager.switchTo(p, str);
                }
            }
            case CLEAR_SLOT -> clearSlot(p, data);
            case SET_SKILL -> equipSkill(p, index, str);
            case MOVE_PRIORITY_UP -> {
                data.moveSkillPriority(index, true);
                MercenarySlotType.afterChange(p);
            }
            case MOVE_PRIORITY_DOWN -> {
                data.moveSkillPriority(index, false);
                MercenarySlotType.afterChange(p);
            }
        }
    }

    private void clearSlot(Player p, MercenaryData data) {
        MercenarySlotType type;
        try {
            type = MercenarySlotType.valueOf(str);
        } catch (IllegalArgumentException e) {
            return;
        }
        MyInventory inv = type.inventoryOf(data);
        if (index < 0 || index >= inv.getContainerSize()) {
            return;
        }
        ItemStack stack = inv.getItem(index);
        if (stack.isEmpty()) {
            return;
        }
        inv.setItem(index, ItemStack.EMPTY);
        MercenarySlotType.giveBack(p, stack);
        MercenarySlotType.afterChange(p);
    }

    /**
     * Slot a learned skill into one of the 4 active slots. Public and static so the picker's
     * {@code GuiAction} and this packet share one copy of the validation - a GuiAction runs server
     * side through InvGuiPacket, so both paths reach the same code with a real player.
     */
    public static void equipSkill(Player p, int slot, String spellId) {
        MercenaryData data = Load.player(p).mercs.getActive();

        if (slot < 0 || slot >= MercenaryClass.EQUIPPED_SKILLS) {
            return;
        }
        MercenaryClass mc = data.getMercClass();
        if (mc == null) {
            return;
        }
        // empty clears the slot; anything else has to be a skill this mercenary has actually learned
        if (!spellId.isEmpty() && !mc.isSkillUnlocked(spellId, data.lvl)) {
            return;
        }
        // a skill can only sit in one slot - move it rather than letting it be equipped twice
        int existing = spellId.isEmpty() ? -1 : data.slotOfSpell(spellId);
        if (existing > -1 && existing != slot) {
            data.setEquippedSkill(existing, data.getEquippedSkill(slot));
        }
        data.setEquippedSkill(slot, spellId);

        // the support gems under a slot belong to whatever skill is in it, so a changed skill has to
        // drop them back to the player rather than silently buffing a different one.
        returnSupports(p, data, slot);
        if (existing > -1 && existing != slot) {
            returnSupports(p, data, existing);
        }

        MercenarySlotType.afterChange(p);
    }

    private static void returnSupports(Player p, MercenaryData data, int skillSlot) {
        MyInventory inv = data.getSupports();
        for (int i = 0; i < MercenaryClass.SUPPORTS_PER_SKILL; i++) {
            int idx = skillSlot * MercenaryClass.SUPPORTS_PER_SKILL + i;
            ItemStack stack = inv.getItem(idx);
            if (!stack.isEmpty()) {
                inv.setItem(idx, ItemStack.EMPTY);
                MercenarySlotType.giveBack(p, stack);
            }
        }
    }

    @Override
    public MyPacket<MercenaryActionPacket> newInstance() {
        return new MercenaryActionPacket();
    }
}
