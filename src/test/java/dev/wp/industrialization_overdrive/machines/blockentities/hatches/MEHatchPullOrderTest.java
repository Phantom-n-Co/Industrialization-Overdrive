package dev.wp.industrialization_overdrive.machines.blockentities.hatches;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MEHatchPullOrderTest {
    private static MEHatchPullOrder.Claim claim(int priority, long unmet) {
        return new MEHatchPullOrder.Claim(priority, unmet);
    }

    @Test
    void onlyStrictlyHigherPrioritiesReserve() {
        List<MEHatchPullOrder.Claim> claims = List.of(claim(5, 100), claim(1, 40), claim(0, 7), claim(-3, 1000));
        assertEquals(140, MEHatchPullOrder.reservedAbove(0, claims));
        assertEquals(100, MEHatchPullOrder.reservedAbove(1, claims));
        assertEquals(0, MEHatchPullOrder.reservedAbove(5, claims));
        assertEquals(1147, MEHatchPullOrder.reservedAbove(Integer.MIN_VALUE, claims));
    }

    @Test
    void ownClaimNeverCountsAgainstItself() {
        // The puller is among the grid's hatches; its own priority is not higher than itself.
        assertEquals(0, MEHatchPullOrder.reservedAbove(3, List.of(claim(3, 500))));
    }

    @Test
    void nonPositiveClaimsAreIgnoredAndSumsSaturate() {
        assertEquals(10, MEHatchPullOrder.reservedAbove(0, List.of(claim(1, 0), claim(1, -5), claim(2, 10))));
        assertEquals(Long.MAX_VALUE, MEHatchPullOrder.reservedAbove(0, List.of(claim(1, Long.MAX_VALUE), claim(2, 10))));
    }

    @Test
    void noClaimsReserveNothing() {
        assertEquals(0, MEHatchPullOrder.reservedAbove(0, List.of()));
    }
}
