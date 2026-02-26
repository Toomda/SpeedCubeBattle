package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import com.tomlucksted.speedcubebattle.backend.websocket.ErrorType;

public record ErrorPayload(String message, ErrorType errorType) {
}


