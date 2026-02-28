package com.tomlucksted.speedcubebattle.backend.websocket;

import com.tomlucksted.speedcubebattle.backend.cube.Move;
import com.tomlucksted.speedcubebattle.backend.match.result.*;
import com.tomlucksted.speedcubebattle.backend.match.Match;
import com.tomlucksted.speedcubebattle.backend.match.MatchService;
import com.tomlucksted.speedcubebattle.backend.match.participant.MatchParticipant;
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
            case LOBBY_CREATE_MATCH -> {
                CreateMatchPayload payload = om.convertValue(incoming.payload(), CreateMatchPayload.class);
                String matchId = matchService.createMatch();
                var join = matchService.joinMatch(matchId, session.getId(), payload.playerId());
                if (join.type() != JoinResultType.OK) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Failed to join created match: " + join.type(), ErrorType.CRITICAL));
                    return;
                }

                send(session, WsMessageType.LOBBY_MATCH_CREATED, new MatchCreatedPayload(matchId));

                broadcastToMatch(join.match(),
                        WsMessageType.LOBBY_PLAYER_JOINED,
                        new PlayerJoinedPayload(matchId, payload.playerId(), join.match().toPlayerInfos())
                );
            }

            case LOBBY_JOIN_MATCH -> {
                JoinMatchPayload payload = om.convertValue(incoming.payload(), JoinMatchPayload.class);
                var result = matchService.joinMatch(payload.matchId(), session.getId(), payload.playerId());

                if(result.type() == JoinResultType.MATCH_NOT_FOUND) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == JoinResultType.ALREADY_IN_MATCH) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Player already in match: " + payload.playerId(), ErrorType.INFO));
                    return;
                } else if (result.type() == JoinResultType.MATCH_FULL) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Match is already full: " + payload.matchId(), ErrorType.INFO));
                    return;
                } else if (result.type() == JoinResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("The Match has already started: " + payload.matchId(), ErrorType.INFO));
                    return;
                }

                var match = result.match();
                broadcastToMatch(match, WsMessageType.LOBBY_PLAYER_JOINED, new PlayerJoinedPayload(match.id(), payload.playerId(), match.toPlayerInfos()));
            }

            case LOBBY_SET_READY -> {
                SetReadyPayload payload = om.convertValue(incoming.payload(), SetReadyPayload.class);
                var result = matchService.setReady(payload.matchId(), payload.playerId(), payload.ready());

                if(result.type() == ReadyResultType.NOT_FOUND) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if (result.type() == ReadyResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("The game has already started: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == ReadyResultType.NOT_IN_MATCH) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("You are not part of this match: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                }

                var match = result.match();
                broadcastToMatch(match, WsMessageType.LOBBY_READY_UPDATED, new ReadyUpdatePayload(match.id(), payload.playerId(),payload.ready(), match.toPlayerInfos()));
            }

            case LOBBY_START_MATCH -> {
                StartMatchPayload payload = om.convertValue(incoming.payload(), StartMatchPayload.class);
                var result = matchService.tryStartMatch(payload.matchId(), session.getId());

                if(result.type() == StartMatchResultType.NOT_FOUND)
                {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_IN_LOBBY) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("The match has already started: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_HOST) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("You are not the host of this match: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if(result.type() == StartMatchResultType.NOT_READY) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Not all players are ready: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == StartMatchResultType.NOT_IN_MATCH) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("You are not part of this match: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                }

                var match = result.match();
                var scr = match.scramble().stream().map(Move::notation).toList();
                broadcastToMatch(match, WsMessageType.GAME_MATCH_STARTED,
                        new MatchStartedPayload(match.id(), match.startTime(), match.scrambleSeed(), scr, match.toPlayerInfos())
                );

                for (MatchParticipant p : match.participants()) {
                    WebSocketSession s = sessions.get(p.sessionId());
                    if (s != null && s.isOpen()) {
                        send(s, WsMessageType.GAME_CUBE_STATE,
                                new CubeStatePayload(match.id(), p.playerId(), match.cubeOf(p.playerId()).facelets(), 0, false));
                    }
                }
            }

            case GAME_SUBMIT_MOVE -> {
                SubmitMovePayload payload = om.convertValue(incoming.payload(), SubmitMovePayload.class);
                var result = matchService.applyMove(payload.matchId(), session.getId(), payload.move());

                if(result.type() == MoveResultType.NOT_FOUND) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Match not found: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if(result.type() == MoveResultType.NOT_IN_MATCH) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("You are not part of this match: " + payload.matchId(), ErrorType.CRITICAL));
                    return;
                } else if(result.type() == MoveResultType.NOT_RUNNING) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("The game has not started yet: " + payload.matchId(), ErrorType.WARNING));
                    return;
                } else if (result.type() == MoveResultType.INVALID_MOVE) {
                    send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Invalid move: " + payload.move(), ErrorType.WARNING));
                    return;
                }

                broadcastToMatch(result.match(), WsMessageType.GAME_MOVE_APPLIED, result.applied());
                send(session, WsMessageType.GAME_CUBE_STATE, result.cube());
            }

            default -> send(session, WsMessageType.SYS_ERROR, new ErrorPayload("Unhandled message type: " + type, ErrorType.WARNING));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        var leaveResult = matchService.leaveBySessionId(session.getId());

        if(leaveResult.type() != LeaveMatchResultType.OK) return;
        var match = leaveResult.match();

        if(match == null || match.participants().isEmpty()) return;

        broadcastToMatch(match, WsMessageType.LOBBY_LEFT_MATCH, new PlayerLeftPayload(match.id(), leaveResult.playerId(), match.toPlayerInfos()));
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
