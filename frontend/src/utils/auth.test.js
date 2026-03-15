import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getUserIdFromToken } from './auth';

vi.mock('jwt-decode', () => ({
  jwtDecode: vi.fn()
}));

import { jwtDecode } from 'jwt-decode';

describe('auth utils', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('getUserIdFromToken', () => {
    it('should return null when no token exists', () => {
      localStorage.getItem.mockReturnValue(null);

      const result = getUserIdFromToken();

      expect(result).toBeNull();
      expect(localStorage.getItem).toHaveBeenCalledWith('token');
    });

    it('should return userId from decoded token (id field)', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { id: 123, username: 'testuser' };

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockReturnValue(mockDecoded);

      const result = getUserIdFromToken();

      expect(result).toBe(123);
      expect(jwtDecode).toHaveBeenCalledWith(mockToken);
    });

    it('should return userId from decoded token (userId field)', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { userId: 456, username: 'testuser' };

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockReturnValue(mockDecoded);

      const result = getUserIdFromToken();

      expect(result).toBe(456);
    });

    it('should return userId from decoded token (sub field)', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { sub: 789, username: 'testuser' };

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockReturnValue(mockDecoded);

      const result = getUserIdFromToken();

      expect(result).toBe(789);
    });

    it('should prioritize id over userId over sub', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { id: 1, userId: 2, sub: 3 };

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockReturnValue(mockDecoded);

      const result = getUserIdFromToken();

      expect(result).toBe(1); // id has priority
    });

    it('should return null for invalid token', () => {
      const mockToken = 'invalid.token';

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockImplementation(() => {
        throw new Error('Invalid token');
      });

      const result = getUserIdFromToken();

      expect(result).toBeNull();
      expect(console.error).toHaveBeenCalled();
    });

    it('should return null when token has no id fields', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { username: 'testuser', email: 'test@example.com' };

      localStorage.getItem.mockReturnValue(mockToken);
      jwtDecode.mockReturnValue(mockDecoded);

      const result = getUserIdFromToken();

      expect(result).toBeNull();
    });
  });
});
