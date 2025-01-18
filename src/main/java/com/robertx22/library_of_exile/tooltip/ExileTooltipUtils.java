package com.robertx22.library_of_exile.tooltip;

import com.robertx22.library_of_exile.wrappers.ExileText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExileTooltipUtils {

    public static List<Component> mutableToComp(List<MutableComponent> list) {
        return new ArrayList<Component>(list);
    }

    public static List<MutableComponent> splitLongText(MutableComponent comp) {
        //   if (true) return Arrays.asList(comp);
        List<MutableComponent> componentList = new ArrayList<>();
        Style format = comp.getStyle();
        String[] originalList = comp.getString().split("\n");

        for (String comp1 : originalList) {
            componentList.add(ExileText.ofText(comp1).get().withStyle(format));
        }
        return componentList;
    }

    public static List<Component> splitLongText(List<? extends Component> comps) {
        //  if (true) return comps.stream().collect(Collectors.toList());

        ArrayList<Component> arrayList = new ArrayList<>();
        for (Component target : comps) {
            if (target.getString().contains("\n")) {
                Style format = target.getStyle();
                String[] originalList = target.getString().split("\n");

                for (String comp1 : originalList) {
                    arrayList.add(Component.literal(comp1).withStyle(format));
                }
            } else {
                arrayList.add(target);
            }

        }
        return arrayList;
    }

    public static MutableComponent joinMutableComps(Iterator<? extends Component> iterator, MutableComponent separator) {
        if (separator == null) {
            separator = Component.literal("");
        }

        MutableComponent starter = Component.literal("");
        while (iterator.hasNext()) {
            starter.append(iterator.next());
            if (iterator.hasNext()) {
                starter.append(separator);
            }
        }
        return starter;
    }
}
