package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class GenerateThreatEvent extends EffectEvent {

    public static String ID = "on_gen_threat";

    // who the mob ends up hating. source is whose stat sheet scales the threat, and the two differ for
    // minions: the damage is calculated on the owner's spell unit, but the summon is what pulls aggro.
    public LivingEntity threatOwner;

    @Override
    public String GUID() {
        return ID;
    }

    public GenerateThreatEvent(LivingEntity player, Mob mob, ThreatGenType threatGenType, float threat) {
        this(player, mob, threatGenType, threat, null);
    }

    public GenerateThreatEvent(LivingEntity player, Mob mob, ThreatGenType threatGenType, float threat, Spell spell) {
        super(threat, player, mob);
        this.data.setString(EventData.THREAT_GEN_TYPE, threatGenType.name());
        this.threatOwner = player;

        if (spell != null) {
            // without this the event isn't a spell effect, so support gems socketed into the spell never
            // reach the threat number, even though they scaled the damage that generated it
            this.data.setString(EventData.SPELL, spell.GUID());
        }
    }

    @Override
    public String getName() {
        return "Threat Event";
    }

    @Override
    protected void activate() {

        int threat = (int) data.getNumber();


        if (threat == 0) {
            return;
        }

        Load.Unit(target).getThreat().addThreat(threatOwner, (Mob) target, threat);
    }

}