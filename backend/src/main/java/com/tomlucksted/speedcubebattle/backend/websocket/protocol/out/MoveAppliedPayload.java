package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

public record MoveAppliedPayload(String matchId, String playerId, String move, long seq, long serverTs) {}
