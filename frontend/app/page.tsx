"use client";

import dynamic from "next/dynamic";
import { useEffect, useMemo, useState } from "react";

// --- types ---
type WsEnvelope<T = any> = { type: string; payload?: T };
type ErrorPayload = { message: string; errorType?: string };

type PlayerInfo = { playerId: string; ready: boolean; role?: "HOST" | "GUEST" };

type MatchCreatedPayload = { matchId: string };
type PlayerJoinedPayload = { matchId: string; joinedPlayerId: string; players: PlayerInfo[] };
type ReadyUpdatePayload = { matchId: string; changedPlayerId: string; ready: boolean; players: PlayerInfo[] };
type MatchStartedPayload = {
  matchId: string;
  startedAt?: number;
  scrambleSeed?: number;
  scramble?: string[];
  players?: PlayerInfo[];
};
type PlayerLeftPayload = { matchId: string; leftPlayerId: string; players: PlayerInfo[] };

// Moves
type MoveAppliedPayload = { matchId: string; playerId: string; move: string; seq: number; serverTs: number };

// Cube state
type CubeStatePayload = {
  matchId: string;
  playerId: string;
  facelets: string; // 54 chars
  moveCount: number;
  solved: boolean;
};

function genId() {
  return Math.random().toString(16).slice(2) + "-" + Date.now().toString(16);
}

const MOVE_PRESETS = ["R", "R'", "L", "L'", "U", "U'", "D", "D'", "F", "F'", "B", "B'"];

// --- 3D viewer (dynamic, no SSR) ---
const CubeViewer3D = dynamic(() => import("./viewer/CubeViewer3D"), { ssr: false });

