# auth-service

Spring Boot servis za autentifikaciju (registracija, login, JWT). Deo Instagram replike.

---

## Šta treba

- **Java 17**
- **Maven** (ili Maven wrapper `./mvnw` uključen u projekat)
- **PostgreSQL** (npr. u Dockeru na portu 5433)

---

## Kako pokrenuti

### 1. PostgreSQL

Pokreni bazu na portu **5433** (da ne konflikta sa lokalnim PostgreSQL-om na 5432).

**Docker (jednostavno):**
```bash
docker run -d -p 5433:5432 \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth_user \
  -e POSTGRES_PASSWORD=auth_password \
  postgres:16-alpine
```



### 2. Pokretanje servisa

Iz foldera **auth-service** :

```bash
./mvnw spring-boot:run
```

Servis se podiže na **http://localhost:8080**.

---

## API (kratko)

| Metoda | Putanja | Opis |
|--------|---------|------|
| POST | `/api/v1/auth/register` | Registracija. Body (JSON): `firstName`, `lastName`, `username`, `email`, `password`. Odgovor: `{ "token": "..." }`. |
| POST | `/api/v1/auth/login` | Login. Body (JSON): `email`, `password`. Odgovor: `{ "token": "..." }`. |

Za zaštićene zahteve: header **`Authorization: Bearer <token>`**.

**Content-Type** za register i login: **`application/json`**.

---

## Primer (curl)

**Registracija:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ime","lastName":"Prezime","username":"imeprezime","email":"test@example.com","password":"lozinka123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"lozinka123"}'
```
