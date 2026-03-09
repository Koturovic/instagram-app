import { useState, useEffect, useCallback, useRef } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import EditProfileModal from "../components/EditProfileModal";
import PostDetailModal from "../components/PostDetailModal";
import apiClient, { getUrl } from "../services/apiClient";
import { getProfileByUserId } from "../services/authService";
import { getUserIdFromToken } from "../utils/auth";
import { normalizePosts } from "../utils/postMapper";
import { POST_CAPTION_PREVIEW_LINES } from "../constants/postLimits";
import { followUser, unfollowUser, getFollowersCount, getFollowingCount, blockUser, unblockUser, acceptFollowRequest, rejectFollowRequest } from "../services/userService";
import "./Profile.css";

export default function Profile() {
    const EDIT_PROFILE_ENABLED = true;
    const { userId: profileUserId } = useParams(); // id korisnika iz url-a
    const currentUserId = getUserIdFromToken(); // ulogovani korisnik
    const hasInvalidProfileParam = Boolean(profileUserId) && !/^\d+$/.test(profileUserId);
    const targetUserId = hasInvalidProfileParam ? currentUserId : (profileUserId || currentUserId); // ako nema korisnika u url-u, prikazujemo ulogovan profil
    const isOwnProfile = hasInvalidProfileParam || !profileUserId || profileUserId === String(currentUserId);

    const [user, setUser] = useState({ 
        username: "", 
        firstName: "", 
        lastName: "", 
        bio: "",
        profileImageUrl: "",
        isPrivate: false
    });
    const [userPosts, setUserPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [followersCount, setFollowersCount] = useState(0);
    const [followingCount, setFollowingCount] = useState(0);
    const [isFollowing, setIsFollowing] = useState(false);
    const [isPendingRequest, setIsPendingRequest] = useState(false);
    const [isBlocked, setIsBlocked] = useState(false);
    const [showMenu, setShowMenu] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isPostDetailOpen, setIsPostDetailOpen] = useState(false);
    const [selectedPost, setSelectedPost] = useState(null);
    const [selectedPostIndex, setSelectedPostIndex] = useState(0);
    const [backendUnavailable, setBackendUnavailable] = useState(false);
    const [isBioExpanded, setIsBioExpanded] = useState(false);
    const [isBioOverflowing, setIsBioOverflowing] = useState(false);
    const bioRef = useRef(null);
    const [pendingFollowRequests, setPendingFollowRequests] = useState([]);
    const [loadingFollowRequests, setLoadingFollowRequests] = useState(false);

    const fetchProfileData = useCallback(async () => {
        try {
            setLoading(true);
            setBackendUnavailable(false);
            
            // fetchovanje user podataka (auth-service - 8080)
            const authProfile = await getProfileByUserId(targetUserId);
            setUser(prev => ({
                ...prev,
                userId: authProfile.userId,
                username: authProfile.username || "",
                firstName: authProfile.firstName || "",
                lastName: authProfile.lastName || "",
                bio: authProfile.bio || "",
                profileImageUrl: authProfile.profileImageUrl || "",
                isPrivate: Boolean(authProfile.isPrivate),
            }));

            // fetchovanje post-ova (post-service - 8082)
            const postsRes = await apiClient.get(getUrl("POST", `/posts/user/${targetUserId}`));
            let posts = normalizePosts(postsRes.data);

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

            setUserPosts(posts);

            // Učitavanje postova ne sme da čeka user-service brojače.
            // Brojače osvežavamo odvojeno da profil ne ostane na "Loading posts...".
            Promise.all([
                getFollowersCount(targetUserId),
                getFollowingCount(targetUserId)
            ])
                .then(([followers, following]) => {
                    setFollowersCount(followers);
                    setFollowingCount(following);
                })
                .catch((err) => {
                    console.error("User service not available yet:", err);
                });

        } catch (err) {
            console.error("Error fetching profile data:", err);
            setBackendUnavailable(true);
        } finally {
            setLoading(false);
        }
    }, [targetUserId]);

    useEffect(() => {
        document.title = "Profile | Instagram";
        
        if (targetUserId) {
            fetchProfileData();
        }
    }, [targetUserId, fetchProfileData]);

    useEffect(() => {
        setIsBioExpanded(false);
    }, [targetUserId, user.bio]);

    // Učitaj pending follow requests samo za own profil
    useEffect(() => {
        if (!isOwnProfile) {
            setPendingFollowRequests([]);
            return;
        }

        const fetchPendingRequests = async () => {
            try {
                setLoadingFollowRequests(true);
                const response = await apiClient.get(getUrl("USER", "/follow-requests/pending"));
                setPendingFollowRequests(response.data || []);
            } catch (err) {
                console.error("Error fetching pending follow requests:", err);
                setPendingFollowRequests([]);
            } finally {
                setLoadingFollowRequests(false);
            }
        };

        fetchPendingRequests();
    }, [isOwnProfile, targetUserId]);

    useEffect(() => {
        if (isBioExpanded) {
            return;
        }

        const bioElement = bioRef.current;
        if (!bioElement) {
            setIsBioOverflowing(false);
            return;
        }

        const measureOverflow = () => {
            setIsBioOverflowing(bioElement.scrollHeight > bioElement.clientHeight + 1);
        };

        measureOverflow();
        window.addEventListener("resize", measureOverflow);
        return () => {
            window.removeEventListener("resize", measureOverflow);
        };
    }, [user.bio, isBioExpanded, targetUserId]);

    const handleFollowToggle = async () => {
        try {
            if (isFollowing) {
                await unfollowUser(targetUserId);
                setIsFollowing(false);
                setFollowersCount(prev => prev - 1);
            } else if (isPendingRequest) {
                // TODO: implementirati cancel follow request (backend needs endpoint)
                alert("Cannot cancel request yet - backend endpoint pending");
            } else {
                await followUser(targetUserId);
                if (user.isPrivate) {
                    setIsPendingRequest(true);
                    alert("Follow request sent! Waiting for approval.");
                } else {
                    setIsFollowing(true);
                    setFollowersCount(prev => prev + 1);
                }
            }
        } catch (err) {
            console.error("Follow/Unfollow error:", err);
            alert("Action failed. User service might not be available.");
        }
    };

    const handleProfileUpdate = (updatedUser) => {
        // Ažurira sve poljea sa backend response-a
        setUser(prev => ({
            ...prev,
            userId: updatedUser.userId,
            username: updatedUser.username || "",
            firstName: updatedUser.firstName || "",
            lastName: updatedUser.lastName || "",
            bio: updatedUser.bio || "",
            profileImageUrl: updatedUser.profileImageUrl || "",
            isPrivate: Boolean(updatedUser.isPrivate)
        }));
        // Sada učitaj postove ponovo jer se korisnik mogao promeniti
        fetchProfileData();
    };

    const handleBlockToggle = async () => {
        try {
            if (isBlocked) {
                await unblockUser(targetUserId);
                setIsBlocked(false);
                alert("User unblocked successfully");
            } else {
                const confirmed = window.confirm("Are you sure you want to block this user? They won't be able to find your profile or see your posts.");
                if (!confirmed) return;
                
                await blockUser(targetUserId);
                setIsBlocked(true);
                setIsFollowing(false); 
                alert("User blocked successfully");
            }
            setShowMenu(false);
        } catch (err) {
            console.error("Block/Unblock error:", err);
            alert("Action failed. User service might not be available.");
        }
    };

    const handleOpenPostDetail = (post, index) => {
        setSelectedPost(post);
        setSelectedPostIndex(index);
        setIsPostDetailOpen(true);
    };

    const handleClosePostDetail = () => {
        setIsPostDetailOpen(false);
        setSelectedPost(null);
        setSelectedPostIndex(0);
    };

    const handleNavigatePost = (newIndex) => {
        if (newIndex >= 0 && newIndex < userPosts.length) {
            setSelectedPost(userPosts[newIndex]);
            setSelectedPostIndex(newIndex);
        }
    };

    const handlePostDelete = (postId) => {
        // Uklanja post iz grid-a nakon brisanja
        setUserPosts(prev => prev.filter(post => post.id !== postId));
    };

    const handlePostUpdate = (updatedPost) => {
        // Ažurira post u listi nakon editovanja caption-a
        setUserPosts(prev => prev.map(p => p.id === updatedPost.id ? { ...p, caption: updatedPost.caption } : p));
        setSelectedPost(prev => prev?.id === updatedPost.id ? { ...prev, caption: updatedPost.caption } : prev);
    };

    const handleAcceptFollowRequest = async (requestId, requesterUserId) => {
        try {
            await acceptFollowRequest(requestId);
            // Ukloni prihvaćeni zahtev iz liste
            setPendingFollowRequests(prev => prev.filter(req => req.requestId !== requestId));
            // Ažuriraj brojač followers-a
            setFollowersCount(prev => prev + 1);
            alert("Follow request accepted!");
        } catch (err) {
            console.error("Error accepting follow request:", err);
            alert("Failed to accept follow request");
        }
    };

    const handleRejectFollowRequest = async (requestId) => {
        try {
            await rejectFollowRequest(requestId);
            // Ukloni odbijeni zahtev iz liste
            setPendingFollowRequests(prev => prev.filter(req => req.requestId !== requestId));
            alert("Follow request rejected!");
        } catch (err) {
            console.error("Error rejecting follow request:", err);
            alert("Failed to reject follow request");
        }
    };

    return (
        <>
            <Navbar />
            <div className="profile-container">
                {hasInvalidProfileParam && (
                    <p className="profile-notice">Neispravan profile URL parametar. Prikazan je tvoj profil.</p>
                )}
                {backendUnavailable && (
                    <p className="profile-notice">Backend servisi nisu pokrenuti. Ovo je frontend fallback prikaz.</p>
                )}
                <header className="profile-header">
                    <div className="profile-image">
                        <img 
                            src={user.profileImageUrl || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg"} 
                            alt={user.username} 
                        />
                    </div>
                    
                    <section className="profile-info">
                        <div className="profile-top-row">
                            <h2 className="profile-username">{user.username || "username"}</h2>
                            
                            {isOwnProfile ? (
                                <button 
                                    className="edit-profile-btn"
                                    onClick={() => EDIT_PROFILE_ENABLED && setIsEditModalOpen(true)}
                                    disabled={!EDIT_PROFILE_ENABLED}
                                    title={EDIT_PROFILE_ENABLED ? "Edit profile" : "Backend endpoint pending"}
                                >
                                    Edit Profile
                                </button>
                            ) : (
                                <>
                                    <button 
                                        className={isPendingRequest ? "requested-btn" : (isFollowing ? "unfollow-btn" : "follow-btn")}
                                        onClick={handleFollowToggle}
                                    >
                                        {isPendingRequest ? "Requested" : (isFollowing ? "Unfollow" : "Follow")}
                                    </button>
                                    
                                    <div className="profile-menu-container">
                                        <button 
                                            className="profile-menu-btn"
                                            onClick={() => setShowMenu(!showMenu)}
                                        >
                                            ⋯
                                        </button>
                                        
                                        {showMenu && (
                                            <div className="profile-dropdown-menu">
                                                <button 
                                                    className="menu-item"
                                                    onClick={handleBlockToggle}
                                                >
                                                    {isBlocked ? "Unblock User" : "Block User"}
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </>
                            )}
                        </div>

                        <div className="profile-stats">
                            <span><b>{userPosts.length}</b> posts</span>
                            <span><b>{followersCount}</b> followers</span>
                            <span><b>{followingCount}</b> following</span>
                        </div>

                        <div className="profile-bio">
                            <b>{user.firstName} {user.lastName}</b>
                            <p
                                ref={bioRef}
                                className={`profile-bio-text ${!isBioExpanded ? "collapsed" : ""}`}
                                style={{ WebkitLineClamp: POST_CAPTION_PREVIEW_LINES }}
                            >
                                {user.bio || "No bio yet"}
                            </p>
                            {!isBioExpanded && isBioOverflowing && (
                                <button
                                    type="button"
                                    className="profile-bio-expand-btn"
                                    onClick={() => setIsBioExpanded(true)}
                                >
                                    See more
                                </button>
                            )}
                            {user.isPrivate && <span className="private-badge">This is private account.</span>}
                        </div>
                    </section>
                </header>

                {isOwnProfile && pendingFollowRequests.length > 0 && (
                    <section className="follow-requests-section">
                        <h3 className="follow-requests-title">Follow Requests ({pendingFollowRequests.length})</h3>
                        <div className="follow-requests-list">
                            {pendingFollowRequests.map(request => (
                                <div key={request.requestId} className="follow-request-item">
                                    <div className="follow-request-user">
                                        <img 
                                            src={request.requesterProfileImage || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg"}
                                            alt={request.requesterUsername}
                                            className="follow-request-avatar"
                                        />
                                        <span className="follow-request-username">{request.requesterUsername}</span>
                                    </div>
                                    <div className="follow-request-actions">
                                        <button 
                                            className="follow-request-accept-btn"
                                            onClick={() => handleAcceptFollowRequest(request.requestId, request.requesterUserId)}
                                        >
                                            Accept
                                        </button>
                                        <button 
                                            className="follow-request-reject-btn"
                                            onClick={() => handleRejectFollowRequest(request.requestId)}
                                        >
                                            Reject
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </section>
                )}

                <hr className="profile-divider" />

                <div className="profile-grid">
                    {loading ? (
                        <p className="loading-text">Loading posts...</p>
                    ) : isBlocked ? (
                        <div className="blocked-user-message">
                            <div className="blocked-icon">🚫</div>
                            <h3>You blocked this user</h3>
                            <p>Unblock to see their posts</p>
                        </div>
                    ) : user.isPrivate && !isOwnProfile && !isFollowing ? (
                        <div className="private-account-message">
                            <div className="private-icon">🔒</div>
                            <h3>This Account is Private</h3>
                            <p>Follow this account to see their posts</p>
                        </div>
                    ) : userPosts.length > 0 ? (
                        userPosts.map((post, index) => (
                            <div 
                                key={post.id} 
                                className="grid-item"
                                onClick={() => handleOpenPostDetail(post, index)}
                            >
                                {post.mediaFiles?.[0]?.contentType?.startsWith("video/") ? (
                                    <video
                                        src={post.mediaFiles[0].fileUrl}
                                        className="grid-item-media"
                                        muted
                                    />
                                ) : (
                                    <img src={post.image} alt="user post" />
                                )}
                            </div>
                        ))
                    ) : (
                        <div className="no-posts">
                            <h3>No Posts Yet</h3>
                        </div>
                    )}
                </div>
            </div>

            <EditProfileModal
                isOpen={isEditModalOpen}
                onClose={() => setIsEditModalOpen(false)}
                currentUser={user}
                onUpdate={handleProfileUpdate}
            />

            <PostDetailModal
                post={selectedPost}
                isOpen={isPostDetailOpen}
                onClose={handleClosePostDetail}
                onDelete={handlePostDelete}
                onUpdate={handlePostUpdate}
                allPosts={userPosts}
                currentIndex={selectedPostIndex}
                onNavigate={handleNavigatePost}
            />
        </>
    );
}
