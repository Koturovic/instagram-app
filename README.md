# Instagram Replika - Projektni Zadatak

Replika društvene mreže Instagram implementirana kao mikroservisna arhitektura sa 5 nezavisnih backend servisa i React frontend aplikacijom.



## 🏗️ Arhitektura Aplikacije

Aplikacija je implementirana kao **mikroservisna arhitektura** sa 5 nezavisnih backend servisa. Svaki servis ima svoju bazu podataka (Database per Service pattern) i može se nezavisno deploy-ovati i skalirati.

### Struktura Repozitorijuma

```
instagram-app/
├── services/auth-service/   ← auth-service (Spring Boot)
├── src/                     ← user-service (Spring Boot, root projekat)
├── post-service/            ← post-service (Spring Boot)
├── interaction-service/     ← interaction-service (Spring Boot)
├── feed-service/            ← feed-service (Spring Boot)
├── frontend/                ← React frontend aplikacija
├── docker-compose.yml
└── pom.xml
```

### Mikroservisi

#### 1. auth-service (Port: 8080)
**Lokacija**: `services/auth-service/`  
**Odgovornost**: Autentifikacija, autorizacija i upravljanje korisničkim profilima

**Funkcionalnosti**:
- Registracija i prijava korisnika
- Generisanje i validacija JWT tokena
- Upravljanje korisničkim profilima (CRUD operacije)
- Upravljanje tipom profila (javni/privatni)

**Baza podataka**: `auth_db` (PostgreSQL)
- `users` - korisnički nalozi
- `profiles` - korisnički profili

#### 2. user-service (Port: 8081)
**Lokacija**: `src/` (root projekat)  
**Odgovornost**: Upravljanje relacijama između korisnika

**Funkcionalnosti**:
- Slanje i upravljanje zahtevima za praćenje
- Praćenje/otpraćivanje korisnika
- Blokiranje/odblokiranje korisnika
- Pretraga korisnika
- Brojači (followers/following)

**Baza podataka**: `user_db` (PostgreSQL)
- `follow_requests` - zahtevi za praćenje
- `follows` - relacije praćenja
- `blocks` - blokirani korisnici

#### 3. post-service (Port: 8082)
**Lokacija**: `post-service/`  
**Odgovornost**: Upravljanje objavama i media fajlovima

**Funkcionalnosti**:
- Kreiranje, ažuriranje i brisanje objava
- Upload slika i video fajlova
- Validacija (max 20 elemenata, max 50MB po fajlu)
- Upravljanje kolažem (dodavanje/uklanjanje media fajlova)

**Baza podataka**: `post_db` (PostgreSQL)
- `posts` - objave
- `post_media` - media fajlovi objava

**File Storage**: MinIO (S3-compatible object storage), bucket `instagram-media`

#### 4. interaction-service (Port: 8083)
**Lokacija**: `interaction-service/`  
**Odgovornost**: Lajkovi i komentari na objave

**Funkcionalnosti**:
- Lajkovanje/otpoziv lajka
- Komentarisanje objava
- Ažuriranje i brisanje komentara
- Brojači (likes, comments)

**Baza podataka**: `interaction_db` (PostgreSQL)
- `likes` - lajkovi objava
- `comments` - komentari objava

#### 5. feed-service (Port: 8084)
**Lokacija**: `feed-service/`  
**Odgovornost**: Generisanje personalizovanog feeda

**Funkcionalnosti**:
- Agregacija objava profila koje korisnik prati
- Hronološko sortiranje (najnovije prvo)
- Integracija sa drugim servisima za kompletan feed

**Baza podataka**: `feed_db` (PostgreSQL)

### Frontend Aplikacija (Port: 5173)
**Lokacija**: `frontend/`  
**Odgovornost**: Korisnički interfejs

**Tehnologije**:
- React 19 + React Router 7
- Vite (build tool)
- axios (HTTP klijent)
- Nginx (serving u produkciji)

