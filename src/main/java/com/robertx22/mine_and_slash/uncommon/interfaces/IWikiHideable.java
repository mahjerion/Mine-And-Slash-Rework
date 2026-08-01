package com.robertx22.mine_and_slash.uncommon.interfaces;

// for retired content: the entry stays registered so items already in inventories still resolve, it
// just doesn't get listed in the wiki anymore. pair it with weight 0 so it stops dropping too
public interface IWikiHideable {

    // nullable on purpose: gson skips null fields, so entries that never set it serialize exactly as
    // before and old datapack jsons still pass compareLoadedJsonAndFinalClass
    Boolean getHideFromWiki();

    default boolean isHiddenFromWiki() {
        return Boolean.TRUE.equals(getHideFromWiki());
    }
}
