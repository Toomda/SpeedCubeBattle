package com.tomlucksted.speedcubebattle.backend.websocket.protocol.in;

public record StartMatchPayload(String matchId, String playerId) {
}
