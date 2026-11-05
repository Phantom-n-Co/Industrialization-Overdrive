package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

public enum MEHatchType {
    ITEM(true, false),
    FLUID(false, true),
    MIXED(true, true);

    private final boolean items;
    private final boolean fluids;

    MEHatchType(boolean items, boolean fluids) {
        this.items = items;
        this.fluids = fluids;
    }

    public boolean handlesItems() {
        return items;
    }

    public boolean handlesFluids() {
        return fluids;
    }
}
