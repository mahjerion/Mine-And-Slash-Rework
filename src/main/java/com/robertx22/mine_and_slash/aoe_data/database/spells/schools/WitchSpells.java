package com.robertx22.mine_and_slash.aoe_data.database.spells.schools;

import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.mine_and_slash.aoe_data.database.spells.PartBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.SpellCalcs;
import com.robertx22.mine_and_slash.aoe_data.database.spells.builders.DamageBuilder;
import com.robertx22.mine_and_slash.aoe_data.database.spells.builders.ParticleBuilder;
import com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SpellAction;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.tags.all.SpellTags;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.PlayStyle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

/**
 * Skills only the wizard monsters cast - see {@link com.robertx22.mine_and_slash.database.data.wizard.WizardType}.
 * <p>
 * Separate entries rather than reuses of the player versions, for the reasons
 * {@link MercenarySpells} documents plus two of their own:
 * <ul>
 * <li>{@code defaultAndMaxLevel(1)}, because {@code Spell.getLevelOf} returns {@code default_lvl}
 * for anything that isn't a Player, and a wizard has nowhere to store a rank. Every value calc
 * these reference is pinned min == max to match - a level 1 skill reading a min..max range would
 * come out at about a ninth of it.</li>
 * <li><b>No exile effects.</b> A player's Ice Shard applies Bone Chill, Chilling Field slows, and
 * the pack's lightning skills stack Static - none of that belongs on a monster. Bone Chill and
 * Static are player-facing mechanics with their own screens and interactions, and handing a mob a
 * skill that manipulates them makes it a source of things the player's own build reacts to. These
 * versions deal damage and nothing else.</li>
 * </ul>
 * Costs are zero - a monster has no mana - and {@code weight(0)} plus {@code hideFromWiki()} keeps
 * them out of gem drops and out of the wiki: they are not player content.
 * <p>
 * <b>Range is authored, not declared.</b> {@code WizardSpellCaster.computeReach} works out how far
 * each skill reaches from its own components - {@code life_ticks * proj_speed} for a projectile,
 * the radius for an aoe - and the wizard only casts what currently reaches its target. So the
 * numbers below ARE the engagement distances, and a projectile lifespan copied straight from the
 * player version would have the mob throwing from across the map. Frozen Orb is the clearest case:
 * the player's drifts for ten seconds, which would read as a hundred block reach.
 */
public class WitchSpells implements ExileRegistryInit {

    public static String WITCH_FIREBALL = "witch_fireball";
    public static String WITCH_FIRE_NOVA = "witch_fire_nova";
    public static String WITCH_METEOR = "witch_meteor";

    public static String WITCH_FROSTBALL = "witch_frostball";
    public static String WITCH_FROZEN_ORB = "witch_frozen_orb";
    public static String WITCH_CHILLING_FIELD = "witch_chilling_field";

    public static String WITCH_LIGHTNING_SPEAR = "witch_lightning_spear";
    public static String WITCH_CHAIN_LIGHTNING = "witch_chain_lightning";
    public static String WITCH_LIGHTNING_TOTEM = "witch_lightning_totem";

    public static String WITCH_POISON_BALL = "witch_poison_ball";

    /** radius the single target projectiles detonate in, same as the player versions */
    private static final double BALL_RADIUS = 1.5D;
    /** every wizard projectile flies at this speed for this long: 2.5 * 8 = 20 blocks of reach */
    private static final double BALL_SPEED = 2.5D;
    private static final double BALL_LIFE = 8D;

