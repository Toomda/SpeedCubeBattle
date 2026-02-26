package com.tomlucksted.speedcubebattle.backend.match.result;

import com.tomlucksted.speedcubebattle.backend.match.Match;

public record StartMatchResult(StartMatchResultType type, Match match) {
}
