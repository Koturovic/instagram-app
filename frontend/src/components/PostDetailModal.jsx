import { useState, useEffect, useCallback, useRef } from "react";
import { toggleLike, getComments, addComment, isPostLiked, getLikesCount } from "../services/interactionService";
import { deletePost } from "../services/postService";
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
    const captionRef = useRef(null);

    const currentUserId = getUserIdFromToken();
    const fallbackUsername = post?.userId ? `user${post.userId}` : "Unknown user";
    const displayUsername = resolvedUsername || post?.username || fallbackUsername;
    const avatarUrl = post?.avatar || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg";
    
    const mediaFiles = post?.mediaFiles && post.mediaFiles.length > 0
        ? post.mediaFiles
        : [{ fileUrl: post?.image || "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d?w=800&auto=format&fit=crop" }];
    
    const hasMultipleMedia = mediaFiles.length > 1;
    const hasPrevPost = allPosts && currentIndex > 0;
    const hasNextPost = allPosts && currentIndex < allPosts.length - 1;

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
            setCurrentCaption(post.caption || "");
            setCurrentMediaIndex(0);
            setIsCaptionExpanded(false);
        }
        if (!isOpen) {
            setComments([]);
            setNewComment("");
            setCommentsLoaded(false);
            setCurrentMediaIndex(0);
            setCommentUsernames({});
            setResolvedUsername("");
            setShowSettings(false);
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

        // Optimistic update - odmah menja UI pre nego što dobije response
        const wasLiked = liked;
        setLiked(!wasLiked);
        setLikes(prev => Math.max(0, prev + (wasLiked ? -1 : 1)));

        try {
            await toggleLike(post.id, currentUserId);
            const freshCount = await getLikesCount(post.id);
            setLikes(Number(freshCount ?? 0));
        } catch (err) {
            // Rollback ako nije uspelo
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
        setCurrentMediaIndex(prev => (prev === 0 ? mediaFiles.length - 1 : prev - 1));
    };

    const handleNextMedia = () => {
        setCurrentMediaIndex(prev => (prev === mediaFiles.length - 1 ? 0 : prev + 1));
    };

    const isOwner = currentUserId && post?.userId === currentUserId;

    if (!isOpen) return null;

    return (
        <div className="post-detail-modal-overlay" onClick={onClose}>
            <div
                className="post-detail-modal"
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
                    <div className="modal-media-side">
                        {mediaFiles[currentMediaIndex]?.contentType?.startsWith("video/") ? (
                            <video
                                src={mediaFiles[currentMediaIndex].fileUrl}
                                className="modal-media"
                                controls
                            />
                        ) : (
                            <img 
                                src={mediaFiles[currentMediaIndex].fileUrl} 
                                className="modal-media" 
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

                    {/* RIGHT SIDE - Details */}
                    <div className="modal-details-side">
                        {/* Header with username and avatar */}
                        <div className="modal-header">
                            <div className="modal-user-info">
                                <img src={avatarUrl} className="modal-avatar" alt="avatar" />
                                <span className="modal-username">{displayUsername}</span>
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
                                <b>{displayUsername}</b> {currentCaption}
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
                        </div>

                        {/* Likes */}
                        <div className="modal-likes">
                            <p><b>{likes} likes</b></p>
                        </div>

                        {/* Actions */}
                        <div className="modal-actions">
                            <button onClick={handleLike} className="like-btn">
                                {liked ? "❤️ Liked" : "🤍 Like"}
                            </button>
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
