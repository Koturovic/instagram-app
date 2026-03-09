import { useState, useEffect, useCallback, useRef } from "react";
import { toggleLike, getComments, addComment, isPostLiked, getLikesCount } from "../services/interactionService";
import { deletePost } from "../services/postService";
import { getUserIdFromToken } from "../utils/auth";
import { getUsernameById } from "../services/authService";
import { POST_CAPTION_PREVIEW_LINES } from "../constants/postLimits";
import EditCaptionModal from "./EditCaptionModal";
import "./PostCard.css";

export default function PostCard({ post, onDelete }) {
    const [liked, setLiked] = useState(false);
    const [likes, setLikes] = useState(post.likes ?? 0);
    const [comments, setComments] = useState([]);
    const [showComments, setShowComments] = useState(false);
    const [newComment, setNewComment] = useState("");
    const [loadingComments, setLoadingComments] = useState(false);
    const [commentsLoaded, setCommentsLoaded] = useState(false);
    const [currentMediaIndex, setCurrentMediaIndex] = useState(0);
    const [commentUsernames, setCommentUsernames] = useState({});
    const [resolvedUsername, setResolvedUsername] = useState("");
    const [isCaptionExpanded, setIsCaptionExpanded] = useState(false);
    const [isCaptionOverflowing, setIsCaptionOverflowing] = useState(false);
    const [showSettings, setShowSettings] = useState(false);
    const [isEditCaptionOpen, setIsEditCaptionOpen] = useState(false);
    const [currentCaption, setCurrentCaption] = useState(post.caption || "");
    const captionRef = useRef(null);
    const settingsMenuRef = useRef(null);

    const currentUserId = getUserIdFromToken();
    const fallbackUsername = post.userId ? `user${post.userId}` : "Unknown user";
    const displayUsername = resolvedUsername || post.username || fallbackUsername;
    const fallbackUsername = post.userId ? `user${post.userId}` : "Unknown user";
    const displayUsername = resolvedUsername || post.username || fallbackUsername;
    const avatarUrl = post.avatar || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg";
    
    const mediaFiles = post.mediaFiles && post.mediaFiles.length > 0
        ? post.mediaFiles
    const mediaFiles = post.mediaFiles && post.mediaFiles.length > 0
        ? post.mediaFiles
        : [{ fileUrl: post.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }];
    
    const hasMultipleMedia = mediaFiles.length > 1;
    const captionText = currentCaption || "";

    const fetchComments = useCallback(async () => {
        try {
            setLoadingComments(true);
            const data = await getComments(post.id);
            setComments(data);
            setCommentsLoaded(true);
            
            const userIds = [...new Set(data.map(c => c.userId))]; 
            const usernamePromises = userIds.map(async (userId) => {
                const username = await getUsernameById(userId);
                return [userId, username];
            });
            
            const usernameEntries = await Promise.all(usernamePromises);
            const usernameMap = Object.fromEntries(usernameEntries);
            setCommentUsernames(usernameMap);
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

    useEffect(() => {
        let cancelled = false;
        if (currentUserId && post.id) {
            isPostLiked(post.id, currentUserId)
                .then((value) => {
                    if (!cancelled) {
                        setLiked(Boolean(value));
                    }
                })
                .catch((err) => {
                    console.error("Error fetching like status:", err);
                });
        }
        return () => {
            cancelled = true;
        };
    }, [currentUserId, post.id]);

    useEffect(() => {
        let cancelled = false;
        if (post.id) {
            getLikesCount(post.id)
                .then((count) => {
                    if (!cancelled) {
                        setLikes(Number(count ?? 0));
                    }
                })
                .catch((err) => {
                    console.error("Error fetching likes count:", err);
                });
        }

        return () => {
            cancelled = true;
        };
    }, [post.id]);

    useEffect(() => {
        let cancelled = false;
        if (post.userId && post.username === fallbackUsername) {
            getUsernameById(post.userId)
                .then((name) => {
                    if (!cancelled && name) {
                        setResolvedUsername(name);
                    }
                })
                .catch((err) => {
                    console.error("Error fetching username:", err);
                });
        }
        return () => {
            cancelled = true;
        };
    }, [post.userId, post.username, fallbackUsername]);

    useEffect(() => {
        setIsCaptionExpanded(false);
        setCurrentCaption(post.caption || "");
        setShowSettings(false);
        setIsEditCaptionOpen(false);
    }, [post.id, post.caption]);

    useEffect(() => {
        if (!showSettings) {
            return;
        }

        const handleOutsideClick = (event) => {
            if (settingsMenuRef.current && !settingsMenuRef.current.contains(event.target)) {
                setShowSettings(false);
            }
        };

        const handleEscape = (event) => {
            if (event.key === "Escape") {
                setShowSettings(false);
            }
        };

        document.addEventListener("mousedown", handleOutsideClick);
        document.addEventListener("keydown", handleEscape);
        return () => {
            document.removeEventListener("mousedown", handleOutsideClick);
            document.removeEventListener("keydown", handleEscape);
        };
    }, [showSettings]);

    useEffect(() => {
        if (isCaptionExpanded) {
            return;
        }

        const captionElement = captionRef.current;
        if (!captionElement) {
            setIsCaptionOverflowing(false);
            return;
        }

        const measureOverflow = () => {
            setIsCaptionOverflowing(captionElement.scrollHeight > captionElement.clientHeight + 1);
        };

        measureOverflow();
        window.addEventListener("resize", measureOverflow);
        return () => {
            window.removeEventListener("resize", measureOverflow);
        };
    }, [captionText, isCaptionExpanded, post.id]);

    const handleLike = async () => {
        if (!currentUserId) {
            alert("Please login to like posts");
            return;
        }

        const wasLiked = liked;
        setLiked(!wasLiked);
        setLikes(prev => Math.max(0, prev + (wasLiked ? -1 : 1)));

        try {
            await toggleLike(post.id, currentUserId);
            const freshCount = await getLikesCount(post.id);
            setLikes(Number(freshCount ?? 0));
        } catch (err) {
            console.error("Error toggling like:", err);
            setLiked(wasLiked);
            setLikes(prev => Math.max(0, prev + (wasLiked ? 1 : -1)));
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
            
            const username = await getUsernameById(currentUserId);
            setCommentUsernames(prev => ({ ...prev, [currentUserId]: username }));
        } catch (err) {
            console.error("Error adding comments:", err);
        }
    };

    const toggleCommentsView = () => {
        setShowComments(!showComments);
    };

    const handlePrevMedia = () => {
        setCurrentMediaIndex(prev => (prev === 0 ? mediaFiles.length - 1 : prev - 1));
    };

    const handleNextMedia = () => {
        setCurrentMediaIndex(prev => (prev === mediaFiles.length - 1 ? 0 : prev + 1));
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

    const handleEditCaption = () => {
        setShowSettings(false);
        setIsEditCaptionOpen(true);
    };

    const handleCaptionUpdate = (updatedPost) => {
        setCurrentCaption(updatedPost.caption || "");
        setIsCaptionExpanded(false);
    };

    const isOwner = currentUserId && post.userId === currentUserId;

    return (
        <>
        <div className="post-card">
            <div className="post-header">
                <div className="post-header-left">
                    <img src={avatarUrl} className="post-avatar" alt="avatar" />
                    <span className="post-username">{displayUsername}</span>
                </div>
                {isOwner && (
                    <div className="feed-settings-menu" ref={settingsMenuRef}>
                        <button
                            className="feed-settings-btn"
                            onClick={() => setShowSettings((prev) => !prev)}
                            title="Post settings"
                        >
                            ⋯
                        </button>
                        {showSettings && (
                            <div className="feed-settings-dropdown">
                                <button
                                    className="feed-settings-item"
                                    onClick={handleEditCaption}
                                >
                                    Edit Caption
                                </button>
                                <button
                                    className="feed-settings-item delete-item"
                                    onClick={handleDelete}
                                >
                                    Delete Post
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Media karousel */}
            <div className="post-media-container">
                {mediaFiles[currentMediaIndex].contentType?.startsWith("video/") ? (
                    <video
                        src={mediaFiles[currentMediaIndex].fileUrl}
                        className="post-image"
                        controls
                    />
                ) : (
                    <img 
                        src={mediaFiles[currentMediaIndex].fileUrl} 
                        className="post-image" 
                        alt="post content" 
                    />
                )}
                {mediaFiles[currentMediaIndex].contentType?.startsWith("video/") ? (
                    <video
                        src={mediaFiles[currentMediaIndex].fileUrl}
                        className="post-image"
                        controls
                    />
                ) : (
                    <img 
                        src={mediaFiles[currentMediaIndex].fileUrl} 
                        className="post-image" 
                        alt="post content" 
                    />
                )}
                
                {hasMultipleMedia && (
                    <>
                        <button className="carousel-btn carousel-btn-prev" onClick={handlePrevMedia}>
                            ‹
                        </button>
                        <button className="carousel-btn carousel-btn-next" onClick={handleNextMedia}>
                            ›
                        </button>
                        <div className="carousel-indicator">
                            {currentMediaIndex + 1} / {mediaFiles.length}
                        </div>
                    </>
                )}
            </div>

            <div className="post-actions">
                <button onClick={handleLike} className="like-btn">
                    {liked ? "❤️" : "🤍"}
                </button>
                <button onClick={toggleCommentsView} className="comment-btn">
                    💬
                </button>
            </div>

            <p className="post-likes">{likes} likes</p>

            <div className="post-caption-block">
                <p
                    ref={captionRef}
                    className={`post-caption ${!isCaptionExpanded ? "collapsed" : ""}`}
                    style={{ WebkitLineClamp: POST_CAPTION_PREVIEW_LINES }}
                >
                    <b>{displayUsername}</b> {captionText}
                </p>
                {!isCaptionExpanded && isCaptionOverflowing && (
                    <button
                        type="button"
                        className="caption-expand-btn"
                        onClick={() => setIsCaptionExpanded(true)}
                        title="Show full caption"
                    >
                        See more
                    </button>
                )}
            </div>

            {/* link za gledanje svih komentara */}
            {!showComments && (
                <button className="view-comments-btn" onClick={toggleCommentsView}>
                    View comments
                    View comments
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
                                        <b>{commentUsernames[comment.userId] || `user${comment.userId}`}</b>
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

        <EditCaptionModal
            post={{ ...post, caption: currentCaption }}
            isOpen={isEditCaptionOpen}
            onClose={() => setIsEditCaptionOpen(false)}
            onUpdate={handleCaptionUpdate}
        />
        </>
    );
}
