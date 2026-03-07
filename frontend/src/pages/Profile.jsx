import { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import EditProfileModal from "../components/EditProfileModal";
import apiClient, { getUrl } from "../services/apiClient";
import { getProfileByUserId } from "../services/authService";
import { getUserIdFromToken } from "../utils/auth";
import { normalizePosts } from "../utils/postMapper";
import { followUser, unfollowUser, getFollowersCount, getFollowingCount, blockUser, unblockUser } from "../services/userService";
import "./Profile.css";

export default function Profile() {
    const EDIT_PROFILE_ENABLED = false;
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
    const [backendUnavailable, setBackendUnavailable] = useState(false);

    const fetchProfileData = useCallback(async () => {
        try {
            setLoading(true);
            setBackendUnavailable(false);
 
            // fetchovanje followers/following countera (user-service - 8081)
            // mora biti nezavisno od post endpointa, da brojac ne ostane 0 ako posts call padne
            try {
                const [followers, following] = await Promise.all([
                    getFollowersCount(targetUserId),
                    getFollowingCount(targetUserId),
                ]);
                setFollowersCount(followers);
                setFollowingCount(following);
            } catch (err) {
                console.error("Failed to fetch followers/following counters:", err?.response?.status, err);
            }

            // fetchovanje user podataka (auth-service - 8080)
            const authProfile = await getProfileByUserId(targetUserId);
            setUser(prev => ({
                ...prev,
                userId: authProfile.userId,
                username: authProfile.username || "",
                isPrivate: Boolean(authProfile.isPrivate),
            }));

            // fetchovanje post-ova (post-service - 8082)
            try {
                const postsRes = await apiClient.get(getUrl("POST", `/posts/user/${targetUserId}`));
                setUserPosts(normalizePosts(postsRes.data));
            } catch (postErr) {
                console.error("Failed to fetch user posts:", postErr?.response?.status, postErr);
                setUserPosts([]);
            }

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
        setUser(updatedUser);
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
                            <p>{user.bio || "No bio yet"}</p>
                            {user.isPrivate && <span className="private-badge">This is private account.</span>}
                        </div>
                    </section>
                </header>

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
                        userPosts.map(post => (
                            <div key={post.id} className="grid-item">
                                <img src={post.image} alt="user post" />
                                <div className="grid-item-overlay">
                                    <span>❤️ {post.likes}</span>
                                </div>
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
        </>
    );
}