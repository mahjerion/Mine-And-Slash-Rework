package com.robertx22.mine_and_slash.characters;

import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerBuffData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
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

        // before load(p), so the removals still see the outgoing character's level and stats
        clearTemporaryBuffs(p);

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

    // buffs are stored per player, not per character, so anything still running when you switch would be
    // inherited by whoever you switch to - eat a meal, swap, and the whole roster rides along on it. death
    // already wipes these for the same reason (see OnPlayerDeath); a switch has to as well.
    //
    // two separate stores hold them. PlayerData.buff is the food/elixir side (meals, seafood, alchemy
    // elixirs) plus the vanilla effect that only exists to draw its hud icon. EntityData.statusEffects is
    // the exile effect side - auras, stances, shrine buffs, anything a spell put on you.
    //
    // summons are per player too, and CharacterData.load swaps the hotbar wholesale rather than going
    // through setHotbar, so nothing else would notice their skill just left the bar.
    private static void clearTemporaryBuffs(Player p) {

        var data = Load.player(p);
        var unit = Load.Unit(p);

        SummonPetAction.despawnAllSummons(p);

        data.buff = new PlayerBuffData();
        for (PlayerBuffData.Type type : PlayerBuffData.Type.values()) {
            // the vanilla effect is purely the icon for the buff above, so it goes with it
            p.removeEffect(type.effect.get());
        }

        var statuses = unit.getStatusEffectsData();

        if (!statuses.exileMap.isEmpty()) {
            // go through onRemove rather than dropping the map. that's what takes the vanilla attribute
            // modifiers back off (ExileEffect.mc_stats) - clearing alone would leave them on the player
            // permanently, with no effect left to ever remove them. it's the same path expiry takes.
            for (String id : new ArrayList<>(statuses.exileMap.keySet())) {
                ExileEffect eff = ExileDB.ExileEffects().get(id);
                if (eff != null) {
                    eff.onRemove(p);
                }
            }
            // after the loop, so an on-expire spell that re-applies something doesn't survive the switch
            statuses.exileMap.clear();
        }

        // statusEffects is part of the entity cap's client nbt, so the client keeps showing the old
        // effects until this resyncs. afterSwap only marks the player cap.
        unit.sync.setDirty();
        unit.setEquipsChanged();
    }

    // getOrDefault evaluates its default eagerly, so this used to build a throwaway CharacterData -
    // four MyInventory totalling 75 ItemStack slots, plus four POJOs - on every call, even when the
    // entry exists. only build the blank on a real miss.
    //
    // that blank leaves `name` null, which is why anything comparing it has to be null safe - see
    // PlayerStatUtils.addBonusExpPerCharacters. a shared static blank would be cheaper still, but
    // callers get a mutable object back, so keep it per-miss.
    public CharacterData getCurrent() {
        CharacterData data = this.map.get(current);
        return data != null ? data : new CharacterData();
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

