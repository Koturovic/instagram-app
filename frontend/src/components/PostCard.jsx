import { useState, useEffect, useCallback } from "react";
import { toggleLike, getComments, addComment } from "../services/interactionService";
import { deletePost } from "../services/postService";
import { getUserIdFromToken } from "../utils/auth";
import "./PostCard.css";

export default function PostCard({ post, onDelete }) {
    const [liked, setLiked] = useState(false);
    const [likes, setLikes] = useState(post.likes);
    const [comments, setComments] = useState([]);
    const [showComments, setShowComments] = useState(false);
    const [newComment, setNewComment] = useState("");
    const [loadingComments, setLoadingComments] = useState(false);
    const [commentsLoaded, setCommentsLoaded] = useState(false);

    const currentUserId = getUserIdFromToken();

    const fetchComments = useCallback(async () => {
        try {
            setLoadingComments(true);
            const data = await getComments(post.id);
            setComments(data);
            setCommentsLoaded(true);
        } catch (err) {
            console.error("Error fetching comments:", err);
        } finally {
            setLoadingComments(false);
        }
    }, [post.id]);

    useEffect(() => {
        if (showComments && !commentsLoaded) {
            fetchComments();
        }
    }, [showComments, commentsLoaded, fetchComments]);

    const handleLike = async () => {
        if (!currentUserId) {
            alert("Please login to like posts");
            return;
        }

        try {
            await toggleLike(post.id, currentUserId);

            if (liked) {
                setLikes(prev => prev - 1);
            } else {
                setLikes(prev => prev + 1);
            }
            setLiked(!liked);
        } catch (err) {
            console.error("Error toggling like:", err);
        }
    };

    const handleAddComment = async (e) => {
        e.preventDefault();
        
        if (!currentUserId) {
            alert("Please login to comment");
            return;
        }

        if (!newComment.trim()) return;

        try {
            const comment = await addComment(post.id, currentUserId, newComment);
            setComments([...comments, comment]);
            setNewComment("");
        } catch (err) {
            console.error("Error adding comments:", err);
        }
    };

    const toggleCommentsView = () => {
        setShowComments(!showComments);
    };

    const handleDelete = async () => {
        if (!currentUserId) {
            alert("Please login to delete posts");
            return;
        }

        if (post.userId !== currentUserId) {
            alert("You can only delete your own posts");
            return;
        }

        const confirmed = window.confirm("Are you sure you want to delete this post?");
        if (!confirmed) return;

        try {
            await deletePost(post.id);
            alert("Post deleted successfully!");
            if (onDelete) {
                onDelete(post.id); // roditelj je obavesten da je post obrisan
                                   // i moze da osvezi listu post-ova
            }
        } catch (err) {
            console.error("Error deleting post:", err);
            alert("Failed to delete post. Please try again.");
        }
    };

    const isOwner = currentUserId && post.userId === currentUserId;

    return (
        <div className="post-card">
            <div className="post-header">
                <div className="post-header-left">
                    <img src={post.avatar} className="post-avatar" alt="avatar" />
                    <span className="post-username">{post.username}</span>
                </div>
                {isOwner && (
                    <button className="delete-post-btn" onClick={handleDelete} title="Delete post">
                        🗑️
                    </button>
                )}
            </div>

            <img src={post.image} className="post-image" alt="post content" />

            <div className="post-actions">
                <button onClick={handleLike} className="like-btn">
                    {liked ? "❤️" : "🤍"}
                </button>
                <button onClick={toggleCommentsView} className="comment-btn">
                    💬
                </button>
            </div>

            <p className="post-likes">{likes} likes</p>

            <p className="post-caption">
                <b>{post.username}</b> {post.caption}
            </p>

            {/* link za gledanje svih komentara */}
            {!showComments && (
                <button className="view-comments-btn" onClick={toggleCommentsView}>
                    View all {comments.length} comments
                </button>
            )}

            {/* sekcija za komentare */}
            {showComments && (
                <div className="comments-section">
                    {loadingComments ? (
                        <p className="loading-text">Loading comments...</p>
                    ) : comments.length > 0 ? (
                        <div className="comments-list">
                            {comments.map((comment) => (
                                <div key={comment.id} className="comment-item">
                                    <span className="comment-username">
                                        <b>User {comment.userId}</b>
                                    </span>
                                    <span className="comment-content">{comment.content}</span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="no-comments">No comments yet</p>
                    )}

                    {/* forma za komentarisanje */}
                    <form onSubmit={handleAddComment} className="add-comment-form">
                        <input
                            type="text"
                            placeholder="Add a comment..."
                            value={newComment}
                            onChange={(e) => setNewComment(e.target.value)}
                            className="comment-input"
                        />
                        <button 
                            type="submit" 
                            disabled={!newComment.trim()}
                            className="post-comment-btn"
                        >
                            Post
                        </button>
                    </form>
                </div>
            )}
        </div>
    );
}