import apiClient, { getUrl } from "./apiClient";

export const searchUsers = async (query) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/search?q=${encodeURIComponent(query)}`)
    );
    return response.data;
};

export const followUser = async (targetUserId) => {
    const response = await apiClient.post(
        getUrl("USER", `/users/follow-request/${targetUserId}`)
    );
    return response.data;
};

export const unfollowUser = async (targetUserId) => {
    const response = await apiClient.delete(
        getUrl("USER", `/users/following/${targetUserId}`)
    );
    return response.data;
};

export const getFollowersCount = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/followers/count`)
    );
    return response.data?.count ?? 0;
};

export const getFollowingCount = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/following/count`)
    );
    return response.data?.count ?? 0;
};

export const acceptFollowRequest = async (requestId) => {
    const response = await apiClient.post(
        getUrl("USER", `/users/follow-request/${requestId}/accept`)
    );
    return response.data;
};

export const rejectFollowRequest = async () => {
    throw new Error("Reject follow request endpoint is not implemented in user-service yet.");
};

export const blockUser = async (targetUserId) => {
    const response = await apiClient.post(
        getUrl("USER", `/users/block/${targetUserId}`)
    );
    return response.data;
};

export const unblockUser = async (targetUserId) => {
    const response = await apiClient.delete(
        getUrl("USER", `/users/block/${targetUserId}`)
    );
    return response.data;
};
