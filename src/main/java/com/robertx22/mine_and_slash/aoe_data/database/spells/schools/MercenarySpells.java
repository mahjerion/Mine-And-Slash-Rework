package com.robertx22.mine_and_slash.aoe_data.database.spells.schools;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.a_libraries.player_animations.AnimationHolder;
import com.robertx22.mine_and_slash.a_libraries.player_animations.SpellAnimations;
import com.robertx22.mine_and_slash.aoe_data.database.exile_effects.adders.ModEffects;
import com.robertx22.mine_and_slash.aoe_data.database.spells.PartBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellCalcs;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SummonType;
import com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.AggroAction;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.ExileEffectAction;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.PositionSource;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.components.conditions.EffectCondition;
import com.robertx22.mine_and_slash.database.data.spells.components.selectors.TargetSelector;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

/**
 * Spells only mercenaries cast.
 * <p>
 * They are separate entries rather than reuses of the player versions for two reasons:
 * every mercenary skill is capped at level 1, so they need {@code defaultAndMaxLevel(1)} -
 * {@link com.robertx22.mine_and_slash.database.data.spells.components.Spell#getLevelOf} returns
 * {@code default_lvl} for any non player, which is how a mercenary gets a usable rank with no
 * rank storage of its own; and they cost nothing, because mercenaries have no mana or energy.
 * <p>
 * {@code weaponReq} is real here and worth setting: {@code CastingWeapon}'s predicates are typed to
 * LivingEntity, and {@code MercenarySpellCaster.hasCastingWeapon} enforces them, so a mercenary
 * holding the wrong weapon skips the skill the same way a player is refused the cast.
 * <p>
 * Charge skills are paced by {@code charge_regen} rather than a real charge pool - see
 * {@code MercenarySpellCaster.cooldownTicksFor}. A mercenary has no {@code ChargeData}, so authoring
 * a burst of charges here gives a steady one-cast-per-regen instead.
 * <p>
 * {@code weight(0)} keeps them out of skill gem drops - they are learned by levelling, never found.
 */
public class MercenarySpells implements ExileRegistryInit {

    public static String MERC_GONG_STRIKE = "merc_gong_strike";
    public static String MERC_PULL = "merc_pull";
    public static String MERC_TAUNT = "merc_taunt";
    public static String MERC_FIREBALL = "merc_fireball";
    public static String MERC_METEOR = "merc_meteor";
    public static String MERC_MAGE_CIRCLE = "merc_mage_circle";
    public static String MERC_FROST_NOVA = "merc_frost_nova";

    public static String MERC_ARROW_BARRAGE = "merc_arrow_barrage";
    public static String MERC_FIRE_TRAP = "merc_fire_trap";
    public static String MERC_SUMMON_WOLF = "merc_summon_wolf";
    public static String MERC_WOLF_BASIC = "merc_wolf_basic";

    @Override
    public void registerAll() {

        SpellBuilder.of(MERC_GONG_STRIKE, PlayStyle.STR, SpellConfiguration.Builder.instant(0, 20 * 8)
                                .setSwingArm(), "Gong Strike",
                        Arrays.asList(SpellTags.weapon_skill, SpellTags.area, SpellTags.damage, SpellTags.PHYSICAL))
                .manualDesc("Your mercenary bashes enemies in front of it for " +
                        SpellCalcs.GONG_STRIKE.getLocDmgTooltip(Elements.Physical) + ", stunning them.")

                .onCast(PartBuilder.playSound(SoundEvents.ANVIL_PLACE, 1D, 1D))
                .onCast(PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 1D, 1D))

