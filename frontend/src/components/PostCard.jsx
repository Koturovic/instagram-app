import { useState, useEffect, useCallback, useRef } from "react";
import { Link } from "react-router-dom";
import { toggleLike, getComments, addComment, isPostLiked, getLikesCount, getCommentsCount, updateComment, deleteComment } from "../services/interactionService";
import { deletePost, deleteMediaFromPost } from "../services/postService";
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
    const [commentsCount, setCommentsCount] = useState(0);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editingContent, setEditingContent] = useState("");
    const captionRef = useRef(null);
    const settingsMenuRef = useRef(null);

    const currentUserId = getUserIdFromToken();
    const fallbackUsername = post.userId ? `user${post.userId}` : "Unknown user";
    const displayUsername = resolvedUsername || post.username || fallbackUsername;
    const avatarUrl = post.avatar || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg";

    const [mediaFiles, setMediaFiles] = useState(
        post.mediaFiles && post.mediaFiles.length > 0
            ? post.mediaFiles
            : [{ fileUrl: post.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }]
    );

    const hasMultipleMedia = mediaFiles.length > 1;
    const canGoPrevMedia = currentMediaIndex > 0;
    const canGoNextMedia = currentMediaIndex < mediaFiles.length - 1;
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
            getCommentsCount(post.id)
                .then((count) => {
                    if (!cancelled) {
                        setCommentsCount(Number(count ?? 0));
                    }
                })
                .catch((err) => {
                    console.error("Error fetching comments count:", err);
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
        setMediaFiles(
            post.mediaFiles && post.mediaFiles.length > 0
                ? post.mediaFiles
                : [{ fileUrl: post.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }]
        );
        setCurrentMediaIndex(0);
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
            setCommentsCount(prev => prev + 1);

            const username = await getUsernameById(currentUserId);
            setCommentUsernames(prev => ({ ...prev, [currentUserId]: username }));
        } catch (err) {
            console.error("Error adding comments:", err);
        }
    };

    const handleEditComment = (comment) => {
        setEditingCommentId(comment.id);
        setEditingContent(comment.content);
    };

    const handleSaveEdit = async (commentId) => {
        if (!editingContent.trim()) return;
        try {
            const updated = await updateComment(commentId, currentUserId, editingContent);
            setComments(prev => prev.map(c => c.id === commentId ? updated : c));
            setEditingCommentId(null);
            setEditingContent("");
        } catch (err) {
            console.error("Error editing comment:", err);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!window.confirm("Delete this comment?")) return;
        try {
            await deleteComment(commentId, currentUserId);
            setComments(prev => prev.filter(c => c.id !== commentId));
            setCommentsCount(prev => Math.max(0, prev - 1));
        } catch (err) {
            console.error("Error deleting comment:", err);
        }
    };

    const toggleCommentsView = () => {
        setShowComments(!showComments);
    };

    const handlePrevMedia = () => {
        setCurrentMediaIndex(prev => (prev > 0 ? prev - 1 : prev));
    };

    const handleNextMedia = () => {
        setCurrentMediaIndex(prev => (prev < mediaFiles.length - 1 ? prev + 1 : prev));
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

    const handleDeleteCurrentMedia = async () => {
        const currentMedia = mediaFiles[currentMediaIndex];
        if (!currentMedia || !currentMedia.id) return;
        const mediaType = currentMedia.contentType?.startsWith("video/") ? "klip" : "sliku";
        if (!window.confirm(`Obrisati ovu ${mediaType} iz karosela?`)) return;
        try {
            const updatedPost = await deleteMediaFromPost(post.id, currentMedia.id);
            const newMediaFiles = updatedPost.mediaFiles && updatedPost.mediaFiles.length > 0
                ? updatedPost.mediaFiles
                : [];
            setMediaFiles(newMediaFiles);
            setCurrentMediaIndex(prev => Math.min(prev, newMediaFiles.length - 1));
            setShowSettings(false);
        } catch (err) {
            console.error("Error deleting media:", err);
            alert("Greška pri brisanju medija.");
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
                        {post.userId ? (
                            <Link to={`/profile/${post.userId}`} className="post-avatar-link">
                                <img src={avatarUrl} className="post-avatar" alt="avatar" />
                            </Link>
                        ) : (
                            <img src={avatarUrl} className="post-avatar" alt="avatar" />
                        )}
                        {post.userId ? (
                            <Link to={`/profile/${post.userId}`} className="post-username-link">
                                <span className="post-username">{displayUsername}</span>
                            </Link>
                        ) : (
                            <span className="post-username">{displayUsername}</span>
                        )}
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
                                    {hasMultipleMedia && (
                                        <button
                                            className="feed-settings-item delete-item"
                                            onClick={handleDeleteCurrentMedia}
                                        >
                                            Delete {mediaFiles[currentMediaIndex]?.contentType?.startsWith("video/") ? "Video" : "Image"}
                                        </button>
                                    )}
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

                    {hasMultipleMedia && (
                        <>
                            {canGoPrevMedia && (
                                <button className="postcard-carousel-btn postcard-carousel-btn-prev" onClick={handlePrevMedia}>
                                    ‹
                                </button>
                            )}
                            {canGoNextMedia && (
                                <button className="postcard-carousel-btn postcard-carousel-btn-next" onClick={handleNextMedia}>
                                    ›
                                </button>
                            )}
                            <div className="postcard-carousel-indicator">
                                {currentMediaIndex + 1} / {mediaFiles.length}
                            </div>
                        </>
                    )}
                </div>

                <div className="post-actions">
                    <div className="post-action-item">
                        <button onClick={handleLike} className="postcard-like-btn" aria-label="Like post">
                            {liked ? "❤️" : "🤍"}
                        </button>
                        <span className="post-action-count">{likes}</span>
                    </div>
                    <div className="post-action-item">
                        <button onClick={toggleCommentsView} className="postcard-comment-btn" aria-label="Show comments">
                            💬
                        </button>
                        <span className="post-action-count">{commentsCount}</span>
                    </div>
                </div>

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
                                        <div className="comment-main-row">
                                            {comment.userId ? (
                                                <Link to={`/profile/${comment.userId}`} className="comment-username-link">
                                                    <span className="comment-username">
                                                        <b>{commentUsernames[comment.userId] || `user${comment.userId}`}</b>
                                                    </span>
                                                </Link>
                                            ) : (
                                                <span className="comment-username">
                                                    <b>{commentUsernames[comment.userId] || `user${comment.userId}`}</b>
                                                </span>
                                            )}
                                            {editingCommentId === comment.id ? (
                                                <div className="comment-edit-row">
                                                    <input
                                                        className="comment-edit-input"
                                                        value={editingContent}
                                                        onChange={(e) => setEditingContent(e.target.value)}
                                                        autoFocus
                                                    />
                                                    <button className="comment-save-btn" onClick={() => handleSaveEdit(comment.id)}>Save</button>
                                                    <button className="comment-cancel-btn" onClick={() => setEditingCommentId(null)}>Cancel</button>
                                                </div>
                                            ) : (
                                                <span className="comment-content">{comment.content}</span>
                                            )}
                                            {Number(comment.userId) === Number(currentUserId) && editingCommentId !== comment.id && (
                                                <div className="comment-actions">
                                                    <button className="comment-edit-btn" onClick={() => handleEditComment(comment)} title="Edit">✏️</button>
                                                    <button className="comment-delete-btn" onClick={() => handleDeleteComment(comment.id)} title="Delete">🗑️</button>
                                                </div>
                                            )}
                                        </div>
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
