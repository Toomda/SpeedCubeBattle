package com.tomlucksted.speedcubebattle.backend.match.participant;

public class MatchParticipant {
    private final String playerId;
    private final String sessionId;
    private final ParticipantRole role;
    private volatile boolean ready;

    public MatchParticipant(String playerId, String sessionId, ParticipantRole role) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.role = role;
        this.ready = false;
    }

    public String playerId() { return playerId; }
    public String sessionId() { return sessionId; }
    public ParticipantRole role() { return role; }

    public boolean ready() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
