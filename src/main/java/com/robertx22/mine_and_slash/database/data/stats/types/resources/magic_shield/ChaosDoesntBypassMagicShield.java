package com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.name_regex.StatNameRegex;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class ChaosDoesntBypassMagicShield extends Stat {
    public static String GUID = "chaos_doesnt_bypass_magic_shield";

    private ChaosDoesntBypassMagicShield() {
        this.min = 0;
        this.scaling = StatScaling.NORMAL;
        this.group = StatGroup.Misc;
        this.is_long = true;
    }

    @Override
    public StatNameRegex getStatNameRegex() {
        return StatNameRegex.JUST_NAME;
    }

    @Override
    public String locDescForLangFile() {
        return "Normally " + (int) MagicShield.CHAOS_BYPASS_PERCENT + "% of Chaos Damage ignores Magic Shield and is dealt straight to Health. Poison counts as Chaos Damage.";
    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locNameForLangFile() {
        return ChatFormatting.GRAY + Elements.Shadow.getIconNameDmg() + " taken no longer bypasses " + MagicShield.getInstance()
                .getIconNameFormat();
    }

    public static ChaosDoesntBypassMagicShield getInstance() {
        return ChaosDoesntBypassMagicShield.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final ChaosDoesntBypassMagicShield INSTANCE = new ChaosDoesntBypassMagicShield();
    }
}
