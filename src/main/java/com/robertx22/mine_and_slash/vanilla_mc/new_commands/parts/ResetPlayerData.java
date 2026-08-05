package com.robertx22.mine_and_slash.vanilla_mc.new_commands.parts;

import com.robertx22.mine_and_slash.capability.player.data.PlayerProfessionsData;
import com.robertx22.mine_and_slash.capability.player.data.RestedExpData;
import com.robertx22.mine_and_slash.capability.player.data.StatPointsData;
import com.robertx22.mine_and_slash.characters.CharStorageData;
import com.robertx22.mine_and_slash.characters.CharacterEquipment;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.saveclasses.atlas.AtlasData;
import com.robertx22.mine_and_slash.saveclasses.perks.TalentsData;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellSchoolsData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

public enum ResetPlayerData {
    LEVEL() {
        @Override
        public void reset(Player p) {
            Load.Unit(p).setLevel(1);
        }
    },
    SPELL_COOLDOWNS() {
        @Override
        public void reset(Player p) {
            Load.Unit(p).getCooldowns().onTicksPass(555555);
            for (int i = 0; i < 10; i++) {
                Load.player(p).spellCastingData.charges.onTicks(p, 500000);
            }
        }
    },
    SPELLS() {
        @Override
        public void reset(Player p) {
            Load.player(p).ascClass = new SpellSchoolsData();
        }
    },
    PROFESSIONS() {
        @Override
        public void reset(Player p) {
            Load.player(p).professions = new PlayerProfessionsData();
        }
    },
    RESTED_EXP() {
        @Override
        public void reset(Player p) {
            Load.player(p).rested_xp = new RestedExpData();
        }
    },
    // characters store the gear they had equipped when switched away from, so throwing the storage away
    // would destroy real items belonging to every alt. hand them back first - they land in the player's
    // inventory, or on the ground when it's full.
    CHARACTERS() {
        @Override
        public void reset(Player p) {
            var chars = Load.player(p).characters;
            chars.getAllCharacters().forEach(c -> CharacterEquipment.returnAllToPlayer(p, c.getEquipment()));
            Load.player(p).characters = new CharStorageData();
        }
    },
    BONUS_TALENTS() {
        @Override
        public void reset(Player p) {
            Load.player(p).bonusTalents = 0;
        }
    },
    STATS() {
        @Override
        public void reset(Player p) {
            Load.player(p).statPoints = new StatPointsData();
        }
    },
    TALENTS() {
        @Override
        public void reset(Player p) {
            Load.player(p).talents = new TalentsData();
        }
    },
    // atlas map progress and the atlas passive tree are one unit: the map is the only source of
    // atlas points (PlayerPointsType.ATLAS has base_points 0 and points_per_lvl 0), so wiping the
    // completions without also taking the points back would leave the player able to keep a fully
    // allocated tree while re-earning every node again. A blank AtlasData covers node unlocks,
    // completions and the pinnacle flag in one go - it re-unlocks the starting_node entries lazily
    // on first read, exactly like a brand new player's.
    ATLAS() {
        @Override
        public void reset(Player p) {
            Load.player(p).atlas = new AtlasData();
            PlayerPointsType.ATLAS.fullReset(p); // un-allocate the atlas passive tree
            Load.player(p).points.get(PlayerPointsType.ATLAS).resetBonusPoints(); // take back the points it granted
        }
    };

    public abstract void reset(Player p);
}
