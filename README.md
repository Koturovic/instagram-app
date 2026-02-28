# User Service – Instagram-like backend

Mikroservis za korisničke profile, praćenje, blokiranje i pretragu (prema projektnoj dokumentaciji). Pristup zaštićenim resursima ide **preko Auth servisa** – user-service ne parsuje JWT, već poziva Auth endpoint **validate** za svaki zahtev.

---

## Šta sve radi user-service

| Funkcionalnost | Opis |
|----------------|------|
| **Profili** | Kreiranje i ažuriranje profila (displayName, bio, profileImageUrl, isPrivate). Jedan profil po korisniku (userId iz Auth). Pregled tuđeg profila – pun ili samo osnovni podaci (ime, slika, bio) ako je profil privatni i ne pratiš ga. |
| **Praćenje** | Follow / unfollow. Javni profil → follow odmah **ACCEPTED**. Privatni profil → follow u statusu **PENDING** (čeka prihvatanje). Blokirani ne mogu da prate. |
| **Blokiranje** | Block / unblock. Pri blokiranju brišu se follow relacije u **oba smera**. Blokirani ne mogu da vide profil ni da prate. |
| **Pretraga** | Pretraga profila po username / display name. Blokirani korisnici se ne vraćaju u rezultatima. |
| **Validacija tokena** | Za sve zaštićene rute token se **ne parsuje** u user-service – šalje se Auth servisu na **GET /api/v1/auth/validate**. Na 200 koristi se **userId** i **email**; na 401 user-service vraća **401** klijentu. |

---

## Struktura projekta

```
user-service/
├── pom.xml
├── mvnw, mvnw.cmd
├── src/main/
│   ├── java/com/instagram/user_service/
│   │   ├── UserServiceApplication.java
│   │   ├── config/
│   │   │   └── RestTemplateConfig.java          # RestTemplate za poziv Auth servisa
│   │   ├── domain/
│   │   │   ├── Profile.java
│   │   │   ├── Follow.java
│   │   │   └── Block.java
│   │   ├── repository/
│   │   │   ├── ProfileRepository.java
│   │   │   ├── FollowRepository.java
│   │   │   └── BlockRepository.java
│   │   ├── dto/
│   │   │   ├── ProfileResponse.java
│   │   │   ├── ProfileBasicResponse.java
│   │   │   ├── ProfileUpdateRequest.java
│   │   │   └── ProfileSearchResult.java
│   │   ├── service/
│   │   │   ├── ProfileService.java
│   │   │   ├── FollowService.java
│   │   │   ├── BlockService.java
│   │   │   └── ... (ResourceNotFoundException, ForbiddenException, BadRequestException)
│   │   ├── controller/
│   │   │   ├── ProfileController.java
│   │   │   ├── FollowController.java
│   │   │   ├── BlockController.java
│   │   │   └── DevController.java                # POST /dev/test-token (samo za lokalno testiranje)
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java      # Poziva Auth validate, postavlja CurrentUser
│   │   │   ├── AuthServiceClient.java            # GET {AUTH_URL}/api/v1/auth/validate
│   │   │   ├── AuthServiceProperties.java        # app.auth.service-url
│   │   │   ├── AuthValidateResponse.java         # { userId, email }
│   │   │   ├── JwtProperties.java                # Samo za /dev/test-token
│   │   │   ├── CurrentUser.java
│   │   │   └── SecurityConfig.java
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       └── application.properties
└── README.md
```

---

## Integracija sa Auth servisom

### Pravilo

Pristup svakom zaštićenom resursu mora da prođe kroz **validaciju tokena**. User-service **ne parsuje JWT** – šalje token Auth servisu; Auth odlučuje da li je token validan i vraća **userId** i **email**.

### Tok zahteva

1. **Klijent** šalje zahtev na user-service sa headerom:  
   `Authorization: Bearer <token>`  
   (token dobijen od Auth servisa – npr. posle login-a.)

2. **User-service** (filter) za sve zaštićene rute:
   - Uzme `Authorization` iz ulaznog zahteva.
   - Pozove **Auth servis:**  
     **GET** `{AUTH_SERVICE_URL}/api/v1/auth/validate`  
     **Header:** `Authorization: Bearer <token>` (isti token).
   - **Auth odgovor 200 OK** i body `{ "userId": number, "email": string }`  
     → user-service postavi **CurrentUser(userId, email)** i nastavi sa obradom zahteva (kontroler, servis, baza).
   - **Auth odgovor 401** (ili greška)  
     → user-service **vrati 401** klijentu i ne poziva kontroler.

3. U kontrolerima i servisima koristi se **CurrentUser** (userId, email) za autorizaciju (npr. „moj profil“, follow, block).

### Konfiguracija Auth servisa

U `application.properties` (ili env varijablama):

