package com.robertx22.mine_and_slash.database.data.stats.types.loot;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class SkillGemFind extends Stat {

    private SkillGemFind() {
        this.group = StatGroup.Misc;
        this.icon = "♣";
        this.format = ChatFormatting.YELLOW.getName();
    }

    public static SkillGemFind getInstance() {
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
        return "Increases chance to find Support and Augment Gems.";
    }

    @Override
    public String GUID() {
        return "skill_gem_find";
    }

    @Override
    public String locNameForLangFile() {
        return "Skill Gem Find";
    }

    private static class SingletonHolder {
        private static final SkillGemFind INSTANCE = new SkillGemFind();
    }
}
