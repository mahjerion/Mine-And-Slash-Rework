package com.robertx22.mine_and_slash.database.data.stats.types.summon;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

/**
 * Bonus chance for a golem to cast the spells its summon skill lists.
 * <p>
 * A plain value stat with no {@code IStatEffect} of its own. It used to carry one - a
 * {@code GolemSpellEffect} that watched the owner's damage events for a {@code GolemSummon} and did
 * the cast itself off a hard-coded {@code aoeSpell()} - but the trigger now lives in one place, in
 * {@code SummonSpellCaster}, which reads this value alongside the datapack authored base chance.
 * <p>
 * The GUID, name, description and every unique, perk and implicit granting it are deliberately
 * unchanged: only the plumbing moved, so no save and no datapack sees a difference.
 */
public class GolemSpellChance extends Stat {

    private GolemSpellChance() {
        this.format = ChatFormatting.AQUA.getName();
    }

    public static GolemSpellChance getInstance() {
        return GolemSpellChance.SingletonHolder.INSTANCE;
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
        return "Bonus Chance for golems to cast their AOE nova spells";
    }

    @Override
    public String GUID() {
        return "golem_spell_chance";
    }

    @Override
    public String locNameForLangFile() {
        return "Golem Spell Chance";
    }

    private static class SingletonHolder {
        private static final GolemSpellChance INSTANCE = new GolemSpellChance();
    }
}
