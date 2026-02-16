import apiClient, { getUrl } from "./apiClient";

export const getFeed = async () => {
    // port 8084, ruta /api/v1/feed
    const response = await apiClient.get(getUrl("FEED", "/feed"));
    return response.data;
};