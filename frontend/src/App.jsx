import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Home from "./pages/Home";
import ProtectedRoute from "./components/ProtectedRoute";
import Profile from "./pages/Profile";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {
          /* Kasnije prepraviti ovako da izgleda, jer host ne dozvoljava prelazak na home 
             sve dok token nije True, odnosno dok logovanje nije uspesno. */
          /* <Route path="/home" element={<ProtectedRoute> <Home /> </ProtectedRoute>} */
        }
        <Route path="/home" element={<Home />} />

        {
          /* Kasnije prepraviti ovako da izgleda:
          /* <Route path="/profile" element={<ProtectedRoute> <Profile /> </ProtectedRoute>} */
        }
        <Route path="/profile" element={<Profile />} />

      </Routes>
    </BrowserRouter>
  );
}
