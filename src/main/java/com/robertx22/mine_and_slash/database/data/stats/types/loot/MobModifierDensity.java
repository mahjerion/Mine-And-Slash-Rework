package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class MobModifierDensity extends Stat {

    private MobModifierDensity() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static MobModifierDensity getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public boolean IsPercent() {
        return true;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public String locDescForLangFile() {
        return "Increases the chance for mobs to spawn with rare modifiers.";
    }

    @Override
    public String GUID() {
        return "mob_modifier_density";
    }

    @Override
    public String locNameForLangFile() {
        return "Mob Rarity";
    }

    private static class SingletonHolder {
        private static final MobModifierDensity INSTANCE = new MobModifierDensity();
    }
}
