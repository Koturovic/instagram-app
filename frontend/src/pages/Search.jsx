import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { searchUsers, followUser } from "../services/userService";
import "./Search.css";

export default function Search() {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);
    const navigate = useNavigate();

    const handleSearch = async (e) => {
        e.preventDefault();
        
        if (!query.trim()) return;

        try {
            setLoading(true);
            setSearched(true);
            const data = await searchUsers(query);
            setResults(data);
        } catch (err) {
            console.error("Search error:", err);
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    const handleFollow = async (userId) => {
        try {
            await followUser(userId);
            alert("Follow request sent!");
            const data = await searchUsers(query);
            setResults(data);
        } catch (err) {
            console.error("Follow error:", err);
            alert("Failed to send follow request");
        }
    };

    return (
        <>
            <Navbar />
            <div className="search-container">
                <div className="search-box">
                    <h2>Search Users</h2>
                    
                    <form onSubmit={handleSearch} className="search-form">
                        <input
                            type="text"
                            placeholder="Search by username or name..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            className="search-input"
                        />
                        <button type="submit" className="search-btn">
                            🔍 Search
                        </button>
                    </form>

                    {loading && <p className="loading-text">Searching...</p>}

                    {!loading && searched && results.length === 0 && (
                        <div className="no-results">
                            <p>🔍 No users found for "{query}"</p>
                        </div>
                    )}

                    {!loading && results.length > 0 && (
                        <div className="results-list">
                            {results.map((user) => (
                                <div key={user.id} className="user-result-item">
                                    <div className="user-info">
                                        <img
                                            src={user.profileImageUrl || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg"}
                                            alt={user.username}
                                            className="user-avatar"
                                        />
                                        <div className="user-details">
                                            <h4 className="user-username">{user.username}</h4>
                                            <p className="user-fullname">
                                                {user.firstName} {user.lastName}
                                            </p>
                                            {user.isPrivate && (
                                                <span className="private-badge">🔒 Private</span>
                                            )}
                                        </div>
                                    </div>
                                    
                                    <div className="user-actions">
                                        <button
                                            className="view-profile-btn"
                                            onClick={() => navigate(`/profile/${user.id}`)}
                                        >
                                            View Profile
                                        </button>
                                        <button
                                            className="follow-btn"
                                            onClick={() => handleFollow(user.id)}
                                        >
                                            Follow
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
