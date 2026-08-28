package com.robertx22.mine_and_slash.database.data.stats.datapacks.stats;

import com.robertx22.mine_and_slash.aoe_data.database.stats.old.DatapackStats;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.datapacks.base.BaseDatapackStat;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class AttributeStat extends BaseDatapackStat {

    public static String SER_ID = "vanilla_attribute_stat_ser";

    transient String locname;

    public UUID uuid;
    public String attributeId;
    public Attribute attribute;
    public AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
    public boolean cut_by_hundred = true;

    public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc, AttributeModifier.Operation operation, boolean cut) {
        super(SER_ID);
        this.id = id;
        this.operation = operation;
        this.locname = locname;
        this.uuid = uuid;
        this.cut_by_hundred = cut;
        this.attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute)
                .toString();
        this.attribute = attribute;
        this.is_perc = perc;
        this.scaling = StatScaling.NONE;

        DatapackStats.tryAdd(this);
    }

    @Override
    public String locDescForLangFile() {
        return "Increase vanilla attribute.";
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    public void addToEntity(LivingEntity en, StatData data) {

        float val = data.getValue();
        if (cut_by_hundred) {
            val = val / 100F;
        }

        // both multiplying operations read the value as a fraction of 1, so anything past -1 means
        // "take away more than all of it". vanilla applies that literally and the running attribute
        // value goes negative, which any second negative multiplier on the entity (vanilla slowness,
        // a cc effect) flips back into a large POSITIVE value. -1 is already a full shutdown - the
        // attribute floors at its own minimum from there.
        if (operation != AttributeModifier.Operation.ADDITION) {
            val = Math.max(val, -1F);
        }

        AttributeModifier mod = new AttributeModifier(
                uuid,
                attributeId,
                val,
                operation
        );

        AttributeInstance atri = en.getAttribute(attribute);

        if (atri != null) {
            if (atri.hasModifier(mod)) {
                atri.removeModifier(mod); // KEEP THIS OR UPDATE WONT MAKE HP CORRECT!!!
            }
            atri.addTransientModifier(mod);
        }

    }
}
