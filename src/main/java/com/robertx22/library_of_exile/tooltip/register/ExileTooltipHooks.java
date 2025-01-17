package com.robertx22.library_of_exile.tooltip.register;

import com.robertx22.library_of_exile.tooltip.TooltipItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExileTooltipHooks {
    public static HashMap<String, List<ExileTooltipHook>> map = new HashMap<>();

    public static void register(TooltipItem tip, ExileTooltipHook hook) {
        if (!map.containsKey(tip.id)) {
            map.put(tip.id, new ArrayList<>());
        }
        map.get(tip.id).add(hook);
    }
}
