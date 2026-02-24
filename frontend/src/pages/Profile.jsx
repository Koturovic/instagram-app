import { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import EditProfileModal from "../components/EditProfileModal";
import apiClient, { getUrl } from "../services/apiClient";
import { getUserIdFromToken } from "../utils/auth";
import { followUser, unfollowUser, getFollowers, getFollowing } from "../services/userService";
import "./Profile.css";

export default function Profile() {
    const { userId: profileUserId } = useParams(); // id korisnika iz url-a
    const currentUserId = getUserIdFromToken(); // ulogovani korisnik
    const targetUserId = profileUserId || currentUserId; // ako nema korisnika u url-u, prikazujemo ulogovan profil
    const isOwnProfile = !profileUserId || profileUserId === String(currentUserId);

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
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    const fetchProfileData = useCallback(async () => {
        try {
            setLoading(true);
            
            // fetchovanje user podataka (auth-service - 8080)
            const userRes = await apiClient.get(getUrl("AUTH", `/users/${targetUserId}`));
            setUser(userRes.data);

            // fetchovanje post-ova (post-service - 8082)
            const postsRes = await apiClient.get(getUrl("POST", `/posts/user/${targetUserId}`));
            setUserPosts(postsRes.data);

            // fetchovanje followers/following countera (user-service - 8081)
            try {
                const followersRes = await getFollowers(targetUserId);
                const followingRes = await getFollowing(targetUserId);
                setFollowersCount(followersRes.length);
                setFollowingCount(followingRes.length);

                if (!isOwnProfile) {
                    const isUserFollowing = followersRes.some(
                        follower => follower.id === currentUserId
                    );
                    setIsFollowing(isUserFollowing);
                }
            } catch (err) {
                console.error("User service not available yet:", err);
                // brojac je 0, sve dok servis ne bude AKTIVAN
                // nakon refresh-a stranice, brojaci ce biti True
            }

        } catch (err) {
            console.error("Error fetching profile data:", err);
        } finally {
            setLoading(false);
        }
    }, [targetUserId, currentUserId, isOwnProfile]);

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
            } else {
                await followUser(targetUserId);
                setIsFollowing(true);
                setFollowersCount(prev => prev + 1);
            }
        } catch (err) {
            console.error("Follow/Unfollow error:", err);
            alert("Action failed. User service might not be available.");
        }
    };

    const handleProfileUpdate = (updatedUser) => {
        setUser(updatedUser);
    };

    return (
        <>
            <Navbar />
            <div className="profile-container">
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
                                    onClick={() => setIsEditModalOpen(true)}
                                >
                                    Edit Profile
                                </button>
                            ) : (
                                <button 
                                    className={isFollowing ? "unfollow-btn" : "follow-btn"}
                                    onClick={handleFollowToggle}
                                >
                                    {isFollowing ? "Unfollow" : "Follow"}
                                </button>
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