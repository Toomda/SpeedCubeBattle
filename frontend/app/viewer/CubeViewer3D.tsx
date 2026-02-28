"use client";

import * as THREE from "three";
import { Canvas } from "@react-three/fiber";
import { OrbitControls } from "@react-three/drei";
import React, { useEffect, useMemo, useRef } from "react";

type Props = {
  facelets: string; // 54 chars in order: U D L R F B (each 9)
};

const COLORS: Record<string, string> = {
  W: "#ffffff",
  Y: "#ffd500",
  O: "#ff8c00",
  R: "#d30000",
  G: "#00a651",
  B: "#0057b7",
  ".": "#111111",
};

function splitFaces(facelets: string) {
  const safe = (facelets ?? "").padEnd(54, ".").slice(0, 54);
  return {
    U: safe.slice(0, 9),
    D: safe.slice(9, 18),
    L: safe.slice(18, 27),
    R: safe.slice(27, 36),
    F: safe.slice(36, 45),
    B: safe.slice(45, 54),
  };
}

function StickersPlane({
  face,
  chars,
  position,
  rotation,
}: {
  face: string;
  chars: string;
  position: [number, number, number];
  rotation: [number, number, number];
}) {
const stickers = useMemo(() => {
  return chars.padEnd(9, ".").slice(0,9).split("");
}, [chars]);

  const size = 1.0;

  // ✅ control seams better:
  const outerBorder = 0.01;   // thin border around the whole face
  const innerGap = 0.02;      // gap between stickers

  const cell = (size - 2 * outerBorder - 2 * innerGap) / 3; // 3 cells + 2 gaps
  const step = cell + innerGap;

  // base plate slightly larger to hide cracks between faces
  const baseSize = 1.002;

  // ✅ Unlit stickers so they never go "black" due to lighting
const mats = useMemo(() => {
  const map: Record<string, THREE.MeshBasicMaterial> = {};
  for (const k of Object.keys(COLORS)) {
    map[k] = new THREE.MeshBasicMaterial({
      color: COLORS[k],
      side: THREE.DoubleSide,
      polygonOffset: true,
      polygonOffsetFactor: -2,  // pull closer
      polygonOffsetUnits: -2,
    });
  }
  return map;
}, []);

  const baseMat = useMemo(
  () =>
    new THREE.MeshStandardMaterial({
      color: "#0b0b0b",
      side: THREE.DoubleSide,
      roughness: 1,
      metalness: 0,
      polygonOffset: true,
      polygonOffsetFactor: 1,   // push away
      polygonOffsetUnits: 1,
    }),
  []
);

  return (
    <group position={position} rotation={rotation}>
      {/* base plate */}
      <mesh>
        <planeGeometry args={[baseSize, baseSize]} />
        <primitive object={baseMat} attach="material" />
      </mesh>

      {stickers.map((c, i) => {
        const row = Math.floor(i / 3);
        const col = i % 3;

        const x = (col - 1) * step;
        const y = (1 - row) * step;

        return (
          <mesh key={`${face}-${i}`} position={[x, y, 0.002]}>
            <planeGeometry args={[cell, cell]} />
            <primitive object={mats[c] ?? mats["."]} attach="material" />
          </mesh>
        );
      })}
    </group>
  );
}

function Face({ face, chars, position, normal }) {
  const ref = useRef()

 useEffect(() => {
  if (!ref.current) return;

  const target = new THREE.Vector3(
    position[0] + normal[0],
    position[1] + normal[1],
    position[2] + normal[2]
  );

  ref.current.lookAt(target);
}, [position, normal]);

  return (
    <group ref={ref} position={position}>
      <StickersPlane face={face} chars={chars} />
    </group>
  )
}

function Cube({ facelets }) {
  const faces = splitFaces(facelets)
  const d = 0.5

  return (
    <group>

      <Face face="F" chars={faces.F} position={[0,0,d]} normal={[0,0,1]} />
      <Face face="B" chars={faces.B} position={[0,0,-d]} normal={[0,0,-1]} />
      <Face face="U" chars={faces.U} position={[0,d,0]} normal={[0,1,0]} />
      <Face face="D" chars={faces.D} position={[0,-d,0]} normal={[0,-1,0]} />
      <Face face="R" chars={faces.R} position={[d,0,0]} normal={[1,0,0]} />
      <Face face="L" chars={faces.L} position={[-d,0,0]} normal={[-1,0,0]} />

    </group>
  )
}

export default function CubeViewer3D({ facelets }: Props) {
  return (
    <Canvas camera={{ position: [2.2, 1.8, 2.2], fov: 45, near: 0.1, far: 100 }}>
      <color attach="background" args={["#0a0a0a"]} />

      {/* ✅ makes the cube readable from all angles */}
      <hemisphereLight intensity={0.9} groundColor={"#111"} />
      <ambientLight intensity={0.4} />
      <directionalLight position={[4, 6, 3]} intensity={1.0} />
      <directionalLight position={[-4, 2, -3]} intensity={0.9} />

      <Cube facelets={facelets} />

      <OrbitControls enablePan={false} enableDamping dampingFactor={0.08} />
    </Canvas>
  );
}