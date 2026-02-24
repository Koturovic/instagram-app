import { useState } from "react";
import { getUserIdFromToken } from "../utils/auth";
import apiClient, { getUrl } from "../services/apiClient";
import "./EditProfileModal.css";

export default function EditProfileModal({ isOpen, onClose, currentUser, onUpdate }) {
    const [formData, setFormData] = useState({
        firstName: currentUser.firstName || "",
        lastName: currentUser.lastName || "",
        username: currentUser.username || "",
        bio: currentUser.bio || "",
        profileImageUrl: currentUser.profileImageUrl || "",
        isPrivate: currentUser.isPrivate || false
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const userId = getUserIdFromToken();

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            const response = await apiClient.put(
                getUrl("AUTH", `/users/${userId}`),
                formData
            );

            alert("Profile updated successfully!");
            onUpdate(response.data);
            onClose();
        } catch (err) {
            console.error("Error updating profile:", err);
            setError("Failed to update profile. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content edit-profile-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Edit Profile</h2>
                    <button className="close-btn" onClick={onClose}>×</button>
                </div>

                <form onSubmit={handleSubmit} className="edit-form">
                    <div className="form-group">
                        <label>First Name</label>
                        <input
                            type="text"
                            name="firstName"
                            value={formData.firstName}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Last Name</label>
                        <input
                            type="text"
                            name="lastName"
                            value={formData.lastName}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Username</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Bio</label>
                        <textarea
                            name="bio"
                            value={formData.bio}
                            onChange={handleChange}
                            rows={4}
                            placeholder="Tell us about yourself..."
                        />
                    </div>

                    <div className="form-group">
                        <label>Profile Image URL</label>
                        <input
                            type="url"
                            name="profileImageUrl"
                            value={formData.profileImageUrl}
                            onChange={handleChange}
                            placeholder="https://example.com/image.jpg"
                        />
                    </div>

                    <div className="form-group checkbox-group">
                        <label>
                            <input
                                type="checkbox"
                                name="isPrivate"
                                checked={formData.isPrivate}
                                onChange={handleChange}
                            />
                            <span>Private Account</span>
                        </label>
                        <p className="hint-text">
                            {formData.isPrivate 
                                ? "Only followers can see your posts" 
                                : "Anyone can see your posts"
                            }
                        </p>
                    </div>

                    {error && <p className="error-message">{error}</p>}

                    <button 
                        type="submit" 
                        className="submit-btn"
                        disabled={loading}
                    >
                        {loading ? "Saving..." : "Save changes"}
                    </button>
                </form>
            </div>
        </div>
    );
}
