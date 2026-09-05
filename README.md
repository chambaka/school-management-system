# Chambaka School Management System

White-label SaaS backend for running many schools on one platform. Each school is a tenant with its own branding, users, academics, attendance, fees, and notices.

Package: `tz.co.chambaka.school.management`

---

## Contents

- [What it does](#what-it-does)
- [Stack](#stack)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Local development](#local-development)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Logging and audit trail](#logging-and-audit-trail)
- [Deployment guide](#deployment-guide)
- [Security checklist](#security-checklist)

---

## What it does

- Multi-tenant SaaS: one database, isolated by `school_id`
- White-label branding (logo, colors, timezone, currency, custom domain)
- School signup and JWT authentication
- Roles: `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`, `PARENT`
- Academic structure: years, classes, sections, subjects, timetable
- Exams, grades, and report cards
- Student and teacher attendance
- Fee structures, invoices, payments, receipts
- Notices targeted by audience
- Request correction IDs, structured logs, and a platform + tenant audit trail

---

## Stack

| Layer | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (access + refresh) |
| Persistence | Spring Data JPA, Flyway |
| Database | MySQL 8 |
| Mapping | MapStruct |
| API docs | springdoc OpenAPI / Swagger UI |
| Build | Maven |

---

## Architecture

`User` is only the login identity. Role data lives on profile tables:

```
User
 ├── Student
 ├── Teacher
 └── Parent
```

School structure (class teacher sits on **Section**, not on the class):

```
School
 └── AcademicYear
      └── SchoolClass
           └── Section  (optional class teacher)
                └── Students

Teacher
 └── TeacherSubject
      └── SchoolClass / Section
```

Every school-owned row has `school_id`. The JWT puts `schoolId` into `TenantContext`. Services always query that tenant.

| Role | Purpose |
| --- | --- |
| `SUPER_ADMIN` | Platform operator. Manages all schools. |
| `ADMIN` | School administrator. |
| `TEACHER` | Teaching staff. |
| `STUDENT` | Learner. |
| `PARENT` | Guardian linked to one or more students. |

---

## Requirements

- JDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` on this machine)
- Maven 3.8+
- Docker (for local MySQL), or a MySQL 8 server
- 512 MB RAM minimum for the JVM in production; 1 GB recommended

---

## Local development

Dev defaults reuse the existing MySQL on **3306**:

| Setting | Value |
| --- | --- |
| Host / port | `localhost:3306` |
| Database | `db_school_sms` |
| User | `sms_user` |
| Password | `Uhambule1972!` |

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

mvn spring-boot:run
```

If you do not already have that database, `docker compose up -d` starts a separate MySQL on **3310** (set `MYSQL_PORT=3310`, `MYSQL_DATABASE=school_sms`, `MYSQL_USER=sms`, `MYSQL_PASSWORD=sms`). Then:

| Resource | URL |
| --- | --- |
| API | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

Default platform admin (created on first boot if none exists):

- Email: `oscar.d@example.net`
- Password: `ChangeMe123!`

Create a school tenant:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register-school \
  -H 'Content-Type: application/json' \
  -d '{
    "schoolName": "Chambaka Secondary",
    "adminName": "School Admin",
    "adminEmail": "yuki.t@example.com",
    "password": "ChangeMe123!",
    "currency": "TZS",
    "timezone": "Africa/Dar_es_Salaam",
    "country": "Tanzania"
  }'
```

Use the returned `accessToken` as `Authorization: Bearer <token>`.

Frontend white-label (no login):

```text
GET /api/v1/public/branding/chambaka-secondary
GET /api/v1/public/branding?host=school.example.com
```

### Tests

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn test
```

### Build a runnable JAR

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -DskipTests clean package
# artifact: target/sms-1.0.0-SNAPSHOT.jar
```

---

## Configuration

Profiles:

- `dev` (default) — existing local MySQL on port `3306`, SSL off
- `prod` — requires env vars; SSL on; Hibernate `ddl-auto=validate`

| Variable | Default (dev) | Required in prod | Meaning |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Active profile |
| `SERVER_PORT` | `8080` | no | HTTP port |
| `MYSQL_HOST` | `localhost` | yes | Database host |
| `MYSQL_PORT` | `3306` | yes | Database port |
| `MYSQL_DATABASE` | `db_school_sms` | yes | Database name |
| `MYSQL_USER` | `sms_user` | yes | Database user |
| `MYSQL_PASSWORD` | `Uhambule1972!` | yes | Database password |
| `SMS_JWT_SECRET` | insecure default | **yes** | HMAC secret, **at least 32 bytes** |
| `SMS_JWT_ACCESS_TTL` | `PT1H` | no | Access token lifetime (ISO-8601 duration) |
| `SMS_JWT_REFRESH_TTL` | `P7D` | no | Refresh token lifetime |
| `SMS_CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` | yes | Allowed frontend origins |
| `SMS_SUPER_ADMIN_EMAIL` | `oscar.d@example.net` | yes | Seeded only if no `SUPER_ADMIN` exists |
| `SMS_SUPER_ADMIN_PASSWORD` | `ChangeMe123!` | yes | Seeded only if no `SUPER_ADMIN` exists |
| `SMS_SUPER_ADMIN_NAME` | `Platform Admin` | no | Display name for the seed account |

---

## API overview

| Area | Base path |
| --- | --- |
| Auth | `/api/v1/auth` |
| Branding | `/api/v1/public/branding` |
| School | `/api/v1/schools`, `/api/v1/platform/schools` |
| Years / classes / sections / subjects | `/api/v1/academic-years`, `/classes`, `/sections`, `/subjects` |
| People | `/api/v1/teachers`, `/students`, `/parents` |
| Academics | `/api/v1/allocations`, `/timetable`, `/exams`, `/grades` |
| Attendance | `/api/v1/attendance` |
| Finance | `/api/v1/fees`, `/invoices`, `/payments` |
| Notices | `/api/v1/notices` |
| Tenant audit | `/api/v1/audit-events` |
| Platform audit | `/api/v1/platform/audit-events` |

Full method-level docs live in Swagger. Typical flow:

1. `POST /api/v1/auth/register-school` or platform login
2. Create academic year → class → section
3. Create teachers, students, parents; link parents
4. Allocate subjects, timetable, exams, grades
5. Mark attendance
6. Define fees, generate invoices, record payments
7. Publish notices
8. Trace a change with the `X-Correction-Id` response header and the audit APIs

---

## Logging and audit trail

Every HTTP request gets a **correction ID**. Use it to join application logs with persisted audit events.

### Correction ID

- Send `X-Correction-Id` (or `X-Correlation-Id`) if the client already has one. Otherwise the API generates a UUID.
- The same value is returned on every response as `X-Correction-Id`.
- Error bodies include `correctionId`.
- Logs include `correctionId`, `schoolId`, and `userId` on each line.

Valid incoming IDs are 8–64 characters of `A–Z`, `a–z`, `0–9`, `.`, `_`, or `-`. Invalid values are replaced.

```bash
curl -i http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -H 'X-Correction-Id: support-ticket-1042' \
  -d '{"email":"yuki.t@example.com","password":"ChangeMe123!"}'
```

Look for `X-Correction-Id` in the response, then search journal logs or the audit APIs for that value.

### What is recorded

Events are stored in `audit_events` (Flyway `V2__audit_events.sql`). Each row has scope, actor, action, resource, HTTP path, status, IP, user agent, and the request correction ID.

| Scope | Who / what |
| --- | --- |
| `TENANT` | School users changing data in their school |
| `PLATFORM` | `SUPER_ADMIN` actions, school registration, and platform-wide login failures |

| Action | When |
| --- | --- |
| `LOGIN`, `LOGIN_FAILED`, `TOKEN_REFRESH`, `PASSWORD_CHANGE`, `REGISTER_SCHOOL` | Auth service (explicit) |
| `CREATE`, `UPDATE`, `DELETE` | `POST` / `PUT` / `PATCH` / `DELETE` on tenant or platform APIs |
| `ACCESS_DENIED`, `ERROR` | 401/403 and 5xx (except auth paths already covered above) |

Passwords, tokens, and similar fields are stripped from stored request bodies. Reading the audit APIs themselves is not written back as an event. A failed audit write is logged and does not fail the original request.

### Query APIs

School `ADMIN` — current tenant only:

```text
GET /api/v1/audit-events
GET /api/v1/audit-events/{correctionId}
```

Platform `SUPER_ADMIN` — all tenants:

```text
GET /api/v1/platform/audit-events
GET /api/v1/platform/audit-events/{correctionId}
```

Optional query params: `correctionId`, `action`, `resourceType`, `actorEmail`, `from`, `to` (ISO-8601 instants). Platform list also accepts `scope` and `schoolId`. Results are paged (`page`, `size`).

```bash
# Tenant trail
curl -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/v1/audit-events?action=CREATE&resourceType=Student&size=20'

# Everything for one correction ID (platform)
curl -H "Authorization: Bearer $PLATFORM_TOKEN" \
  http://localhost:8080/api/v1/platform/audit-events/support-ticket-1042
```

---

## Deployment guide

### 1. Prepare the server

- Ubuntu 22.04+ (or similar)
- JDK 21
- MySQL 8
- Nginx (recommended reverse proxy + TLS)
- A dedicated OS user, e.g. `sms`

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk-headless nginx
java -version   # 21.x
```

### 2. Create the production database

On the MySQL host:

```sql
CREATE DATABASE school_sms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'sms'@'%' IDENTIFIED BY 'a-long-random-db-password';
GRANT ALL PRIVILEGES ON school_sms.* TO 'sms'@'%';
FLUSH PRIVILEGES;
```

Restrict `'%'` to the app server IP when you can. Flyway applies `V1__init.sql` and later migrations (`V2__audit_events.sql` for the audit trail) on startup.

### 3. Build the artifact

On a CI machine or the server:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
git clone <your-repo-url> school_management_system
cd school_management_system
mvn -DskipTests clean package
```

Copy `target/sms-1.0.0-SNAPSHOT.jar` to the server, for example `/opt/chambaka-sms/sms.jar`.

### 4. Environment file

Create `/opt/chambaka-sms/sms.env` (mode `600`, owned by the service user):

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=school_sms
MYSQL_USER=sms
MYSQL_PASSWORD=a-long-random-db-password

# openssl rand -base64 48
SMS_JWT_SECRET=replace-with-at-least-32-random-bytes-of-secret

SMS_CORS_ORIGINS=https://app.example.com,https://admin.example.com

SMS_SUPER_ADMIN_EMAIL=you@example.com
SMS_SUPER_ADMIN_PASSWORD=replace-with-a-strong-password
SMS_SUPER_ADMIN_NAME=Platform Admin
```

Generate a JWT secret:

```bash
openssl rand -base64 48
```

### 5. Run with systemd

`/etc/systemd/system/chambaka-sms.service`:

```ini
[Unit]
Description=Chambaka School Management API
After=network.target mysql.service

[Service]
User=sms
Group=sms
WorkingDirectory=/opt/chambaka-sms
EnvironmentFile=/opt/chambaka-sms/sms.env
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /opt/chambaka-sms/sms.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo useradd --system --home /opt/chambaka-sms --shell /usr/sbin/nologin sms
sudo mkdir -p /opt/chambaka-sms
sudo chown -R sms:sms /opt/chambaka-sms
sudo chmod 600 /opt/chambaka-sms/sms.env
sudo systemctl daemon-reload
sudo systemctl enable --now chambaka-sms
sudo systemctl status chambaka-sms
```

Health check:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Logs:

```bash
journalctl -u chambaka-sms -f
```

### 6. Nginx reverse proxy and TLS

Point DNS (`api.example.com`) at the server, then:

```nginx
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/letsencrypt/live/api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.example.com/privkey.pem;

    client_max_body_size 10m;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
}
```

Issue a certificate:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com
```

Do **not** expose port `8080` on the public interface. Bind the app to localhost (already the default) and only publish `443`.

### 7. Deploy with Docker (optional)

Local Compose only starts MySQL. For a full stack, add an app service or run the JAR in a container:

```bash
# build
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -DskipTests clean package

# run (example)
docker run --rm \
  --env-file /opt/chambaka-sms/sms.env \
  -p 127.0.0.1:8080:8080 \
  -v "$(pwd)/target/sms-1.0.0-SNAPSHOT.jar:/app/sms.jar:ro" \
  eclipse-temurin:21-jre \
  java -jar /app/sms.jar
```

Keep MySQL on a named volume or a managed database. Do not put production secrets in `docker-compose.yml`.

### 8. First login after deploy

1. Confirm `GET https://api.example.com/actuator/health` returns `{"status":"UP"}`.
2. Log in as the platform admin from `SMS_SUPER_ADMIN_*`.
3. Change that password immediately (`POST /api/v1/auth/change-password`).
4. Create the first school with `POST /api/v1/auth/register-school`, or have the school self-register.
5. Point each school frontend at `GET /api/v1/public/branding/{slug}` (or `?host=`) so it can white-label before login.

### 9. Updates

```bash
# build new JAR, then:
sudo systemctl stop chambaka-sms
sudo cp target/sms-1.0.0-SNAPSHOT.jar /opt/chambaka-sms/sms.jar
sudo systemctl start chambaka-sms
```

Flyway runs pending migrations on startup. Take a MySQL dump before upgrading:

```bash
mysqldump -u sms -p school_sms > school_sms-$(date +%F).sql
```

### 10. Rollback

1. Stop the service.
2. Restore the previous JAR.
3. If a migration ran, restore the dump taken in step 9 (do not edit applied Flyway versions by hand).
4. Start the service.

---

## Security checklist

- [ ] Set a unique `SMS_JWT_SECRET` (32+ bytes). Never ship the sample value.
- [ ] Change `SMS_SUPER_ADMIN_PASSWORD` before the first public login.
- [ ] Use a strong MySQL password and bind MySQL to a private network.
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Set `SMS_CORS_ORIGINS` to real HTTPS frontend origins only.
- [ ] Terminate TLS at Nginx; do not expose `8080` publicly.
- [ ] Restrict `/actuator` if you later expose more than `health` and `info`.
- [ ] Turn off or protect Swagger in production if the API is public.
- [ ] Back up MySQL on a schedule.
- [ ] Keep `audit_events`; treat it as an append-only trail. Review tenant and platform logs after incidents using `X-Correction-Id`.

---

## License

Proprietary — Chambaka. All rights reserved.
