package com.robertx22.mine_and_slash.database.data.wizard;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * An elemental caster monster: which skills it knows, what it looks like, how often it casts.
 * <p>
 * Keyed on the entity type's own path - {@code fire_wizard} is the entry for
 * {@code mmorpg:fire_wizard} - so the four wizards share one entity class and one renderer, and a
 * pack retunes them without touching code. That matters more than it sounds: the shipped Chaos
 * Wizard knows one skill because the base mod only has one chaos spell worth casting, while a pack
 * with a fuller chaos school gives it three, and neither needs a different mob.
 * <p>
 * Modelled on {@link com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass}, which
 * answers the same three questions for the hired version. It is deliberately much smaller: a
 * wizard has no skill grid, no levels of its own and no gear, because its stats come from the
 * normal monster path - {@code OnMobSpawn} rolls its level off the map's tier and its rarity and
 * affixes like any other mob.
 */
public class WizardType implements JsonExileRegistry<WizardType>, IAutoGson<WizardType> {

    public static WizardType SERIALIZER = new WizardType();

    /** matches the registered entity type's path, e.g. {@code fire_wizard} */
    public String id = "";

    /** full ResourceLocation of the entity texture */
    public String texture = "mmorpg:textures/entity/fire_wizard.png";

    /** spell GUIDs this wizard picks from. order is irrelevant - the pick is uniformly random */
    public List<String> spells = new ArrayList<>();

    /**
     * How long the wizard waits between casts, rolled fresh after every one.
     * <p>
     * This is the entire pacing mechanism. A wizard deliberately ignores its skills' own cooldowns:
     * they are authored for a player who ranks them up and pays mana, and honouring them would have
     * a three skill wizard standing idle most of a fight. It has no basic attack to fall back on.
     */
    public int min_cast_interval_ticks = 20 * 4;
    public int max_cast_interval_ticks = 20 * 6;

    public WizardType() {
    }

    public WizardType(String id, String texture, List<String> spells) {
        this.id = id;
        this.texture = texture;
        this.spells = spells;
    }

    public ResourceLocation getTextureLoc() {
        return new ResourceLocation(texture);
    }

    /**
     * The skills this wizard can actually cast right now.
     * <p>
     * Silently drops ids the spell registry doesn't know. A datapack can name a skill it never
     * added, or remove one a wizard still lists, and the mob should keep fighting with what it has
     * left rather than throwing out of the middle of an entity tick.
     */
    public List<Spell> getSpells() {
        List<Spell> list = new ArrayList<>();
        for (String id : spells) {
            Spell spell = ExileDB.Spells().get(id);
            if (spell != null) {
                list.add(spell);
            }
        }
        return list;
    }

    /** guards a datapack that authored the interval backwards or at zero */
    public int rollCastInterval(net.minecraft.util.RandomSource random) {
        int min = Math.max(1, min_cast_interval_ticks);
        int max = Math.max(min, max_cast_interval_ticks);
        return min + random.nextInt(max - min + 1);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.WIZARD;
    }

    @Override
    public Class<WizardType> getClassForSerialization() {
        return WizardType.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }
}