    @Override
    public void registerAll() {

        // ------------------------------------------------------------------ fire

        SpellBuilder.of(WITCH_FIREBALL, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20)
                                .setSwingArm(), "Fire Ball",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.FIRE))
                .manualDesc("Throws a ball of fire, dealing " + SpellCalcs.WITCH_FIREBALL.getLocDmgTooltip()
                        + " " + Elements.Fire.getIconNameDmg())

                .onCast(PartBuilder.playSound(SoundEvents.BLAZE_SHOOT, 1D, 0.6D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.FIREBALL.get(), 1D, BALL_SPEED, SlashEntities.SIMPLE_PROJECTILE.get(), BALL_LIFE, false)))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.FLAME, 1D, 0.1D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.SMOKE, 1D, 0.01D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.WITCH_FIREBALL, Elements.Fire, BALL_RADIUS).noKnock())
                .onExpire(PartBuilder.playSound(SoundEvents.GENERIC_BURN, 1D, 2D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.SMOKE, 3D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // the wizard's only close range option. the 5 block radius is what puts it out of reach until
        // the player closes, which is the whole point - see the class note on range.
        SpellBuilder.of(WITCH_FIRE_NOVA, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20 * 6), "Fire Nova",
                        Arrays.asList(SpellTags.area, SpellTags.damage, SpellTags.FIRE))
                .manualDesc("Engulfs the area in flames, dealing " + SpellCalcs.WITCH_FIRE_NOVA.getLocDmgTooltip()
                        + " " + Elements.Fire.getIconNameDmg() + " to nearby enemies.")

                .onCast(PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 1D, 1D))
                .onCast(PartBuilder.nova(ParticleTypes.FLAME, 200D, 5D, 0.05D))
                .onCast(PartBuilder.nova(ParticleTypes.FLAME, 100D, 3.5D, 0.05D))
                .onCast(PartBuilder.nova(ParticleTypes.FLAME, 100D, 1.5D, 0.05D))
                .onCast(PartBuilder.nova(ParticleTypes.SMOKE, 200D, 1D, 0.05D))
                .onCast(PartBuilder.groundEdgeParticles(ParticleTypes.EXPLOSION, 1D, 0D, 0.2D))
                .onCast(PartBuilder.damageInAoe(SpellCalcs.WITCH_FIRE_NOVA, Elements.Fire, 5D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // SUMMON_AT_SIGHT drops onto the target's own position for a non player caster, so this has
        // no distance to close and the wizard can throw it from wherever it is standing. That is what
        // makes it the skill it opens with, and the reason a wizard is dangerous before you reach it.
        SpellBuilder.of(WITCH_METEOR, PlayStyle.INT, SpellConfiguration.Builder.nonInstant(0, 20 * 8, 20), "Meteor",
                        Arrays.asList(SpellTags.area, SpellTags.damage, SpellTags.FIRE))
                .manualDesc("Summons a meteor that falls from the sky, dealing " +
                        SpellCalcs.WITCH_METEOR.getLocDmgTooltip(Elements.Fire) + " in an area.")

                .onCast(PartBuilder.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 7D)))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(Blocks.MAGMA_BLOCK, 200D)
                        .put(MapField.ENTITY_NAME, "block")
                        .put(MapField.FIND_NEAREST_SURFACE, false)
                        .put(MapField.BLOCK_FALL_SPEED, -0.03D)
                        .put(MapField.IS_BLOCK_FALLING, true)))
                .onTick("block", PartBuilder.particleOnTick(2D, ParticleTypes.LAVA, 2D, 0.5D))
                .onExpire("block", PartBuilder.damageInAoe(SpellCalcs.WITCH_METEOR, Elements.Fire, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.LAVA, 150D, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.ASH, 25D, 3D))
                .onExpire("block", PartBuilder.aoeParticles(ParticleTypes.EXPLOSION, 15D, 3D))
                .onExpire("block", PartBuilder.playSound(SoundEvents.GENERIC_EXPLODE, 1D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // ------------------------------------------------------------------ ice

        SpellBuilder.of(WITCH_FROSTBALL, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20)
                                .setSwingArm(), "Ice Shard",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.COLD))
                .manualDesc("Throws a shard of ice, dealing " + SpellCalcs.WITCH_FROSTBALL.getLocDmgTooltip()
                        + " " + Elements.Cold.getIconNameDmg())

                .onCast(PartBuilder.playSound(SoundEvents.SNOWBALL_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.SNOWBALL.get(), 1D, BALL_SPEED, SlashEntities.SIMPLE_PROJECTILE.get(), BALL_LIFE, false)))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ITEM_SNOWBALL, 2D, 0.15D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.SNOWFLAKE, 7D, 0.3D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.WITCH_FROSTBALL, Elements.Cold, BALL_RADIUS).noKnock())
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.ITEM_SNOWBALL, 5D, 1D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.SNOWFLAKE, 15D, 0.5D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // 1.5 speed for 5 seconds is 150 blocks of raw travel, which computeReach would read as the
        // range - so it is deliberately slow AND short lived: 0.75 * 60 = 45, and the orb still drifts
        // for three seconds pulsing on the way. It tracks, so it does not need to outrange anything.
        SpellBuilder.of(WITCH_FROZEN_ORB, PlayStyle.INT, SpellConfiguration.Builder.nonInstant(0, 20 * 10, 15)
                                .setSwingArm(), "Frozen Orb",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.area, SpellTags.COLD))
                .manualDesc("Throws an orb of ice which drifts towards enemies, dealing "
                        + SpellCalcs.WITCH_FROZEN_ORB.getLocDmgTooltip() + " "
                        + Elements.Cold.getIconNameDmg() + " in an area every second.")

                .onCast(PartBuilder.playSound(SoundEvents.SNOWBALL_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(Items.SNOWBALL, 1D, 0.5D, SlashEntities.SIMPLE_PROJECTILE.get(), 20 * 3D, false)
                        .put(MapField.TRACKS_ENEMIES, true)
                        .put(MapField.EXPIRE_ON_ENTITY_HIT, false)
                ))
                .onTick(ParticleBuilder.of(ParticleTypes.SNOWFLAKE, 0.15F).amount(2).tickReq(3).build())
                .onTick(ParticleBuilder.of(ParticleTypes.ITEM_SNOWBALL, 0.15F).amount(7).tickReq(3).build())
                .onTick(ParticleBuilder.of(ParticleTypes.SNOWFLAKE, 3F).amount(15).tickReq(3).build().tick(20D))
                .onTick(DamageBuilder.radius(Elements.Cold, 3, SpellCalcs.WITCH_FROZEN_ORB).build().noKnock().tick(20D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // an air block that ticks for its whole life. SUMMON_AT_SIGHT again, so it is dropped on the
        // player rather than under the wizard - a field the wizard stands in would only ever hit
        // something that had already reached it.
        SpellBuilder.of(WITCH_CHILLING_FIELD, PlayStyle.INT, SpellConfiguration.Builder.nonInstant(0, 20 * 12, 20)
                                .setSwingArm(), "Chilling Field",
                        Arrays.asList(SpellTags.damage, SpellTags.area, SpellTags.COLD))
                .manualDesc("Spawns a cloud of frost, dealing "
                        + SpellCalcs.WITCH_CHILLING_FIELD.getLocDmgTooltip() + " "
                        + Elements.Cold.getIconNameDmg() + " every second.")

                .onCast(PartBuilder.playSound(SoundEvents.END_PORTAL_SPAWN, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 0D)))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(Blocks.AIR, 20D * 6)
                        .put(MapField.ENTITY_NAME, "block")
                        .put(MapField.BLOCK_FALL_SPEED, 0D)
                        .put(MapField.FIND_NEAREST_SURFACE, true)
                        .put(MapField.IS_BLOCK_FALLING, false)))

                .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.CLOUD, 5D, 3D, 0.2D))
                .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.SNOWFLAKE, 30D, 2.5D, 0.2D))
                .onTick("block", PartBuilder.playSound(SoundEvents.HORSE_BREATHE, 0.8D, 1.5D).tick(20D))
                .onTick("block", PartBuilder.damageInAoe(SpellCalcs.WITCH_CHILLING_FIELD, Elements.Cold, 4D)
                        .noKnock()
                        .tick(20D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // ------------------------------------------------------------------ lightning

        SpellBuilder.of(WITCH_LIGHTNING_SPEAR, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20)
                                .setSwingArm(), "Lightning Spear",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.LIGHTNING))
                .manualDesc("Throws an electric spear, dealing "
                        + SpellCalcs.WITCH_LIGHTNING_SPEAR.getLocDmgTooltip()
                        + " " + Elements.Nature.getIconNameDmg())

                .onCast(PartBuilder.playSound(SoundEvents.TRIDENT_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.SLIMEBALL.get(), 1D, 3D, SlashEntities.SIMPLE_TRIDENT.get(), 7D, true)))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ELECTRIC_SPARK, 1D, 0.15D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ELECTRIC_SPARK, 10D, 0.2D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.WITCH_LIGHTNING_SPEAR, Elements.Nature, 2D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.ELECTRIC_SPARK, 100D, 2D))
                .onExpire(PartBuilder.playSound(SoundEvents.TRIDENT_HIT, 1D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // chains, so it punishes a party standing together - the one wizard skill that is not simply
        // damage aimed at one person. No Static: see the class note.
        SpellBuilder.of(WITCH_CHAIN_LIGHTNING, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20), "Chain Lightning",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.chaining, SpellTags.LIGHTNING))
                .manualDesc("Strikes enemies with chaining lightning that deals "
                        + SpellCalcs.WITCH_CHAIN_LIGHTNING.getLocDmgTooltip()
                        + " " + Elements.Nature.getIconNameDmg())

                .onCast(PartBuilder.playSound(SoundEvents.ALLAY_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.LIGHTNING.get(), 1D, 1.5D, SlashEntities.SIMPLE_PROJECTILE.get(), 12D, true)
                        .put(MapField.CHAIN_COUNT, 3D)
                        .put(MapField.GRAVITY, false)
                ))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ELECTRIC_SPARK, 10D, 0.01D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.WITCH_CHAIN_LIGHTNING, Elements.Nature, 1D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.ELECTRIC_SPARK, 50D, 0.5D))
                .onExpire(PartBuilder.playSound(SoundEvents.TRIDENT_HIT, 1D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // no SUMMON_LIMIT_GROUP: that whole path is only applied to a Player caster, and a wizard
        // that dies leaves the totem behind to expire on its own lifespan - harmless, it stops
        // dealing damage the moment its caster is gone.
        SpellBuilder.of(WITCH_LIGHTNING_TOTEM, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20 * 15)
                                .setSwingArm(), "Lightning Totem",
                        Arrays.asList(SpellTags.damage, SpellTags.area, SpellTags.totem, SpellTags.LIGHTNING))
                .manualDesc("Summons a lightning totem that deals "
                        + SpellCalcs.WITCH_LIGHTNING_TOTEM.getLocDmgTooltip(Elements.Nature) + " in an area every second.")

                .onCast(PartBuilder.playSound(SoundEvents.GRASS_PLACE, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_AT_SIGHT.create(SlashEntities.SIMPLE_PROJECTILE.get(), 1D, 0D)))
                .onExpire(PartBuilder.justAction(SpellAction.SUMMON_BLOCK.create(SlashBlocks.GUARD_TOTEM.get(), 20D * 6)
                        .put(MapField.ENTITY_NAME, "block")
                        .put(MapField.BLOCK_FALL_SPEED, 0D)
                        .put(MapField.FIND_NEAREST_SURFACE, true)
                        .put(MapField.IS_BLOCK_FALLING, false)))

                .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.ELECTRIC_SPARK, 20D, 1.5D, 0.2D))
                .onTick("block", PartBuilder.groundEdgeParticles(ParticleTypes.POOF, 5D, 1.5D, 0.2D))
                .onTick("block", PartBuilder.playSound(SoundEvents.FIRE_EXTINGUISH, 0.75D, 1D).tick(20D))
                .onTick("block", PartBuilder.damageInAoe(SpellCalcs.WITCH_LIGHTNING_TOTEM, Elements.Nature, 5D)
                        .noKnock()
                        .tick(20D)
                        .addPerEntityHit(PartBuilder.groundEdgeParticles(ParticleTypes.ELECTRIC_SPARK, 75D, 1.5D, 0.1D)))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();

        // ------------------------------------------------------------------ chaos

        SpellBuilder.of(WITCH_POISON_BALL, PlayStyle.INT, SpellConfiguration.Builder.instant(0, 20)
                                .setSwingArm(), "Acid Blast",
                        Arrays.asList(SpellTags.projectile, SpellTags.damage, SpellTags.CHAOS))
                .manualDesc("Blasts out a ball of poison, dealing " + SpellCalcs.WITCH_POISON_BALL.getLocDmgTooltip()
                        + " " + Elements.Shadow.getIconNameDmg())

                .onCast(PartBuilder.playSound(SoundEvents.SNOWBALL_THROW, 1D, 1D))
                .onCast(PartBuilder.justAction(SpellAction.SUMMON_PROJECTILE.create(SlashItems.SLIMEBALL.get(), 1D, BALL_SPEED, SlashEntities.SIMPLE_PROJECTILE.get(), BALL_LIFE, false)))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.SNEEZE, 1D, 0.15D))
                .onTick(PartBuilder.particleOnTick(1D, ParticleTypes.ITEM_SLIME, 10D, 0.15D))
                .onExpire(PartBuilder.damageInAoe(SpellCalcs.WITCH_POISON_BALL, Elements.Shadow, BALL_RADIUS).noKnock())
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.ITEM_SLIME, 100D, 1D))
                .onExpire(PartBuilder.aoeParticles(ParticleTypes.SNEEZE, 25D, 1D))

                .defaultAndMaxLevel(1)
                .levelReq(1)
                .weight(0)
                .hideFromWiki()
                .build();
    }
}
