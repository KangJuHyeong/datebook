import { spawn, spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(scriptDir, "..");
const projectRoot = resolve(frontendDir, "..");
const backendDir = resolve(projectRoot, "backend");

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: projectRoot,
    encoding: "utf8",
    stdio: "inherit",
    ...options,
  });

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

function waitForMysql() {
  const deadline = Date.now() + 90_000;

  while (Date.now() < deadline) {
    const result = spawnSync("docker", ["exec", "couple-diary-mysql", "mysqladmin", "ping", "-uroot", "-proot"], {
      cwd: projectRoot,
      encoding: "utf8",
      stdio: "ignore",
    });

    if (result.status === 0) {
      return;
    }

    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000);
  }

  console.error("Timed out waiting for MySQL container.");
  process.exit(1);
}

run("docker", ["compose", "up", "-d", "mysql"]);
waitForMysql();
run("docker", [
  "exec",
  "couple-diary-mysql",
  "mysql",
  "-uroot",
  "-proot",
  "-e",
  "DROP DATABASE IF EXISTS couple_diary_e2e; CREATE DATABASE couple_diary_e2e CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON couple_diary_e2e.* TO 'couple_diary'@'%'; FLUSH PRIVILEGES;",
]);

const gradleCommand = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
const child = spawn(gradleCommand, ["run"], {
  cwd: backendDir,
  shell: process.platform === "win32",
  stdio: "inherit",
  env: {
    ...process.env,
    DB_HOST: "127.0.0.1",
    DB_PORT: "3307",
    DB_NAME: "couple_diary_e2e",
    DB_USERNAME: "couple_diary",
    DB_PASSWORD: "couple_diary",
    DB_TIMEZONE: "UTC",
    SERVER_PORT: "18080",
    CORS_ALLOWED_ORIGIN: "http://127.0.0.1:3100",
  },
});

function stop() {
  child.kill();
}

process.on("SIGINT", stop);
process.on("SIGTERM", stop);
child.on("exit", (code) => process.exit(code ?? 0));
