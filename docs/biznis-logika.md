# Modeli podataka i biznis logika



- `services/auth-service` (port `8080`)
- `src` (user-service, port `8081`)
- `post-service` (port `8082`)
- `interaction-service` (port `8083`)
- `feed-service` (port `8084`)

---

## 1) auth-service

**Baza:** `auth_db` (PostgreSQL)  
**Glavne rute:** `/api/v1/auth/*`

### 1.1 Entiteti

#### User (`users`)
- `id` (Integer, PK)
- `email` (String, unique, not null)
- `first_name` (String, not null)
- `last_name` (String, not null)
- `password` (String, BCrypt hash)
- `created_at` (Instant, auto on create)

#### Profile (`profiles`)
- `id` (Integer, PK)
- `user_id` (OneToOne → `users.id`, unique, not null)
- `user_name` (mapirano na `username`, unique, not null)
- `is_private` (Boolean, default `false`)
- `profile_image_url` (TEXT)
- `bio` (TEXT)
- `created_at` (Instant, auto on create)

### 1.2 DTO

- `RegisterRequest`: `firstName`, `lastName`, `username`, `email`, `password`
- `LoginRequest`: `email`, `password`
- `AuthenticationResponse`: `token`
- `ValidateResponse`: `userId`, `email`
- `ProfileResponse`: `userId`, `username`, `firstName`, `lastName`, `bio`, `profileImageUrl`, `isPrivate`
- `ProfileSearchResponse`: `userId`, `username`, `firstName`, `lastName`, `profileImageUrl`, `isPrivate` + JSON alias `id`

### 1.3 Biznis logika

- **Register**: proverava unique `email` i `username`, kreira `User` + `Profile`, generiše JWT sa claim-om `userId`.
- **Login**: autentifikacija preko `AuthenticationManager`, JWT sa `userId`.
- **Validate**: proverava Bearer token i vraća `{userId, email}` (401 za nevažeći token).
- **Get profile**: `/profiles/{userId}` vraća proširen profil za user-service/frontend.
- **Search profiles**: `/profiles/search?q=...` pretraga po `username`, `firstName`, `lastName`.
- **Update profile**: `/profiles/{userId}` (multipart) menja ime, prezime, username, bio, privatnost i opciono profilnu sliku (slika se čuva kao Base64 data URL u `profileImageUrl`).

---

## 2) user-service

**Baza:** `user_db` (PostgreSQL)  
**Glavne rute:** `/api/v1/users/*`

### 2.1 Entiteti

#### Follow (`follows`)
- `id` (Long, PK)
- `follower_user_id` (Long)
- `following_user_id` (Long)
- `created_at` (Instant)
- unique: (`follower_user_id`, `following_user_id`)

#### FollowRequest (`follow_requests`)
- `id` (Long, PK)
- `requester_user_id` (Long)
- `target_user_id` (Long)
- `status` (Enum: `PENDING`, `ACCEPTED`, `REJECTED`)
- `created_at` (Instant)
- unique: (`requester_user_id`, `target_user_id`)

#### Block (`blocks`)
- `id` (Long, PK)
- `blocker_user_id` (Long)
- `blocked_user_id` (Long)
- `created_at` (Instant)
- unique: (`blocker_user_id`, `blocked_user_id`)

### 2.2 DTO i inter-service modeli

- `CountResponse`: `count`
- `FollowRequestResponse`: `requestId`, `followed`
- `FollowUserDto`: `userId`, `username`, `profileImageUrl`
- `PendingFollowRequestDto`: `requestId`, `requesterUserId`, `requesterUsername`, `requesterProfileImage`, `createdAt`
- `RelationshipStatusDto`: `following`, `pending`, `blocked`
- `UserSearchResultDto`: `id`, `username`, `firstName`, `lastName`, `profileImageUrl`, `isPrivate`, `blockedByCurrentUser`
- `AuthValidateResponse`: `userId`, `email` (iz auth-service `/validate`)
- `AuthProfileResponse`: `userId`, `username`, `isPrivate`, `profileImageUrl`

### 2.3 Biznis logika

- **Count endpoints**: broj follower/following relacija iz `follows`.
- **Followers/Following liste**: ID-jevi iz baze + enrich profil podacima iz auth-service (`/profiles/{id}`).
- **Relationship status**: vraća da li je trenutni korisnik u odnosu sa target-om `following` / `pending` / `blocked`.
- **Send follow request**:
  - self-follow → 400
  - block između korisnika → 403
  - već prati → 400
  - privatni profil → kreira `FollowRequest(PENDING)`
  - javni profil → odmah kreira `Follow`
- **Accept/Reject request**: samo target korisnik sme da obradi zahtev; dozvoljeno samo iz `PENDING`.
- **Pending requests**: lista svih `PENDING` zahteva ka trenutnom korisniku, sa requester profil informacijama.
- **Unfollow**: briše red iz `follows`.
- **Block**: self-block 400, provera da target postoji, briše follow veze u oba smera, dodaje block.
- **Unblock**: briše block relaciju.
- **Search users**: poziva auth `/profiles/search`, zatim filtrira trenutnog korisnika i korisnike koji su blokirali trenutnog korisnika; korisnici koje je trenutni korisnik blokirao ostaju u rezultatu uz `blockedByCurrentUser=true`.

