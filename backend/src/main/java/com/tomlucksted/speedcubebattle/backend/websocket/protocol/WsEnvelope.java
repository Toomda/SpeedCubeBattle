package com.tomlucksted.speedcubebattle.backend.websocket.protocol;

public record WsEnvelope<T>(WsMessageType type, T payload) {}