**Stranice**: Login, Register, Home (feed), Profile, Search

**Testiranje**: Vitest + Testing Library

### Infrastruktura

| Servis | Port | Opis |
|--------|------|------|
| MinIO | 9000 | Object storage za media fajlove |
| MinIO Console | 9001 | Web UI za MinIO |
| pgAdmin | 5051 | Web UI za PostgreSQL baze |

### Komunikacija između Servisa

Svi servisi komuniciraju preko **REST API** poziva:

1. **JWT Token Flow**:
   - Korisnik se prijavljuje na `auth-service` → dobija JWT token
   - Svi zahtevi drugim servisima sadrže JWT token u header-u (`Authorization: Bearer <token>`)
   - Svaki servis validira JWT preko `auth-service/api/auth/validate`

2. **Inter-Service Communication**:
   - Servisi pozivaju jedan drugog za validaciju i dohvatanje podataka
   - `user-service` poziva `auth-service` za profile/search podatke, a `feed-service` agregira podatke iz `post-service` i `interaction-service`

### Tehnološki Stack

**Backend**:
- Java 17
- Spring Boot 
- Spring Security (JWT autentifikacija)
- Spring Data JPA
- PostgreSQL 16
- Lombok
- Maven

**Frontend**:
- React 19
- React Router 7
- Vite
- axios

**DevOps**:
- Docker & Docker Compose
- MinIO (S3-compatible object storage)
- CI/CD (GitHub Actions — `ci-main.yml`, `ci-pr.yml`)

**Testing**:
- JUnit 5
- Mockito
- JaCoCo (code coverage — backend)
- Vitest + Testing Library (frontend)

---

## 🔄 Tok Izvršavanja Funkcionalnosti

### 1. Registracija i Prijava Korisnika

```
1. Korisnik šalje POST /api/v1/auth/register sa firstName, lastName, username, email, password
2. auth-service validira podatke (obavezna polja, email format)
3. auth-service proverava da li email/username već postoje
4. auth-service hash-uje lozinku sa BCrypt
5. auth-service kreira User i Profile entitete u auth_db
6. auth-service vraća 200 OK sa JWT tokenom

7. Korisnik šalje POST /api/v1/auth/login sa email i password
8. auth-service pronalazi korisnika po email-u
9. auth-service proverava lozinku (BCrypt)
10. auth-service generiše JWT token sa `userId` claim-om
11. auth-service vraća 200 OK sa JWT tokenom
```

### 2. Praćenje Korisnika

#### Javni Profil:
```
1. Korisnik A šalje POST /api/v1/users/follow-request/{targetUserId} + JWT
2. user-service validira JWT preko auth-service
3. user-service proverava da li target profil postoji i da li je javni
4. user-service automatski kreira follow relaciju u follows tabeli
5. user-service vraća 201 Created sa `{ requestId: null, followed: true }`
```

#### Privatni Profil:
```
1. Korisnik A šalje POST /api/v1/users/follow-request/{targetUserId} + JWT
2. user-service validira JWT preko auth-service
3. user-service proverava da li target profil postoji i da li je privatni
4. user-service kreira follow_requests sa statusom PENDING
5. user-service vraća 201 Created sa requestId

6. Korisnik B (target) dobija obaveštenje o zahtevu
7. Korisnik B šalje POST /api/v1/users/follow-request/{requestId}/accept + JWT
8. user-service ažurira follow_requests status na ACCEPTED
9. user-service kreira follow relaciju u follows tabeli
10. user-service vraća 200 OK
```

### 3. Kreiranje Objave

```
1. Korisnik šalje POST /api/posts (multipart/form-data) sa description, userId i files[]
2. post-service trenutno ne validira JWT u samom endpoint-u
3. post-service validira fajlove i priprema upload u MinIO
4. post-service validira fajlove:
   - Maksimalno 20 fajlova
   - Maksimalno 50MB po fajlu
   - Dozvoljeni tipovi: slike i video
5. post-service upload-uje fajlove u File Storage
6. post-service kreira Post entitet u post_db
7. post-service kreira PostMedia entitete za svaki fajl
8. post-service vraća kreirani `Post` objekat
```