---

## 3) post-service

**Baza:** `post_db` (PostgreSQL)  
**Storage:** MinIO bucket `instagram-media`  
**Glavne rute:** `/api/posts/*`

### 3.1 Entiteti

#### Post (`posts`)
- `id` (Long, PK)
- `userId` (Long)
- `description` (String)
- `createdAt` (LocalDateTime, `@CreationTimestamp`)
- `mediaFiles` (`List<PostMedia>`, `@OneToMany`, cascade ALL, orphanRemoval)

#### PostMedia (`post_media`)
- `id` (Long, PK)
- `fileUrl` (String)
- `contentType` (String)
- `post_id` (FK ka `posts.id`)

### 3.2 Biznis logika

- **Create post** (`POST /api/posts`):
  - max 20 fajlova
  - max 50MB po fajlu
  - osigurava bucket i public read policy
  - upload u MinIO i čuva `fileUrl` + `contentType`
- **Get all** (`GET /api/posts`): vraća postove sortirane po `createdAt DESC`.
- **Get by user** (`GET /api/posts/user/{userId}`): postovi korisnika, `createdAt DESC`.
- **Delete post** (`DELETE /api/posts/{id}`): briše MinIO objekte pa post.
- **Update post** (`PUT /api/posts/{id}`): menja opis; ako stignu novi fajlovi, stari media se brišu i zamenjuju novim.



---

## 4) interaction-service

**Baza:** `interaction_db` (PostgreSQL)  
**Glavne rute:** `/api/likes/*`, `/api/comments/*`

### 4.1 Entiteti

#### Like (`likes`)
- `id` (Long, PK)
- `postId` (Long)
- `userId` (Long)
- `createdAt` (LocalDateTime)

#### Comment (`comments`)
- `id` (Long, PK)
- `postId` (Long)
- `userId` (Long)
- `content` (String, not null)
- `createdAt` (LocalDateTime)

### 4.2 Biznis logika

- **Toggle like** (`POST /api/likes/{postId}?userId=...`):
  - pokušava proveru posta pozivom ka post-service (`APP_POST_SERVICE_URL`, default `http://post-service:8082/api/posts/`)
  - provera je soft-fail (izuzeci se ignorišu)
  - ako like postoji → delete (`Post unliked`), inače create (`Post liked`)
- **Likes count** (`GET /api/likes/{postId}/count`): broj lajkova za post.
- **Is liked** (`GET /api/likes/{postId}/users/{userId}`): bool da li korisnik lajkovao post.
- **Add comment** (`POST /api/comments/{postId}?userId=...`): soft-fail provera posta + snimanje komentara.
- **Get comments** (`GET /api/comments/{postId}`): lista komentara.
- **Delete comment** (`DELETE /api/comments/{commentId}`): brisanje po ID.

---

## 5) feed-service

**Port:** `8084`  
**Napomena:** ima datasource konfiguraciju za `feed_db`, ali u trenutnom kodu nema JPA entiteta/repozitorijuma (servis radi kao agregator preko REST poziva).

### 5.1 DTO

#### FeedResponseDTO
- `postId` (Long)
- `userId` (Long)
- `description` (String)
- `likesCount` (Long)
- `recentComments` (`List<Object>`)
- `mediaFiles` (`List<Object>`)

### 5.2 Biznis logika

- **Get feed** (`GET /api/feed/{userId}`):
  1. Feed se formira samo za prosleđeni `userId` (`List.of(userId)`).
  2. Za tog korisnika zove post-service `/api/posts/user/{id}`.
  3. Za svaki post zove interaction-service:
     - `/api/likes/{postId}/count`
     - `/api/comments/{postId}`
  4. Sastavlja `FeedResponseDTO`.
- Greške tokom agregacije se loguju (`System.out.println`) i feed nastavlja sa sledećim stavkama.

---

## 6) Izuzeci i HTTP statusi 

### auth-service
- `EmailAlreadyExistsException` → `409 CONFLICT`
- `UsernameAlreadyExistsException` → `409 CONFLICT`
- `BadCredentialsException` → `401 UNAUTHORIZED`
- `ProfileNotFoundException` → `404 NOT_FOUND`
- `MethodArgumentNotValidException` → `400 BAD_REQUEST`
- invalid/missing bearer na `/validate` → `401 UNAUTHORIZED`

### user-service
- `ResourceNotFoundException` → `404 NOT_FOUND`
- `ProfileNotFoundException` → `404 NOT_FOUND`
- `ForbiddenException` → `403 FORBIDDEN`
- `BadRequestException` → `400 BAD_REQUEST`
- `AuthenticationException` → `401 UNAUTHORIZED`
- `MethodArgumentNotValidException` → `400 BAD_REQUEST`

### post/interaction/feed
- pretežno direktni odgovori iz kontrolera (`String`, entitet ili DTO) bez centralizovanog `@ControllerAdvice` mapiranja.

---


### Env promenljive koje utiču na međuservisne URL-ove

- `AUTH_SERVICE_URL` (user-service)
- `APP_POST_SERVICE_URL` (interaction + feed)
- `APP_INTERACTION_SERVICE_URL` (feed)
- `APP_USER_SERVICE_URL` (feed)
- `MINIO_PUBLIC_URL` (post-service za javni URL fajla)
