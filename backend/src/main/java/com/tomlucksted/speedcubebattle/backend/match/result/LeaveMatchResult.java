package com.tomlucksted.speedcubebattle.backend.match.result;

import com.tomlucksted.speedcubebattle.backend.match.Match;

public record LeaveMatchResult(
        LeaveMatchResultType type,
        String matchId,
        String playerId,
        Match match
) {}
