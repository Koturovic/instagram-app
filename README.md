# Instagram Replica - Projekatni Zadatak

Replika društvene mreže Instagram implementirana kao mikroservisna arhitektura sa 5 nezavisnih servisa.



## 🏗️ Arhitektura Aplikacije

Aplikacija je implementirana kao **mikroservisna arhitektura** sa 5 nezavisnih servisa. Svaki servis ima svoju bazu podataka (Database per Service pattern) i može se nezavisno deploy-ovati i skalirati.

### Mikroservisi

#### 1. auth-service (Port: 8080)
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
**Odgovornost**: Upravljanje objavama i media fajlovima

**Funkcionalnosti**:
- Kreiranje, ažuriranje i brisanje objava
- Upload slika i video fajlova
- Validacija (max 20 elemenata, max 50MB po fajlu)
- Upravljanje kolаžem (dodavanje/uklanjanje media fajlova)

**Baza podataka**: `post_db` (PostgreSQL)
- `posts` - objave
- `post_media` - media fajlovi objava

**File Storage**: Lokalni fajl sistem ili MinIO (S3-compatible)

#### 4. interaction-service (Port: 8083)
**Odgovornost**: Laјkovi i komentari na objave

**Funkcionalnosti**:
- Laјkovanje/otpoziv laјka
- Komentarisanje objava
- Ažuriranje i brisanje komentara
- Brojači (likes, comments)

**Baza podataka**: `interaction_db` (PostgreSQL)
- `likes` - laјkovi objava
- `comments` - komentari objava

#### 5. feed-service (Port: 8084)
**Odgovornost**: Generisanje personalizovanog feeda

**Funkcionalnosti**:
- Agregacija objava profila koje korisnik prati
- Hronološko sortiranje (najnovije prvo)
- Integracija sa drugim servisima za kompletan feed

**Baza podataka**: `feed_db` (PostgreSQL) ili Redis za caching

### Komunikacija između Servisa

Svi servisi komuniciraju preko **synchronous REST API** poziva:

1. **JWT Token Flow**:
   - Korisnik se prijavljuje na `auth-service` → dobija JWT token
   - Svi zahtevi drugim servisima sadrže JWT token u header-u (`Authorization: Bearer <token>`)
   - Svaki servis validira JWT preko `auth-service/api/auth/validate`

2. **Inter-Service Communication**:
   - Servisi pozivaju jedan drugog za validaciju i dohvatanje podataka
   - Primer: `post-service` proverava da li korisnik može videti objavu preko `user-service`

### Tehnološki Stack

**Backend**:
- Java 17
- Spring Boot 4.0.1
- Spring Security (JWT autentifikacija)
- Spring Data JPA
- Postgresql
- Lombok
- Maven

**DevOps**:
- Docker & Docker Compose
- CI/CD (GitHub Actions / GitLab CI)

**Testing**:
- JUnit 5
- Mockito
- JaCoCo (code coverage)

---

## 🔄 Tok Izvršavanja Funkcionalnosti

### 1. Registracija i Prijava Korisnika

```
1. Korisnik šalje POST /api/auth/register sa email, username, password, name
2. auth-service validira podatke (email format, username format, password strength)
3. auth-service proverava da li email/username već postoje
4. auth-service hash-uje lozinku sa BCrypt
5. auth-service kreira User i Profile entitete u auth_db
6. auth-service vraća 201 Created sa userId

7. Korisnik šalje POST /api/auth/login sa username i password
8. auth-service pronalazi korisnika u bazi
9. auth-service proverava lozinku (BCrypt)
10. auth-service generiše JWT token (sa userId, username, email)
11. auth-service vraća 200 OK sa JWT tokenom
```

### 2. Praćenje Korisnika

#### Javni Profil:
```
1. Korisnik A šalje POST /api/follow/request/{targetUserId} + JWT
2. user-service validira JWT preko auth-service
3. user-service proverava da li target profil postoji i da li je javni
4. user-service automatski kreira follow relaciju u follows tabeli
5. user-service vraća 201 Created
```

#### Privatni Profil:
```
1. Korisnik A šalje POST /api/follow/request/{targetUserId} + JWT
2. user-service validira JWT preko auth-service
3. user-service proverava da li target profil postoji i da li je privatni
4. user-service kreira follow_requests sa statusom PENDING
5. user-service vraća 201 Created sa requestId

6. Korisnik B (target) dobija obaveštenje o zahtevu
7. Korisnik B šalje POST /api/follow/accept/{requestId} + JWT
8. user-service ažurira follow_requests status na ACCEPTED
9. user-service kreira follow relaciju u follows tabeli
10. user-service vraća 200 OK
```

### 3. Kreiranje Objave

```
1. Korisnik šalje POST /api/posts (multipart/form-data) sa description i files[] + JWT
2. post-service validira JWT preko auth-service
3. post-service proverava da li korisnik može kreirati objave (preko user-service)
4. post-service validira fajlove:
   - Maksimalno 20 fajlova
   - Maksimalno 50MB po fajlu
   - Dozvoljeni tipovi: slike i video
5. post-service upload-uje fajlove u File Storage
6. post-service kreira Post entitet u post_db
7. post-service kreira PostMedia entitete za svaki fajl
8. post-service vraća 201 Created sa postId i detaljima
```

### 4. Laјkovanje Objave

```
1. Korisnik šalje POST /api/likes/{postId} + JWT
2. interaction-service validira JWT preko auth-service
3. interaction-service proverava da li objava postoji (preko post-service)
4. interaction-service proverava da li korisnik može videti objavu (preko user-service)
5. interaction-service proverava da li korisnik nije blokiran (preko user-service)
6. interaction-service kreira Like entitet u interaction_db
7. interaction-service vraća 200 OK sa likeId
```

### 5. Komentarisanje Objave

```
1. Korisnik šalje POST /api/comments/{postId} sa content + JWT
2. interaction-service validira JWT preko auth-service
3. interaction-service proverava da li objava postoji (preko post-service)
4. interaction-service proverava da li korisnik može videti objavu (preko user-service)
5. interaction-service proverava da li korisnik nije blokiran (preko user-service)
6. interaction-service kreira Comment entitet u interaction_db
7. interaction-service vraća 201 Created sa commentId
```

### 6. Generisanje Feeda

```
1. Korisnik šalje GET /api/feed + JWT
2. feed-service validira JWT preko auth-service
3. feed-service dohvata listu profila koje korisnik prati (preko user-service)
4. Za svaki profil koji se prati:
   a. feed-service dohvata objave (preko post-service)
   b. Za svaku objavu:
      - feed-service dohvata broj laјkova (preko interaction-service)
      - feed-service dohvata poslednjih N komentara (preko interaction-service)
5. feed-service filtrira objave blokiranih korisnika
6. feed-service sortira objave po created_at DESC
7. feed-service primenjuje paginaciju
8. feed-service vraća 200 OK sa feed array-om
```

### 7. Pretraga Korisnika

```
1. Korisnik šalje GET /api/users/search?q={query} + JWT
2. user-service validira JWT preko auth-service
3. user-service pretražuje korisnike po username i name u auth_db (preko auth-service)
4. user-service filtrira blokirane korisnike
5. user-service primenjuje paginaciju
6. user-service vraća 200 OK sa listom korisnika
```

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

**Napomena**: Ova dokumentacija je ažurirana u skladu sa zahtevima projektnog zadatka. Za najnovije informacije, pogledaj commit istoriju.
