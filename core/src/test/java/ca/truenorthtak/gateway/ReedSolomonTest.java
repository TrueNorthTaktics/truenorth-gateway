package ca.truenorthtak.gateway;

import ca.truenorthtak.gateway.fec.Fragmenter;
import ca.truenorthtak.gateway.fec.RedundancyPolicy;
import ca.truenorthtak.gateway.fec.ReedSolomon;
import ca.truenorthtak.gateway.model.Precedence;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReedSolomonTest {

    @Test
    public void recoversFromEveryErasurePatternUpToParityCount() {
        int k = 5, m = 3, len = 37;
        Random rng = new Random(1);
        byte[][] original = new byte[k + m][len];
        for (int i = 0; i < k; i++) rng.nextBytes(original[i]);
        ReedSolomon rs = new ReedSolomon(k, m);
        rs.encode(original);
        int n = k + m;
        for (int mask = 0; mask < (1 << n); mask++) {
            int lost = Integer.bitCount(mask);
            byte[][] shards = new byte[n][];
            boolean[] present = new boolean[n];
            for (int i = 0; i < n; i++) {
                present[i] = (mask & (1 << i)) == 0;
                shards[i] = present[i] ? original[i].clone() : new byte[len];
            }
            boolean ok = rs.reconstructData(shards, present);
            if (lost <= m) {
                assertTrue("mask " + mask, ok);
                for (int i = 0; i < k; i++) assertArrayEquals("mask " + mask, original[i], shards[i]);
            } else {
                assertFalse("mask " + mask, ok);
            }
        }
    }

    @Test
    public void fragmenterSurvivesLossInAnyOrder() {
        byte[] payload = new byte[300];
        new Random(2).nextBytes(payload);
        List<byte[]> frags = Fragmenter.split(payload, 99, 32, Precedence.IMMEDIATE, RedundancyPolicy.standard());
        int k = frags.get(0)[2] & 0xFF;
        int m = frags.get(0)[3] & 0xFF;
        assertTrue(m >= k); // IMMEDIATE -> 100% parity
        for (int trial = 0; trial < 200; trial++) {
            List<byte[]> shuffled = new ArrayList<>(frags);
            Collections.shuffle(shuffled, new Random(trial));
            Fragmenter.Reassembler r = new Fragmenter.Reassembler();
            byte[] out = null;
            for (byte[] f : shuffled.subList(0, k)) {
                byte[] res = r.accept(f);
                if (res != null) out = res;
            }
            assertArrayEquals(payload, out);
        }
    }

    @Test
    public void tooFewFragmentsYieldsNothing() {
        byte[] payload = new byte[100];
        List<byte[]> frags = Fragmenter.split(payload, 1, 32, Precedence.ROUTINE, RedundancyPolicy.standard());
        int k = frags.get(0)[2] & 0xFF;
        Fragmenter.Reassembler r = new Fragmenter.Reassembler();
        for (byte[] f : frags.subList(0, k - 1)) assertNull(r.accept(f));
        assertFalse(r.isComplete());
    }
}
