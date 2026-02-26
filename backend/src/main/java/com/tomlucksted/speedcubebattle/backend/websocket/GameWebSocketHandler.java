package com.tomlucksted.speedcubebattle.backend.websocket;

import com.tomlucksted.speedcubebattle.backend.match.result.JoinResultType;
import com.tomlucksted.speedcubebattle.backend.match.Match;
import com.tomlucksted.speedcubebattle.backend.match.MatchService;
import com.tomlucksted.speedcubebattle.backend.match.result.ReadyResultType;
import com.tomlucksted.speedcubebattle.backend.match.participant.MatchParticipant;
import com.tomlucksted.speedcubebattle.backend.match.result.StartMatchResultType;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.WsEnvelope;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.WsMessageType;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.in.*;
import com.tomlucksted.speedcubebattle.backend.websocket.protocol.out.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper om;
    private final MatchService matchService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GameWebSocketHandler(MatchService matchService, ObjectMapper om) {
        this.matchService = matchService;
        this.om = om;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("WS connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        var incoming = om.readValue(message.getPayload(), IncomingWsMessage.class);
        var type = incoming.type();

        switch (type) {
            case CREATE_MATCH -> {
                CreateMatchPayload payload = om.convertValue(incoming.payload(), CreateMatchPayload.class);
                String matchId = matchService.createMatch();
                var join = matchService.joinMatch(matchId, session.getId(), payload.playerId());
                if (join.type() != JoinResultType.OK) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Failed to join created match: " + join.type(), ErrorType.CRITICAL));
                    return;
                }

                send(session, WsMessageType.MATCH_CREATED, new MatchCreatedPayload(matchId));
            }

            case JOIN_MATCH -> {
                JoinMatchPayload payload = om.convertValue(incoming.payload(), JoinMatchPayload.class);
                var result = matchService.joinMatch(payload.matchId(), session.getId(), payload.playerId());

                if(result.type() == JoinResultType.MATCH_NOT_FOUND) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == JoinResultType.ALREADY_IN_MATCH) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Player already in match: " + payload.playerId(), ErrorType.INFO));
                    return;
                } else if (result.type() == JoinResultType.MATCH_FULL) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Match is already full: " + payload.matchId(), ErrorType.INFO));
                    return;
                } else if (result.type() == JoinResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("The Match has already started: " + payload.matchId(), ErrorType.INFO));
                    return;
                }

                var match = result.match();
                broadcastToMatch(match, WsMessageType.PLAYER_JOINED, new PlayerJoinedPayload(match.id(), payload.playerId(), match.toPlayerInfos()));
            }

            case SET_READY -> {
                SetReadyPayload payload = om.convertValue(incoming.payload(), SetReadyPayload.class);
                var result = matchService.setReady(payload.matchId(), payload.playerId(), payload.ready());

                if(result.type() == ReadyResultType.NOT_FOUND) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if (result.type() == ReadyResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("The game has already started: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == ReadyResultType.NOT_IN_MATCH) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("You are not part of this match: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                }

                var match = result.match();
                broadcastToMatch(match, WsMessageType.READY_UPDATED, new ReadyUpdatePayload(match.id(), payload.playerId(),payload.ready(), match.toPlayerInfos()));
            }

            case START_MATCH -> {
                StartMatchPayload payload = om.convertValue(incoming.payload(), StartMatchPayload.class);
                var result = matchService.tryStartMatch(payload.matchId(), payload.playerId(), session.getId());

                if(result.type() == StartMatchResultType.NOT_FOUND)
                {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("The match has already started: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_HOST) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("You are not the host of this match: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_READY) {
                    send(session, WsMessageType.ERROR, new ErrorPayload("Not all players are ready: " + payload.matchId(), ErrorType.WARNING));
                    return;
                }

                var match = result.match();
                broadcastToMatch(match, WsMessageType.MATCH_STARTED, new MatchStartedPayload(match.id(), match.startTime(), match.toPlayerInfos()));
            }

            default -> send(session, WsMessageType.ERROR, new ErrorPayload("Unhandled message type: " + type, ErrorType.WARNING));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("WS closed: " + session.getId() + " (" + status + ")");
    }

    private <T> void send(WebSocketSession session, WsMessageType type, T payload) throws Exception {
        WsEnvelope<T> envelope = new WsEnvelope<>(type, payload);
        session.sendMessage(new TextMessage(om.writeValueAsString(envelope)));
    }

    private <T> void broadcastToMatch(Match match, WsMessageType type, T payload) throws Exception {
        WsEnvelope<T> envelope = new WsEnvelope<>(type, payload);
        String json = om.writeValueAsString(envelope);

        var participants = List.copyOf(match.participants());

        for (MatchParticipant participant : participants) {
            WebSocketSession s = sessions.get(participant.sessionId());
            if (s != null && s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
}
