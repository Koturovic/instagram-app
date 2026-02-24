// sadrzi funkciju za dobijanje userId iz JWT tokena
// saznajemo koji je trenutno ulogovani korisnik, na osnovu tokena
import { jwtDecode } from "jwt-decode";

export const getUserIdFromToken = () => {
    const token = localStorage.getItem("token");
    if (!token) return null;

    try {
        const decoded = jwtDecode(token);
        console.log("Sadržaj tokena:", decoded);
        return decoded.id || decoded.userId || decoded.sub; 
    } catch (error) {
        console.error("Nevalidan token:", error);
        return null;
    }
};