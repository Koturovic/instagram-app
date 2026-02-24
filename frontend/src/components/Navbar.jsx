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
                    <button onClick={() => navigate("/home")}>
                        Home
                    </button>

                    <button onClick={() => navigate("/search")}>
                        🔍 Search
                    </button>

                    <button onClick={() => setIsModalOpen(true)} className="create-btn">
                        + Create
                    </button>

                    <button onClick={() => navigate("/profile")}>
                        Profile
                    </button>

                    <button onClick={() => {
                        localStorage.removeItem("token");
                        navigate("/");
                    }}>
                        Logout
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