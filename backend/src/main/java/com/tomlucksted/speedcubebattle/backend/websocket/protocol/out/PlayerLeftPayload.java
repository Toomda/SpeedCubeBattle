package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import java.util.List;

public record PlayerLeftPayload(String matchId, String leftPlayerId, List<PlayerInfo> players) {}
