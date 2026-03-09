import apiClient, { getUrl } from "./apiClient";
import { getUserIdFromToken } from "../utils/auth";

// kreiranje novog post-a
export const createPost = async (description, files) => {
    const formData = new FormData();
    
    // deskripcija 
    formData.append("description", description);

    // userId iz JWT tokena (automatski izvlaci ID ulogovanog korisnika)
    const userId = getUserIdFromToken();
    if (!userId) {
        throw new Error("User not authenticated. Please login again.");
    }
    formData.append("userId", userId);
    
    // dodavanje fajlova (slika/video)
    files.forEach((file) => {
        formData.append("files", file);
    });
    
    // saljemo na post endpoint (port 8082, ruta /api/v1/posts)
    const response = await apiClient.post(
        getUrl("POST", "/posts"),
        formData,
        {
            headers: {
                "Content-Type": "multipart/form-data"
            }
        }
    );
    
    return response.data;
};

// fetch svih post-ova (za feed)
export const getAllPosts = async () => {
    const response = await apiClient.get(getUrl("POST", "/posts"));
    return response.data;
};

// fetch post-ova jednog korisnika (za profil) 
export const getUserPosts = async (userId) => {
    const response = await apiClient.get(getUrl("POST", `/posts/user/${userId}`));
    return response.data;
};

// brisanje post-a (ako je korisnik VLASNIK posta)
export const deletePost = async (postId) => {
    const response = await apiClient.delete(getUrl("POST", `/posts/${postId}`));
    return response.data;
};

export const updatePostDescription = async (postId, description) => {
    const formData = new FormData();
    formData.append("description", description);

    const response = await apiClient.put(
        getUrl("POST", `/posts/${postId}`),
        formData
    );

    return response.data;
};
