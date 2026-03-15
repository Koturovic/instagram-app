import { describe, it, expect } from 'vitest';
import { normalizePost, normalizePosts } from '../utils/postMapper';

describe('postMapper', () => {
  describe('normalizePost', () => {
    it('should normalize a post with all fields present', () => {
      const rawPost = {
        id: 1,
        userId: 123,
        username: 'testuser',
        avatar: 'https://example.com/avatar.jpg',
        caption: 'Test caption',
        description: 'Test description',
        likes: 42,
        image: 'https://example.com/image.jpg',
        mediaFiles: []
      };

      const result = normalizePost(rawPost);

      expect(result.id).toBe(1);
      expect(result.userId).toBe(123);
      expect(result.username).toBe('testuser');
      expect(result.avatar).toBe('https://example.com/avatar.jpg');
      expect(result.caption).toBe('Test caption');
      expect(result.likes).toBe(42);
      expect(result.image).toBe('https://example.com/image.jpg');
    });

    it('should use fallback values for missing fields', () => {
      const rawPost = {};

      const result = normalizePost(rawPost);

      expect(result.id).toBeNull();
      expect(result.userId).toBeNull();
      expect(result.username).toBe('Unknown user');
      expect(result.avatar).toContain('default-avatar');
      expect(result.caption).toBe('');
      expect(result.likes).toBe(0);
      expect(result.image).toContain('unsplash');
    });

    it('should extract first mediaFile url when image is missing', () => {
      const rawPost = {
        mediaFiles: [
          { fileUrl: 'https://example.com/media1.jpg' },
          { fileUrl: 'https://example.com/media2.jpg' }
        ]
      };

      const result = normalizePost(rawPost);

      expect(result.image).toBe('https://example.com/media1.jpg');
    });

    it('should handle userId variations (userId, ownerId, authorId)', () => {
      const post1 = { userId: 1 };
      const post2 = { ownerId: 2 };
      const post3 = { authorId: 3 };
      const post4 = { user: { id: 4 } };

      expect(normalizePost(post1).userId).toBe(1);
      expect(normalizePost(post2).userId).toBe(2);
      expect(normalizePost(post3).userId).toBe(3);
      expect(normalizePost(post4).userId).toBe(4);
    });

    it('should handle id variations (id, postId, postID, post_id)', () => {
      const post1 = { id: 1 };
      const post2 = { postId: 2 };
      const post3 = { postID: 3 };
      const post4 = { post_id: 4 };

      expect(normalizePost(post1).id).toBe(1);
      expect(normalizePost(post2).id).toBe(2);
      expect(normalizePost(post3).id).toBe(3);
      expect(normalizePost(post4).id).toBe(4);
    });

    it('should convert likes to number', () => {
      const post1 = { likes: '42' };
      const post2 = { likesCount: '100' };
      const post3 = { likeCount: 50 };

      expect(normalizePost(post1).likes).toBe(42);
      expect(normalizePost(post2).likes).toBe(100);
      expect(normalizePost(post3).likes).toBe(50);
    });

    it('should generate fallback username with userId', () => {
      const rawPost = { userId: 123 };

      const result = normalizePost(rawPost);

      expect(result.username).toBe('user123');
    });

    it('should preserve raw post data', () => {
      const rawPost = { id: 1, customField: 'test' };

      const result = normalizePost(rawPost);

      expect(result.raw).toEqual(rawPost);
      expect(result.raw.customField).toBe('test');
    });
  });

  describe('normalizePosts', () => {
    it('should normalize an array of posts', () => {
      const rawPosts = [
        { id: 1, username: 'user1' },
        { id: 2, username: 'user2' },
        { id: 3, username: 'user3' }
      ];

      const result = normalizePosts(rawPosts);

      expect(result).toHaveLength(3);
      expect(result[0].id).toBe(1);
      expect(result[0].username).toBe('user1');
      expect(result[1].id).toBe(2);
      expect(result[2].id).toBe(3);
    });

    it('should return empty array for empty input', () => {
      const result = normalizePosts([]);

      expect(result).toEqual([]);
    });

    it('should handle null/undefined input', () => {
      const result1 = normalizePosts(null);
      const result2 = normalizePosts(undefined);
      const result3 = normalizePosts();

      expect(result1).toEqual([]);
      expect(result2).toEqual([]);
      expect(result3).toEqual([]);
    });
  });
});
