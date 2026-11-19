import { execFileSync } from "node:child_process";

const modo = { ...process.env, NEXT_PUBLIC_MODO_DEMO: "navegador", NEXT_TELEMETRY_DISABLED: "1" };
execFileSync(process.execPath, ["node_modules/next/dist/bin/next", "build"], { env: modo, stdio: "inherit" });
execFileSync(process.execPath, ["scripts/huellas-csp.mjs", "--meta"], { stdio: "inherit" });
