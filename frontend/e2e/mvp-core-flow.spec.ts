import { expect, test, type Page } from "@playwright/test";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const artifactsDir = resolve(process.cwd(), "e2e-artifacts");
const screenshotsDir = resolve(artifactsDir, "screenshots");

type FlowStep = {
  name: string;
  status: "PASS" | "FAIL";
  screenshot?: string;
};

type DownloadSummary = {
  format: "json" | "text";
  filename: string;
  status: "PASS" | "FAIL";
};

const steps: FlowStep[] = [];
const downloads: DownloadSummary[] = [];

test.afterEach(async ({}, testInfo) => {
  const status = testInfo.status === testInfo.expectedStatus ? "PASS" : "FAIL";
  await writeFile(
    resolve(artifactsDir, "flow-results.json"),
    JSON.stringify(
      {
        status,
        generatedAt: new Date().toISOString(),
        steps,
        downloads,
      },
      null,
      2,
    ),
    "utf8",
  );
});

test("MVP core flow with screenshots and downloads", async ({ browser }) => {
  await mkdir(screenshotsDir, { recursive: true });

  const runId = Date.now();
  const password = "password123";
  const userA = {
    email: `e2e-a-${runId}@example.com`,
    displayName: "E2E A",
    answer: `A answer ${runId}`,
  };
  const userB = {
    email: `e2e-b-${runId}@example.com`,
    displayName: "E2E B",
    answer: `B answer ${runId}`,
  };

  const contextA = await browser.newContext({ acceptDownloads: true });
  const contextB = await browser.newContext({ acceptDownloads: true });
  const pageA = await contextA.newPage();
  const pageB = await contextB.newPage();

  try {
    await signup(pageA, userA.email, password, userA.displayName);
    await expect(pageA).toHaveURL(/\/couple$/);
    await capture(pageA, "01-signup-user-a", "User A signup reaches couple connection");

    await contextA.request.post("http://127.0.0.1:18080/api/auth/logout");
    await pageA.goto("/login");
    await login(pageA, userA.email, password);
    await expect(pageA).toHaveURL(/\/couple$/);
    await capture(pageA, "02-login-user-a", "User A login returns to couple connection");

    await pageA.getByTestId("create-invite").click();
    await expect(pageA.getByTestId("invite-code-value")).toBeVisible();
    const inviteCode = (await pageA.getByTestId("invite-code-value").innerText()).trim();
    expect(inviteCode.length).toBeGreaterThanOrEqual(6);
    await capture(pageA, "03-create-invite", "User A creates invite code");

    await signup(pageB, userB.email, password, userB.displayName);
    await expect(pageB).toHaveURL(/\/couple$/);
    await capture(pageB, "04-signup-user-b", "User B signup reaches couple connection");

    await pageB.getByTestId("invite-code-input").fill(inviteCode);
    await pageB.getByTestId("join-invite").click();
    await expect(pageB).toHaveURL(/\/today$/);
    await capture(pageB, "05-join-invite", "User B joins with invite code");

    await answerToday(pageB, userB.answer);
    await capture(pageB, "06-answer-user-b", "User B answers today's question");

    await pageA.goto("/today");
    await expect(pageA.getByTestId("today-answer")).toBeVisible();
    await expect(pageA.getByText(userB.answer)).toHaveCount(0);
    await capture(pageA, "07-answer-user-a-locked-state", "User A sees partner answer locked before answering");

    await answerToday(pageA, userA.answer);
    await expect(pageA.getByText(userB.answer)).toBeVisible();
    await capture(pageA, "08-both-answers-revealed", "Both answers are revealed after User A answers");

    await pageA.goto("/diary");
    await expect(pageA.locator("article")).toHaveCount(1);
    await capture(pageA, "09-diary", "Diary shows today's completed entry");

    await pageA.goto("/export");
    const firstExportableEntry = pageA.locator('[data-testid^="export-entry-"][aria-disabled="false"]').first();
    await expect(firstExportableEntry).toBeVisible();
    await firstExportableEntry.click();
    await capture(pageA, "10-export-select", "Export flow selects an exportable diary entry");

    await pageA.getByTestId("create-export-order").click();
    await expect(pageA.getByText(userA.answer)).toBeVisible();
    await expect(pageA.getByText(userB.answer)).toBeVisible();
    await capture(pageA, "11-export-preview", "Export preview shows both answers");

    await pageA.getByTestId("complete-export-order").click();
    await expect(pageA.getByTestId("download-json")).toBeVisible();
    await expect(pageA.getByTestId("download-text")).toBeVisible();
    await capture(pageA, "12-export-completed", "Export order is completed with download buttons");

    await verifyDownload(pageA, "json", userA.answer, userB.answer);
    await verifyDownload(pageA, "text", userA.answer, userB.answer);
  } finally {
    await contextA.close();
    await contextB.close();
  }
});

async function signup(page: Page, email: string, password: string, displayName: string) {
  await page.goto("/signup");
  await page.locator('input[name="displayName"]').fill(displayName);
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="password"]').fill(password);
  await page.getByTestId("signup-submit").click();
}

async function login(page: Page, email: string, password: string) {
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="password"]').fill(password);
  await page.getByTestId("login-submit").click();
}

async function answerToday(page: Page, answer: string) {
  await expect(page.getByTestId("today-answer")).toBeVisible();
  await page.getByTestId("today-answer").fill(answer);
  await page.getByTestId("save-answer").click();
  await expect(page.getByTestId("today-answer")).toHaveValue(answer);
}

async function capture(page: Page, slug: string, name: string) {
  const filename = `${slug}.png`;
  await page.screenshot({ path: resolve(screenshotsDir, filename), fullPage: true });
  steps.push({ name, status: "PASS", screenshot: `screenshots/${filename}` });
}

async function verifyDownload(page: Page, format: "json" | "text", answerA: string, answerB: string) {
  const button = page.getByTestId(format === "json" ? "download-json" : "download-text");
  const [download] = await Promise.all([page.waitForEvent("download"), button.click()]);
  const filename = download.suggestedFilename();
  const content = await download.createReadStream().then(
    (stream) =>
      new Promise<string>((resolvePromise, rejectPromise) => {
        const chunks: Buffer[] = [];
        stream.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
        stream.on("end", () => resolvePromise(Buffer.concat(chunks).toString("utf8")));
        stream.on("error", rejectPromise);
      }),
  );

  expect(filename).toMatch(format === "json" ? /^couple-diary-\d+\.json$/ : /^couple-diary-\d+\.txt$/);
  expect(content).toContain(answerA);
  expect(content).toContain(answerB);
  if (format === "json") {
    expect(JSON.parse(content).entries.length).toBeGreaterThan(0);
  }

  downloads.push({ format, filename, status: "PASS" });
}
