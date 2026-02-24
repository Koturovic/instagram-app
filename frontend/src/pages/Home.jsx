import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import PostCard from "../components/PostCard";
import { getFeed } from "../services/feedService";

export default function Home() {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchPosts = async () => {
        try {
            setLoading(true);
            const data = await getFeed();
            setPosts(data);
        } catch (err) {
            console.error("Error fetching feed:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPosts();
    }, []);

    const handlePostDelete = (postId) => {
        // nakon brisanja post-a, osvezavamo feed tako sto uklanjamo obrisani post iz stanja
        setPosts(prevPosts => prevPosts.filter(p => p.id !== postId));
    };

    return (
        <>
            <Navbar />
            <div style={{ maxWidth: "600px", margin: "0 auto" }}>
                {loading ? (
                    <p>Loading...</p>
                ) : (
                    posts.map(post => <PostCard key={post.id} post={post} onDelete={handlePostDelete} />)
                )}
            </div>
        </>
    );
}