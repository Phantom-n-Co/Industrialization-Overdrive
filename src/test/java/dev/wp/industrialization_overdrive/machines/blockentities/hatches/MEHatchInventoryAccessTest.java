package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEHatchInventoryAccessTest {
    @Test
    void itemCapacityRoundTripsAndStaysOffMIsPath() {
        for (long capacity : new long[] {0, 1, 63, 64, 65, 1_000_000, Integer.MAX_VALUE}) {
            int encoded = MEHatchInventoryAccess.encodeItemCapacity(capacity);
            assertEquals(capacity, MEHatchInventoryAccess.itemCapacity(encoded), "capacity " + capacity);
            assertTrue(MEHatchInventoryAccess.isUncapped(encoded), "capacity " + capacity + " fell back to MI's path");
        }
        assertEquals(Integer.MAX_VALUE, MEHatchInventoryAccess.itemCapacity(MEHatchInventoryAccess.encodeItemCapacity(Long.MAX_VALUE)));
        assertEquals(0, MEHatchInventoryAccess.itemCapacity(MEHatchInventoryAccess.encodeItemCapacity(-5)));
    }
}
