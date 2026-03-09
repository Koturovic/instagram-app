import { useState } from "react";
import { createPost } from "../services/postService";
import { POST_CAPTION_MAX_LENGTH } from "../constants/postLimits";
import "./CreatePostModal.css";

export default function CreatePostModal({ isOpen, onClose }) {
    const [description, setDescription] = useState("");
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [previewUrls, setPreviewUrls] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleFileChange = (e) => {
        const files = Array.from(e.target.files);
        
        if (files.length > 20) {
            setError("Maximum 20 files allowed");
            return;
        }
        
        const oversizedFiles = files.filter(file => file.size > 50 * 1024 * 1024);
        if (oversizedFiles.length > 0) {
            setError("Each file must be less than 50MB");
            return;
        }
        
        const invalidFiles = files.filter(file => 
            !file.type.startsWith("image/") && !file.type.startsWith("video/")
        );
        if (invalidFiles.length > 0) {
            setError("Only images and videos are allowed");
            return;
        }
        
        setError("");
        setSelectedFiles(files);
        
        const urls = files.map(file => URL.createObjectURL(file));
        setPreviewUrls(urls);
    };

    const removeFile = (index) => {
        const newFiles = selectedFiles.filter((_, i) => i !== index);
        const newUrls = previewUrls.filter((_, i) => i !== index);
        
        URL.revokeObjectURL(previewUrls[index]);
        
        setSelectedFiles(newFiles);
        setPreviewUrls(newUrls);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (selectedFiles.length === 0) {
            setError("Please select at least one file");
            return;
        }
        
        setLoading(true);
        setError("");
        
        try {
            await createPost(description, selectedFiles);
            // RESET forme
            setDescription("");
            setSelectedFiles([]);
            setPreviewUrls([]);
            
            alert("Post created successfully!");
            onClose();
        } catch (err) {
            console.error("Error creating post:", err);
            setError("Failed to create post. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        previewUrls.forEach(url => URL.revokeObjectURL(url));
        setDescription("");
        setSelectedFiles([]);
        setPreviewUrls([]);
        setError("");
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Create new post</h2>
                    <button className="close-btn" onClick={handleClose}>×</button>
                </div>

                <form onSubmit={handleSubmit}>
                    {/* upload fajla */}
                    <div className="file-upload-section">
                        <label htmlFor="file-input" className="file-upload-label">
                            {selectedFiles.length === 0 ? (
                                <>
                                    <span className="upload-icon">📁</span>
                                    <span>Select photos and videos</span>
                                    <span className="upload-hint">Max 20 files, 50MB each</span>
                                </>
                            ) : (
                                <span>Change files ({selectedFiles.length}/20)</span>
                            )}
                        </label>
                        <input
                            id="file-input"
                            type="file"
                            multiple
                            accept="image/*,video/*"
                            onChange={handleFileChange}
                            style={{ display: "none" }}
                        />
                    </div>

                    {/* preview slika*/}
                    {previewUrls.length > 0 && (
                        <div className="preview-grid">
                            {previewUrls.map((url, index) => (
                                <div key={index} className="preview-item">
                                    {selectedFiles[index].type.startsWith("video/") ? (
                                        <video src={url} controls className="preview-media" />
                                    ) : (
                                        <img src={url} alt={`Preview ${index + 1}`} className="preview-media" />
                                    )}
                                    <button
                                        type="button"
                                        className="remove-file-btn"
                                        onClick={() => removeFile(index)}
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* deskripcija */}
                    <textarea
                        className="description-input"
                        placeholder="Write a caption..."
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        rows={4}
                        maxLength={POST_CAPTION_MAX_LENGTH}
                    />

                    <p className="caption-counter">
                        {description.length}/{POST_CAPTION_MAX_LENGTH}
                    </p>

                    {/* error poruka */}
                    {error && <p className="error-message">{error}</p>}

                    {/* share button */}
                    <button 
                        type="submit" 
                        className="submit-btn"
                        disabled={loading || selectedFiles.length === 0}
                    >
                        {loading ? "Sharing..." : "Share"}
                    </button>
                </form>
            </div>
        </div>
    );
}
