package com.tomlucksted.speedcubebattle.backend.websocket.protocol.out;

import com.tomlucksted.speedcubebattle.backend.match.participant.ParticipantRole;

public record PlayerInfo(String playerId, boolean ready, ParticipantRole role) {}
