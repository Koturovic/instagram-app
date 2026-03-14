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

export const removeFollower = async (followerUserId) => {
    const response = await apiClient.delete(
        getUrl("USER", `/users/followers/${followerUserId}`)
    );
    return response.data;
};

export const getFollowersCount = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/followers/count`)
    );
    const payload = response.data;
    if (typeof payload === "number") return payload;
    if (typeof payload?.count === "number") return payload.count;
    if (typeof payload?.count === "string") return Number(payload.count) || 0;
    return 0;
};

export const getRelationshipStatus = async (targetUserId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/relationship/${targetUserId}`)
    );
    return response.data;
};

export const getFollowingCount = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/following/count`)
    );
    const payload = response.data;
    if (typeof payload === "number") return payload;
    if (typeof payload?.count === "number") return payload.count;
    if (typeof payload?.count === "string") return Number(payload.count) || 0;
    return 0;
};

export const getFollowersList = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/followers`)
    );
    return response.data || [];
};

export const getFollowingList = async (userId) => {
    const response = await apiClient.get(
        getUrl("USER", `/users/${userId}/following`)
    );
    return response.data || [];
};

export const acceptFollowRequest = async (requestId) => {
    const response = await apiClient.post(
        getUrl("USER", `/users/follow-request/${requestId}/accept`)
    );
    return response.data;
};

export const rejectFollowRequest = async (requestId) => {
    const response = await apiClient.post(
        getUrl("USER", `/users/follow-request/${requestId}/reject`)
    );
    return response.data;
};

export const getPendingFollowRequests = async () => {
    const response = await apiClient.get(
        getUrl("USER", `/users/follow-requests/pending`)
    );
    return response.data;
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
