import { useState, useEffect, useCallback, useRef } from "react";
import { Link } from "react-router-dom";
import { toggleLike, getComments, addComment, isPostLiked, getLikesCount, getCommentsCount, updateComment, deleteComment } from "../services/interactionService";
import { deletePost, deleteMediaFromPost } from "../services/postService";
import { getUserIdFromToken } from "../utils/auth";
import { getUsernameById } from "../services/authService";
import EditCaptionModal from "./EditCaptionModal";
import { POST_CAPTION_PREVIEW_LINES } from "../constants/postLimits";
import "./PostDetailModal.css";

export default function PostDetailModal({ post, isOpen, onClose, onDelete, onUpdate, allPosts, currentIndex, onNavigate }) {
    const [liked, setLiked] = useState(false);
    const [likes, setLikes] = useState(post?.likes ?? 0);
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loadingComments, setLoadingComments] = useState(false);
    const [commentsLoaded, setCommentsLoaded] = useState(false);
    const [currentMediaIndex, setCurrentMediaIndex] = useState(0);
    const [commentUsernames, setCommentUsernames] = useState({});
    const [resolvedUsername, setResolvedUsername] = useState("");
    const [showSettings, setShowSettings] = useState(false);
    const [isEditCaptionOpen, setIsEditCaptionOpen] = useState(false);
    const [currentCaption, setCurrentCaption] = useState(post?.caption || "");
    const [isCaptionExpanded, setIsCaptionExpanded] = useState(false);
    const [isCaptionOverflowing, setIsCaptionOverflowing] = useState(false);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editingContent, setEditingContent] = useState("");
    const [commentsCount, setCommentsCount] = useState(0);
    const [currentImageAspectRatio, setCurrentImageAspectRatio] = useState(1);
    const [mediaFiles, setMediaFiles] = useState(
        post?.mediaFiles && post.mediaFiles.length > 0
            ? post.mediaFiles
            : [{ fileUrl: post?.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }]
    );
    const captionRef = useRef(null);

    const currentUserId = getUserIdFromToken();
    const fallbackUsername = post?.userId ? `user${post.userId}` : "Unknown user";
    const displayUsername = resolvedUsername || post?.username || fallbackUsername;
    const avatarUrl = post?.avatar || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg";
    
    const hasMultipleMedia = mediaFiles.length > 1;
    const canGoPrevMedia = currentMediaIndex > 0;
    const canGoNextMedia = currentMediaIndex < mediaFiles.length - 1;
    const hasPrevPost = allPosts && currentIndex > 0;
    const hasNextPost = allPosts && currentIndex < allPosts.length - 1;
    const isCurrentMediaVideo = mediaFiles[currentMediaIndex]?.contentType?.startsWith("video/");
    const isCurrentMediaImage = !isCurrentMediaVideo;

    const fetchComments = useCallback(async () => {
        if (!post?.id) return;
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
    }, [post?.id]);

    useEffect(() => {
        if (isOpen && !commentsLoaded && post?.id) {
            fetchComments();
        }
    }, [isOpen, commentsLoaded, fetchComments, post?.id]);

    useEffect(() => {
        let cancelled = false;
        if (isOpen && post?.id) {
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
    }, [isOpen, post?.id]);

    useEffect(() => {
        let cancelled = false;
        if (currentUserId && post?.id) {
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
    }, [currentUserId, post?.id, isOpen]);

    useEffect(() => {
        let cancelled = false;
        if (post?.userId && post?.username === fallbackUsername) {
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
    }, [post?.userId, post?.username, fallbackUsername, isOpen]);

    // Reset state kada se modal otvori/zatvori ili post promeni
    useEffect(() => {
        if (isOpen && post) {
            setLikes(post.likes ?? 0);
            setCommentsCount(0);
            setCurrentCaption(post.caption || "");
            setCurrentMediaIndex(0);
            setCurrentImageAspectRatio(1);
            setIsCaptionExpanded(false);
            setMediaFiles(
                post.mediaFiles && post.mediaFiles.length > 0
                    ? post.mediaFiles
                    : [{ fileUrl: post.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }]
            );
        }
        if (!isOpen) {
            setComments([]);
            setNewComment("");
            setCommentsLoaded(false);
            setCurrentMediaIndex(0);
            setCommentUsernames({});
            setResolvedUsername("");
            setShowSettings(false);
            setEditingCommentId(null);
            setEditingContent("");
            setCurrentImageAspectRatio(1);
            setMediaFiles(
                post?.mediaFiles && post.mediaFiles.length > 0
                    ? post.mediaFiles
                    : [{ fileUrl: post?.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }]
            );
        }
    }, [isOpen, post]);

    useEffect(() => {
        if (!isOpen || isCaptionExpanded) {
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
    }, [isOpen, currentCaption, isCaptionExpanded, post?.id]);

    useEffect(() => {
        if (!isOpen) return;

        const handleKeyDown = (event) => {
            if (event.key === "Escape") {
                if (showSettings) {
                    setShowSettings(false);
                } else if (isEditCaptionOpen) {
                    setIsEditCaptionOpen(false);
                } else {
                    onClose();
                }
            }
        };

        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [isOpen, showSettings, isEditCaptionOpen, onClose]);

    const handleLike = async () => {
        if (!currentUserId) {
            alert("Please login to like posts");
            return;
        }

        if (!post?.id) return;

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

        if (!newComment.trim() || !post?.id) return;

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
        } catch (err) {
            console.error("Error deleting comment:", err);
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

    const handleDelete = async () => {
        if (!currentUserId) {
            alert("Please login to delete posts");
            return;
        }

        if (post?.userId !== currentUserId) {
            alert("You can only delete your own posts");
            return;
        }

        const confirmed = window.confirm("Are you sure you want to delete this post?");
        if (!confirmed) return;

        try {
            await deletePost(post.id);
            alert("Post deleted successfully!");
            if (onDelete) {
                onDelete(post.id);
            }
            onClose();
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
        setCurrentCaption(updatedPost.caption);
        if (onUpdate) {
            onUpdate(updatedPost);
        }
    };

    const handlePrevPost = () => {
        if (hasPrevPost && onNavigate) {
            onNavigate(currentIndex - 1);
            setShowSettings(false);
            setCommentsLoaded(false);
        }
    };

    const handleNextPost = () => {
        if (hasNextPost && onNavigate) {
            onNavigate(currentIndex + 1);
            setShowSettings(false);
            setCommentsLoaded(false);
        }
    };

    const handlePrevMedia = () => {
        setCurrentMediaIndex(prev => (prev > 0 ? prev - 1 : prev));
    };

    const handleNextMedia = () => {
        setCurrentMediaIndex(prev => (prev < mediaFiles.length - 1 ? prev + 1 : prev));
    };

    const handleImageLoad = (event) => {
        const { naturalWidth, naturalHeight } = event.currentTarget;
        if (!naturalWidth || !naturalHeight) {
            setCurrentImageAspectRatio(1);
            return;
        }

        const ratio = naturalWidth / naturalHeight;
        setCurrentImageAspectRatio(Math.max(0.45, Math.min(2.2, ratio)));
    };

    const isOwner = currentUserId && post?.userId === currentUserId;

    if (!isOpen) return null;

    return (
        <div className="post-detail-modal-overlay" onClick={onClose}>
            <div
                className={`post-detail-modal ${isCurrentMediaImage ? "image-mode" : "video-mode"}`}
                onClick={(e) => {
                    e.stopPropagation();
                    if (showSettings) {
                        setShowSettings(false);
                    }
                }}
            >
                {/* Navigation arrows za post grid */}
                {hasPrevPost && (
                    <button className="post-nav-btn post-nav-prev" onClick={handlePrevPost}>
                        ‹
                    </button>
                )}
                {hasNextPost && (
                    <button className="post-nav-btn post-nav-next" onClick={handleNextPost}>
                        ›
                    </button>
                )}

                <div className="post-detail-content">
                    {/* LEFT SIDE - Media */}
                    <div
                        className="modal-media-side"
                        style={isCurrentMediaImage ? { aspectRatio: String(currentImageAspectRatio) } : undefined}
                    >
                        {isCurrentMediaVideo ? (
                            <video
                                src={mediaFiles[currentMediaIndex].fileUrl}
                                className="modal-media modal-media-video"
                                controls
                            />
                        ) : (
                            <img 
                                src={mediaFiles[currentMediaIndex].fileUrl} 
                                className="modal-media modal-media-image" 
                                alt="post content" 
                                onLoad={handleImageLoad}
                            />
                        )}
                        
                        {hasMultipleMedia && (
                            <>
                                {canGoPrevMedia && (
                                    <button className="modal-carousel-btn modal-carousel-btn-prev" onClick={handlePrevMedia}>
                                        ‹
                                    </button>
                                )}
                                {canGoNextMedia && (
                                    <button className="modal-carousel-btn modal-carousel-btn-next" onClick={handleNextMedia}>
                                        ›
                                    </button>
                                )}
                                <div className="modal-carousel-indicator">
                                    {currentMediaIndex + 1} / {mediaFiles.length}
                                </div>
                            </>
                        )}
                    </div>

                    {/* RIGHT SIDE - Details */}
                    <div className="modal-details-side">
                        {/* Header with username and avatar */}
                        <div className="modal-header">
                            <div className="modal-user-info">
                                <img src={avatarUrl} className="modal-avatar" alt="avatar" />
                                {post?.userId ? (
                                    <Link to={`/profile/${post.userId}`} className="modal-username-link">
                                        <span className="modal-username">{displayUsername}</span>
                                    </Link>
                                ) : (
                                    <span className="modal-username">{displayUsername}</span>
                                )}
                            </div>
                            <div className="modal-header-actions">
                                {isOwner && (
                                <div className="settings-menu-container" onClick={(e) => e.stopPropagation()}>
                                    <button 
                                        className="settings-btn" 
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setShowSettings(!showSettings);
                                        }}
                                        title="Post settings"
                                    >
                                        ⋯
                                    </button>
                                    
                                    {showSettings && (
                                        <div className="settings-dropdown">
                                            <button 
                                                className="settings-item"
                                                onClick={handleEditCaption}
                                            >
                                                Edit Caption
                                            </button>
                                            {hasMultipleMedia && (
                                                <button 
                                                    className="settings-item"
                                                    onClick={handleDeleteCurrentMedia}
                                                >
                                                    Delete {mediaFiles[currentMediaIndex]?.contentType?.startsWith("video/") ? "Video" : "Image"}
                                                </button>
                                            )}
                                            <button 
                                                className="settings-item delete-item"
                                                onClick={handleDelete}
                                            >
                                                Delete Post
                                            </button>
                                        </div>
                                    )}
                                </div>
                                )}
                                <button
                                    className="modal-close-inline-btn"
                                    onClick={onClose}
                                    title="Close"
                                >
                                    ✕
                                </button>
                            </div>
                        </div>

                        {/* Caption */}
                        <div className={`modal-caption ${isCaptionExpanded ? "expanded" : ""}`}>
                            <p
                                ref={captionRef}
                                className={!isCaptionExpanded ? "collapsed" : ""}
                                style={{ WebkitLineClamp: POST_CAPTION_PREVIEW_LINES }}
                            >
                                {post?.userId ? (
                                    <Link to={`/profile/${post.userId}`} className="modal-caption-username-link">
                                        <b>{displayUsername}</b>
                                    </Link>
                                ) : (
                                    <b>{displayUsername}</b>
                                )} {currentCaption}
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

                        {/* Comments section */}
                        <div className="modal-comments-section">
                            {loadingComments ? (
                                <p className="loading-text">Loading comments...</p>
                            ) : comments.length > 0 ? (
                                <div className="comments-list">
                                    {comments.map((comment) => (
                                        <div key={comment.id} className="comment-item">
                                            <div className="comment-main-row">
                                                {comment.userId ? (
                                                    <Link to={`/profile/${comment.userId}`} className="modal-comment-username-link">
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
                        </div>


                        {/* Actions */}
                        <div className="modal-actions">
                            <div className="modal-action-item">
                                <button onClick={handleLike} className="modal-like-btn" aria-label="Like post">
                                    {liked ? "❤️" : "🤍"}
                                </button>
                                <span className="modal-action-count">{likes}</span>
                            </div>
                            <div className="modal-action-item">
                                <span className="modal-comment-btn" aria-label="Comments">
                                    💬
                                </span>
                                <span className="modal-action-count">{commentsCount}</span>
                            </div>
                        </div>

                        {/* Add comment form */}
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
                </div>
            </div>

            {/* Edit Caption Modal */}
            <EditCaptionModal
                post={post}
                isOpen={isEditCaptionOpen}
                onClose={() => setIsEditCaptionOpen(false)}
                onUpdate={handleCaptionUpdate}
            />
        </div>
    );
}
