// src/utils/auth.js
import { jwtDecode } from "jwt-decode";

export const getUserIdFromToken = () => {
    const token = localStorage.getItem("token");
    if (!token) return null;

    try {
        const decoded = jwtDecode(token);
        console.log("Sadržaj tokena:", decoded); // OBAVEZNO: Pogledaj ovo u konzoli brauzera!

        // Ako Miljan nije dodao numerički ID, 'sub' će biti email.
        // Nemanja na portu 8083 traži Long userId (broj).
        // Ako u konzoli ne vidiš 'id' ili 'userId', Miljan mora da dopuni AuthService.java
        return decoded.id || decoded.userId || decoded.sub; 
    } catch (error) {
        console.error("Nevalidan token:", error);
        return null;
    }
};