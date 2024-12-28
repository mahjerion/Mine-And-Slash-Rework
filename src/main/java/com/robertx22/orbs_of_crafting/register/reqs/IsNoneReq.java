package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.orbs_of_crafting.main.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public class IsNoneReq extends ItemRequirement {

    public Data data;

    public static record Data(List<String> requirements) {
    }

    transient String desc;

    public IsNoneReq(String id, Data data, String desc) {
        super(ItemReqSers.IS_NONE, id);
        this.data = data;
        this.desc = desc;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsNoneReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getDescParams();
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        var all = data.requirements.stream().map(x -> ExileDB.ItemReq().get(x)).collect(Collectors.toList());
        for (ItemRequirement req : all) {
            if (req.isValid(p, obj)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String locDescForLangFile() {
        return desc;
    }

}
