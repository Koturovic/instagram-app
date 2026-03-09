import { useState, useEffect, useCallback } from "react";
import Navbar from "../components/Navbar";
import PostCard from "../components/PostCard";
import { getFeed } from "../services/feedService";
import { getUserIdFromToken } from "../utils/auth";
import { getProfileByUserId } from "../services/authService";
import { normalizePosts } from "../utils/postMapper";

export default function Home() {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const currentUserId = getUserIdFromToken();

    const fetchPosts = useCallback(async () => {
        try {
            if (!currentUserId) {
                console.error("User not authenticated");
                setLoading(false);
                return;
            }

            setLoading(true);
            const data = await getFeed(currentUserId);
            let posts = normalizePosts(data);

            // Fetch avatara za sve korisnike iz postova
            const uniqueUserIds = [...new Set(posts.map(p => p.userId))];
            try {
                const profilePromises = uniqueUserIds.map(uid => getProfileByUserId(uid).catch(() => null));
                const profiles = await Promise.all(profilePromises);
                const profileMap = {};
                profiles.forEach((profile, idx) => {
                    if (profile) {
                        profileMap[uniqueUserIds[idx]] = {
                            avatar: profile.profileImageUrl || "",
                            username: profile.username || ""
                        };
                    }
                });

                // Ažuriraj postove sa avatarima
                posts = posts.map(post => ({
                    ...post,
                    avatar: profileMap[post.userId]?.avatar || post.avatar,
                    username: profileMap[post.userId]?.username || post.username
                }));
            } catch (err) {
                console.error("Error fetching avatars:", err);
            }

            setPosts(posts);
        } catch (err) {
            console.error("Error fetching feed:", err);
            setPosts([]); // Fallback na praznu listu ako feed-service ne radi
        } finally {
            setLoading(false);
        }
    }, [currentUserId]);

    useEffect(() => {
        fetchPosts();
    }, [fetchPosts]);

    const handlePostDelete = (postId) => {
        // nakon brisanja post-a, osvezavamo feed tako sto uklanjamo obrisani post iz stanja
        setPosts(prevPosts => prevPosts.filter(p => p.id !== postId));
    };

    return (
        <>
            <Navbar />
            <div style={{ maxWidth: "600px", margin: "0 auto", paddingTop: "80px" }}>
                {loading ? (
                    <p>Loading...</p>
                ) : (
                    posts.map(post => <PostCard key={post.id} post={post} onDelete={handlePostDelete} />)
                )}
            </div>
        </>
    );
}