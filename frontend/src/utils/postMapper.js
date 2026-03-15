const PLACEHOLDER_IMAGE = "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop";
const PLACEHOLDER_AVATAR = "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg";

const getFirstMediaUrl = (raw) => {
    if (Array.isArray(raw.mediaFiles) && raw.mediaFiles.length > 0) {
        return raw.mediaFiles[0]?.fileUrl || null;
    }
    if (Array.isArray(raw.media) && raw.media.length > 0) {
        return raw.media[0]?.fileUrl || raw.media[0]?.url || null;
    }
    return null;
};

export const normalizePost = (raw = {}) => {
    const fallbackUsername = raw.userId ? `user${raw.userId}` : "Unknown user";
    const mediaUrl = raw.image
        || raw.imageUrl
        || raw.mediaUrl
        || getFirstMediaUrl(raw)
        || PLACEHOLDER_IMAGE;

    return {
        id: raw.id ?? raw.postId ?? raw.postID ?? raw.post_id ?? null,
        userId: raw.userId ?? raw.ownerId ?? raw.authorId ?? raw.user?.id ?? null,
        username: raw.username ?? raw.user?.username ?? fallbackUsername,
        avatar: raw.avatar ?? raw.user?.avatar ?? PLACEHOLDER_AVATAR,
        caption: raw.caption ?? raw.description ?? "",
        description: raw.description ?? raw.caption ?? "",
        likes: Number(raw.likes ?? raw.likesCount ?? raw.likeCount ?? 0),
        image: mediaUrl,
        mediaFiles: Array.isArray(raw.mediaFiles) ? raw.mediaFiles : [],
        raw,
    };
};

export const normalizePosts = (list = []) => {
    if (!list || !Array.isArray(list)) return [];
    return list.map(normalizePost);
};
