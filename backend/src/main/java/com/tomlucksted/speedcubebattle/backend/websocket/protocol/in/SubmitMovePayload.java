package com.tomlucksted.speedcubebattle.backend.websocket.protocol.in;

public record SubmitMovePayload(String matchId, String move) {}
