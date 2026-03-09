import axios from "axios";

const PORTS = {
    AUTH: "8080",
    USER: "8081",
    POST: "8082",
    INTERACTION: "8083",
    FEED: "8084"
};

const BASE_URL = "http://localhost";

// Osnovni axios klijent. Content-Type ne forsiramo globalno:
// - JSON za obične objekte Axios postavlja sam
// - multipart/form-data za FormData postavlja browser sa boundary vrednošću
const apiClient = axios.create();

// presretac = lepi token pre svakog slanja zahteva
apiClient.interceptors.request.use((config) => {
    // For FormData requests (file upload), let Axios/browser set multipart boundary.
    if (config.data instanceof FormData) {
        if (config.headers) {
            delete config.headers["Content-Type"];
            delete config.headers["content-type"];
        }
    }

    const requestUrl = config.url || "";
    const isPublicAuthEndpoint =
        requestUrl.includes("/api/v1/auth/login") ||
        requestUrl.includes("/api/v1/auth/register");

    if (isPublicAuthEndpoint) {
        return config;
    }

    const token = localStorage.getItem("token");
    if (token) {
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