package com.robertx22.library_of_exile.tooltip;

import com.robertx22.library_of_exile.tooltip.order.ExileTooltipPart;
import com.robertx22.library_of_exile.tooltip.register.ExileTooltipHook;
import com.robertx22.library_of_exile.tooltip.register.ExileTooltipHooks;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class TooltipBuilder<T extends TooltipItem> {

    public T item;
    private List<Function<T, ExileTooltipPart>> parts = new ArrayList<>();

    private boolean addedHooks = false;

    public TooltipBuilder(T item) {
        this.item = item;
    }

    public TooltipBuilder<T> add(Function<T, ExileTooltipPart> p) {
        parts.add(p);
        return this;
    }

    public List<Component> build() {
        return build(new ExileTooltipOptions());
    }

    public List<Component> build(ExileTooltipOptions options) {
        if (!addedHooks) {
            for (ExileTooltipHook hook : ExileTooltipHooks.map.getOrDefault(item.id, new ArrayList<>())) {
                hook.apply(this);
            }
            addedHooks = true;
        }

        List<Component> tip = new ArrayList<>();

        List<ExileTooltipPart> allParts = new ArrayList<>();
        for (Function<T, ExileTooltipPart> part : parts) {
            allParts.add(part.apply(item));
        }

        allParts.sort(Comparator.comparingInt(x -> x.order.order));

        for (ExileTooltipPart part : allParts) {
            tip.addAll(part.text);

            if (options.addSpaceInBetween) {
                tip.add(Component.empty());
            }
        }

        return tip;

    }


}