                .onCast(PartBuilder.damageInFront(SpellCalcs.GONG_STRIKE, Elements.Physical, 2D, 3D))
                .onCast(PartBuilder.addExileEffectToEnemiesInFront(ModEffects.STUN.resourcePath, 2D, 2D, 20D * 3))

                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 300D, 2D, 0.1D))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.EXPLOSION, 5D, 2D, 0.1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        SpellBuilder.of(MERC_PULL, PlayStyle.STR, SpellConfiguration.Builder.instant(0, 20 * 20), "Pull",
                        Arrays.asList(SpellTags.weapon_skill, SpellTags.area, SpellTags.damage, SpellTags.PHYSICAL))
                .manualDesc("Your mercenary pulls nearby enemies to it, dealing " +
                        SpellCalcs.PULL.getLocDmgTooltip() + " " +
                        Elements.Physical.getIconNameDmg() + " and slowing them.")

                .onCast(PartBuilder.playSound(SoundEvents.ANVIL_HIT, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.TP_TARGET_TO_SELF.create())
                        .addActions(SpellAction.POTION.createGive(MobEffects.MOVEMENT_SLOWDOWN, 20D * 5))
                        .addActions(SpellAction.DEAL_DAMAGE.create(SpellCalcs.PULL, Elements.Physical))
                        .addActions(SpellAction.EXILE_EFFECT.create(ModEffects.STUN.resourcePath, ExileEffectAction.GiveOrTake.GIVE_STACKS, 20D * 2))
                        .addTarget(TargetSelector.AOE.create(8D, EntityFinder.SelectionType.RADIUS, AllyOrEnemy.enemies)))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.CRIT, 100D, 6D, 0.1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        SpellBuilder.of(MERC_TAUNT, PlayStyle.STR, SpellConfiguration.Builder.instant(0, 20 * 10)
                                .setSwingArm(), "Taunt",
                        Arrays.asList(SpellTags.area, SpellTags.BUFF))
                .manualDesc(
                        "Shout, making nearby enemies want to attack you. " +
                                "Generates " + SpellCalcs.TAUNT.getLocDmgTooltip() + " threat."
                )
                .animations(AnimationHolder.none(), SpellAnimations.TAUNT)
                .weaponReq(CastingWeapon.MELEE_WEAPON)
                .onCast(PartBuilder.playSound(SoundEvents.SHIELD_BLOCK, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.AGGRO.create(SpellCalcs.TAUNT, AggroAction.Type.AGGRO))
                        .addTarget(TargetSelector.AOE.create(3D, EntityFinder.SelectionType.RADIUS, AllyOrEnemy.enemies)))
                .onCast(PartBuilder.aoeParticles(ParticleTypes.CLOUD, 20D, 3D))
                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        SpellBuilder.of(MERC_FIREBALL, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20 * 6)
                                .setSwingArm(), "Fire Ball",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.FIRE))
                .manualDesc(
                        "Throw out a ball of fire, dealing " + SpellCalcs.FIREBALL.getLocDmgTooltip()
                                + " " + Elements.Fire.getIconNameDmg())
                .weaponReq(CastingWeapon.MAGE_WEAPON)

                .onCast(PartBuilder.playSound(SoundEvents.BLAZE_SHOOT, 1D, 0.6D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.FIREBALL.get(), 1D, 2.5D, SlashEntities.SIMPLE_PROJECTILE.get(), 8D, false)
                ))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.FLAME, 1D, 0.1D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.FALLING_LAVA, 1D, 0.5D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.SMOKE, 1D, 0.01D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.FIREBALL, Elements.Fire, 1.5D).noKnock())
                .onExpire(PartBuilder.playSound(SoundEvents.GENERIC_BURN, 1D, 2D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.SMOKE, 3D, 1D))
                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();


        SpellBuilder.of(MERC_METEOR, PlayStyle.INT, SpellConfiguration.Builder.nonInstant(0, 20 * 10, 20)
                                .setChargesAndRegen(MERC_METEOR, 3, 20 * 20), "Meteor",
                        Arrays.asList(SpellTags.area, SpellTags.damage, SpellTags.FIRE)
                )
                .animations(SpellAnimations.HAND_UP_CAST, SpellAnimations.CAST_FINISH)

                .manualDesc("Summon a meteor that falls from the sky, dealing " +
                        SpellCalcs.METEOR.getLocDmgTooltip(Elements.Fire) + " in an area.")

                .weaponReq(CastingWeapon.MAGE_WEAPON)
                .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 1D))
                // lands on the enemy with no POS_SOURCE of its own: SummonAtSightAction defaults a
                // non player caster to its target, since a mercenary has no crosshair to aim by.
                // stating it here would have worked too, but only for this entry - the modpack
                // ships its own mercenary spell set that overrides this file, and those want the
                // same thing without having to declare it.
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 7D)))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(Blocks.MAGMA_BLOCK, 200D)
                        .put(MapField.ENTITY_NAME, "block")
                        .put(MapField.FIND_NEAREST_SURFACE, false)
                        .put(MapField.BLOCK_FALL_SPEED, -0.03D)
                        .put(MapField.IS_BLOCK_FALLING, true)))
                .onTick("block", PartBuilder.particleOnTick(2D, ParticleTypes.LAVA, 2D, 0.5D))
                .onExpire("block", PartBuilder.damageInAoe(SpellCalcs.METEOR, Elements.Fire, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.LAVA, 150D, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.ASH, 25D, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.EXPLOSION, 15D, 3D))
                .onExpire("block", PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 1D, 1D))
                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();


        SpellBuilder.of(MERC_FROST_NOVA, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20 * 8), "Frost Nova",
                        Arrays.asList(SpellTags.area, SpellTags.damage, SpellTags.COLD))
                .manualDesc(
                        "Explode with frost around you, dealing " + SpellCalcs.FROST_NOVA.getLocDmgTooltip()
                                + " " + Elements.Cold.getIconNameDmg() + " to nearby enemies.")

                .weaponReq(CastingWeapon.ANY_WEAPON)
                .onCast(PartBuilder.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1D, 1D))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.ITEM_SNOWBALL, 200D, 4D, 0.5D))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.ITEM_SNOWBALL, 200D, 3.5D, 0.5D))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.SNOWFLAKE, 200D, 2.5D, 0.5D))
                .onCast(PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 0.8D, 1.5D))
                .onCast(PartBuilder.damageInAoe(SpellCalcs.FROST_NOVA, Elements.Cold, 5D)
                        .addPerEntityHit(PartBuilder.playSoundPerTarget(SoundEvents.GENERIC_HURT, 1D, 1D)))
                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();


        SpellBuilder.of(MERC_MAGE_CIRCLE, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20 * 20)
                        , "Mage Circle", Arrays.asList(SpellTags.movement))

                .manualDesc(
                        "Summon a Magic Circle. Standing in it increases your damage." +
                                " After a certain duration you will be teleported to its location.")

                .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 1D))
                // pinned to the caster, and still worth stating outright: this circle buffs whoever
                // stands in it and teleports the caster to it ten seconds later, so dropped on the
                // enemy it would drag a kiting elementalist into melee. redundant with the caster
                // default a mercenary now casts with, but it is the one skill where being wrong
                // here is a movement bug rather than a cosmetic one.
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 0D)
                        .put(MapField.POS_SOURCE, PositionSource.CASTER.name())))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(SlashBlocks.GLYPH.get(), 20D * 10)
                        .put(MapField.ENTITY_NAME, "block")
                        .put(MapField.BLOCK_FALL_SPEED, 0D)
                        .put(MapField.FIND_NEAREST_SURFACE, false)
                        .put(MapField.IS_BLOCK_FALLING, false)))

                .onExpire("block", PartBuilder.justAction(SpellAction.TP_TARGET_TO_SELF.create())
                        .addTarget(TargetSelector.CASTER.create()))
                .onExpire("block", PartBuilder.playSound(SoundEvents.ENDERMAN_TELEPORT, 1D, 1D))

                .onTick("block", PartBuilder.giveSelfExileEffect(ModEffects.MAGE_CIRCLE, 20D)
                        .addCondition(EffectCondition.IS_ENTITY_IN_RADIUS.alliesInRadius(2D)))

                .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.WITCH, 3D, 1.2D, 0.5D)
                        .addCondition(EffectCondition.EVERY_X_TICKS.create(3D)))
                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        // ------------------------------------------------------------------ hunter
        //
        // every hunter skill requires a ranged weapon, traps and the summon included. that gate is
        // real - MercenarySpellCaster.hasCastingWeapon enforces it per cast, and REQUIRE_SHOOTABLE
        // wants an actual MnS gear item with the ranged_weapon slot tag, not merely a vanilla bow -
        // so a hunter with nothing equipped casts nothing at all. deliberate, not an oversight.

        // authored as a multicast rather than the player version's channel: a channel's
        // cast_time_ticks is the gap between pulses and needs an input held down, which a mercenary
        // has no equivalent of, so MercenarySpellCaster.castTimeTicksFor treats one as instant and a
        // channelled barrage would fire exactly one arrow. times_to_cast is the mechanism its
        // tickCast actually paces.
        SpellBuilder.of(MERC_ARROW_BARRAGE, PlayStyle.DEX, SpellConfiguration.Builder.multiCast(0, 20 * 8, 20, 6), "Arrow Barrage",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.PHYSICAL))
                .manualDesc("Your mercenary shoots out arrows in rapid succession, each dealing " +
                        SpellCalcs.ARROW_STORM.getLocDmgTooltip(Elements.Physical))
                .weaponReq(CastingWeapon.RANGED)
                .animations(SpellAnimations.SHOOT_ARROW_FAST, SpellAnimations.CAST_FINISH)

                .onCast(PartBuilder.playSound(SoundEvents.ARROW_SHOOT, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.createArrow(1D)))
                .onHit(PartBuilder.particleOnTick(3D, ParticleTypes.CLOUD, 3D, 0.1D))
                .onHit(PartBuilder.playSound(SoundEvents.ARROW_HIT, 1D, 1D))
                .onHit(PartBuilder.damage(SpellCalcs.ARROW_STORM, Elements.Physical))
                .onTick(PartBuilder.particleOnTick(5D, ParticleTypes.CRIT, 5D, 0.1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        // the player trap throws a 100 tick projectile. MercenarySpellCaster.computeReach sizes
        // engagement as LIFESPAN_TICKS * PROJECTILE_SPEED * ENGAGE_FACTOR, so lifting that number
        // unchanged would have the mercenary lobbing traps from ~40 blocks out. 20 ticks puts it at
        // a sane ~8. a plain cooldown rather than charges, too: a mercenary has no ChargeData, so
        // authoring charges here only yields one cast per regen anyway.
        SpellBuilder.of(MERC_FIRE_TRAP, PlayStyle.DEX, SpellConfiguration.Builder.instant(0, 20 * 10)
                                .setSwingArm(), "Fire Trap",
                        Arrays.asList(SpellTags.damage, SpellTags.area, SpellTags.trap, SpellTags.FIRE))
                .manualDesc("Your mercenary throws out a trap that stays on the ground and activates when an enemy approaches, dealing "
                        + SpellCalcs.RANGER_TRAP.getLocDmgTooltip() + " " + Elements.Fire.getIconNameDmg() + " in an area around itself.")
                .weaponReq(CastingWeapon.RANGED)

                .onCast(PartBuilder.playSound(SoundEvents.SNOWBALL_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(Items.IRON_INGOT, 1D, 0.5D, SlashEntities.SIMPLE_PROJECTILE.get(), 20D, true)))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(SlashBlocks.TRAP.get(), 30 * 20D)
                        .put(MapField.ENTITY_NAME, "trap")
                        .put(MapField.FIND_NEAREST_SURFACE, true)
                        .put(MapField.IS_BLOCK_FALLING, false)))

                .onTick("trap", PartBuilder.aoeParticles(ParticleTypes.FLAME, 5D, 1D)
                        .addCondition(EffectCondition.IS_ENTITY_IN_RADIUS.enemiesInRadius(1D))
                        .addActions(SpellAction.EXPIRE.create())
                        .addActions(SpellAction.SPECIFIC_ACTION.create("explode"))
                        .tick(2D))

                .addSpecificAction("explode", PartBuilder.damageInAoe(SpellCalcs.RANGER_TRAP, Elements.Fire, 3D))
                .addSpecificAction("explode", PartBuilder.aoeParticles(ParticleTypes.FLAME, 30D, 3D))
                .addSpecificAction("explode", PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 1D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        // BEAST rather than NONE on purpose: the summon type is what the beast damage stats are
        // conditioned on (EventData.SUMMON_TYPE), so NONE would quietly cut the wolf off from a
        // whole stat category. it stays out of the owner's cap by not counting towards it, which is
        // the field that exists for exactly that - see SummonPetAction.updatePlayerSummons.
        SpellBuilder.of(MERC_SUMMON_WOLF, PlayStyle.DEX, SpellConfiguration.Builder.instant(0, 20 * 30)
                                .setSummonBasicAttack(MERC_WOLF_BASIC)
                                .setSummonType(SummonType.BEAST)
                                .setSummonAggroRadius(15), "Summon Wolf",
                        Arrays.asList(SpellTags.summon, SpellTags.damage, SpellTags.beast, SpellTags.has_pet_ability, SpellTags.PHYSICAL))
                .manualDesc("Your mercenary summons a Spirit Wolf to fight alongside it.")
                .weaponReq(CastingWeapon.RANGED)

                .summons(SlashEntities.SPIRIT_WOLF.get(), 20 * 30, 1, SummonType.BEAST, false)

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();

        // the wolf's bite. borrows the summon skill's support gems so a support socketed under
        // Summon Wolf reaches it - MercenaryData.getSpellUnit follows that redirect.
        SpellBuilder.of(MERC_WOLF_BASIC, PlayStyle.DEX, SpellConfiguration.Builder.instant(0, 1)
                                .setUsesSupportGemsFrom(MERC_SUMMON_WOLF), "Wolf Attack",
                        Arrays.asList(SpellTags.summon, SpellTags.damage, SpellTags.beast, SpellTags.PHYSICAL))
                .manualDesc("Attack dealing " + SpellCalcs.PET_BASIC.getLocDmgTooltip() + " "
                        + Elements.Physical.getIconNameDmg() + " to a single enemy.")
                .weaponReq(CastingWeapon.ANY_WEAPON)
                .onHit(PartBuilder.damage(SpellCalcs.PET_BASIC, Elements.Physical))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .build();
    }
}
