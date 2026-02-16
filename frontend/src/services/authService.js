import apiClient, { getUrl } from "./apiClient";

export async function login(email, password) {
    // Koristimo apiClient koji već zna da treba da šalje JSON
    const response = await apiClient.post(getUrl("AUTH", "/auth/login"), { 
        email, 
        password 
    });
    return response.data; // Axios automatski radi .json()
}

export async function register(user) {
    const response = await apiClient.post(getUrl("AUTH", "/auth/register"), user);
    return response.data;
}