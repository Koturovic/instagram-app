import axios from "axios";

const PORTS = {
    AUTH: "8080",
    USER: "8081",
    POST: "8082",
    INTERACTION: "8083",
    FEED: "8084"
};

const BASE_URL = "http://localhost";

// Kreiramo osnovnu instancu
const apiClient = axios.create({
    headers: {
        "Content-Type": "application/json",
    },
});

// presretac = lepi token pre svakog slanja
apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// povezivanje razlicitih servisa 
export const getUrl = (service, endpoint) => `${BASE_URL}:${PORTS[service]}/api/v1${endpoint}`;

export default apiClient;