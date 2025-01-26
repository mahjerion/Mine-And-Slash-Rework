package com.robertx22.mine_and_slash.database.data.spells;

import com.robertx22.mine_and_slash.aoe_data.database.spells.PartBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellCalcs;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import net.minecraft.sounds.SoundEvents;

import java.util.Arrays;

public class TestSpell {

    public static String USE_THIS_EXACT_ID = "test_spell"; // USE THE EXACT "ID" HERE OR THE TEST SPELL GEM WONT WORK

    public static Spell get() {
        return
                SpellBuilder.of(USE_THIS_EXACT_ID, PlayStyle.INT, SpellConfiguration.Builder.instant(7, 20)
                                        .setSwingArm()
                                        .applyCastSpeedToCooldown(), "Poison Ball",
                                Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.SELF_DAMAGE))
                        .weaponReq(CastingWeapon.MAGE_WEAPON)

                        .onCast(PartBuilder.damage(SpellCalcs.FIREBALL, Elements.Fire).addTarget(TargetSelector.CASTER.create()))
                        .onCast(PartBuilder.playSound(SoundEvents.BLAZE_SHOOT, 1D, 0.6D))

                        
                        .weight(0)
                        .buildForEffect();

    }
}
