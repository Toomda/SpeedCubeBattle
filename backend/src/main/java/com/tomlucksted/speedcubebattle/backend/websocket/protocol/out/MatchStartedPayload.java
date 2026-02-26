package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import java.util.List;

public record MatchStartedPayload(String matchId, long startedAt, List<PlayerInfo> players) {
}
