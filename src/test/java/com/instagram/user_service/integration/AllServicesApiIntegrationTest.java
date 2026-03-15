package com.instagram.user_service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API integracioni testovi za sve servise (auth, user, post, interaction, feed).
 * Koristi Java HttpClient (bez RestAssured) da izbegne proxy NPE na Windows/corporate okruženju.
 * Pokretanje: mvn test -Dtest=AllServicesApiIntegrationTest -DexcludedGroups=
 * Pre toga: docker compose up -d u instagram-app.
 */
@Tag("integration")
@DisplayName("API integracioni testovi - svi servisi")
class AllServicesApiIntegrationTest {

    private static String authBaseUrl;
    private static String userBaseUrl;
    private static String postBaseUrl;
    private static String interactionBaseUrl;
    private static String feedBaseUrl;

    private static String token;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void initBaseUrls() {
        authBaseUrl = System.getProperty("AUTH_SERVICE_URL", System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8080"));
        userBaseUrl = System.getProperty("USER_SERVICE_URL", System.getenv().getOrDefault("USER_SERVICE_URL", "http://localhost:8081"));
        postBaseUrl = System.getProperty("POST_SERVICE_URL", System.getenv().getOrDefault("POST_SERVICE_URL", "http://localhost:8082"));
        interactionBaseUrl = System.getProperty("INTERACTION_SERVICE_URL", System.getenv().getOrDefault("INTERACTION_SERVICE_URL", "http://localhost:8083"));
        feedBaseUrl = System.getProperty("FEED_SERVICE_URL", System.getenv().getOrDefault("FEED_SERVICE_URL", "http://localhost:8084"));
    }

    private static int request(String method, String url, String authToken, String body) throws Exception {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15));
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        if (body != null && !body.isEmpty()) {
            builder.header("Content-Type", "application/json");
        }
        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException(method);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (method.equals("POST") && response.statusCode() == 200 && url.contains("/auth/") && response.body() != null && !response.body().isEmpty()) {
            try {
                JsonNode node = objectMapper.readTree(response.body());
                if (node.has("token") && !node.get("token").isNull()) {
                    token = node.get("token").asText();
                }
            } catch (Exception ignored) { }
        }
        return response.statusCode();
    }

    private static int post(String baseUrl, String path, String authToken, String jsonBody) throws Exception {
        return request("POST", baseUrl + path, authToken, jsonBody);
    }

    private static int get(String baseUrl, String path, String authToken) throws Exception {
        return request("GET", baseUrl + path, authToken, null);
    }

    private static int delete(String baseUrl, String path, String authToken) throws Exception {
        return request("DELETE", baseUrl + path, authToken, null);
    }

    // ---------- AUTH SERVICE ----------

    @Test
    @DisplayName("Auth: registracija vraća 200 i token")
    void auth_register_returnsOkAndToken() throws Exception {
        String username = "integration_user_" + System.currentTimeMillis();
        String email = username + "@integration.test";
        String body = String.format("""
            {"firstName":"Integration","lastName":"Test","username":"%s","email":"%s","password":"password123"}
            """, username, email);
        int status = post(authBaseUrl, "/api/v1/auth/register", null, body);
        assertEquals(200, status);
        assertNotNull(token);
    }

    @Test
    @DisplayName("Auth: login vraća 200 i token")
    void auth_login_returnsOkAndToken() throws Exception {
        String username = "login_test_" + System.currentTimeMillis();
        String email = username + "@test.local";
        post(authBaseUrl, "/api/v1/auth/register", null,
                String.format("{\"firstName\":\"A\",\"lastName\":\"B\",\"username\":\"%s\",\"email\":\"%s\",\"password\":\"password123\"}", username, email));
        token = null;
        String loginBody = String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email);
        int status = post(authBaseUrl, "/api/v1/auth/login", null, loginBody);
        assertEquals(200, status);
        assertNotNull(token);
    }

    @Test
    @DisplayName("Auth: GET /profiles/{userId} sa tokenom vraća 200 ili 404")
    void auth_getProfile_withToken_returnsProfile() throws Exception {
        assumeTokenAvailable();
        int status = get(authBaseUrl, "/api/v1/auth/profiles/1", token);
        assertTrue(status == 200 || status == 404);
    }

    @Test
    @DisplayName("Auth: GET /validate sa tokenom vraća 200")
    void auth_validateToken_returnsOk() throws Exception {
        assumeTokenAvailable();
        int status = get(authBaseUrl, "/api/v1/auth/validate", token);
        assertEquals(200, status);
    }

    // ---------- USER SERVICE ----------

    @Test
    @DisplayName("User: pretraga bez tokena vraća 401")
    void user_search_withoutToken_returns401() throws Exception {
        int status = get(userBaseUrl, "/api/v1/users/search?q=test", null);
        assertEquals(401, status);
    }

    @Test
    @DisplayName("User: pretraga sa tokenom vraća 200")
    void user_search_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(userBaseUrl, "/api/v1/users/search?q=test", token);
        assertEquals(200, status);
    }

    @Test
    @DisplayName("User: GET followers count sa tokenom vraća 200")
    void user_followersCount_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(userBaseUrl, "/api/v1/users/1/followers/count", token);
        assertEquals(200, status);
    }

    @Test
    @DisplayName("User: GET following count sa tokenom vraća 200")
    void user_followingCount_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(userBaseUrl, "/api/v1/users/1/following/count", token);
        assertEquals(200, status);
    }

    @Test
    @DisplayName("User: POST follow-request sa tokenom vraća 201 ili 4xx")
    void user_followRequest_withToken_returns201Or4xx() throws Exception {
        assumeTokenAvailable();
        int status = post(userBaseUrl, "/api/v1/users/follow-request/2", token, null);
        assertTrue(status == 201 || (status >= 400 && status < 500), "Expected 201 or 4xx, got " + status);
    }

    @Test
    @DisplayName("User: GET relationship status sa tokenom vraća 200")
    void user_relationshipStatus_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(userBaseUrl, "/api/v1/users/relationship/2", token);
        assertEquals(200, status);
    }

    // ---------- POST SERVICE ----------

    @Test
    @DisplayName("Post: GET /posts sa tokenom vraća 200 ili 401")
    void post_getPosts_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(postBaseUrl, "/api/posts", token);
        assertTrue(status == 200 || status == 401);
    }

    @Test
    @DisplayName("Post: GET /posts/user/{userId} sa tokenom vraća 200 ili 401")
    void post_getUserPosts_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(postBaseUrl, "/api/posts/user/1", token);
        assertTrue(status == 200 || status == 401);
    }

    // ---------- INTERACTION SERVICE ----------

    @Test
    @DisplayName("Interaction: GET /comments/{postId} vraća 200, 401 ili 404")
    void interaction_getComments_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(interactionBaseUrl, "/api/comments/1", token);
        assertTrue(status == 200 || status == 401 || status == 404);
    }

    @Test
    @DisplayName("Interaction: POST /likes/{postId} sa tokenom vraća 2xx ili 4xx")
    void interaction_toggleLike_returns2xxOr4xx() throws Exception {
        assumeTokenAvailable();
        int status = request("POST", interactionBaseUrl + "/api/likes/1?userId=1", token, null);
        assertTrue((status >= 200 && status < 300) || (status >= 400 && status < 500), "Expected 2xx or 4xx, got " + status);
    }

    // ---------- FEED SERVICE ----------

    @Test
    @DisplayName("Feed: GET /feed/{userId} sa tokenom vraća 200 ili 401")
    void feed_getFeed_withToken_returns200() throws Exception {
        assumeTokenAvailable();
        int status = get(feedBaseUrl, "/api/feed/1", token);
        assertTrue(status == 200 || status == 401);
    }

    // ---------- FULL FLOW ----------

    @Test
    @DisplayName("Full flow: register -> login -> user search")
    void fullFlow_registerLoginThenUserSearch() throws Exception {
        String username = "flow_" + System.currentTimeMillis();
        String email = username + "@flow.test";
        String registerBody = String.format("{\"firstName\":\"Flow\",\"lastName\":\"User\",\"username\":\"%s\",\"email\":\"%s\",\"password\":\"password123\"}", username, email);
        assertEquals(200, post(authBaseUrl, "/api/v1/auth/register", null, registerBody));
        String t = token;
        token = null;
        assertEquals(200, post(authBaseUrl, "/api/v1/auth/login", null, String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email)));
        String flowToken = token;
        assertNotNull(flowToken);
        assertEquals(200, get(userBaseUrl, "/api/v1/users/search?q=flow", flowToken));
    }

    private void assumeTokenAvailable() throws Exception {
        if (token == null) {
            String username = "assume_" + System.currentTimeMillis();
            String email = username + "@assume.test";
            post(authBaseUrl, "/api/v1/auth/register", null,
                    String.format("{\"firstName\":\"Assume\",\"lastName\":\"User\",\"username\":\"%s\",\"email\":\"%s\",\"password\":\"password123\"}", username, email));
            token = null;
            post(authBaseUrl, "/api/v1/auth/login", null, String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email));
            Assumptions.assumeTrue(token != null, "Login failed - auth service may be down");
        }
    }
}