| Okruženje | Vrednost |
|-----------|----------|
| **Lokalno** | `app.auth.service-url=http://localhost:8080` ili env `AUTH_SERVICE_URL=http://localhost:8080` |
| **Docker** | `AUTH_SERVICE_URL=http://auth-service:8080` |

- **Bez trailing slash** u URL-u (npr. `http://localhost:8080`, ne `http://localhost:8080/`).
- Putanja `/api/v1/auth/validate` se u kodu dodaje automatski.

### Komponente u kodu

| Komponenta | Uloga |
|------------|--------|
| **AuthServiceProperties** | Čita `app.auth.service-url` (ili `AUTH_SERVICE_URL`). |
| **AuthServiceClient** | Šalje **GET** na `{serviceUrl}/api/v1/auth/validate` sa headerom `Authorization: Bearer <token>`; na 200 parsira body u **AuthValidateResponse** (userId, email); na 401 ili grešku baca **TokenInvalidException**. |
| **JwtAuthenticationFilter** | Za rute koje nisu `/actuator/`, `/error`, `/dev/`: izvadi token iz headera → pozove **AuthServiceClient.validate(token)** → na uspeh postavi **CurrentUser(userId, email)** i nastavi; na izuzetak vrati **401** i ne nastavlja lanac. |
| **CurrentUser** | Drži **userId** i **username** (u praksi email iz validate odgovora) – koristi se u servisima za „trenutni korisnik“. |

---

## API endpointi

Svi zahtevi na ove endpointe zaštićeni su: mora postojati header  
`Authorization: Bearer <token>`  
gde je **token** onaj koji Auth servis vrati (npr. posle login-a). User-service taj token šalje na Auth **validate**; bez uspešnog validate (200) dobijaš **401**.

| Metoda | Endpoint | Opis |
|--------|----------|------|
| **GET** | `/profiles/{id}` | Pregled profila. Blok → 403. Privatni + ne pratiš → samo osnovni podaci (ime, slika, bio). |
| **PATCH** | `/profiles` | Izmena **svog** profila (body: displayName, bio, profileImageUrl, isPrivate). Ako nemaš profil – kreira se pri prvom PATCH-u. |
| **GET** | `/profiles/search?q=...` | Pretraga po username / display name. Blokirani se ne vraćaju. |
| **POST** | `/follows/{profileId}` | Follow. Javni → ACCEPTED, privatni → PENDING. Ako postoji block → 403. |
| **DELETE** | `/follows/{profileId}` | Unfollow. |
| **POST** | `/blocks/{profileId}` | Block; briše follow u oba smera. |
| **DELETE** | `/blocks/{profileId}` | Unblock. |

### Dev endpoint (samo za lokalno testiranje)

| Metoda | Endpoint | Opis |
|--------|----------|------|
| **POST** | `/dev/test-token` | Body: `{ "userId": number, "username": string }`. Vraća JWT koji **ne prolazi** Auth validate – koristi se samo kada Auth servis **nije** uključen u tok (npr. samo user-service + Postman). Uključuje se sa `app.dev.test-token-enabled=true`. |

---

## Pokretanje

1. **PostgreSQL** – radi na `localhost:5432`, baza **user_db** (npr. user/pass `postgres`/`postgres`).
2. **Auth servis** (ako testiraš ceo tok) – pokrenut na npr. `http://localhost:8080` i ima endpoint **GET /api/v1/auth/validate** koji prihvata `Authorization: Bearer <token>` i vraća `{ "userId": number, "email": string }`.
3. U `application.properties` (ili env):  
   `app.auth.service-url=http://localhost:8080` (ili `AUTH_SERVICE_URL`).

```bash
mvn spring-boot:run
```

Aplikacija sluša na **http://localhost:8081**.

---

## Konfiguracija (application.properties)

| Svojstvo | Značenje |
|-----------|----------|
| `server.port` | Port user-service-a (npr. 8081). |
| `spring.datasource.url` | JDBC URL za PostgreSQL (npr. `jdbc:postgresql://localhost:5432/user_db`). |
| `spring.datasource.username` / `password` | Kredencijali za bazu. |
| `app.auth.service-url` | Base URL Auth servisa (bez trailing slash). Lokalno: `http://localhost:8080`, Docker: `http://auth-service:8080`. Može preko env: `AUTH_SERVICE_URL`. |
| `app.jwt.secret` | Koristi se **samo** za **POST /dev/test-token** (generisanje test tokena). Nije potreban za validate – validate radi Auth servis. |
| `app.dev.test-token-enabled` | `true` = uključen **POST /dev/test-token** za lokalno testiranje bez Auth servisa. |

---

## Kako testirati

### A) Testiranje sa Auth servisom (preporučeno – kao u produkciji)

