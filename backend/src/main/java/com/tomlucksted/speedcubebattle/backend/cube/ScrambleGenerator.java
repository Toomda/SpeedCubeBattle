package com.tomlucksted.speedcubebattle.backend.cube;

import java.util.*;

public final class ScrambleGenerator {
    private static final Move[] ALL = {
            Move.U, Move.Up, Move.U2,
            Move.D, Move.Dp, Move.D2,
            Move.L, Move.Lp, Move.L2,
            Move.R, Move.Rp, Move.R2,
            Move.F, Move.Fp, Move.F2,
            Move.B, Move.Bp, Move.B2
    };

    public static List<Move> generate(long seed, int length) {
        Random r = new Random(seed);
        List<Move> out = new ArrayList<>(length);

        Axis lastAxis = null;

        while (out.size() < length) {
            Move m = ALL[r.nextInt(ALL.length)];
            Axis ax = axisOf(m);

            if (lastAxis != null && ax == lastAxis) continue;

            out.add(m);
            lastAxis = ax;
        }

        return out;
    }

    private static Axis axisOf(Move m) {
        return switch (m) {
            case U, Up, U2, D, Dp, D2 -> Axis.Y;
            case L, Lp, L2, R, Rp, R2 -> Axis.X;
            case F, Fp, F2, B, Bp, B2 -> Axis.Z;
        };
    }

    private enum Axis { X, Y, Z }
}
