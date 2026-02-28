package com.tomlucksted.speedcubebattle.backend.match.result;

import com.tomlucksted.speedcubebattle.backend.match.Match;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.CubeStatePayload;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.MoveAppliedPayload;

public record MoveResult(MoveResultType type, Match match, MoveAppliedPayload applied, CubeStatePayload cube) {}
