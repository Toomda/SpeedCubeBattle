package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import java.util.List;

public record MatchStartedPayload(
        String matchId,
        long startedAt,
        long scrambleSeed,
        List<String> scramble,
        List<PlayerInfo> players
) {}
