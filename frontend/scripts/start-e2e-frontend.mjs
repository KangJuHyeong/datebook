import { spawn } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(scriptDir, "..");
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";

const child = spawn(npmCommand, ["run", "dev", "--", "--hostname", "127.0.0.1", "--port", "3100"], {
  cwd: frontendDir,
  shell: process.platform === "win32",
  stdio: "inherit",
  env: {
    ...process.env,
    NEXT_PUBLIC_API_BASE_URL: "http://127.0.0.1:18080",
  },
});

function stop() {
  child.kill();
}

process.on("SIGINT", stop);
process.on("SIGTERM", stop);
child.on("exit", (code) => process.exit(code ?? 0));
