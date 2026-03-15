import { useState, useEffect } from "react";
import { register } from "../services/authService";
import { useNavigate } from "react-router-dom";
import "./Register.css";
import retroCameras from "./slika1.jfif";

function Register() {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
  });

  const navigate = useNavigate();

  const [errors, setErrors] = useState({});
  const [usernameAvailable, setUsernameAvailable] = useState(true);

  const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  // Ovde ide baza za kreirani username
  // Ovo je samo simulacija kako radi ogranicenje za iskorisceni username
  const takenUsernames = ["admin", "testuser", "instagram", "root"];

  useEffect(() => {
    document.title = "Register | Instagram";
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData({
      ...formData,
      [name]: value,
    });

    if (name === "username") {
      if (!usernameRegex.test(value)) {
        setErrors((prev) => ({
          ...prev,
          username: "Username must be 3-20 chars, letters, numbers or _",
        }));
      } else if (takenUsernames.includes(value.toLowerCase())) {
        setUsernameAvailable(false);
        setErrors((prev) => ({
          ...prev,
          username: "Username already taken",
        }));
      } else {
        setUsernameAvailable(true);
        setErrors((prev) => ({ ...prev, username: "" }));
      }
    }
  };

  const validateForm = () => {
    let newErrors = {};

    if (!formData.firstName) newErrors.firstName = "First name is required";

    if (!formData.lastName) newErrors.lastName = "Last name is required";

    if (!usernameRegex.test(formData.username)) {
      newErrors.username = "Invalid username format";
    }
    if (!emailRegex.test(formData.email)) {
      newErrors.email = "Invalid email format";
    }
    if (formData.password.length < 6) {
      newErrors.password = "Password must be at least 6 characters";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      const data = await register(formData);

      localStorage.setItem("token", data.token);

      alert("Registration successful!");
      navigate("/");
    } catch (err) {
      console.error(err);
      const backendMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        (typeof err?.response?.data === "string" ? err.response.data : null);
      alert(backendMessage ? `Registration failed: ${backendMessage}` : "Registration failed! Please try again.");
    }
  };

  return (
    <div className="register-container">
      <section className="register-left-panel" style={{ backgroundImage: `url(${retroCameras})` }}>
        <div className="register-left-overlay" />
        <div className="register-left-copy">
          <p className="register-kicker">Retro Social Club</p>
          <h1>Capture moments, share stories, stay iconic.</h1>
          <p className="register-subcopy">
            Grab your digital film roll, let's make memories together. Join us.
          </p>
        </div>
      </section>

      <section className="register-right-panel">
        <div className="register-box">
          <h2>Create an Instagram account</h2>
          <p className="register-intro">Become a member of our vintage community.</p>

          <form onSubmit={handleSubmit}>
            <div className={`register-form-field ${formData.firstName ? "is-filled" : ""}`}>
              <input
                id="firstName"
                type="text"
                name="firstName"
                placeholder="First name"
                value={formData.firstName}
                onChange={handleChange}
              />
              <label htmlFor="firstName" className="floating-placeholder">First name</label>
            </div>
            {errors.firstName && <p className="error">{errors.firstName}</p>}

            <div className={`register-form-field ${formData.lastName ? "is-filled" : ""}`}>
              <input
                id="lastName"
                type="text"
                name="lastName"
                placeholder="Last name"
                value={formData.lastName}
                onChange={handleChange}
              />
              <label htmlFor="lastName" className="floating-placeholder">Last name</label>
            </div>
            {errors.lastName && <p className="error">{errors.lastName}</p>}

            <div className={`register-form-field ${formData.username ? "is-filled" : ""}`}>
              <input
                id="username"
                type="text"
                name="username"
                placeholder="Username"
                value={formData.username}
                onChange={handleChange}
              />
              <label htmlFor="username" className="floating-placeholder">Username</label>
            </div>
            {errors.username && <p className="error">{errors.username}</p>}
            {!usernameAvailable && (
              <p className="error">Username is already taken</p>
            )}

            <div className={`register-form-field ${formData.email ? "is-filled" : ""}`}>
              <input
                id="email"
                type="text"
                name="email"
                placeholder="Email"
                value={formData.email}
                onChange={handleChange}
              />
              <label htmlFor="email" className="floating-placeholder">Email</label>
            </div>
            {errors.email && <p className="error">{errors.email}</p>}

            <div className={`register-form-field ${formData.password ? "is-filled" : ""}`}>
              <input
                id="password"
                type="password"
                name="password"
                placeholder="Password"
                value={formData.password}
                onChange={handleChange}
              />
              <label htmlFor="password" className="floating-placeholder">Password</label>
            </div>
            {errors.password && <p className="error">{errors.password}</p>}

            <button type="submit">Sign up</button>
          </form>
        </div>
      </section>
    </div>
  );
}

export default Register;
