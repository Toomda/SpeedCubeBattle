package com.tomlucksted.speedcubebattle.backend.match;

import com.tomlucksted.speedcubebattle.backend.match.participant.MatchParticipant;
import com.tomlucksted.speedcubebattle.backend.match.participant.ParticipantRole;
import com.tomlucksted.speedcubebattle.backend.match.result.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchService {

    private static final int MAX_PLAYERS = 2;

    private final Map<String, Match> matches = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToMatchId = new ConcurrentHashMap<>();

    public String createMatch() {
        String id = UUID.randomUUID().toString();
        matches.put(id, new Match(id));
        return id;
    }

    public Match getMatch(String matchId) {
        return matches.get(matchId);
    }

    public JoinResult joinMatch(String matchId, String sessionId, String playerId) {
        Match match = matches.get(matchId);
        if(match == null) return new JoinResult(JoinResultType.MATCH_NOT_FOUND, null);

        synchronized (match) {
            if (match.state() != MatchState.LOBBY) return new JoinResult(JoinResultType.NOT_IN_LOBBY, match);
            if (match.containsPlayer(playerId)) return new JoinResult(JoinResultType.ALREADY_IN_MATCH, match);
            if (match.participants().size() >= MAX_PLAYERS) return new JoinResult(JoinResultType.MATCH_FULL, match);

            ParticipantRole role = match.participants().isEmpty() ? ParticipantRole.HOST : ParticipantRole.GUEST;

            match.addParticipant(new MatchParticipant(playerId, sessionId, role));
            sessionToMatchId.put(sessionId, matchId);
            return new JoinResult(JoinResultType.OK, match);
        }
    }

    public ReadyResult setReady(String matchId, String playerId, boolean ready) {
        Match match = matches.get(matchId);
        if (match == null) return new ReadyResult(ReadyResultType.NOT_FOUND, null);

        synchronized (match) {
            if (match.state() != MatchState.LOBBY) return new ReadyResult(ReadyResultType.NOT_IN_LOBBY, match);
            if (!match.containsPlayer(playerId)) return new ReadyResult(ReadyResultType.NOT_IN_MATCH, match);;

            match.setReady(playerId, ready);
            return new ReadyResult(ReadyResultType.OK, match);
        }
    }

    public StartMatchResult tryStartMatch(String matchId, String starterPlayerId, String sessionId) {
        Match match = matches.get(matchId);
        if (match == null) return new StartMatchResult(StartMatchResultType.NOT_FOUND, null);

        synchronized (match) {
            if (match.state() != MatchState.LOBBY)
                return new StartMatchResult(StartMatchResultType.NOT_IN_LOBBY, match);

            var caller = match.participantBySession(sessionId);
            if (caller == null)
                return new StartMatchResult(StartMatchResultType.NOT_IN_MATCH, match);

            if (caller.role() != ParticipantRole.HOST)
                return new StartMatchResult(StartMatchResultType.NOT_HOST, match);

            if (!match.allReady(MAX_PLAYERS))
                return new StartMatchResult(StartMatchResultType.NOT_READY, match);

            match.start();
            return new StartMatchResult(StartMatchResultType.OK, match);
        }
    }

}
