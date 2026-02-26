package com.tomlucksted.speedcubebattle.backend.websocket.protocol.in;

public record JoinMatchPayload(String matchId, String playerId) {}
