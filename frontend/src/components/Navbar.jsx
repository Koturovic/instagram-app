import { useState } from "react";
import { useNavigate } from "react-router-dom";
import CreatePostModal from "./CreatePostModal";
import "./Navbar.css";

export default function Navbar() {
    const navigate = useNavigate();
    const [isModalOpen, setIsModalOpen] = useState(false);

    return (
        <>
            <div className="navbar">
                <h2 
                className="nav-logo" 
                onClick={() => navigate("/home")}>
                    Instagram
                </h2>

                <div className="nav-links">
                    <button aria-label="Home" title="Home" onClick={() => navigate("/home")}>
                        <svg viewBox="0 0 24 24" className="nav-icon" aria-hidden="true">
                            <path d="M3 10.5L12 3l9 7.5v9a1.5 1.5 0 0 1-1.5 1.5H15a1 1 0 0 1-1-1v-5H10v5a1 1 0 0 1-1 1H4.5A1.5 1.5 0 0 1 3 19.5v-9z" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round"/>
                        </svg>
                    </button>

                    <button aria-label="Search" title="Search" onClick={() => navigate("/search")}>
                        <svg viewBox="0 0 24 24" className="nav-icon" aria-hidden="true">
                            <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.6"/>
                            <path d="M16.2 16.2L21 21" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                        </svg>
                    </button>

                    <button aria-label="Create" title="Create" onClick={() => setIsModalOpen(true)} className="create-btn">
                        <svg viewBox="0 0 24 24" className="nav-icon" aria-hidden="true">
                            <path d="M12 5v14M5 12h14" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                        </svg>
                    </button>

                    <button aria-label="Profile" title="Profile" onClick={() => navigate("/profile")}>
                        <svg viewBox="0 0 24 24" className="nav-icon" aria-hidden="true">
                            <circle cx="12" cy="8" r="3.5" fill="none" stroke="currentColor" strokeWidth="1.6"/>
                            <path d="M4.5 20c1.7-3.4 5-5 7.5-5s5.8 1.6 7.5 5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                        </svg>
                    </button>

                    <button
                        aria-label="Logout"
                        title="Logout"
                        className="logout-btn"
                        onClick={() => {
                            localStorage.removeItem("token");
                            navigate("/");
                        }}
                    >
                        <svg viewBox="0 0 24 24" className="nav-icon" aria-hidden="true">
                            <path d="M10 4H6.5A1.5 1.5 0 0 0 5 5.5v13A1.5 1.5 0 0 0 6.5 20H10" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                            <path d="M14 7l5 5-5 5M19 12H10" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </button>
                </div>
            </div>

            <CreatePostModal 
                isOpen={isModalOpen} 
                onClose={() => setIsModalOpen(false)} 
            />
        </>
    )
}