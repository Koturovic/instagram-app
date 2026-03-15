import { useState, useEffect } from "react";
import { getUserIdFromToken } from "../utils/auth";
import { updateProfile } from "../services/authService";
import "./EditProfileModal.css";

export default function EditProfileModal({ isOpen, onClose, currentUser, onUpdate }) {
    const [formData, setFormData] = useState({
        firstName: "",
        lastName: "",
        username: "",
        bio: "",
        isPrivate: false
    });
    const [profileImage, setProfileImage] = useState(null);
    const [imagePreview, setImagePreview] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const userId = getUserIdFromToken();

    // Popunjavanje forme sa trenutnim vrednostima kada se modal otvori
    // Ovo se izvršava svaki put kada se modal otvori ili se currentUser promeni
    useEffect(() => {
        if (isOpen && currentUser) {
            setFormData({
                firstName: currentUser.firstName || "",
                lastName: currentUser.lastName || "",
                username: currentUser.username || "",
                bio: currentUser.bio || "",
                isPrivate: currentUser.isPrivate === true || false
            });
            setImagePreview(currentUser.profileImageUrl || "");
            setProfileImage(null);
            setError("");
        }
    }, [isOpen, currentUser]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === "checkbox" ? checked : value
        }));
    };

    const handleImageChange = (e) => {
        const file = e.target.files?.[0];
        if (file) {
            setProfileImage(file);
            const reader = new FileReader();
            reader.onload = (ev) => {
                setImagePreview(ev.target?.result || "");
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            const form = new FormData();
            form.append("firstName", formData.firstName);
            form.append("lastName", formData.lastName);
            form.append("username", formData.username);
            form.append("bio", formData.bio);
            form.append("isPrivate", formData.isPrivate);
            
            if (profileImage) {
                form.append("profileImage", profileImage);
            }

            const response = await updateProfile(userId, form);
            alert("Profile updated successfully!");
            onUpdate(response);
            onClose();
        } catch (err) {
            console.error("Error updating profile:", err);
            setError(err.response?.data?.message || "Failed to update profile. Please try again.");
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
                        <label>Profile Picture</label>
                        <div className="profile-image-preview">
                            <img 
                                src={imagePreview || "https://thumbs.dreamstime.com/b/default-avatar-profile-trendy-style-social-media-user-icon-187599373.jpg"} 
                                alt="Profile preview" 
                            />
                        </div>
                        <input
                            type="file"
                            name="profileImage"
                            accept="image/*"
                            onChange={handleImageChange}
                        />
                        <small>Upload a new profile picture (optional)</small>
                    </div>

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