1. Pokreni **Auth servis** (npr. na portu 8080) i uveri se da ima **GET /api/v1/auth/validate** sa odgovorom `{ "userId": number, "email": string }`.
2. Pokreni **user-service** sa `app.auth.service-url=http://localhost:8080`.
3. **Login na Auth servis** (npr. POST login sa email/lozinka) i u odgovoru uzmi **accessToken** (ili kako Auth servis vraća token).
4. U **Postmanu** za sve zahteve na user-service (GET/PATCH profiles, follow, block, search) dodaj header:  
   **Authorization:** `Bearer <token_iz_koraka_3>`  
   i šalji zahteve na `http://localhost:8081/...` (npr. PATCH `/profiles`, GET `/profiles/1`, itd.).
5. Očekivano: **200** / **204** kada je token validan; **401** kada token nije poslat ili ga Auth validate odbije.

Ovim proveravaš da pristup resursima **zaista ide preko Auth validate** i da bez validnog tokena ne možeš pristupiti zaštićenim rutama.

### B) Testiranje samo user-service (bez Auth servisa) – /dev/test-token

Korisno kada Auth servis još nije spreman ili radiš samo na user-service.

1. U `application.properties` stavi:  
   `app.dev.test-token-enabled=true`  
   i **isključi** Auth (ili stavi neki dummy URL za `app.auth.service-url` – zaštićene rute će i dalje zvati validate i dobiti grešku ako Auth ne radi).
2. **Problem:** token koji dobiješ od **POST /dev/test-token** Auth servis **ne priznaje** (nije izdat od Auth-a). Zato, ako je user-service podešen da zove Auth validate, zaštićene rute (profiles, follows, blocks) **neće raditi** sa tim tokenom – dobićeš **401** jer validate vraća 401.

Znači: **sa uključenom integracijom sa Auth servisom** zaštićene rute moraju koristiti **token od Auth servisa** (login). **POST /dev/test-token** ima smisla samo ako privremeno **isključiš** poziv ka Auth (npr. za čisto lokalno testiranje baze i logike bez Auth-a) – što za produkciju nije dozvoljeno.

### C) Korak-po-korak vodič (Postman + pgAdmin)

- **PRVI-PUT-TESTIRANJE.md** – detaljan vodič: kako otvoriti Postman, dobiti token (npr. od /dev/test-token ako Auth nije uključen), kreirati profile, follow, block, proveriti u pgAdmin-u tabele **profiles**, **follows**, **blocks**.
- **POSTMAN-KORACI.md** – skraćeni koraci za Postman.

Kada Auth servis radi, u tim vodičima umesto **POST /dev/test-token** koristi **token koji dobiješ od Auth login** i u headeru svih zahteva na user-service stavi **Authorization: Bearer &lt;taj_token&gt;** – na taj način testiraš i povezivanje sa Auth servisom.

---

## API testiranje (automatski testovi)

U projektu postoje **integracioni testovi** koji pozivaju REST API (MockMvc). Auth servis je **mockovan** – `AuthServiceClient` u testovima vraća `userId` i `email` bez poziva na pravi Auth servis.

- **Lokacija:** `src/test/java/com/instagram/user_service/api/UserServiceApiTest.java`
- **Profil:** `test` – koristi H2 in-memory bazu (ne treba PostgreSQL pri pokretanju testova).
- **Pokretanje:**  
  `mvn test -Dtest=UserServiceApiTest`  
  ili u IntelliJ-u: Run na `UserServiceApiTest`.

**Šta se testira:**

| Grupa | Testovi |
|-------|--------|
| **Bez tokena** | GET /profiles/1 i PATCH /profiles bez Authorization → **401** |
| **Profili** | PATCH /profiles (kreiranje), GET /profiles/{id}, PATCH (ažuriranje), GET nepostojeći profil → **404** |
| **Follow** | POST /follows/{id} → **204**, DELETE /follows/{id} → **204**, follow nepostojeći profil → **404** |
| **Block** | POST /blocks/{id} → **204**, DELETE /blocks/{id} → **204** |
| **Pretraga** | GET /profiles/search?q=... → **200**, niz |
| **Dev** | POST /dev/test-token (bez auth) → **200**, accessToken u odgovoru |

Za zaštićene endpointe u testovima koriste se tokeni `Bearer token-user-1` i `Bearer token-user-2`; mock za `AuthServiceClient.validate()` vraća userId 1 odnosno 2 i odgovarajući email.

---

## Rezime

- User-service **ne parsuje JWT** – za zaštićene rute poziva **Auth servis: GET {AUTH_SERVICE_URL}/api/v1/auth/validate** sa headerom **Authorization: Bearer &lt;token&gt;**.
- Na **200** i body `{ "userId", "email" }` koristi **userId** (i email) za **CurrentUser** i nastavlja obradu; na **401** vraća **401** klijentu.
- Konfiguracija: **app.auth.service-url** (lokalno `http://localhost:8080`, Docker `http://auth-service:8080`).
- Testiranje: **automatski** – `mvn test -Dtest=UserServiceApiTest`; **ručno** – sa Auth servisom, token od login-a, zahtevi na user-service sa **Authorization: Bearer &lt;token&gt;**.
