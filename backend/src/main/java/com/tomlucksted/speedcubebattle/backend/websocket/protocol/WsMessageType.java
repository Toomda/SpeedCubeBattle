package com.tomlucksted.speedcubebattle.backend.websocket.protocol;

public enum WsMessageType {
    // LOBBY
    LOBBY_CREATE_MATCH,
    LOBBY_MATCH_CREATED,
    LOBBY_JOIN_MATCH,
    LOBBY_LEFT_MATCH,
    LOBBY_PLAYER_JOINED,
    LOBBY_SET_READY,
    LOBBY_READY_UPDATED,
    LOBBY_START_MATCH,

    // GAME
    GAME_SUBMIT_MOVE,
    GAME_MOVE_APPLIED,
    GAME_MATCH_ENDED,
    GAME_MATCH_STARTED,
    GAME_CUBE_STATE,

    // SYS
    SYS_ERROR
}
