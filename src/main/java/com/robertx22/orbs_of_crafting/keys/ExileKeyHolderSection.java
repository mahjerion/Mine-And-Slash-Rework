package com.robertx22.orbs_of_crafting.keys;

public abstract class ExileKeyHolderSection<T extends ExileKeyHolder> {

    T holder;

    public ExileKeyHolderSection(T holder) {
        this.holder = holder;

        this.holder.sections.add(this);
    }

    public T get() {
        return holder;
    }


    public abstract void init();
}