### 4. Lajkovanje Objave

```
1. Korisnik šalje POST /api/likes/{postId}?userId={userId}
2. interaction-service pokušava proveru da li objava postoji preko post-service
3. Ako provera ne uspe, obrada se i dalje nastavlja (soft-fail)
4. Ako like već postoji, briše ga; ako ne postoji, kreira novi like
5. interaction-service vraća tekstualnu poruku (`Post liked` / `Post unliked`)
```

### 5. Komentarisanje Objave

```
1. Korisnik šalje POST /api/comments/{postId}?userId={userId} sa body-jem koji sadrži tekst komentara
2. interaction-service pokušava proveru da li objava postoji preko post-service
3. Ako provera ne uspe, obrada se i dalje nastavlja (soft-fail)
4. interaction-service kreira Comment entitet u interaction_db
5. interaction-service vraća kreirani `Comment`
```

### 6. Generisanje Feeda

```
1. Korisnik šalje GET /api/feed/{userId}
2. feed-service trenutno ne dohvata realnu following listu iz user-service
3. Umesto toga, feed se formira samo od postova prosleđenog `userId`
4. Za svaki post feed-service dohvata broj lajkova i komentare iz interaction-service
5. Greške u agregaciji se loguju preko `System.out.println`, a obrada se nastavlja
6. feed-service vraća listu `FeedResponseDTO` objekata
```

### 7. Pretraga Korisnika

```
1. Korisnik šalje GET /api/v1/users/search?q={query} + JWT
2. user-service validira JWT preko auth-service
3. user-service pretražuje korisnike po username i name u auth_db (preko auth-service)
4. user-service uklanja trenutnog korisnika iz rezultata
5. user-service skriva korisnike koji su blokirali trenutnog korisnika
6. korisnike koje je trenutni korisnik blokirao i dalje vraća, ali uz flag `blockedByCurrentUser`
7. user-service trenutno ne primenjuje paginaciju
```

---

## 🚀 Pokretanje Aplikacije

### Preduslovi
- Docker & Docker Compose

### Pokretanje

```bash
docker compose up --build
```

### Servisi nakon pokretanja

| Servis | URL |
|--------|-----|
| Frontend | http://localhost:5173 |
| auth-service | http://localhost:8080 |
| user-service | http://localhost:8081 |
| post-service | http://localhost:8082 |
| interaction-service | http://localhost:8083 |
| feed-service | http://localhost:8084 |
| MinIO Console | http://localhost:9001 |
| pgAdmin | http://localhost:5051 |

---

## 👥 Članovi Tima

### Backend Engineer A 
**Ime**: Miljan Koturović  
**Odgovornosti**:
- Implementacija `auth-service`
- DevOps aktivnosti (Docker, CI/CD)
- Unit testovi za auth-service (70%+ coverage)
- Arhitekturna dokumentacija


### Backend Engineer B
**Ime**: Đorđe Jovanović  
**Odgovornosti**:
- Implementacija `user-service`
- API integracioni testovi za sve servise
- Unit testovi za user-service (70%+ coverage)



### Backend Engineer C
**Ime**: Nemanja Gligorov    
**Odgovornosti**:
- Implementacija `post-service`
- Implementacija `interaction-service`
- Implementacija `feed-service`
- UI integracioni testovi
- Unit testovi za sve tri servisa (70%+ coverage)



### Frontend Engineer
**Ime**: Slobodan Petković   
**Odgovornosti**:
- Frontend aplikacija (React)
- UI/UX dizajn
- Integracija sa backend servisima


---

Ovaj projekat je deo projektnog zadatka iz predmeta **"Projektovanje informacionih sistema i baza podataka"**.

---

