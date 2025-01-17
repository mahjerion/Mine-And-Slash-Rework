package com.robertx22.library_of_exile.registry.helpers;

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
