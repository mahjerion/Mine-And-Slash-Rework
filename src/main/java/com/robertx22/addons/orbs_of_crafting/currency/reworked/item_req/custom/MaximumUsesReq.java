package com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.custom;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.item_req.ItemReqSers;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.MaxUsesKey;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.rework.DataKey;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.StringUTIL;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

public class MaximumUsesReq extends ItemRequirement {


    public Data data;

    public static record Data(String use_id, int max_uses) {
        public MaxUsesKey toKey() {
            return new MaxUsesKey(this);
        }

        // human-readable label for the shared use-counter this requirement keys off of, eg "seed_uses" ->
        // "Seed Uses" - shown in the tooltip so mutually-exclusive currencies (like the Imprisoned Monster
        // Seeds, which all share ItemReqs.Datas.SEED_USES) make it clear they can't be mixed with each other.
        public String label() {
            return Arrays.stream(use_id.split("_"))
                    .map(StringUTIL::capitalise)
                    .reduce((a, b) -> a + " " + b)
                    .orElse(use_id);
        }
    }

    public MaximumUsesReq(String id, Data data) {
        super(ItemReqSers.MAX_USES, id);
        this.data = data;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return MaximumUsesReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return this.getTranslation(TranslationType.DESCRIPTION).getTranslatedName(data.max_uses, data.label());
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(SlashRef.MODID)
                .desc(ExileTranslation.registry(this, "Maximum %1$s uses of: %2$s")
                );
    }


    @Override
    public boolean isValid(Player p, StackHolder stack) {
        ExileStack ex = ExileStack.of(stack.stack);
        var uses = ex.get(StackKeys.CUSTOM).getOrCreate().data.get(new DataKey.IntKey(data.use_id));
        return uses < data.max_uses;
    }
}
