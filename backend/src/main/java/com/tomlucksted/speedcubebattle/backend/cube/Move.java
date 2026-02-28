package com.tomlucksted.speedcubebattle.backend.cube;

public enum Move {
    U, Up, U2,
    D, Dp, D2,
    L, Lp, L2,
    R, Rp, R2,
    F, Fp, F2,
    B, Bp, B2;

    public String notation() {
        return switch(this) {
            case U -> "U";
            case Up -> "U'";
            case U2 -> "U2";

            case D -> "D";
            case Dp -> "D'";
            case D2 -> "D2";

            case L -> "L";
            case Lp -> "L'";
            case L2 -> "L2";

            case R -> "R";
            case Rp -> "R'";
            case R2 -> "R2";

            case F -> "F";
            case Fp -> "F'";
            case F2 -> "F2";

            case B -> "B";
            case Bp -> "B'";
            case B2 -> "B2";
        };
    }

    public static Move parse(String s) {
        s = s.trim();
        return switch (s) {
            case "U" -> U;
            case "U'" -> Up;
            case "U2" -> U2;

            case "D" -> D;
            case "D'" -> Dp;
            case "D2" -> D2;

            case "L" -> L;
            case "L'" -> Lp;
            case "L2" -> L2;

            case "R" -> R;
            case "R'" -> Rp;
            case "R2" -> R2;

            case "F" -> F;
            case "F'" -> Fp;
            case "F2" -> F2;

            case "B" -> B;
            case "B'" -> Bp;
            case "B2" -> B2;

            default -> throw new IllegalArgumentException("Invalid move: " + s);
        };
    }
}