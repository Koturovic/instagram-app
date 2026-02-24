import apiClient, { getUrl } from "./apiClient";

export async function login(email, password) {
    // Koristimo apiClient, on vec zna da treba da salje JSON
    // i da doda token ukoliko postoji
    const response = await apiClient.post(getUrl("AUTH", "/auth/login"), { 
        email, 
        password 
    });
    return response.data; // axios automatski radi .json()
}

export async function register(user) {
    const response = await apiClient.post(getUrl("AUTH", "/auth/register"), user);
    return response.data;
}