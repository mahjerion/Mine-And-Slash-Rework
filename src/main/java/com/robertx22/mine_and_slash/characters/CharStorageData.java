package com.robertx22.mine_and_slash.characters;

import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CharStorageData {

    public int current = 0;

    public HashMap<Integer, CharacterData> map = new HashMap<>();

    // switching characters also moves the loadout: whatever the outgoing character has equipped and
    // socketed goes into its own CharacterData, and the incoming character's stored items go back on.
    // that covers worn gear, support gems, aura gems and jewels - see CharacterEquipment.
    //
    // the invariant that keeps this from duplicating items is that the *active* character's storage is
    // always empty - the items live on the player instead. every step below preserves that.
    public void load(int num, Player p) {

        var data = map.get(num);

        if (data == null) {
            return;
        }
        if (num == current) {
            // nothing to do, and going ahead would snapshot the player into the same entry we're about
            // to restore from. ToonActionPacket already refuses this for the LOAD button, but
            // CreateCharPacket reaches here with num == current when naming the very first character.
            return;
        }

        CharacterData snapshot = CharacterData.from(p);
        CharacterEquipment.stashInto(p, snapshot.getEquipment());
        CharacterEquipment.stashGemsAndJewels(p, snapshot);
        map.put(current, snapshot);

        int stored = CharacterEquipment.countEverything(snapshot);
        int restored = CharacterEquipment.countEverything(data); // the restores empty it

        data.load(p); // sets the level last, so do this before handing the items back
        CharacterEquipment.restoreFrom(p, data.getEquipment());
        // after load(p), so the hotbar the support gems belong to is already the incoming character's
        CharacterEquipment.restoreGemsAndJewels(p, data);

        this.current = num;

        CharacterEquipment.afterSwap(p);

        if (stored > 0 || restored > 0) {
            p.sendSystemMessage(Chats.CHARACTER_SWITCHED_GEAR.locName(stored, restored)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public CharacterData getCurrent() {
        return this.map.getOrDefault(current, new CharacterData());
    }

    public boolean canChangeCharactersRightNow(Player p) {
        if (Load.Unit(p).getCooldowns().isOnCooldown(CooldownsData.IN_COMBAT)) {
            p.sendSystemMessage(Chats.CANT_CHANGE_CHAR_IN_COMBAT.locName().withStyle(ChatFormatting.RED));
            return false;
        }
        if (WorldUtils.isMapWorldClass(p.level(), p.blockPosition())) {
            p.sendSystemMessage(Chats.CANT_CHANGE_CHAR_IN_MAP.locName().withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }


    public int tryAddNewCharacter(Player p, String name) {

        if (!canChangeCharactersRightNow(p))
        {
            return -1;
        }

        if (!nameIsValid(p, name)) {
            p.sendSystemMessage(Chats.CREATE_ERROR_NAME.locName().withStyle(ChatFormatting.RED));
            return -1;
        }

        int amount = getAllCharacters().size();

        if (amount < ServerContainer.get().MAX_CHARACTERS.get()) {

            var data = new CharacterData();

            if (map.get(current) == null) {
                data = CharacterData.from(p); // if it's first time making character, let it just set name instead of deleting everything
            }

            data.name = name;

            for (int i = 0; i < ServerContainer.get().MAX_CHARACTERS.get(); i++) {
                if (map.get(i) == null) {
                    map.put(i, data);
                    return i;
                }
            }
        } else {
            p.sendSystemMessage(Chats.CREATE_ERROR_CHAR_LIMIT.locName().withStyle(ChatFormatting.RED));
        }

        return -1;
    }

    public boolean nameIsValid(Player p, String name) {

        if (name.isEmpty()) {
            p.sendSystemMessage(Chats.NAME_EMPTY.locName().withStyle(ChatFormatting.RED));
            return false;
        }
        if (getAllCharacters().stream().anyMatch(x -> x.name.equals(name))) {
            p.sendSystemMessage(Chats.NAME_SAME.locName().withStyle(ChatFormatting.RED));
            return false;
        }
        if (name.length() > 20) {
            p.sendSystemMessage(Chats.NAME_TOO_LONG.locName().withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }


    public List<CharacterData> getAllCharacters() {
        return map.values().stream().filter(x -> x != null).collect(Collectors.toList());
    }


}

