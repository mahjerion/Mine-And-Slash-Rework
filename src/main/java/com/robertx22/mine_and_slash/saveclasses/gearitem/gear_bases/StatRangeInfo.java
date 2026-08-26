package com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.MinMax;
import com.robertx22.mine_and_slash.database.data.stats.tooltips.StatTooltipType;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class StatRangeInfo implements Cloneable {


    public StatRangeInfo(ModRange minmax) {
        this.hasAltDown = Screen.hasAltDown();
        this.hasShiftDown = Screen.hasShiftDown();

        this.player = ClientOnly.getPlayer();
        this.unitdata = Load.Unit(player);

        this.minmax = minmax;
    }

    public Player player;
    public EntityData unitdata;
    /**
     * Whose stats the numbers are scored against. The viewing player unless a screen is showing
     * another unit's copy of the content - the mercenary screen lists the mercenary's own skills, and
     * those have to be costed and scaled off the mercenary rather than off whoever opened the screen.
     */
    private LivingEntity caster = null;
    public ModRange minmax = ModRange.of(new MinMax(0, 100));
    public StatTooltipType statTooltipType = StatTooltipType.NORMAL;

    public boolean hasAltDown = false;
    public boolean hasShiftDown = false;

    public LivingEntity getCaster() {
        return caster == null ? player : caster;
    }

    /** null is ignored, so callers can pass an entity that may not be around right now */
    public StatRangeInfo setCaster(LivingEntity en) {
        if (en != null) {
            this.caster = en;
            this.unitdata = Load.Unit(en);
        }
        return this;
    }

    public boolean shouldShowDescriptions() {
        return hasAltDown && !hasShiftDown;
    }

    public boolean useInDepthStats() {
        return !hasAltDown && hasShiftDown;
    }

}
