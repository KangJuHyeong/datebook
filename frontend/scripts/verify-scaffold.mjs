import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const requiredPaths = [
  "src/app/layout.tsx",
  "src/app/page.tsx",
  "src/app/globals.css",
  "src/components",
  "src/features",
  "src/lib/api/client.ts",
  "src/types",
  "next.config.ts",
  "tailwind.config.ts",
  "postcss.config.js",
  "tsconfig.json",
];

const missing = requiredPaths.filter((path) => !existsSync(join(process.cwd(), path)));

if (missing.length > 0) {
  console.error(`Missing scaffold paths: ${missing.join(", ")}`);
  process.exit(1);
}

const packageJson = JSON.parse(readFileSync(join(process.cwd(), "package.json"), "utf8"));
const dependencies = packageJson.dependencies ?? {};

for (const dependency of ["next", "react", "react-dom"]) {
  if (!dependencies[dependency]) {
    console.error(`Missing frontend dependency: ${dependency}`);
    process.exit(1);
  }
}

const apiClient = readFileSync(join(process.cwd(), "src/lib/api/client.ts"), "utf8");
if (!apiClient.includes('credentials: "include"')) {
  console.error("API client must include session credentials.");
  process.exit(1);
}

const globals = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");
if (!globals.includes("@tailwind base") || !globals.includes("bg-stone-50")) {
  console.error("Tailwind globals must include base directives and the stone page background.");
  process.exit(1);
}

console.log(`frontend scaffold ${process.argv[2] ?? "check"} passed`);
