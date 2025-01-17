package com.robertx22.library_of_exile.tooltip.order;

public enum TooltipOrder {
    FIRST(0, false),
    EARLY(10, false),
    MIDDLE(20, false),
    LATE(30, false),
    LAST(100, true);

    public int order;
    public boolean canHaveMultiple;

    TooltipOrder(int order, boolean canHaveMultiple) {
        this.order = order;
        this.canHaveMultiple = canHaveMultiple;
    }
}
