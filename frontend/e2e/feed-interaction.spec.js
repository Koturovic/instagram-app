import { test, expect } from "@playwright/test";

test.describe("Feed Interaction UI integration", () => {
  test("user can view feed and interact with posts", async ({ page }) => {
    // Mock login
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

    // Mock feed data
    await page.route("http://localhost:8082/api/v1/posts/feed", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: 1,
            content: "Test post content",
            mediaUrls: ["http://localhost:8083/uploads/test.jpg"],
            createdAt: "2024-01-01T00:00:00Z",
            user: { id: 1, username: "testuser" },
            likesCount: 5,
            commentsCount: 2,
          },
        ]),
      });
    });

    // Login first
    await page.goto("/");
    await page.getByPlaceholder("Enter email").fill("test@example.com");
    await page.getByPlaceholder("Enter password").fill("123456");
    await page.getByRole("button", { name: "Log in" }).click();

    // Navigate to home
    await expect(page).toHaveURL(/\/home$/);

    // Check feed loads
    await expect(page.getByText("Test post content")).toBeVisible();
    await expect(page.getByText("testuser")).toBeVisible();

    // Test like functionality
    await page.route("http://localhost:8081/api/v1/interactions/like", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ message: "Liked successfully" }),
      });
    });

    await page.getByRole("button", { name: "Like" }).first().click();
    await expect(page.getByText("Liked successfully")).toBeVisible();

    // Test comment functionality
    await page.route("http://localhost:8081/api/v1/interactions/comment", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: 1,
          content: "Test comment",
          user: { username: "testuser" },
        }),
      });
    });

    await page.getByPlaceholder("Add a comment...").fill("Test comment");
    await page.getByRole("button", { name: "Post" }).click();
    await expect(page.getByText("Test comment")).toBeVisible();
  });

  test("user can create a new post", async ({ page }) => {
    // Mock login
    await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MX0.signature",
        }),
      });
    });

    // Mock post creation
    await page.route("http://localhost:8082/api/v1/posts", async (route) => {
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({
          id: 2,
          content: "New post content",
          mediaUrls: [],
          createdAt: "2024-01-01T00:00:00Z",
          user: { id: 1, username: "testuser" },
        }),
      });
    });

    // Login
    await page.goto("/");
    await page.getByPlaceholder("Enter email").fill("test@example.com");
    await page.getByPlaceholder("Enter password").fill("123456");
    await page.getByRole("button", { name: "Log in" }).click();

    // Go to create post
    await page.getByRole("button", { name: "Create Post" }).click();
    await expect(page).toHaveURL(/\/create$/);

    // Fill post form
    await page.getByPlaceholder("What's on your mind?").fill("New post content");
    await page.getByRole("button", { name: "Share" }).click();

    // Should redirect to home
    await expect(page).toHaveURL(/\/home$/);
    await expect(page.getByText("New post content")).toBeVisible();
  });
});
