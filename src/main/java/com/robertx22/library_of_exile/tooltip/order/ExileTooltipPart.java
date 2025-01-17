package com.robertx22.library_of_exile.tooltip.order;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExileTooltipPart {

    public TooltipOrder order;
    public List<Component> text = new ArrayList<>();

    public ExileTooltipPart(TooltipOrder order, List<? extends Component> tip) {
        this.order = order;
        text.addAll(tip);
    }


    public <T extends Component> ExileTooltipPart(TooltipOrder order, T... tip) {
        this.order = order;
        this.text.addAll(Arrays.asList(tip));
    }
}
