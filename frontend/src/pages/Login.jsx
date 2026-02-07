import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../services/authService";
import "./Login.css";
import avatar from "./avatar.png";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
  document.title = "Login | Instagram";
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");

    if (!email.includes("@")) {
      return setError("Invalid email format");
    }
    if (password.length < 6) {
      return setError("Password must be at least 6 characters");
    }


    try {
      const data = await login(email, password);
      localStorage.setItem("token", data.token);
      alert("Login success!");
    } catch {
      setError("Wrong email or password");
    }
  };

  return (
    <div className="login-bg">
        
      <button className="top-register-btn" 
      onClick={() => navigate("/register")}>
        Create a new account
      </button>

      <div className="login-box">
      <img src={avatar} className="avatar" />
        <h2 className="logo">Instagram</h2>

        <form onSubmit={handleLogin}>
              <label>Email address</label>
              <input
                type="email"
                placeholder="Enter email"
                onChange={(e) => setEmail(e.target.value)}
              />

            <div className="password-row">
              <label>Password</label>
              <button
                type="button"
                className="show-btn"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Enter password"
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && <p className="error">{error}</p>}

          <button className="login-btn" type="submit">
            Log in
          </button>
        </form>

        <p className="signup-text">
          Don’t have an account? <span onClick={() => navigate("/register")}>Sign up</span>
        </p>
      </div>
    </div>
  );
}
