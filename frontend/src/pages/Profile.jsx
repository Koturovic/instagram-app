import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import apiClient, { getUrl } from "../services/apiClient";
import { getUserIdFromToken } from "../utils/auth";
import "./Profile.css";

export default function Profile() {
    const [user, setUser] = useState({ 
        username: "", 
        firstName: "", 
        lastName: "", 
        bio: "My Instagram Clone Profile" 
    });
    const [userPosts, setUserPosts] = useState([]);
    const [loading, setLoading] = useState(true);

    const userId = getUserIdFromToken();

    useEffect(() => {
        document.title = "Profile | Instagram";
        
        if (userId) {
            fetchProfileData();
        }
    }, [userId]);

    const fetchProfileData = async () => {
        try {
            setLoading(true);
            
            // 1. Povlačimo podatke o korisniku (Miljanov Auth Service - Port 8080)
            // Napomena: Proveri sa Miljanom tačnu rutu za profil, pretpostavljam /users/me ili /auth/me
            const userRes = await apiClient.get(getUrl("AUTH", `/users/${userId}`));
            setUser(prev => ({...prev, ...userRes.data}));

            // 2. Povlačimo sve objave tog korisnika (Nemanja/Sloba - Post Service - Port 8082)
            const postsRes = await apiClient.get(getUrl("POST", `/posts/user/${userId}`));
            setUserPosts(postsRes.data);

        } catch (err) {
            console.error("Greška pri učitavanju profila:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <Navbar />
            <div className="profile-container">
                <header className="profile-header">
                    <div className="profile-image">
                        {/* Avatar možeš vući iz user objekta ako ga Miljan čuva */}
                        <img src="https://i.pravatar.cc/150?img=3" alt="profile" />
                    </div>
                    
                    <section className="profile-info">
                        <div className="profile-top-row">
                            <h2 className="profile-username">{user.username || "username"}</h2>
                            <button className="edit-profile-btn">Edit Profile</button>
                        </div>

                        <div className="profile-stats">
                            <span><b>{userPosts.length}</b> posts</span>
                            <span><b>0</b> followers</span>
                            <span><b>0</b> following</span>
                        </div>

                        <div className="profile-bio">
                            <b>{user.firstName} {user.lastName}</b>
                            <p>{user.bio}</p>
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
        </>
    );
}