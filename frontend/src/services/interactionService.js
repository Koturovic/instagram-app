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