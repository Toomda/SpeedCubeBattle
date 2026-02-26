package com.tomlucksted.speedcubebattle.backend.match.result;

import com.tomlucksted.speedcubebattle.backend.match.Match;

public record ReadyResult(ReadyResultType type, Match match) {
}
