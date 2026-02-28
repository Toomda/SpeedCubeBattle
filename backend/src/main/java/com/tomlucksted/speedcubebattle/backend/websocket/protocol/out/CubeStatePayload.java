package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

public record CubeStatePayload(
        String matchId,
        String playerId,
        String facelets,   // 54 chars
        int moveCount,
        boolean solved
) {}
