package com.robertx22.mine_and_slash.database.data.item_set;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.interfaces.IWikiHideable;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// diablo style gear set: wear N of the listed uniques, get the tiered bonuses.
// membership is listed here rather than on UniqueGear so adding a set doesn't restale every
// unique gear json (compareLoadedJsonAndFinalClass demands byte equality) and third party unique
// datapacks keep working untouched.
public class ItemSet implements IAutoGson<ItemSet>, JsonExileRegistry<ItemSet>, IAutoLocName, IWikiHideable {

    public static ItemSet SERIALIZER = new ItemSet();

    public String id = "";
    public transient String name = "";

    // UniqueGear GUIDs that count towards this set
    public List<String> uniques = new ArrayList<>();

    public List<SetBonus> bonuses = new ArrayList<>();

    // nullable on purpose: gson skips null fields, so sets that don't set it serialize exactly as
    // before and old datapack jsons still pass compareLoadedJsonAndFinalClass
    public Boolean hide_from_wiki = null;

    // unique guid -> set. built lazily and thrown away by DatabaseCaches.resetCaches(), which runs
    // on AFTER_DATABASE_LOADED server side and via TellClientResetCaches on the client.
    private static HashMap<String, ItemSet> UNIQUE_TO_SET = null;

    public static void resetCache() {
        UNIQUE_TO_SET = null;
    }

    public static ItemSet ofUnique(String uniqueId) {
        if (uniqueId == null || uniqueId.isEmpty()) {
            return null;
        }
        if (UNIQUE_TO_SET == null) {
            HashMap<String, ItemSet> map = new HashMap<>();
            for (ItemSet set : ExileDB.ItemSets().getList()) {
                for (String unique : set.uniques) {
                    map.put(unique, set);
                }
            }
            UNIQUE_TO_SET = map;
        }
        return UNIQUE_TO_SET.get(uniqueId);
    }

    // the (x/y) denominator. derived instead of stored so it can't disagree with the list.
    public int getSetSize() {
        return uniques.size();
    }

    public List<SetBonus> getSortedBonuses() {
        List<SetBonus> list = new ArrayList<>(bonuses);
        list.sort(Comparator.comparingInt(x -> x.pieces));
        return list;
    }

    // "Oath of Mahj (2/4)" followed by one line per bonus tier, green once that tier is reached and
    // grey while it isn't. fallbackLevel is the hovered item's level, used to scale FLAT stats for
    // the preview when none of the set is actually worn.
    public List<Component> getTooltip(Player player, int fallbackLevel) {
        List<Component> list = new ArrayList<>();

        EquippedSets equipped = EquippedSets.of(player, this);
        int lvl = equipped.pieces > 0 ? equipped.avgLevel : fallbackLevel;

        list.add(Itemtips.SET_PIECES.locName(locName(), equipped.pieces, getSetSize())
                .withStyle(ChatFormatting.GOLD));

        for (SetBonus bonus : getSortedBonuses()) {
            ChatFormatting color = equipped.isActive(bonus) ? ChatFormatting.GREEN : ChatFormatting.GRAY;

            for (ExactStatData stat : bonus.getStats(lvl)) {
                for (MutableComponent line : stat.GetTooltipString()) {
                    list.add(Itemtips.SET_BONUS_TIER.locName(bonus.pieces)
                            .withStyle(color)
                            .append(line.withStyle(color)));
                }
            }
        }

        return list;
    }

    @Override
    public Boolean getHideFromWiki() {
        return hide_from_wiki;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.ITEM_SET;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return 1000;
    }

    @Override
    public Class<ItemSet> getClassForSerialization() {
        return ItemSet.class;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Item_Sets;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".item_set." + id;
    }

    @Override
    public String locNameForLangFile() {
        return name;
    }
}
