import { test, expect } from "@playwright/test";

test.describe("Auth UI integration", () => {
  test("login page opens and can navigate to register", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("heading", { name: "Instagram" })).toBeVisible();
    await page.getByRole("button", { name: "Create a new account" }).click();

    await expect(page).toHaveURL(/\/register$/);
    await expect(page.getByRole("heading", { name: "Create an Instagram account" })).toBeVisible();
  });

  test("login form shows validation error for invalid email", async ({ page }) => {
    await page.goto("/");

    await page.getByPlaceholder("Enter email").fill("invalid-email");
    await page.getByPlaceholder("Enter password").fill("123456");
    await page.getByRole("button", { name: "Log in" }).click();

    await expect(page.getByText("Invalid email format")).toBeVisible();
  });

  test("successful login redirects to home", async ({ page }) => {
    page.on("dialog", async (dialog) => {
      await dialog.accept();
    });

    await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MX0.signature",
        }),
      });
    });

    await page.goto("/");

    await page.getByPlaceholder("Enter email").fill("test@example.com");
    await page.getByPlaceholder("Enter password").fill("123456");
    await page.getByRole("button", { name: "Log in" }).click();

    await expect(page).toHaveURL(/\/home$/);
  });
});
