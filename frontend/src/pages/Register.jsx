import { useState, useEffect } from "react";
import { register } from "../services/authService";
import { useNavigate } from "react-router-dom";
import "./Register.css";

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

      alert("Registration successfull!");
      navigate("/");
    } catch (err) {
      console.error(err)
      alert("Registration failed (email or username already exists)");
      navigate("/")
    }
  };

  return (
    <div className="register-container">
      <div className="register-box">
        <h2>Create an Instagram account</h2>

        <form onSubmit={handleSubmit}>
          <input
            type="text"
            name="firstName"
            placeholder="First name"
            value={formData.firstName}
            onChange={handleChange}
          />
          {errors.firstName && <p className="error">{errors.firstName}</p>}

          <input
            type="text"
            name="lastName"
            placeholder="Last name"
            value={formData.lastName}
            onChange={handleChange}
          />
          {errors.lastName && <p className="error">{errors.lastName}</p>}

          <input
            type="text"
            name="username"
            placeholder="Username"
            value={formData.username}
            onChange={handleChange}
          />
          {errors.username && <p className="error">{errors.username}</p>}
          {!usernameAvailable && (
              <p className="error">Username is already taken</p> 
          )}

          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
          />
          {errors.email && <p className="error">{errors.email}</p>}

          <input
            type="password"
            name="password"
            placeholder="Password"
            value={formData.password}
            onChange={handleChange}
          />
          {errors.password && <p className="error">{errors.password}</p>}

          <button type="submit">Sign up</button>
        </form>
      </div>
    </div>
  );
}

export default Register;
