# Instagram Clone - Frontend

React frontend application for Instagram clone project built with Vite.

## Tech Stack

- **React 19.2.0** - UI framework
- **Vite 7.2.4** - Build tool and dev server
- **React Router 7.13.0** - Client-side routing
- **Axios 1.13.5** - HTTP client
- **Vitest 4.0.18** - Unit testing framework
- **React Testing Library 16.3.2** - Component testing utilities

## Getting Started

### Install Dependencies
```bash
npm install
```

### Development Server
```bash
npm run dev
```
Runs the app in development mode at [http://localhost:5173](http://localhost:5173)

### Build for Production
```bash
npm run build
```

### Preview Production Build
```bash
npm run preview
```

## Testing

This project has comprehensive unit test coverage with **98.03% code coverage** 

### Run Tests
```bash
npm test
```

### Run Tests with Coverage
```bash
npm run test:coverage
```

### Test Coverage Report
- **Overall Coverage: 98.03%**
- **Branch Coverage: 94%**
- **Function Coverage: 95%**
- **Line Coverage: 97.87%**

**Test Files:**
- `src/utils/postMapper.test.js` - Post data normalization (11 tests)
- `src/utils/auth.test.js` - JWT token utilities (7 tests)
- `src/pages/Login.test.jsx` - Login component (7 tests)
- `src/pages/Register.test.jsx` - Register component (8 tests)

**Total: 33 tests, all passing** 

### Test Stack
- **Vitest** - Fast unit testing framework with ESM support
- **@testing-library/react** - Component testing with user-centric queries
- **@testing-library/jest-dom** - Custom matchers for DOM assertions
- **@vitest/coverage-v8** - Code coverage reporting with V8 engine

## API Documentation

Complete API documentation is available in the project root directory:

- **[FRONTEND_API_CONTRACT.md](../FRONTEND_API_CONTRACT.md)** - Complete API specification for all 5 microservices
  - Request/response formats with JSON examples
  - Authentication requirements
  - Error handling patterns
  - List of missing endpoints

- **[api-tests.http](../api-tests.http)** - HTTP test file for VSCode REST Client or IntelliJ HTTP Client
  - Ready-to-use test requests for all endpoints
  - Variables for easy configuration

- **[Instagram_Clone_API.postman_collection.json](../Instagram_Clone_API.postman_collection.json)** - Postman collection
  - Import into Postman for API testing
  - Auto-save JWT tokens

- **[BACKEND_INTEGRATION_GUIDE.md](../BACKEND_INTEGRATION_GUIDE.md)** - Integration guide for backend team
  - Quick start instructions
  - Critical security notes
  - Backend implementation checklist

## Project Structure

```
src/
├── assets/         # Fonts and static assets
├── components/     # Reusable UI components
│   ├── CreatePostModal.jsx
│   ├── EditProfileModal.jsx
│   ├── Navbar.jsx
│   ├── PostCard.jsx
│   └── ProtectedRoute.jsx
├── pages/          # Route pages
│   ├── Home.jsx    # Feed page with posts
│   ├── Login.jsx
│   ├── Register.jsx
│   ├── Profile.jsx
│   └── Search.jsx
├── services/       # API clients
│   ├── apiClient.js         # Axios instance with interceptors
│   ├── authService.js       # Authentication API
│   ├── feedService.js       # Feed API
│   ├── interactionService.js # Likes/comments API
│   ├── postService.js       # Posts API
│   └── userService.js       # User profiles API
├── utils/          # Utility functions
│   ├── auth.js              # JWT token helpers
│   └── postMapper.js        # Post data normalization
├── test/           # Test configuration
│   └── setup.js             # Global test setup
├── App.jsx         # Main app component with routes
└── main.jsx        # Entry point
```

## Features Implemented

### Authentication & Authorization
- ✅ User registration with validation
- ✅ User login with JWT tokens
- ✅ Protected routes (redirect to login if not authenticated)
- ✅ Token-based API authentication via Axios interceptors

### Feed & Posts
- ✅ Home feed with paginated posts
- ✅ Create posts with image upload
- ✅ Multiple media carousel (swipe through images)
- ✅ Like/unlike posts
- ✅ Comment on posts with username display
- ✅ Delete own posts
- ✅ Delete own comments

### User Profiles
- ✅ View user profile with posts grid
- ✅ Follow/unfollow users
- ✅ Pending follow request UI for private profiles
- ✅ Block/unblock users
- ✅ Private profile protection (hide posts from non-followers)
- ✅ Edit profile (name, bio, profile picture)

### Search
- ✅ Search users by username
- ✅ Navigate to user profiles from search results

### UI/UX
- ✅ Responsive design
- ✅ Custom Instagram-style UI
- ✅ Loading states
- ✅ Error handling
- ✅ Form validation

## Blocked Features (Waiting for Backend)

The following features have UI components ready but are blocked by missing backend endpoints:

### High Priority
- ⏳ **Edit Profile** - UI modal exists, missing `PUT /auth/profiles/{userId}` endpoint
- ⏳ **Follow Status Check** - Missing `GET /users/{userId}/following/check` endpoint

### Medium Priority  
- ⏳ **Followers List Modal** - UI exists, missing `GET /users/{userId}/followers` endpoint
- ⏳ **Following List Modal** - UI exists, missing `GET /users/{userId}/following` endpoint

### Low Priority
- ⏳ **Edit Comment** - UI exists, missing `PUT /comments/{commentId}` endpoint
- ⏳ **Delete Comment (by author)** - Missing `DELETE /comments/{commentId}` endpoint

> **Note:** See [FRONTEND_API_CONTRACT.md](../FRONTEND_API_CONTRACT.md) for complete endpoint specifications.

## API Integration

The frontend integrates with 5 microservices:

- **auth-service** (port 8080) - Registration, login, JWT tokens
- **user-service** (port 8081) - User profiles, follow/block
- **post-service** (port 8082) - Create, read, delete posts
- **interaction-service** (port 8083) - Likes, comments
- **feed-service** (port 8084) - Aggregated feed of posts

All services use JWT authentication via `Authorization: Bearer <token>` header.

### Known Backend Issues

⚠️ **Critical Security Vulnerabilities** (documented in [FRONTEND_API_CONTRACT.md](../FRONTEND_API_CONTRACT.md)):

1. **Missing JWT Authentication** - 3 services (post-service, interaction-service, feed-service) do not validate JWT tokens
   - Security risk: Anyone can perform actions (like, comment, delete) as any user
   - Required fix: Add JWT validation filter to all services

2. **userId in Query Parameters** - Likes and comments endpoints accept `userId` as query parameter
   - Security risk: User can impersonate others by changing `userId` value
   - Required fix: Extract `userId` from JWT token on backend

3. **Hardcoded Following List** - Feed service has `List.of(1L)` hardcoded
   - Bug: Feed only shows posts from user ID 1, not actual following list
   - Required fix: Fetch real following list from user-service

> **For Backend Team:** Please read [BACKEND_INTEGRATION_GUIDE.md](../BACKEND_INTEGRATION_GUIDE.md) for complete details and fixes.

## Environment Variables

Create a `.env` file in the root directory:

```env
VITE_API_AUTH_URL=http://localhost:8080/api/v1
VITE_API_USER_URL=http://localhost:8081/api/v1
VITE_API_POST_URL=http://localhost:8082/api
VITE_API_INTERACTION_URL=http://localhost:8083/api
VITE_API_FEED_URL=http://localhost:8084/api
```

## Troubleshooting

### "Network Error" or "ERR_CONNECTION_REFUSED"
- **Cause:** Backend service not running
- **Solution:** Check that all 5 backend services are running on correct ports (8080-8084)
- **Check:** `curl http://localhost:8080/api/v1/auth/health` (if health endpoint exists)

### "401 Unauthorized" on API calls
- **Cause:** JWT token expired or invalid
- **Solution:** Logout and login again to get fresh token
- **Note:** Tokens are stored in `localStorage` under key `token`

### Images not displaying
- **Cause:** CORS issue or incorrect image URL
- **Solution:** Check that post-service allows CORS from `http://localhost:5173`
- **Check:** Inspect Network tab in browser DevTools for CORS errors

### Feed not showing posts from followed users
- **Cause:** Backend bug - feed service has hardcoded `List.of(1L)`
- **Solution:** Backend team must fix feed-service to fetch real following list
- **Workaround:** Posts from user ID 1 will appear in feed

### Tests failing
- **Solution:** Run `npm install` to ensure all dependencies are installed
- **Check:** Node version should be 18+ (run `node --version`)
- **Clean:** Delete `node_modules` and `package-lock.json`, then `npm install`

### Port 5173 already in use
- **Solution:** Kill process using port or change port in `vite.config.js`
- **Windows:** `netstat -ano | findstr :5173` then `taskkill /PID <PID> /F`

## Linting

```bash
npm run lint
```

## Contributing

This is a student project.

**Team:** Slobodan Petkovic (Frontend Engineer)

**Deadline:** March 16, 2026

