import axios from "axios";

const PORTS = {
    AUTH: "8080",
    USER: "8081",
    POST: "8082",
    INTERACTION: "8083",
    FEED: "8084"
};

const BASE_URL = "http://localhost";

// osnovni config za axios klijenta (sve zahteve saljemo preko tokena)
const apiClient = axios.create({
    headers: {
        "Content-Type": "application/json",
    },
});

// presretac = lepi token pre svakog slanja zahteva
apiClient.interceptors.request.use((config) => {
    const requestUrl = config.url || "";
    const isPublicAuthEndpoint =
        requestUrl.includes("/api/v1/auth/login") ||
        requestUrl.includes("/api/v1/auth/register");

    if (isPublicAuthEndpoint) {
        return config;
    }

    const token = localStorage.getItem("token");
    const looksLikeJwt = token && token.split(".").length === 3;
    if (looksLikeJwt) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// povezivanje razlicitih servisa 
export const getUrl = (service, endpoint) => {
    const baseUrl = `${BASE_URL}:${PORTS[service]}`;

    // auth-service i user-service imaju /api/v1/ prefix
    if (service === "AUTH" || service === "USER") {
        return `${baseUrl}/api/v1${endpoint}`;
    }

    // ostali koriste /api/ prefix
    return `${baseUrl}/api${endpoint}`;
};

export default apiClient;
