package com.tomlucksted.speedcubebattle.backend.match;

import com.tomlucksted.speedcubebattle.backend.cube.CubeState;
import com.tomlucksted.speedcubebattle.backend.cube.Move;
import com.tomlucksted.speedcubebattle.backend.match.participant.MatchParticipant;
import com.tomlucksted.speedcubebattle.backend.match.participant.ParticipantRole;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.PlayerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Match {
    private final String id;

    private volatile MatchState state = MatchState.LOBBY;
    private volatile Long startTime;
    private volatile Long endTime;

    private final Map<String, MatchParticipant> participants = new ConcurrentHashMap<>();
    private volatile String hostPlayerId;
    private long moveSeq = 0;

    private volatile Long scrambleSeed;
    private volatile List<Move> scramble = List.of();

    // pro Spieler eigener Cube
    private final Map<String, CubeState> cubes = new ConcurrentHashMap<>();
    private final Map<String, Integer> moveCounts = new ConcurrentHashMap<>();

    public Long scrambleSeed() { return scrambleSeed; }
    public List<Move> scramble() { return scramble; }

    public CubeState cubeOf(String playerId) {
        return cubes.get(playerId);
    }

    public int moveCountOf(String playerId) {
        return moveCounts.getOrDefault(playerId, 0);
    }


    public Match(String id) {
        this.id = id;
    }

    public String id() { return id; }
    public MatchState state() { return state; }
    public Long startTime() { return startTime; }
    public Long endTime() { return endTime; }
    public String hostPlayerId() { return hostPlayerId; }
    public Collection<MatchParticipant> participants() { return participants.values(); }

    public List<String> playerIds() {
        return new ArrayList<>(participants.keySet());
    }

    public MatchParticipant participant(String playerId) {
        return participants.get(playerId);
    }

    public boolean containsPlayer(String playerId) {
        return participants.containsKey(playerId);
    }

    void addParticipant(MatchParticipant participant) {
        if(hostPlayerId == null && participants.isEmpty()) {
            hostPlayerId = participant.playerId();
        }
        participants.put(participant.playerId(), participant);
    }

    void removeParticipant(String playerId) {
        participants.remove(playerId);

        if(participants.isEmpty()) {
            hostPlayerId = null;
            return;
        }

        if(playerId.equals(hostPlayerId)) {
            hostPlayerId = participants.keySet().iterator().next();
        }
    }

    void setReady(String playerId, boolean ready) {
        MatchParticipant participant = participants.get(playerId);
        if(participant == null) return;
        participant.setReady(ready);
    }

    boolean allReady(int expectedPlayers) {
        if(participants.size() < expectedPlayers) return false;
        for(MatchParticipant participant : participants.values()) {
            if(!participant.ready()) return false;
        }
        return true;
    }

    void start(long seed, List<Move> scrambleMoves) {
        state = MatchState.RUNNING;
        startTime = System.currentTimeMillis();

        this.scrambleSeed = seed;
        this.scramble = List.copyOf(scrambleMoves);

        // Für alle Teilnehmer: neuen Cube initialisieren und scramble anwenden
        for (var p : participants.values()) {
            CubeState cube = new CubeState();
            for (Move m : scrambleMoves) cube.apply(m);

            cubes.put(p.playerId(), cube);
            moveCounts.put(p.playerId(), 0);

            // optional: ready resetten, damit lobby-state sauber ist
            p.setReady(false);
        }
    }

    void applyMoveFor(String playerId, Move move) {
        CubeState cube = cubes.get(playerId);
        if (cube == null) {
            // sollte in RUNNING eigentlich nicht passieren, aber defensive
            cube = new CubeState();
            for (Move m : scramble) cube.apply(m);
            cubes.put(playerId, cube);
            moveCounts.put(playerId, 0);
        }

        cube.apply(move);
        moveCounts.put(playerId, moveCountOf(playerId) + 1);
    }

    void finish() {
        state = MatchState.FINISHED;
        endTime = System.currentTimeMillis();
    }

    public List<PlayerInfo> toPlayerInfos() {
        return participants().stream()
                .map(p -> new PlayerInfo(p.playerId(), p.ready(), p.playerId().equals(hostPlayerId) ? ParticipantRole.HOST :  ParticipantRole.GUEST))
                .toList();
    }

    public MatchParticipant participantBySession(String sessionId) {
        return participants().stream()
                .filter(p -> p.sessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }


    long nextMoveSeq() {
        return ++moveSeq;
    }
}
