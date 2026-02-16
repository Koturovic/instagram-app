import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import PostCard from "../components/PostCard";
import { getFeed } from "../services/feedService";

export default function Home() {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchPosts = async () => {
            try {
                const data = await getFeed();
                setPosts(data);
            } catch (err) {
                console.error("Greška pri učitavanju feed-a:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchPosts();
    }, []);

    return (
        <>
            <Navbar />
            <div style={{ maxWidth: "600px", margin: "0 auto" }}>
                {loading ? (
                    <p>Loading...</p>
                ) : (
                    posts.map(post => <PostCard key={post.id} post={post} />)
                )}
            </div>
        </>
    );
}