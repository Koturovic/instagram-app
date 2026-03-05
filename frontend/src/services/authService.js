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

export async function getProfileByUserId(userId) {
    const response = await apiClient.get(getUrl("AUTH", `/auth/profiles/${userId}`));
    return response.data;
}

// cache za username-ove da ne fetchujemo iste korisnike više puta
const usernameCache = new Map();

export async function getUsernameById(userId) {
    if (usernameCache.has(userId)) {
        return usernameCache.get(userId);
    }

    try {
        const profile = await getProfileByUserId(userId);
        const username = profile.username || `user${userId}`;
        usernameCache.set(userId, username);
        return username;
    } catch (err) {
        console.error(`Failed to fetch username for userId ${userId}:`, err);
        return `user${userId}`; 
    }
}