import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import { searchUsers, followUser, unblockUser, getRelationshipStatus } from "../services/userService";
import "./Search.css";

export default function Search() {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);
    const [followStates, setFollowStates] = useState({});
    const [followLoading, setFollowLoading] = useState({});
    const navigate = useNavigate();

    const handleSearch = async (e) => {
        e.preventDefault();

        if (!query.trim()) return;

        try {
            setLoading(true);
            setSearched(true);
            const data = await searchUsers(query);

            const relationshipChecks = await Promise.all(
                (data || []).map(async (user) => {
                    try {
                        const status = await getRelationshipStatus(user.id);
                        return {
                            userId: user.id,
                            blocked: Boolean(status?.blocked),
                            following: Boolean(status?.following),
                            pending: Boolean(status?.pending),
                        };
                    } catch {
                        return {
                            userId: user.id,
                            blocked: Boolean(user?.blockedByCurrentUser),
                            following: false,
                            pending: false,
                        };
                    }
                })
            );

            const relationshipMap = Object.fromEntries(
                relationshipChecks.map((item) => [item.userId, item])
            );

            const enrichedResults = (data || []).map((user) => ({
                ...user,
                blockedByCurrentUser: Boolean(relationshipMap[user.id]?.blocked ?? user?.blockedByCurrentUser),
            }));

            const initialFollowStates = {};
            relationshipChecks.forEach((item) => {
                if (item.blocked) {
                    return;
                }
                if (item.following) {
                    initialFollowStates[item.userId] = "following";
                } else if (item.pending) {
                    initialFollowStates[item.userId] = "requested";
                }
            });

            setFollowStates(initialFollowStates);
            setResults(enrichedResults);
        } catch (err) {
            console.error("Search error:", err);
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    const handleFollow = async (userId) => {
        if (followStates[userId] === "requested" || followStates[userId] === "following") {
            return;
        }

        try {
            setFollowLoading((prev) => ({ ...prev, [userId]: true }));
            const response = await followUser(userId);
            const status = response?.followed === true ? "following" : "requested";
            setFollowStates((prev) => ({ ...prev, [userId]: status }));
        } catch (err) {
            console.error("Follow error:", err);
            alert("Failed to send follow request");
        } finally {
            setFollowLoading((prev) => ({ ...prev, [userId]: false }));
        }
    };

    const handleUnblock = async (userId) => {
        try {
            setFollowLoading((prev) => ({ ...prev, [userId]: true }));
            await unblockUser(userId);
            setResults((prev) => prev.map((u) => (
                u.id === userId ? { ...u, blockedByCurrentUser: false } : u
            )));
        } catch (err) {
            console.error("Unblock error:", err);
            alert("Failed to unblock user");
        } finally {
            setFollowLoading((prev) => ({ ...prev, [userId]: false }));
        }
    };

    const getFollowButtonLabel = (userId) => {
        if (followLoading[userId]) return "...";
        if (followStates[userId] === "requested") return "Requested";
        if (followStates[userId] === "following") return "Following";
        return "Follow";
    };

    const getFollowButtonClassName = (userId) => {
        const state = followStates[userId];
        if (state === "requested") return "follow-btn requested";
        if (state === "following") return "follow-btn following";
        return "follow-btn";
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
                                        {user.blockedByCurrentUser ? (
                                            <button
                                                className="follow-btn unblock-btn"
                                                onClick={() => handleUnblock(user.id)}
                                                disabled={followLoading[user.id]}
                                            >
                                                {followLoading[user.id] ? "..." : "Unblock"}
                                            </button>
                                        ) : (
                                            <button
                                                className={getFollowButtonClassName(user.id)}
                                                onClick={() => handleFollow(user.id)}
                                                disabled={
                                                    followLoading[user.id] ||
                                                    followStates[user.id] === "requested" ||
                                                    followStates[user.id] === "following"
                                                }
                                            >
                                                {getFollowButtonLabel(user.id)}
                                            </button>
                                        )}
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
