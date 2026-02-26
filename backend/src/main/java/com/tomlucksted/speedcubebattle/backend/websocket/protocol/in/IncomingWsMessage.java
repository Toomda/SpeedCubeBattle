package com.tomlucksted.speedcubebattle.backend.websocket.protocol.in;

import com.tomlucksted.speedcubebattle.backend.websocket.protocol.WsMessageType;
import tools.jackson.databind.JsonNode;

public record IncomingWsMessage(WsMessageType type, JsonNode payload) {
}
