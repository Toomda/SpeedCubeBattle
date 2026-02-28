package com.tomlucksted.speedcubebattle.backend.cube;

public final class CubeState {
    
    private final char[][][] f = new char[6][3][3];

    private static final int U=0, R=1, F=2, D=3, L=4, B=5;

    public CubeState() {
        reset();
    }

    public void reset() {
        fill(U,'W');
        fill(R,'R');
        fill(F,'G');
        fill(D,'Y');
        fill(L,'O');
        fill(B,'B');
    }

    private void fill(int face, char c) {
        for(int r=0;r<3;r++)
            for(int c2=0;c2<3;c2++)
                f[face][r][c2]=c;
    }

    public void apply(Move m) {
        switch(m) {
            case U  -> turnU();
            case Up -> { turnU(); turnU(); turnU(); }

            case D  -> turnD();
            case Dp -> { turnD(); turnD(); turnD(); }

            case R  -> turnR();
            case Rp -> { turnR(); turnR(); turnR(); }

            case L  -> turnL();
            case Lp -> { turnL(); turnL(); turnL(); }

            case F  -> turnF();
            case Fp -> { turnF(); turnF(); turnF(); }

            case B  -> turnB();
            case Bp -> { turnB(); turnB(); turnB(); }

            case U2 -> { turnU(); turnU(); }
            case D2 -> { turnD(); turnD(); }
            case R2 -> { turnR(); turnR(); }
            case L2 -> { turnL(); turnL(); }
            case F2 -> { turnF(); turnF(); }
            case B2 -> { turnB(); turnB(); }
        }
    }

    public boolean isSolved() {
        for(int face=0;face<6;face++) {
            char c = f[face][0][0];
            for(int r=0;r<3;r++)
                for(int col=0;col<3;col++)
                    if(f[face][r][col]!=c) return false;
        }
        return true;
    }

    // Viewer wants U D L R F B
    public String facelets() {
        return faceToString(U)
                + faceToString(D)
                + faceToString(L)
                + faceToString(R)
                + faceToString(F)
                + faceToString(B);
    }

    private String faceToString(int face) {
        StringBuilder sb=new StringBuilder(9);
        for(int r=0;r<3;r++)
            for(int c=0;c<3;c++)
                sb.append(f[face][r][c]);
        return sb.toString();
    }

    private void rotateCW(int face) {
        char[][] t=new char[3][3];
        for(int r=0;r<3;r++)
            for(int c=0;c<3;c++)
                t[c][2-r]=f[face][r][c];
        f[face]=t;
    }

    private void turnU() {
        rotateCW(U);
        char[] temp=f[F][0].clone();
        f[F][0]=f[R][0].clone();
        f[R][0]=f[B][0].clone();
        f[B][0]=f[L][0].clone();
        f[L][0]=temp;
    }

    private void turnD() {
        rotateCW(D);
        char[] temp=f[F][2].clone();
        f[F][2]=f[L][2].clone();
        f[L][2]=f[B][2].clone();
        f[B][2]=f[R][2].clone();
        f[R][2]=temp;
    }

    private void turnR() {
        rotateCW(R);
        char[] temp=new char[]{f[U][0][2],f[U][1][2],f[U][2][2]};
        for(int i=0;i<3;i++) f[U][i][2]=f[F][i][2];
        for(int i=0;i<3;i++) f[F][i][2]=f[D][i][2];
        for(int i=0;i<3;i++) f[D][i][2]=f[B][2-i][0];
        for(int i=0;i<3;i++) f[B][2-i][0]=temp[i];
    }

    private void turnL() {
        rotateCW(L);
        char[] temp=new char[]{f[U][0][0],f[U][1][0],f[U][2][0]};
        for(int i=0;i<3;i++) f[U][i][0]=f[B][2-i][2];
        for(int i=0;i<3;i++) f[B][2-i][2]=f[D][i][0];
        for(int i=0;i<3;i++) f[D][i][0]=f[F][i][0];
        for(int i=0;i<3;i++) f[F][i][0]=temp[i];
    }

    private void turnF() {
        rotateCW(F);
        char[] temp=f[U][2].clone();
        for(int i=0;i<3;i++) f[U][2][i]=f[L][2-i][2];
        for(int i=0;i<3;i++) f[L][i][2]=f[D][0][i];
        for(int i=0;i<3;i++) f[D][0][i]=f[R][2-i][0];
        for(int i=0;i<3;i++) f[R][i][0]=temp[i];
    }

    private void turnB() {
        rotateCW(B);
        char[] temp=f[U][0].clone();
        for(int i=0;i<3;i++) f[U][0][i]=f[R][i][2];
        for(int i=0;i<3;i++) f[R][i][2]=f[D][2][2-i];
        for(int i=0;i<3;i++) f[D][2][i]=f[L][i][0];
        for(int i=0;i<3;i++) f[L][i][0]=temp[2-i];
    }
}