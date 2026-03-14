import { useState, useEffect } from "react";
import { updatePostDescription } from "../services/postService";
import { POST_CAPTION_MAX_LENGTH } from "../constants/postLimits";
import "./EditCaptionModal.css";

export default function EditCaptionModal({ post, isOpen, onClose, onUpdate }) {
    const [caption, setCaption] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (isOpen && post) {
            setCaption(post.caption || "");
            setError("");
        }
    }, [isOpen, post]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            await updatePostDescription(post.id, caption);
            alert("Caption updated successfully!");
            onUpdate({ ...post, caption });
            onClose();
        } catch (err) {
            console.error("Error updating caption:", err);
            setError(err.response?.data?.message || "Failed to update caption. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="edit-caption-overlay" onClick={onClose}>
            <div className="edit-caption-modal" onClick={(e) => e.stopPropagation()}>
                <div className="edit-caption-header">
                    <h3>Edit Caption</h3>
                    <button className="close-btn" onClick={onClose}>×</button>
                </div>

                <form onSubmit={handleSubmit} className="edit-caption-form">
                    {error && <div className="error-message">{error}</div>}

                    <div className="form-group">
                        <label>Caption</label>
                        <textarea
                            value={caption}
                            onChange={(e) => setCaption(e.target.value)}
                            rows={5}
                            placeholder="Write a caption..."
                            autoFocus
                            maxLength={POST_CAPTION_MAX_LENGTH}
                        />
                        <p className="caption-counter">
                            {caption.length}/{POST_CAPTION_MAX_LENGTH}
                        </p>
                    </div>

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="cancel-btn" disabled={loading}>
                            Cancel
                        </button>
                        <button type="submit" className="save-btn" disabled={loading}>
                            {loading ? "Saving..." : "Save"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
