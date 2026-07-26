package com.robertx22.mine_and_slash.prophecy;

import com.robertx22.addons.dungeon_realm.MnsLeagues;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.map_affix.MapAffix;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.maps.AffectedEntities;
import com.robertx22.mine_and_slash.maps.MapItemData;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.SimpleStatCtx;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.coins.Coin;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerProphecies implements IStatCtx {

    public List<ProphecyData> rewardOffers = new ArrayList<>();

    public List<String> affixOffers = new ArrayList<>();

    public String mapid = "";

    public List<String> affixesTaken = new ArrayList<>();

    public int numMobAffixesCanAdd = 0;

    public int rerollsUsed = 0;

    public boolean usedFreeRoll = false;


    public void clearIfNewMap(MapItemData map) {

        this.mapid = map.uuid;

        numMobAffixesCanAdd = 0;
        affixesTaken.clear();

        affixOffers.clear();
        rewardOffers.clear();

        rerollsUsed = 0;
        usedFreeRoll = false;
    }


    public void regenAffixOffers() {
        this.affixOffers.clear();

        for (int i = 0; i < 3; i++) {
            // todo did i figure it out correctly
            MapAffix affix = ExileDB.MapAffixes()
                    .getFilterWrapped(x ->
                            x.req.equals(MnsLeagues.INSTANCE.PROPHECY.GUID()) &&
                                    x.affected == AffectedEntities.Players &&
                                    affixOffers.stream().map(a -> ExileDB.MapAffixes().get(a)).allMatch(e -> !e.prophecy_type.equals(x.prophecy_type))
                    ).random();
            affixOffers.add(affix.GUID());
        }
    }


    public void regenerateNewOffers(Player p) {

        rewardOffers = new ArrayList<>();

        for (int i = 0; i < ServerContainer.get().PROPHECY_OFFERS_PER_REROLL.get(); i++) {
            rewardOffers.add(ProphecyGeneration.generate(p));
        }

    }

    // flat cost to reroll, independent of whatever happens to be on offer - scales the same base
    // cost individual offers scale from (ProphecyGeneration.BASE_COST), but through a server-configured
    // multiplier instead of the random amount/modifier cost_multi rolls, so it stays constant reroll
    // to reroll
    public int getRerollCost() {
        return (int) (ProphecyGeneration.BASE_COST * ServerContainer.get().PROPHECY_REROLL_COST_MULTI.get());
    }

    public void tryReroll(Player p) {

        var map = Load.mapAt(p.level(), p.blockPosition());

        if (map == null) {
            p.sendSystemMessage(Chats.MUST_BE_IN_MAP_TO_REROLL_PROPHECY.locName().withStyle(ChatFormatting.RED));
            return;
        }
        if (!WorldUtils.isMapWorldClass(p.level(), p.blockPosition()) || !map.map.uuid.equals(this.mapid)) {
            p.sendSystemMessage(Chats.MUST_BE_IN_MAP_TO_REROLL_PROPHECY.locName().withStyle(ChatFormatting.RED));
            return;
        }

        if (rerollsUsed >= ServerContainer.get().PROPHECY_MAX_REROLLS_PER_MAP.get()) {
            p.sendSystemMessage(Chats.NO_PROPHECY_REROLLS_LEFT.locName().withStyle(ChatFormatting.RED));
            return;
        }

        int cost = getRerollCost();

        if (Coin.PROPHECY.getTotalFromInventory(p) < cost) {
            p.sendSystemMessage(Chats.NOT_ENOUGH_FAVOR_TO_REROLL_PROPHECY.locName().withStyle(ChatFormatting.RED));
            return;
        }

        Coin.PROPHECY.spend(p, cost);

        regenerateNewOffers(p);

        rerollsUsed++;

        SoundUtils.playSound(p, SoundEvents.EXPERIENCE_ORB_PICKUP);
    }


    public void tryAcceptReward(Player p, String uuid) {

        var map = Load.mapAt(p.level(), p.blockPosition());

        if (map == null) {
            p.sendSystemMessage(Chats.MUST_BE_IN_MAP_TO_ACCEPT_PROPHECY.locName().withStyle(ChatFormatting.RED));
            return;
        }
        if (!WorldUtils.isMapWorldClass(p.level(), p.blockPosition()) || !map.map.uuid.equals(this.mapid)) {
            // this is to stop people gathering points in lvl 1 maps and going into lvl 100 tier 100 maps to claim rewards
            p.sendSystemMessage(Chats.MUST_BE_IN_MAP_TO_ACCEPT_PROPHECY.locName().withStyle(ChatFormatting.RED));
            return;
        }
        // orElse(null), not get(): rerolling (tryReroll -> regenerateNewOffers) replaces rewardOffers
        // wholesale, so a client whose screen still shows the pre-reroll offers will send a uuid the
        // server no longer holds. get() would throw NoSuchElementException inside the packet handler.
        ProphecyData data = rewardOffers.stream().filter(x -> x.uuid.equals(uuid)).findAny().orElse(null);

        if (data != null) {

            if (Coin.PROPHECY.getTotalFromInventory(p) < data.cost) {
                p.sendSystemMessage(Chats.NOT_ENOUGH_FAVOR_TO_BUY_PROPHECY.locName().withStyle(ChatFormatting.RED));
                return;
            }

            Coin.PROPHECY.spend(p, data.cost);

            SoundUtils.playSound(p, SoundEvents.EXPERIENCE_ORB_PICKUP);

            rewardOffers.removeIf(x -> x.uuid.equals(data.uuid)); // todo check if this works

            for (ItemStack stack : data.generateRewards(p)) {
                PlayerUtils.giveItem(stack, p);
            }

        }
    }

    @Override
    public List<StatContext> getStatAndContext(LivingEntity en) {
        List<ExactStatData> list = new ArrayList<>();
        WorldUtils.ifMapData(en.level(), en.blockPosition()).ifPresent(map -> {
            if (map != null && map.map.uuid.equals(this.mapid)) {
                for (String s : this.affixesTaken) {
                    list.addAll(ExileDB.MapAffixes().get(s).getStats(100, Load.Unit(en).getLevel()));
                }
            }
        });
        var ctx = new SimpleStatCtx(StatContext.StatCtxType.PROPHECY_CURSE, list);
        return Arrays.asList(ctx);
    }
}
