import { useState } from "react";
import { toggleLike } from "../services/interactionService"; // Importujemo nove funkcije
import "./PostCard.css";

export default function PostCard({ post }) {
    const [liked, setLiked] = useState(false);
    const [likes, setLikes] = useState(post.likes);

    const handleLike = async () => {
        try {
            // Privremeni userId dok ne implementiramo decode tokena
            const testUserId = 1;

            // Nemanja koristi toggle logiku: jedan poziv za oba stanja
            await toggleLike(post.id, testUserId);

            if (liked) {
                setLikes(prev => prev - 1);
            } else {
                setLikes(prev => prev + 1);
            }
            setLiked(!liked);
        } catch (err) {
            console.error("Greška:", err);
        }
    };

    return (
        <div className="post-card">
            <div className="post-header">
                <img src={post.avatar} className="post-avatar" alt="avatar" />
                <span className="post-username">{post.username}</span>
            </div>

            <img src={post.image} className="post-image" alt="post content" />

            <div className="post-actions">
                <button onClick={handleLike} className="like-btn">
                    {liked ? "❤️" : "🤍"}
                </button>
                <button>💬</button>
            </div>

            <p className="post-likes">{likes} likes</p>

            <p className="post-caption">
                <b>{post.username}</b> {post.caption}
            </p>
        </div>
    );
}