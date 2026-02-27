package com.tomlucksted.speedcubebattle.backend.match.participant;

public class MatchParticipant {
    private final String playerId;
    private final String sessionId;
    private volatile boolean ready;

    public MatchParticipant(String playerId, String sessionId) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.ready = false;
    }

    public String playerId() { return playerId; }
    public String sessionId() { return sessionId; }

    public boolean ready() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
