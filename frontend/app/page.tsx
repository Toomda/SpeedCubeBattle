"use client";

import { useEffect, useMemo, useState } from "react";

type WsEnvelope<T = any> = { type: string; payload?: T };
type ErrorPayload = { message: string; errorType?: string };

type PlayerInfo = {
  playerId: string;
  ready: boolean;
  role?: "HOST" | "GUEST";
};

type MatchCreatedPayload = { matchId: string };
type PlayerJoinedPayload = { matchId: string; joinedPlayerId: string; players: PlayerInfo[] };
type ReadyUpdatePayload = { matchId: string; changedPlayerId: string; ready: boolean; players: PlayerInfo[] };
type MatchStartedPayload = { matchId: string; startedAt?: number; players?: PlayerInfo[] };

function genId() {
  return Math.random().toString(16).slice(2) + "-" + Date.now().toString(16);
}

export default function Home() {
  const [playerId, setPlayerId] = useState<string>("");
  const [ws, setWs] = useState<WebSocket | null>(null);

  const [log, setLog] = useState<string[]>([]);
  const [joinMatchId, setJoinMatchId] = useState<string>("");

  const [matchId, setMatchId] = useState<string>("");
  const [players, setPlayers] = useState<PlayerInfo[]>([]);
  const [matchStarted, setMatchStarted] = useState<boolean>(false);
  const [lastError, setLastError] = useState<string>("");

  const me = useMemo(() => players.find((p) => p.playerId === playerId), [players, playerId]);
  const isHost = me?.role === "HOST";
  const isInMatch = !!matchId && players.some((p) => p.playerId === playerId);
  const allReady = players.length === 2 && players.every((p) => p.ready);
  const canStart = isInMatch && isHost && allReady && !matchStarted;

  // Generate stable playerId per tab (no hydration mismatch)
  useEffect(() => {
    const existing = sessionStorage.getItem("playerId");
    const id = existing ?? genId();
    if (!existing) sessionStorage.setItem("playerId", id);
    setPlayerId(id);
  }, []);

  // WS connect when playerId is ready
  useEffect(() => {
    if (!playerId) return;

    const socket = new WebSocket("ws://localhost:8083/ws");

    socket.onopen = () => {
      setLog((l) => [`connected as playerId=${playerId}`, ...l]);
      setWs(socket);
    };

    socket.onmessage = (e) => {
      try {
        const msg: WsEnvelope = JSON.parse(e.data);

        // log
        setLog((l) => [`<= ${msg.type} ${JSON.stringify(msg.payload ?? {})}`, ...l]);

        switch (msg.type) {
          case "MATCH_CREATED": {
            const p = msg.payload as MatchCreatedPayload;
            const id = p?.matchId ?? "";
            setMatchId(id);
            setJoinMatchId(id);
            setMatchStarted(false);
            setLastError("");
            // players list will come via PLAYER_JOINED (if backend broadcasts on create)
            break;
          }

          case "PLAYER_JOINED": {
            const p = msg.payload as PlayerJoinedPayload;
            setMatchId(p.matchId);
            setPlayers(p.players ?? []);
            setLastError("");
            break;
          }

          case "READY_UPDATED": {
            const p = msg.payload as ReadyUpdatePayload;
            setMatchId(p.matchId);
            setPlayers(p.players ?? []);
            setLastError("");
            break;
          }

          case "MATCH_STARTED": {
            const p = msg.payload as MatchStartedPayload;
            setMatchId(p.matchId);
            if (p.players) setPlayers(p.players);
            setMatchStarted(true);
            setLastError("");
            break;
          }

          case "ERROR": {
            const p = msg.payload as ErrorPayload;
            setLastError(p?.message ?? "Unknown error");
            break;
          }

          default:
            break;
        }
      } catch {
        setLog((l) => [`<= ${e.data}`, ...l]);
      }
    };

    socket.onclose = () => setLog((l) => ["closed", ...l]);
    socket.onerror = () => setLog((l) => ["error (see console)", ...l]);

    return () => socket.close();
  }, [playerId]);

  function send(type: string, payload: any = {}) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      setLog((l) => [`!! ws not open (state=${ws?.readyState})`, ...l]);
      return;
    }
    ws.send(JSON.stringify({ type, payload }));
    setLog((l) => [`=> ${type} ${JSON.stringify(payload)}`, ...l]);
  }

  function createMatch() {
    setPlayers([]);
    setMatchId("");
    setMatchStarted(false);
    setLastError("");
    send("CREATE_MATCH", { playerId });
  }

  function joinMatch() {
    setLastError("");
    send("JOIN_MATCH", { matchId: joinMatchId, playerId });
  }

  function toggleReady() {
    if (!matchId) return;
    const next = !me?.ready;
    send("SET_READY", { matchId, playerId, ready: next });
  }

  function startMatch() {
    if (!matchId) return;
    send("START_MATCH", { matchId, playerId });
  }

  return (
    <main className="p-6 space-y-4">
      {/* Header */}
      <div className="rounded border p-4 space-y-2">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <div className="font-semibold">playerId</div>
            <div className="font-mono text-sm">{playerId || "(loading...)"}</div>
          </div>

          <div className="text-right">
            <div className="font-semibold">status</div>
            <div className="text-sm">
              {matchStarted ? (
                <span className="font-semibold">RUNNING</span>
              ) : matchId ? (
                <span className="font-semibold">LOBBY</span>
              ) : (
                <span className="text-neutral-500">not in match</span>
              )}
            </div>
            <div className="font-mono text-xs text-neutral-600">{matchId || ""}</div>
          </div>
        </div>

        {lastError && (
          <div className="rounded border border-red-300 bg-red-50 p-2 text-sm">
            <span className="font-semibold text-red-700">Error:</span> {lastError}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="rounded border p-4 space-y-3">
        <div className="font-semibold">Actions</div>

        <div className="flex gap-2 flex-wrap items-center">
          <button className="px-3 py-2 rounded bg-black text-white" onClick={() => send("PING", { playerId })}>
            Ping
          </button>

          <button className="px-3 py-2 rounded bg-blue-600 text-white" onClick={createMatch}>
            Create Match
          </button>

          <input
            className="px-3 py-2 rounded border w-[420px] max-w-full"
            placeholder="matchId to join"
            value={joinMatchId}
            onChange={(e) => setJoinMatchId(e.target.value ?? "")}
          />

          <button
            className="px-3 py-2 rounded bg-green-600 text-white disabled:opacity-50"
            onClick={joinMatch}
            disabled={!joinMatchId || matchStarted}
            title={matchStarted ? "Match already started" : ""}
          >
            Join Match
          </button>

          <button
            className="px-3 py-2 rounded bg-neutral-800 text-white disabled:opacity-50"
            onClick={toggleReady}
            disabled={!isInMatch || matchStarted}
            title={!isInMatch ? "Join a match first" : matchStarted ? "Already running" : ""}
          >
            {me?.ready ? "Unready" : "Ready"}
          </button>

          <button
            className="px-3 py-2 rounded bg-purple-600 text-white disabled:opacity-50"
            onClick={startMatch}
            disabled={!canStart}
            title={
              !isInMatch
                ? "Join a match first"
                : !isHost
                ? "Only host can start"
                : !allReady
                ? "Both players must be ready"
                : matchStarted
                ? "Already started"
                : ""
            }
          >
            Start Match (Host)
          </button>
        </div>

        <div className="text-xs text-neutral-600">
          Rules: Max 2 players. Both must be Ready. Only HOST can Start.
        </div>
      </div>

      {/* Lobby / Players */}
      <div className="rounded border p-4 space-y-3">
        <div className="font-semibold">Lobby</div>

        {!matchId ? (
          <div className="text-sm text-neutral-600">Create or join a match to see players.</div>
        ) : (
          <div className="space-y-2">
            <div className="text-sm">
              Players: <span className="font-semibold">{players.length}</span>/2{" "}
              {allReady && !matchStarted ? <span className="ml-2 font-semibold">✅ all ready</span> : null}
            </div>

            <div className="grid gap-2">
              {players.map((p) => (
                <div key={p.playerId} className="rounded border p-3 flex items-center justify-between">
                  <div>
                    <div className="font-mono text-sm">{p.playerId}</div>
                    <div className="text-xs text-neutral-600">
                      role: <span className="font-semibold">{p.role ?? "?"}</span>{" "}
                      {p.playerId === playerId ? <span className="ml-2">(you)</span> : null}
                    </div>
                  </div>

                  <div className="text-right">
                    <div className="text-sm font-semibold">{p.ready ? "READY ✅" : "NOT READY ⏳"}</div>
                  </div>
                </div>
              ))}

              {players.length === 0 ? <div className="text-sm text-neutral-600">(no players yet)</div> : null}
            </div>

            {matchStarted && (
              <div className="rounded border border-green-300 bg-green-50 p-2 text-sm">
                ✅ Match started! (You can now begin sending moves.)
              </div>
            )}
          </div>
        )}
      </div>

      {/* Logs */}
      <pre className="mt-2 whitespace-pre-wrap rounded border p-3 text-xs h-[380px] overflow-auto">
        {log.join("\n")}
      </pre>
    </main>
  );
}