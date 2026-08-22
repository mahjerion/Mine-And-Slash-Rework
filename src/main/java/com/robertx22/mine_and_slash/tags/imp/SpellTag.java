package com.robertx22.mine_and_slash.tags.imp;

import com.robertx22.mine_and_slash.tags.ModTag;
import com.robertx22.mine_and_slash.tags.NormalModTag;
import com.robertx22.mine_and_slash.tags.TagType;

import java.util.List;
import java.util.stream.Collectors;

public class SpellTag extends NormalModTag {
    public static SpellTag SERIALIZER = new SpellTag("");

    public static SpellTag of(String id) {
        return (SpellTag) register(TagType.Spell, new SpellTag(id));
    }

    // for ids whose auto name reads badly. weapon_skill would otherwise capitalise to "Weapon skill"
    // and every derived stat inherits it - "Weapon skill Skill Cooldown Reduction" and friends
    public static SpellTag of(String id, String displayName) {
        SpellTag tag = new SpellTag(id);
        tag.displayName = displayName;
        return (SpellTag) register(TagType.Spell, tag);
    }

    public static List<SpellTag> getAll() {
        return ModTag.MAP.get(TagType.Spell).stream().map(x -> (SpellTag) x).collect(Collectors.toList());
    }

    // transient: fromString builds bare copies and TagList writes GUID strings, so this only ever
    // needs to exist on the registered instance the lang generator walks
    private transient String displayName = "";

    public SpellTag(String id) {
        super(id);
    }

    @Override
    public String locNameForLangFile() {
        return displayName.isEmpty() ? super.locNameForLangFile() : displayName;
    }

    @Override
    public ModTag fromString(String s) {
        return new SpellTag(s);
    }

    @Override
    public String getTagType() {
        return TagType.Spell.id;
    }
}
