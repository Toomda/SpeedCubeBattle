package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import java.util.List;

public record ReadyUpdatePayload(String matchId, String playerId, boolean ready, List<PlayerInfo> players) {
}
