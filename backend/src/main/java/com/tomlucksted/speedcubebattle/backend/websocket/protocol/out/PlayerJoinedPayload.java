package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import java.util.List;

public record PlayerJoinedPayload(String matchId, String joinedPlayerId, List<PlayerInfo> players) {}
