package com.robertx22.mine_and_slash.aoe_data.database.spells.impl;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.spells.PartBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellCalcs;
import com.robertx22.mine_and_slash.database.data.spells.TestSpell;
import com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

import java.util.Arrays;

public class TestSpells implements ExileRegistryInit {

    @Override
    public void registerAll() {

        TestSpell.get()
                .addToSerializables(MMORPG.SERIAZABLE_REGISTRATION_INFO);


        SpellBuilder.of("test_self_damage_spell", PlayStyle.INT, SpellConfiguration.Builder.instant(7, 20)
                                .setSwingArm()
                                .applyCastSpeedToCooldown(), "self damage Ball",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.SELF_DAMAGE))
                .weaponReq(CastingWeapon.MAGE_WEAPON)

                .onCast(PartBuilder.damage(SpellCalcs.FIREBALL, Elements.Fire).addTarget(TargetSelector.CASTER.create()))
                .onCast(PartBuilder.playSound(SoundEvents.BLAZE_SHOOT, 1D, 0.6D))


                .weight(0).build();


        SpellBuilder.of("test_command", PlayStyle.INT, SpellConfiguration.Builder.instant(7, 15)
                                .setSwingArm(), "Test command",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage))
                .weight(0)

                .weaponReq(CastingWeapon.MAGE_WEAPON)

                .onCast(PartBuilder.playSound(SoundEvents.BLAZE_SHOOT, 1D, 0.6D))

                .onCast(PartBuilder.justAction(SpellAction.DEAL_DAMAGE.create(SpellCalcs.FIREBALL, Elements.Fire)
                        .put(MapField.ALLOW_SELF_DAMAGE, true)
                        .put(MapField.DISABLE_KNOCKBACK, true)
                ).addTarget(TargetSelector.CASTER.create()))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.SMOKE, 1D, 0.1D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ASH, 1D, 0.3D))


                .build();

    }
}
