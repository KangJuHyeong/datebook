import { spawnSync } from "node:child_process";
import { mkdir, readdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { chromium } from "@playwright/test";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(scriptDir, "..");
const artifactsDir = resolve(frontendDir, "e2e-artifacts");
const screenshotsDir = resolve(artifactsDir, "screenshots");
const markdownPath = resolve(artifactsDir, "E2E_TEST_REPORT.md");
const htmlPath = resolve(artifactsDir, "E2E_TEST_REPORT.html");
const pdfPath = resolve(artifactsDir, "E2E_TEST_REPORT.pdf");
const playwrightBin = resolve(
  frontendDir,
  "node_modules",
  ".bin",
  process.platform === "win32" ? "playwright.cmd" : "playwright",
);

await rm(artifactsDir, { recursive: true, force: true });
await mkdir(screenshotsDir, { recursive: true });

const result = spawnSync(playwrightBin, ["test"], {
  cwd: frontendDir,
  stdio: "inherit",
  shell: process.platform === "win32",
});

await generateReport(result.status ?? 1);

process.exit(result.status ?? 1);

async function generateReport(exitCode) {
  const summaryPath = resolve(artifactsDir, "flow-results.json");
  let summary = {
    status: exitCode === 0 ? "PASS" : "FAIL",
    generatedAt: new Date().toISOString(),
    steps: [],
    downloads: [],
  };

  try {
    summary = JSON.parse(await readFile(summaryPath, "utf8"));
  } catch {
    summary.status = exitCode === 0 ? "PASS" : "FAIL";
  }

  const screenshots = (await readdir(screenshotsDir).catch(() => []))
    .filter((file) => file.endsWith(".png"))
    .sort();

  const markdown = [
    "# MVP Playwright E2E Report",
    "",
    `- Status: ${summary.status}`,
    `- Generated at: ${summary.generatedAt}`,
    `- Frontend: http://127.0.0.1:3100`,
    `- Backend: http://127.0.0.1:18080`,
    `- Database: couple_diary_e2e`,
    "",
    "## Flow Steps",
    "",
    ...(summary.steps.length
      ? summary.steps.map((step) => `- ${step.status}: ${step.name}${step.screenshot ? ` (${step.screenshot})` : ""}`)
      : ["- No flow summary was produced. Check Playwright output above."]),
    "",
    "## Downloads",
    "",
    ...(summary.downloads.length
      ? summary.downloads.map((download) => `- ${download.format}: ${download.filename} (${download.status})`)
      : ["- No download summary was produced."]),
    "",
    "## Screenshots",
    "",
    ...screenshots.map((file) => `![${file}](screenshots/${file})`),
    "",
  ].join("\n");

  await writeFile(markdownPath, markdown, "utf8");
  await writeFile(htmlPath, renderHtml(summary, screenshots), "utf8");

  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto(pathToFileURL(htmlPath).href, { waitUntil: "networkidle" });
  await page.evaluate(async () => {
    await Promise.all(
      Array.from(document.images).map((image) => {
        if (image.complete) {
          return image.decode().catch(() => undefined);
        }

        return new Promise((resolve) => {
          image.addEventListener("load", resolve, { once: true });
          image.addEventListener("error", resolve, { once: true });
        }).then(() => image.decode().catch(() => undefined));
      }),
    );
    await document.fonts.ready;
  });
  await page.pdf({ path: pdfPath, format: "A4", printBackground: true });
  await browser.close();
}

function renderHtml(summary, screenshots) {
  const stepItems = summary.steps.length
    ? summary.steps.map((step) => `<li><strong>${escapeHtml(step.status)}</strong> ${escapeHtml(step.name)}</li>`).join("")
    : "<li>No flow summary was produced. Check Playwright output.</li>";
  const downloadItems = summary.downloads.length
    ? summary.downloads
        .map((download) => `<li><strong>${escapeHtml(download.format)}</strong> ${escapeHtml(download.filename)} ${escapeHtml(download.status)}</li>`)
        .join("")
    : "<li>No download summary was produced.</li>";
  const imageItems = screenshots
    .map((file) => `<section class="shot"><h3>${escapeHtml(file)}</h3><img src="screenshots/${encodeURIComponent(file)}" /></section>`)
    .join("");

  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <title>MVP Playwright E2E Report</title>
  <style>
    body { font-family: "Malgun Gothic", "Apple SD Gothic Neo", Arial, sans-serif; margin: 32px; color: #1c1917; }
    h1 { font-size: 24px; }
    h2 { margin-top: 28px; font-size: 18px; border-bottom: 1px solid #e7e5e4; padding-bottom: 8px; }
    ul { line-height: 1.7; }
    .meta { color: #57534e; line-height: 1.6; }
    .shot { page-break-inside: avoid; margin-top: 24px; }
    .shot h3 { font-size: 13px; color: #57534e; }
    img { width: 100%; border: 1px solid #d6d3d1; border-radius: 6px; }
  </style>
</head>
<body>
  <h1>MVP Playwright E2E Report</h1>
  <div class="meta">
    <div>Status: <strong>${escapeHtml(summary.status)}</strong></div>
    <div>Generated at: ${escapeHtml(summary.generatedAt)}</div>
    <div>Frontend: http://127.0.0.1:3100</div>
    <div>Backend: http://127.0.0.1:18080</div>
    <div>Database: couple_diary_e2e</div>
  </div>
  <h2>Flow Steps</h2>
  <ul>${stepItems}</ul>
  <h2>Downloads</h2>
  <ul>${downloadItems}</ul>
  <h2>Screenshots</h2>
  ${imageItems}
</body>
</html>`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
