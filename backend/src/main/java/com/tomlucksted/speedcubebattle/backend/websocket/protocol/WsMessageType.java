package com.tomlucksted.speedcubebattle.backend.websocket.protocol;

public enum WsMessageType {
    CREATE_MATCH,
    MATCH_CREATED,
    JOIN_MATCH,
    LEFT_MATCH,
    PLAYER_JOINED,
    SET_READY,
    READY_UPDATED,
    MATCH_STARTED,
    START_MATCH,
    MATCH_ENDED,
    ERROR
}
