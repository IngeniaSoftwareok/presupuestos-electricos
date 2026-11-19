import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  testMatch: "**/*.spec.ts",
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  use: {
    baseURL: process.env.DEMO_URL ?? "http://localhost:8083",
    channel: process.env.DEMO_BROWSER,
    trace: "retain-on-failure",
  },
});
