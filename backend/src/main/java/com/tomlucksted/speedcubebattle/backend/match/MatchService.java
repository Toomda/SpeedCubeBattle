package com.tomlucksted.speedcubebattle.backend.match;

import com.tomlucksted.speedcubebattle.backend.cube.Move;
import com.tomlucksted.speedcubebattle.backend.cube.ScrambleGenerator;
import com.tomlucksted.speedcubebattle.backend.match.participant.MatchParticipant;
import com.tomlucksted.speedcubebattle.backend.match.participant.ParticipantRole;
import com.tomlucksted.speedcubebattle.backend.match.result.*;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.CubeStatePayload;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.MoveAppliedPayload;
import org.springframework.stereotype.Service;

import java.util.List;
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

            match.addParticipant(new MatchParticipant(playerId, sessionId));
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

    public StartMatchResult tryStartMatch(String matchId, String sessionId) {
        Match match = matches.get(matchId);
        if (match == null) return new StartMatchResult(StartMatchResultType.NOT_FOUND, null);

        synchronized (match) {
            if (match.state() != MatchState.LOBBY)
                return new StartMatchResult(StartMatchResultType.NOT_IN_LOBBY, match);

            var caller = match.participantBySession(sessionId);
            if (caller == null)
                return new StartMatchResult(StartMatchResultType.NOT_IN_MATCH, match);

            if (!caller.playerId().equals(match.hostPlayerId()))
                return new StartMatchResult(StartMatchResultType.NOT_HOST, match);

            if (!match.allReady(MAX_PLAYERS))
                return new StartMatchResult(StartMatchResultType.NOT_READY, match);

            long seed = System.nanoTime(); // oder Random/UUID
            List<Move> scramble = ScrambleGenerator.generate(seed, 20);

            match.start(seed, scramble);
            return new StartMatchResult(StartMatchResultType.OK, match);
        }
    }

    public LeaveMatchResult leaveBySessionId(String sessionId) {
        String matchId = sessionToMatchId.remove(sessionId);
        if(matchId == null) {
            return new LeaveMatchResult(LeaveMatchResultType.NOT_IN_MATCH, null, null, null);
        }

        Match match = matches.get(matchId);
        if(match == null) {
            return new LeaveMatchResult(LeaveMatchResultType.MATCH_NOT_FOUND, matchId, null, null);
        }

        synchronized (match) {
            MatchParticipant participant = match.participantBySession(sessionId);
            if(participant == null) {
                return new LeaveMatchResult(LeaveMatchResultType.NOT_IN_MATCH, matchId, null, match);
            }

            String playerId = participant.playerId();

            match.removeParticipant(playerId);

            if(match.participants().isEmpty()) {
                matches.remove(matchId);
            }

            return new LeaveMatchResult(LeaveMatchResultType.OK, matchId, playerId, match);
        }
    }

    public MoveResult applyMove(String matchId, String sessionId, String moveStr) {
        Match match = matches.get(matchId);
        if (match == null) return new MoveResult(MoveResultType.NOT_FOUND, null, null, null);

        synchronized (match) {
            if (match.state() != MatchState.RUNNING)
                return new MoveResult(MoveResultType.NOT_RUNNING, match, null, null);

            var caller = match.participantBySession(sessionId);
            if (caller == null)
                return new MoveResult(MoveResultType.NOT_IN_MATCH, match, null, null);

            Move move;
            try {
                move = Move.parse(moveStr);
            } catch (Exception e) {
                return new MoveResult(MoveResultType.INVALID_MOVE, match, null, null);
            }

            match.applyMoveFor(caller.playerId(), move);

            long seq = match.nextMoveSeq();
            long serverTs = System.currentTimeMillis();

            boolean solved = match.cubeOf(caller.playerId()).isSolved();

            var applied = new MoveAppliedPayload(matchId, caller.playerId(), moveStr, seq, serverTs);

            var cubePayload = new CubeStatePayload(
                    matchId,
                    caller.playerId(),
                    match.cubeOf(caller.playerId()).facelets(),
                    match.moveCountOf(caller.playerId()),
                    solved
            );

            return new MoveResult(MoveResultType.OK, match, applied, cubePayload);
        }
    }

}