export default function Home() {
  const [playerId, setPlayerId] = useState<string>("");
  const [ws, setWs] = useState<WebSocket | null>(null);

  const [log, setLog] = useState<string[]>([]);
  const [joinMatchId, setJoinMatchId] = useState<string>("");

  const [matchId, setMatchId] = useState<string>("");
  const [players, setPlayers] = useState<PlayerInfo[]>([]);
  const [matchStarted, setMatchStarted] = useState<boolean>(false);
  const [lastError, setLastError] = useState<string>("");

  // match meta
  const [scramble, setScramble] = useState<string[]>([]);
  const [scrambleSeed, setScrambleSeed] = useState<number | null>(null);

  // moves log
  const [moveInput, setMoveInput] = useState<string>("R");
  const [moves, setMoves] = useState<MoveAppliedPayload[]>([]);

  // my cube from backend
  const [myFacelets, setMyFacelets] = useState<string>(""); // 54 chars
  const [myMoveCount, setMyMoveCount] = useState<number>(0);
  const [mySolved, setMySolved] = useState<boolean>(false);

  const me = useMemo(() => players.find((p) => p.playerId === playerId), [players, playerId]);
  const isHost = me?.role === "HOST";
  const isInMatch = !!matchId && players.some((p) => p.playerId === playerId);
  const allReady = players.length === 2 && players.every((p) => p.ready);
  const canStart = isInMatch && isHost && allReady && !matchStarted;
  const canSendMove = isInMatch && matchStarted;

  // Generate stable playerId per tab
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
        setLog((l) => [`<= ${msg.type} ${JSON.stringify(msg.payload ?? {})}`, ...l]);

        switch (msg.type) {
          case "LOBBY_MATCH_CREATED": {
            const p = msg.payload as MatchCreatedPayload;
            const id = p?.matchId ?? "";
            setMatchId(id);
            setJoinMatchId(id);
            setMatchStarted(false);
            setMoves([]);
            setPlayers([]);
            setLastError("");
            setScramble([]);
            setScrambleSeed(null);
            setMyFacelets("");
            setMyMoveCount(0);
            setMySolved(false);
            break;
          }

          case "LOBBY_PLAYER_JOINED": {
            const p = msg.payload as PlayerJoinedPayload;
            setMatchId(p.matchId);
            setPlayers(p.players ?? []);
            setLastError("");
            break;
          }

          case "LOBBY_LEFT_MATCH": {
            const p = msg.payload as PlayerLeftPayload;
            setMatchId(p.matchId);
            setPlayers(p.players ?? []);
            setLastError("");
            if (p.leftPlayerId === playerId) {
              setMatchId("");
              setPlayers([]);
              setMatchStarted(false);
              setMoves([]);
              setScramble([]);
              setScrambleSeed(null);
              setMyFacelets("");
              setMyMoveCount(0);
              setMySolved(false);
            }
            break;
          }

          case "LOBBY_READY_UPDATED": {
            const p = msg.payload as ReadyUpdatePayload;
            setMatchId(p.matchId);
            setPlayers(p.players ?? []);
            setLastError("");
            break;
          }

          case "GAME_MATCH_STARTED": {
            const p = msg.payload as MatchStartedPayload;
            setMatchId(p.matchId);
            if (p.players) setPlayers(p.players);
            setMatchStarted(true);
            setLastError("");
            setScramble(p.scramble ?? []);
            setScrambleSeed(p.scrambleSeed ?? null);
            // Cube state kommt direkt danach per GAME_CUBE_STATE
            break;
          }

          case "GAME_MOVE_APPLIED": {
            const p = msg.payload as MoveAppliedPayload;
            setMoves((m) => {
              const next = [...m, p];
              next.sort((a, b) => a.seq - b.seq);
              return next;
            });
            setLastError("");
            break;
          }

          case "GAME_CUBE_STATE": {
            const p = msg.payload as CubeStatePayload;
            // nur “mein” cube state anzeigen
            if (p.playerId === playerId) {
              setMyFacelets(p.facelets);
              setMyMoveCount(p.moveCount);
              setMySolved(p.solved);
            }
            setLastError("");
            break;
          }

          case "SYS_ERROR": {
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
    setMoves([]);
    setLastError("");
    setScramble([]);
    setScrambleSeed(null);
    setMyFacelets("");
    setMyMoveCount(0);
    setMySolved(false);
    send("LOBBY_CREATE_MATCH", { playerId });
  }

  function joinMatch() {
    setLastError("");
    send("LOBBY_JOIN_MATCH", { matchId: joinMatchId, playerId });
  }

  function toggleReady() {
    if (!matchId) return;
    const next = !me?.ready;
    send("LOBBY_SET_READY", { matchId, playerId, ready: next });
  }

  function startMatch() {
    if (!matchId) return;
    send("LOBBY_START_MATCH", { matchId });
  }

  function submitMove(move: string) {
    if (!matchId) return;
    send("GAME_SUBMIT_MOVE", { matchId, move });
  }

  function submitMoveFromInput() {
    const m = moveInput.trim();
    if (!m) return;
    submitMove(m);
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
          >
            Join Match
          </button>

          <button
            className="px-3 py-2 rounded bg-neutral-800 text-white disabled:opacity-50"
            onClick={toggleReady}
            disabled={!isInMatch || matchStarted}
          >
            {me?.ready ? "Unready" : "Ready"}
          </button>

          <button className="px-3 py-2 rounded bg-purple-600 text-white disabled:opacity-50" onClick={startMatch} disabled={!canStart}>
            Start Match (Host)
          </button>
        </div>

        <div className="text-xs text-neutral-600">Rules: Max 2 players. Both must be Ready. Only HOST can Start.</div>
      </div>

      {/* Lobby */}
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
                ✅ Match started! Your cube state is driven by the backend.
              </div>
            )}
          </div>
        )}
      </div>

      {/* Scramble + Cube */}
      <div className="rounded border p-4 space-y-3">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="font-semibold">Cube (3D)</div>
          <div className="text-xs text-neutral-600">
            drag to rotate • scroll to zoom • backend state • moves: <span className="font-mono">{myMoveCount}</span>{" "}
            {mySolved ? <span className="ml-2 font-semibold">✅ SOLVED</span> : null}
          </div>
        </div>

        <div className="text-sm text-neutral-700">
          <span className="font-semibold">Scramble:</span>{" "}
          <span className="font-mono">{scramble.length ? scramble.join(" ") : "-"}</span>{" "}
          {scrambleSeed != null ? <span className="text-xs text-neutral-500">(seed={scrambleSeed})</span> : null}
        </div>

        <div className="rounded border overflow-hidden h-[420px]">
          <CubeViewer3D facelets={myFacelets} />
        </div>

        <div className="text-xs text-neutral-600">
          Hinweis: aktuell keine Move-Animation – wir repainten einfach die Sticker. Später können wir echte Cubie-Rotation animieren.
        </div>
      </div>

      {/* Moves */}
      <div className="rounded border p-4 space-y-3">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="font-semibold">Moves</div>
          <div className="text-xs text-neutral-600">{canSendMove ? "RUNNING" : "Start the match to enable move sending."}</div>
        </div>

        <div className="flex gap-2 flex-wrap items-center">
          <select className="px-3 py-2 rounded border" value={moveInput} onChange={(e) => setMoveInput(e.target.value)} disabled={!canSendMove}>
            {MOVE_PRESETS.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>

          <button className="px-3 py-2 rounded bg-indigo-600 text-white disabled:opacity-50" onClick={submitMoveFromInput} disabled={!canSendMove}>
            Send Move
          </button>

          <div className="text-xs text-neutral-600">
            Du hast keine 2er Moves im UI → einfach zweimal klicken für z.B. <span className="font-mono">R2</span>.
          </div>
        </div>

        <div className="grid gap-2">
          {moves.length === 0 ? (
            <div className="text-sm text-neutral-600">(no moves yet)</div>
          ) : (
            moves
              .slice()
              .sort((a, b) => a.seq - b.seq)
              .slice(-20)
              .map((m) => (
                <div key={`${m.seq}-${m.playerId}`} className="rounded border p-2 flex items-center justify-between">
                  <div className="font-mono text-sm">
                    #{m.seq} <span className="font-semibold">{m.move}</span>
                  </div>
                  <div className="text-xs text-neutral-600">
                    by <span className="font-mono">{m.playerId}</span> @ {new Date(m.serverTs).toLocaleTimeString()}
                  </div>
                </div>
              ))
          )}
        </div>
      </div>

      {/* Logs */}
      <pre className="mt-2 whitespace-pre-wrap rounded border p-3 text-xs h-[320px] overflow-auto">{log.join("\n")}</pre>
    </main>
  );
}