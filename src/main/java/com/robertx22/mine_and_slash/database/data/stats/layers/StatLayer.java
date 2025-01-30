package com.robertx22.mine_and_slash.database.data.stats.layers;


import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EffectEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// todo this will be used to combine defensives and cap them
// for example armor will provide a physicial mitigation layer, but physical resistance stat will add to it
// when combined, it will still be capped to say 90%
// this is to make multiple stats affect the same mechanic without jank
// stuff like players have 50% less phys mitigation could easily work
// could also just collect the stats that use layers first, then calculate the layers at the end.
// like additive layer + crit layer + more multi layer
// reduced crit damage taken would reduce the bonus from the crit layer etc
public class StatLayer implements JsonExileRegistry<StatLayer>, IAutoGson<StatLayer>, IAutoLocName {

    public static StatLayer SERIALIZER = new StatLayer();

    public static List<StatLayer> ALL = new ArrayList<>();

    private StatLayer() {
    }

    public String id = "";
    public String name = "";
    public int priority = 0;

    public float min_multi = -1;
    public float max_multi = 1000;


    public LayerAction action = LayerAction.MULTIPLY;

    public enum LayerAction {
        CONVERT_PERCENT() {
            @Override
            public void apply(EffectEvent event, StatLayerData layer, String number) {


                if (event instanceof DamageEvent effect) {
                    // todo this isnt needed as theres only 1 conversion layer
                    if (!event.data.isNumberSetup(EventData.BEFORE_CONVERSION_NUMBER)) {
                        event.data.getNumber(EventData.BEFORE_CONVERSION_NUMBER, effect.data.getNumber(EventData.NUMBER).number);
                    }
                    if (!layer.conversion.normalized) {
                        layer.conversion.normalizeNumbersToCapTo100();
                    }
                    float original = effect.data.getNumber(EventData.BEFORE_CONVERSION_NUMBER).number;

                    for (Map.Entry<Elements, Float> en : layer.conversion.totals.entrySet()) {
                        var ele = en.getKey();
                        float conv = en.getValue();

                        if (conv > 100) {
                            conv = 100;
                        }

                        if (conv > effect.unconvertedDamagePercent) {
                            conv = effect.unconvertedDamagePercent;
                        }
                        if (conv <= 0) {
                            return;
                        }
                        effect.unconvertedDamagePercent -= conv;
                        float dmg = original * conv / 100F;
                        dmg = MathHelper.clamp(dmg, 0, original);
                        if (dmg > 0) {
                            effect.addBonusEleDmg(ele, dmg, layer.side);
                            event.data.getNumber(EventData.NUMBER).number -= dmg;
                        }
                    }
                }

            }
        },
        DAMAGE_TAKEN_AS() {
            @Override
            public void apply(EffectEvent event, StatLayerData layer, String number) {


                if (event instanceof DamageEvent effect) {

                    if (!layer.damageTakenAs.normalized) {
                        layer.damageTakenAs.normalizeNumbersToCapTo100();
                    }
                    float original = effect.data.getNumber(EventData.NUMBER).number;

                    for (Map.Entry<Elements, Float> en : layer.damageTakenAs.totals.entrySet()) {
                        var ele = en.getKey();
                        float conv = en.getValue();

                        if (conv > 100) {
                            conv = 100;
                        }

                        if (conv > effect.unconvertedDamageTakenAsPercent) {
                            conv = effect.unconvertedDamageTakenAsPercent;
                        }
                        if (conv <= 0) {
                            return;
                        }
                        effect.unconvertedDamageTakenAsPercent -= conv;
                        float dmg = original * conv / 100F;
                        dmg = MathHelper.clamp(dmg, 0, original);
                        if (dmg > 0) {
                            effect.addBonusEleDmg(ele, dmg, layer.side);
                            event.data.getNumber(EventData.NUMBER).number -= dmg;
                        }
                    }
                }

            }
        },
        X_AS_BONUS_Y_ELEMENT_DAMAGE() {
            @Override
            public void apply(EffectEvent event, StatLayerData layer, String number) {
                float conv = layer.additionalConversion.percent;

                if (event instanceof DamageEvent effect) {
                    if (conv <= 0) {
                        return;
                    }
                    float dmg = effect.data.getNumber(EventData.NUMBER).number * conv / 100F;
                    dmg = MathHelper.clamp(dmg, 0, Float.MAX_VALUE);
                    if (dmg > 0) {
                        effect.addBonusEleDmg(layer.additionalConversion.to, dmg, layer.side);
                    }
                }
            }
        },
        MULTIPLY() {
            @Override
            public void apply(EffectEvent event, StatLayerData layer, String number) {
                float multi = layer.getMultiplier();
                event.data.getNumber(number).number *= multi;
            }
        },
        ADD() {
            @Override
            public void apply(EffectEvent event, StatLayerData layer, String number) {
                event.data.getNumber(number).number += layer.getNumber();

            }
        };

