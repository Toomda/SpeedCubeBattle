package com.tomlucksted.speedcubebattle.backend.websocket.protocol.in;

public record SetReadyPayload(String matchId, String playerId, boolean ready) {
}
