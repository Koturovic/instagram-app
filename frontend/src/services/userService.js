import apiClient, { getUrl } from "./apiClient";

export const searchUsers = async (query) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/search?q=${encodeURIComponent(query)}`)
    );
    return response.data;
};

export const followUser = async (targetUserId) => {
    const response = await apiClient.post(
        getUrl("USER", `/follow/request/${targetUserId}`)
    );
    return response.data;
};

export const unfollowUser = async (targetUserId) => {
    const response = await apiClient.delete(
        getUrl("USER", `/follow/${targetUserId}`)
    );
    return response.data;
};

export const getFollowers = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/followers`)
    );
    return response.data;
};

export const getFollowing = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/following`)
    );
    return response.data;
};

export const acceptFollowRequest = async (requestId) => {
    const response = await apiClient.post(
        getUrl("USER", `/follow/accept/${requestId}`)
    );
    return response.data;
};

export const rejectFollowRequest = async (requestId) => {
    const response = await apiClient.delete(
        getUrl("USER", `/follow/reject/${requestId}`)
    );
    return response.data;
};
