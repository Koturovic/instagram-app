// components/Navbar.jsx
import { useNavigate } from "react-router-dom";
import "./Navbar.css";

export default function Navbar() {
    const navigate = useNavigate();

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
        </>
    )
}