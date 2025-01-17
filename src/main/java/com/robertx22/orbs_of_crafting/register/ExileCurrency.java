package com.robertx22.orbs_of_crafting.register;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.IWeighted;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.IdKey;
import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.library_of_exile.util.UNICODE;
import com.robertx22.library_of_exile.utils.RandomUtils;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.AdditionalBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.NameBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RarityBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks.LeagueBlock;
import com.robertx22.mine_and_slash.loot.req.DropRequirement;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.mine_and_slash.wip.ExileCached;
import com.robertx22.orbs_of_crafting.api.OrbEvents;
import com.robertx22.orbs_of_crafting.lang.OrbWords;
import com.robertx22.orbs_of_crafting.main.OrbDatabase;
import com.robertx22.orbs_of_crafting.misc.LocReqContext;
import com.robertx22.orbs_of_crafting.misc.ModifyResult;
import com.robertx22.orbs_of_crafting.misc.ResultItem;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExileCurrency implements ITranslated, IAutoGson<ExileCurrency>, JsonExileRegistry<ExileCurrency>, IRarity {

    public static ExileCurrency SERIALIZER = new ExileCurrency();

    String id;

    public int weight = 1000;

    public transient String locname = "";
    public transient String modid = "";

    List<ItemModData> pick_one_item_mod = new ArrayList<>();
    List<ItemModData> always_do_item_mods = new ArrayList<>();
    List<String> req = new ArrayList<>();

    // todo this needs some extension system..
    String rar = IRarity.RARE_ID;
    // public List<WorksOnBlock.ItemType> item_type = new ArrayList<>(Arrays.asList(WorksOnBlock.ItemType.GEAR));
    public DropRequirement drop_req = DropRequirement.Builder.of().build();

    public List<String> item_type_requirement = new ArrayList<>();

    public PotentialData potential = new PotentialData(true, 1);

    public String item_id = "";

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return new TranslationBuilder(modid).name(ExileTranslation.item(getItem(), locname));

    }

    public List<ItemRequirement> getItemTypeRequirement() {

        return item_type_requirement.stream().map(x -> OrbDatabase.ItemReq().get(x)).collect(Collectors.toList());

    }

    // this needs reworking cus library doesnt have all these classes
    public static class PotentialData {

        public boolean needs_potential = true;

        public PotentialData(int potential_cost) {
            this.potential_cost = potential_cost;
        }

        public int potential_cost = 0;

        public PotentialData(boolean needs_potential, int potential_cost) {

            this.needs_potential = needs_potential;
            this.potential_cost = potential_cost;
        }


    }

    public Item getItem() {
        return VanillaUTIL.REGISTRY.items().get(new ResourceLocation(item_id));
    }

    public static class ItemModData implements IWeighted {
        public String id = "";
        public int weight = 1000;

        public ItemModData(String id, int weight) {
            this.id = id;
            this.weight = weight;
        }

        public ItemModification get() {
            return OrbDatabase.ItemMods().get(id);
        }

        @Override
        public int Weight() {
            return weight;
        }
    }


    @Override
    public String getRarityId() {
        return rar;
    }


    public static ExileCached<HashMap<Item, ExileCurrency>> CACHED_MAP = new ExileCached<>(() -> {
        HashMap<Item, ExileCurrency> map = new HashMap<>();
        for (ExileCurrency cur : OrbDatabase.Currency().getList()) {
            var i = cur.getItem();
            if (i != Items.AIR) {
                map.put(i, cur);
            }
        }
        return map;
    });

    public static Optional<ExileCurrency> get(ItemStack stack) {
        var res = CACHED_MAP.get().get(stack.getItem());
        return Optional.ofNullable(res);
    }

    public ResultItem modifyItem(LocReqContext ctx) {

        var event = new OrbEvents.Modify(this, ctx);
        OrbEvents.MODIFY.callEvents(event);
        StackHolder holder = new StackHolder(ctx.stack);

        boolean bad = false;
        try {


            var res = new ItemModificationResult();

            for (ItemModData mod : this.always_do_item_mods) {
                mod.get().applyMod(ctx.player, holder, res);
            }

            if (!pick_one_item_mod.isEmpty()) {
                var picked = RandomUtils.weightedRandom(this.pick_one_item_mod);
                picked.get().applyMod(ctx.player, holder, res);
                if (picked.get().getOutcomeType() == ItemModification.OutcomeType.BAD) {
                    bad = true;
                }
            }

            res.onFinish(ctx.player);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResultItem(holder.stack, ModifyResult.ERROR, ExplainedResult.failure(Component.literal("Code error caused the action to fail.")));
        }

        var res = new ResultItem(holder.stack, ModifyResult.SUCCESS, ExplainedResult.success());
        if (bad) {
            res.outcome = ItemModification.OutcomeType.BAD;
        }
        return res;
    }

    // todo this needs refactoring to work as a separate mod..
    // either not use exiletips system or transfer exiletips to library + make it an event
    // tooltip modifier register in library? so i can do: TooltipModifiers.CURRENCY.blabla( ... )
    // so the tooltip is added only to currency tooltips
    public List<Component> getTooltip() {

        ExileTooltips tip = new ExileTooltips();

        tip.accept(new NameBlock(getTranslation(TranslationType.NAME).getTranslatedName().withStyle(ChatFormatting.BOLD)));
        tip.accept(new RarityBlock(getRarity()));
        tip.accept(new AdditionalBlock(() -> {
            var list = new ArrayList<MutableComponent>();
            for (ItemRequirement req : getItemTypeRequirement()) {
                list.add(req.getDescWithParams().withStyle(ChatFormatting.YELLOW));
            }
            return list;
        }));
        tip.accept(new AdditionalBlock(() -> {
            List<MutableComponent> all = new ArrayList<>();

            int totalWeight = this.pick_one_item_mod.stream().mapToInt(IWeighted::Weight).sum();


            if (!this.pick_one_item_mod.isEmpty()) {
                all.add(Component.empty());

                all.add(OrbWords.RANDOM_OUTCOME.get().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                for (ItemModData data : this.pick_one_item_mod) {
                    var color = data.get().getOutcomeType().color;
                    all.add(getChanceTooltip(data, totalWeight, true).withStyle(color));
                }
            }

            if (!this.always_do_item_mods.isEmpty()) {
                all.add(Component.empty());
                all.add(OrbWords.ALWAYS_DOES.get().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                for (ItemModData data : this.always_do_item_mods) {
                    var color = data.get().getOutcomeType().color;
                    all.add(getChanceTooltip(data, data.weight, false).withStyle(color));
                }
            }
            all.add(Component.empty());

            all.add(OrbWords.Requirements.get().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            for (String s : this.req) {
                var req = OrbDatabase.ItemReq().get(s);
                all.add(Component.literal(UNICODE.ROTATED_CUBE + " ").append(req.getDescWithParams()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return all;
        }));

        if (this.potential.potential_cost > 0) {
            tip.accept(new AdditionalBlock(Collections.singletonList(OrbWords.POTENTIAL_COST.get(Component.literal("" + potential.potential_cost).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GOLD))));
        } else {
            tip.accept(new AdditionalBlock(Collections.singletonList(OrbWords.NOT_A_POTENTIAL_CONSUMER.get().withStyle(ChatFormatting.GOLD))));
        }
        tip.accept(new AdditionalBlock(OrbWords.Currency.get().withStyle(ChatFormatting.GOLD)));


        if (ExileDB.OrbExtension().isRegistered(GUID())) {
            var req = ExileDB.OrbExtension().get(this.GUID());

            if (req != null && req.drop_req.getLeague() != null) {
                tip.accept(new LeagueBlock(req.drop_req.getLeague()));
            }
        }

        return tip.release();
    }

    private MutableComponent getChanceTooltip(ItemModData mod, int totalweight, boolean addchance) {
        if (!addchance) {
            return Component.literal(UNICODE.STAR + " ").append(mod.get().getDescWithParams()).withStyle(ChatFormatting.YELLOW);
        }

        int chance = (int) (((float) mod.weight / (float) totalweight) * 100F);
        return Component.literal(UNICODE.STAR + " ").append(OrbWords.OUTCOME_TIP.get(mod.get().getDescWithParams(), Component.literal(chance + "%").withStyle(ChatFormatting.YELLOW)));
    }

    public ExplainedResult canItemBeModified(LocReqContext context) {

        var type = getItemTypeRequirement();

        if (type.stream().noneMatch(x -> x.isValid(context.player, new StackHolder(context.stack)))) {
            if (type.size() == 1) {
                return ExplainedResult.failure(OrbWords.THIS_IS_NOT_A.get(type.get(0).getTranslation(TranslationType.NAME).getTranslatedName()));

            } else {
                return ExplainedResult.failure(OrbWords.THIS_IS_NOT_A.get(TooltipUtils.joinMutableComps(type.stream().map(x -> x.getTranslation(TranslationType.NAME).getTranslatedName()).iterator(), Component.literal(" or "))));
            }
        }

        var event = new OrbEvents.CanBeModified(this, context);
        OrbEvents.CAN_BE_MODIFIED.callEvents(event);

        if (event.result != null) {
            return event.result;
        }

        for (String s : this.req) {
            var r = OrbDatabase.ItemReq().get(s);
            if (!r.isValid(context.player, new StackHolder(context.stack))) {
                // todo do i want to add custom fail messages instead?
                return ExplainedResult.failure(r.getDescWithParams());
            }
        }
        return ExplainedResult.success();
    }

    @Override
    public void onLoadedFromJson() {
        CACHED_MAP.clear();
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return OrbDatabase.CURRENCY;
    }

    @Override
    public Class<ExileCurrency> getClassForSerialization() {
        return ExileCurrency.class;
    }


    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }


    public static class Builder {

        public List<ItemModData> pickOneMods = new ArrayList<>();
        public List<ItemModData> useAllMods = new ArrayList<>();
        public List<String> req = new ArrayList<>();

        public DropRequirement drop = DropRequirement.Builder.of().build();
        public String id;
        public PotentialData pot = new PotentialData(1);
        public int weight = 1000;
        public String name;
        public List<String> type = new ArrayList<>();
        public String rar = IRarity.RARE_ID;

        public static Builder of(String id, String name, ExileKey<ItemRequirement, ?>... type) {
            return new Builder(id, name, type);
        }

        public Builder(String id, String name, ExileKey<ItemRequirement, ?>... type) {
            this.id = id;
            this.name = name;

            for (ExileKey<ItemRequirement, ?> t : type) {
                this.type.add(t.GUID());
            }
        }


        public Builder addModification(ExileKey<ItemModification, ?> modification, int weight) {
            this.pickOneMods.add(new ItemModData(modification.GUID(), weight));
            return this;
        }

        public Builder addAlwaysUseModification(ExileKey<ItemModification, ?> modification) {
            this.useAllMods.add(new ItemModData(modification.GUID(), 1));
            return this;
        }

        public Builder addRequirement(ExileKey<ItemRequirement, ?> requirement) {
            this.req.add(requirement.GUID());
            return this;
        }

        public Builder dropsOnlyIn(DropRequirement req) {
            this.drop = req;
            return this;
        }

        public Builder rarity(String rar) {
            this.rar = rar;
            return this;
        }

        public Builder edit(Consumer<Builder> c) {
            c.accept(this);
            return this;
        }

        public Builder weight(int w) {
            this.weight = w;
            return this;
        }

        public Builder potentialCost(int cost) {
            this.pot.potential_cost = cost;
            return this;
        }


        public ExileKey<ExileCurrency, IdKey> build(ExileKeyHolder<ExileCurrency> holder) {
            return new ExileKey<>(holder, new IdKey(id), (x, y) -> this.buildCurrency(holder), id);
        }

        public ExileCurrency buildCurrency(ExileKeyHolder<ExileCurrency> holder) {
            ExileCurrency currency = new ExileCurrency();
            currency.modid = holder.modRegisterInfo.modid;
            currency.id = id;
            currency.pick_one_item_mod = this.pickOneMods;
            currency.req = this.req;
            currency.always_do_item_mods = useAllMods;
            currency.item_type_requirement = type;
            currency.rar = rar;
            currency.locname = name;
            currency.weight = weight;
            currency.drop_req = drop;
            currency.potential = pot;
            currency.item_id = holder.getItemId(currency.id).toString();
            return currency;
        }
    }
}
