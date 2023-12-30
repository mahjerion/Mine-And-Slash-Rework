package com.robertx22.age_of_exile.characters;

import com.robertx22.age_of_exile.capability.player.data.RestedExpData;
import com.robertx22.age_of_exile.capability.player.data.StatPointsData;
import com.robertx22.age_of_exile.database.data.spell_school.SpellSchool;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.perks.TalentsData;
import com.robertx22.age_of_exile.saveclasses.spells.SpellSchoolsData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


// todo have to make sure loading and saving works perfectly
// lvl has to save always, the rest can break over big updates like talents
public class CharacterData {

    public String name;
    public int lvl = 1;
    int xp = 0;

    private HashMap<Integer, String> hotbar = new HashMap<>();

    RestedExpData rested = new RestedExpData();
    TalentsData talents = new TalentsData();
    StatPointsData stats = new StatPointsData();
    SpellSchoolsData player_class = new SpellSchoolsData();


    public static CharacterData from(Player p) {
        var data = Load.player(p);
        var unit = Load.Unit(p);

        CharacterData d = new CharacterData();
        d.lvl = unit.getLevel();
        d.xp = unit.getExp();
        d.name = data.name;

        d.hotbar = data.spellCastingData.hotbar;

        d.rested = data.rested_xp;
        d.player_class = data.ascClass;
        d.stats = data.statPoints;
        d.talents = data.talents;


        return d;

    }

    public void load(Player p) {

        var data = Load.player(p);
        var unit = Load.Unit(p);

        data.talents = this.talents;
        data.statPoints = this.stats;
        data.rested_xp = this.rested;
        data.ascClass = this.player_class;

        data.spellCastingData.hotbar = this.hotbar;

        data.name = this.name;

        unit.setLevel(this.lvl);
        unit.setExp(this.xp);

    }

    public List<Component> getTooltip() {
        List<Component> list = new ArrayList<>();

        list.add(Words.Level.locName().append(" " + this.lvl).append(Component.literal(name)));

        // todo add other stuff

        return list;
    }

    public List<SpellSchool> getClasses() {
        return this.player_class.school.stream().map(x -> ExileDB.SpellSchools().get(x)).collect(Collectors.toList());
    }

}
