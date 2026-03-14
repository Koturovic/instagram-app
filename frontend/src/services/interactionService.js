import apiClient, { getUrl } from "./apiClient";

// Lajkovanje (na osnovu LikeController.java)
export const toggleLike = async (postId, userId) => {
    // POST http://localhost:8083/api/v1/likes/{postId}?userId={userId}
    const response = await apiClient.post(
        getUrl("INTERACTION", `/likes/${postId}?userId=${userId}`)
    );
    return response.data; // "Post liked" / "Post unliked"
};

// Komentarisanje (na osnovu CommentController.java)
export const addComment = async (postId, userId, content) => {
    // POST http://localhost:8083/api/v1/comments/{postId}?userId={userId}
    // Saljemo content kao plain text, a ne JSON 
    const response = await apiClient.post(
        getUrl("INTERACTION", `/comments/${postId}?userId=${userId}`),
        content,
        { headers: { "Content-Type": "text/plain" } } 
    );
    return response.data;
};

// Hvatanje komentara za objavu
export const getComments = async (postId) => {
    const response = await apiClient.get(getUrl("INTERACTION", `/comments/${postId}`));
    return response.data;
};

export const isPostLiked = async (postId, userId) => {
    const response = await apiClient.get(
        getUrl("INTERACTION", `/likes/${postId}/users/${userId}`)
    );
    return response.data;
};

export const getLikesCount = async (postId) => {
    const response = await apiClient.get(
        getUrl("INTERACTION", `/likes/${postId}/count`)
    );
    return Number(response.data ?? 0);
};

export const getCommentsCount = async (postId) => {
    const response = await apiClient.get(getUrl("INTERACTION", `/comments/${postId}/count`));
    return Number(response.data ?? 0);
};

export const updateComment = async (commentId, userId, newContent) => {
    const response = await apiClient.put(
        getUrl("INTERACTION", `/comments/${commentId}?userId=${userId}`),
        newContent,
        { headers: { "Content-Type": "text/plain" } }
    );
    return response.data;
};

export const deleteComment = async (commentId, userId) => {
    const response = await apiClient.delete(
        getUrl("INTERACTION", `/comments/${commentId}?userId=${userId}`)
    );
    return response.data;
};