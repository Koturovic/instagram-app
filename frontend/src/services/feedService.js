import apiClient, { getUrl } from "./apiClient";

export const getFeed = async (userId) => {
    // port 8084, ruta /api/feed/{userId}
    const response = await apiClient.get(getUrl("FEED", `/feed/${userId}`));
    return response.data;
};