        public abstract void apply(EffectEvent event, StatLayerData layer, String number);
    }

    public MutableComponent getTooltip(EffectEvent event, StatLayerData data) {

        // todo can a target effect be modified by the source etc..?
        MutableComponent sourcetarget = Component.empty().append("[").append(data.side.word.locName().append("]: "));

        MutableComponent number = Component.literal("x" + MMORPG.DECIMAL_FORMAT.format(data.getMultiplier()));

        MutableComponent locname = locName();

        if (this.action == LayerAction.ADD) {
            String plusminus = data.getNumber() < 0 ? "" : "+";
            number = Component.literal(plusminus + MMORPG.DECIMAL_FORMAT.format(data.getNumber()));
        }
        if (this.action == LayerAction.CONVERT_PERCENT) {
            MutableComponent convtext = Component.literal("");

            for (Map.Entry<Elements, Float> en : data.conversion.totals.entrySet()) {
                var ele = en.getKey();
                Float perc = en.getValue();
                locname = locName(event.data.getElement().getIconNameDmg(), ele.getIconNameDmg());
                number = Component.literal(perc + "%");

                MutableComponent t = Component.empty().append(sourcetarget).append(locname.append(": ").append(number));
                convtext.append(t.append("\n"));
            }
            return convtext;
        }
        if (this.action == LayerAction.DAMAGE_TAKEN_AS) {
            MutableComponent convtext = Component.literal("");

            for (Map.Entry<Elements, Float> en : data.damageTakenAs.totals.entrySet()) {
                var ele = en.getKey();
                Float perc = en.getValue();
                locname = locName(event.data.getElement().getIconNameDmg(), ele.getIconNameDmg());
                number = Component.literal(perc + "%");

                MutableComponent t = Component.empty().append(sourcetarget).append(locname.append(": ").append(number));
                convtext.append(t.append("\n"));
            }
            return convtext;
        }
        if (this.action == LayerAction.X_AS_BONUS_Y_ELEMENT_DAMAGE) {
            locname = locName(event.data.getElement().getIconNameDmg(), data.additionalConversion.to.getIconNameDmg());
            number = Component.literal(data.additionalConversion.percent + "%");
        }

        MutableComponent t = Component.empty().append(sourcetarget).append(locname.append(": ").append(number));
        return t;

    }

    public StatLayer(String id, String name, LayerAction action, int priority, float min_multi, float max_multi) {
        this.id = id;
        this.action = action;
        this.name = name;
        this.priority = priority;
        this.min_multi = min_multi;
        this.max_multi = max_multi;

        ALL.add(this);
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.STAT_LAYER;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".stat_layer." + id;
    }

    @Override
    public String locNameForLangFile() {
        return name;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.STAT_LAYER;
    }

    @Override
    public Class<StatLayer> getClassForSerialization() {
        return StatLayer.class;
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
