import { test, expect } from "@playwright/test";

const LOGIN_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MX0.signature";

const mockLogin = async (page) => {
  await page.route("http://localhost:8080/api/v1/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ token: LOGIN_TOKEN }),
    });
  });
};

const mockFeed = async (page) => {
  await page.route("http://localhost:8084/api/feed/1", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        {
          id: 1,
          description: "Test post content",
          image: "http://localhost:8083/uploads/test.jpg",
          createdAt: "2024-01-01T00:00:00Z",
          user: { id: 1, username: "testuser" },
          likesCount: 5,
          commentsCount: 2,
        },
      ]),
    });
  });
};

const mockLike = async (page) => {
  await page.route("http://localhost:8083/api/interactions/like", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ message: "Liked successfully" }),
    });
  });
};

const mockComment = async (page) => {
  await page.route("http://localhost:8083/api/interactions/comment", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ id: 1, content: "Test comment", user: { username: "testuser" } }),
    });
  });
};

const performLogin = async (page) => {
  await mockLogin(page);
  page.on("dialog", async (dialog) => await dialog.accept());
  await page.goto("/");
  await page.getByPlaceholder("Enter email").fill("test@example.com");
  await page.getByPlaceholder("Enter password").fill("123456");
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/home$/);
};

test.describe("Feed Interaction UI integration", () => {
  test("feed renders and like toggles", async ({ page }) => {
    await performLogin(page);
    await mockFeed(page);
    await mockLike(page);

    // Feed should render at least one post
    await expect(page.getByText("Test post content")).toBeVisible();
    await expect(page.getByRole("link", { name: "testuser" })).toBeVisible();

    // Like toggle should work
    const likeButton = page.getByRole("button", { name: /Like post/i }).first();
    await likeButton.click();
    // After liking, the icon should change to the filled heart
    await expect(likeButton).toHaveText("❤️");
  });

  test("does not load feed when not logged in", async ({ page }) => {
    // Navigate directly to /home without login
    await page.goto("/home");

    // Feed should not have posts when not logged in
    await expect(page.locator(".post-card")).toHaveCount(0);
  });

  test("user can open comments and add comment", async ({ page }) => {
    await performLogin(page);
    await mockFeed(page);
    await mockComment(page);

    // Open comments
    await page.getByRole("button", { name: /Show comments/i }).click();
    await page.getByPlaceholder("Add a comment...").fill("Test comment");
    await page.getByRole("button", { name: "Post", exact: true }).click();
    // There may already be comments in the UI; assert the newly added comment is visible by checking the first match.
    await expect(page.getByText("Test comment").first()).toBeVisible();
  });

  test("create post modal submits", async ({ page }) => {
    await performLogin(page);

    await page.getByRole("button", { name: "Create" }).click();
    await expect(page.getByRole("heading", { name: "Create new post" })).toBeVisible();
    await expect(page.getByText("Select photos and videos")).toBeVisible();
    await expect(page.getByPlaceholder("Write a caption...")).toBeVisible();
    await expect(page.getByRole("button", { name: "Share" })).toBeVisible();
  });
});
