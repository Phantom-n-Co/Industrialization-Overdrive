package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

/** A hatch leaves alone what strictly higher-priority hatches on the grid still need; equal priorities don't reserve against each other. */
final class MEHatchPullOrder {
    private MEHatchPullOrder() {
    }

    record Claim(int priority, long unmet) {
    }

    static long reservedAbove(int priority, Iterable<Claim> claims) {
        long reserved = 0L;
        for (Claim claim : claims) {
            if (claim.priority() > priority && claim.unmet() > 0) {
                reserved = saturatingAdd(reserved, claim.unmet());
            }
        }
        return reserved;
    }

    static long saturatingAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
