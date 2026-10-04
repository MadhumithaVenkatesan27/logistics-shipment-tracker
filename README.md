# Logistics Shipment Tracker

A full-stack CRUD web application for recording shipments and tracking their delivery status.
Users can create, view, edit, delete and filter shipments by status.

## Implemented features

- Create, list, view, edit and delete shipments (delete asks for confirmation)
- Filter by status (All, Created, Picked up, In transit, Out for delivery, Delivered, Cancelled) with a friendly empty state
- Validation on both frontend (field-level messages) and backend (Bean Validation + business rules)
- Duplicate tracking numbers rejected (`409 Conflict`), missing IDs return `404`, bad input returns `400`
- Consistent JSON error responses with no stack traces or SQL exposed
- Loading, empty, success and error states in the UI; submit button disabled while saving
- Flyway database migrations; PostgreSQL data kept in a named Docker volume

**Optional extras (clearly labelled):** Swagger UI, demo seed data (migration `V2`), responsive card layout on mobile.

## Architecture / technology

| Layer    | Technology                                                                                       |
| -------- | ------------------------------------------------------------------------------------------------ |
| Frontend | React 18, Vite, React Router, Axios, Vitest + React Testing Library, plain CSS                   |
| Backend  | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation, Flyway, JUnit 5, Mockito |
| Database | PostgreSQL 16                                                                                    |
| Runtime  | Docker, Docker Compose                                                                           |

Request flow: `React page -> shipmentService.js (Axios) -> ShipmentController -> ShipmentService -> ShipmentRepository -> PostgreSQL`.
The controller works only with DTOs (`ShipmentRequest` / `ShipmentResponse`); `ShipmentMapper` converts to the `Shipment` entity.

```
logistics-shipment-tracker/
├── backend/    Spring Boot API (controller / service / repository / model / dto / exception / config)
├── frontend/   React single-page app
├── compose.yaml
├── .env.example
└── README.md
```

## Prerequisites

Only **Docker** and **Docker Compose** (v2) are needed to run the app.
Java, Node.js and PostgreSQL are _not_ required when using Docker.
(To run tests outside Docker you need JDK 21 + Maven 3.9+, and Node.js 20+.)

## Run the complete application

```bash
git clone <repository-url>
cd logistics-shipment-tracker
docker compose up --build
```

| Component             | URL                                   |
| --------------------- | ------------------------------------- |
| Frontend              | http://localhost:3000                 |
| Backend API           | http://localhost:8080/api/shipments   |
| Swagger UI (optional) | http://localhost:8080/swagger-ui.html |
| PostgreSQL            | localhost:5432                        |

The first build downloads dependencies and can take a few minutes. Wait until the backend log shows `Started ShipmentTrackerApplication`.

## Stop the application

```bash
docker compose down        # stop containers, KEEP the database data
docker compose down -v     # stop containers and DELETE the database volume (fresh start)
```

## Environment variables

Everything has a working default, so no `.env` file is needed. To override, copy `.env.example` to `.env`.

| Variable                                              | Default                                         | Purpose                                                                    |
| ----------------------------------------------------- | ----------------------------------------------- | -------------------------------------------------------------------------- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `shipments` / `shipment_user` / `shipment_pass` | Database credentials (local assessment values only)                        |
| `CORS_ALLOWED_ORIGINS`                                | `http://localhost:3000`                         | Origins allowed to call the API                                            |
| `VITE_API_BASE_URL`                                   | `http://localhost:8080`                         | API URL as seen **by the browser**; baked into the frontend at build time  |
| `APP_TIMEZONE`                                        | `UTC`                                           | Time zone used for the "delivery date not earlier than creation date" rule |

## Run the tests

Backend (JUnit 5 + Mockito + H2 in-memory; no PostgreSQL needed):

```bash
cd backend
mvn test
```

Frontend (Vitest + React Testing Library):

```bash
cd frontend
npm install
npm test
```

## API examples

```bash
# create
curl -i -X POST http://localhost:8080/api/shipments -H "Content-Type: application/json" -d '{
  "trackingNumber": "SHP-3001", "senderName": "North Warehouse", "receiverName": "City Pharmacy",
  "origin": "Leeds", "destination": "London", "carrier": "QuickMove Logistics",
  "status": "CREATED", "expectedDeliveryDate": "2099-01-01" }'

curl http://localhost:8080/api/shipments                      # list all
curl "http://localhost:8080/api/shipments?status=IN_TRANSIT"  # filter
curl http://localhost:8080/api/shipments/1                    # one shipment
curl -X PUT http://localhost:8080/api/shipments/1 -H "Content-Type: application/json" -d '{ ...full body... }'
curl -i -X DELETE http://localhost:8080/api/shipments/1       # 204
curl -i http://localhost:8080/api/shipments/9999              # 404 JSON error
```

| Method | Endpoint                      | Success                           |
| ------ | ----------------------------- | --------------------------------- |
| POST   | `/api/shipments`              | 201 Created (+ `Location` header) |
| GET    | `/api/shipments[?status=...]` | 200 OK                            |
| GET    | `/api/shipments/{id}`         | 200 OK                            |
| PUT    | `/api/shipments/{id}`         | 200 OK                            |
| DELETE | `/api/shipments/{id}`         | 204 No Content                    |

Error body: `{ "timestamp", "status", "error", "message", "path", "fieldErrors": { "<field>": "<message>" } }` (`fieldErrors` only when relevant).

## Assumptions and design decisions

- `status` is optional on create (defaults to `CREATED`) and required on update (`PUT` is a full replacement).
- "Expected delivery date must not be earlier than the creation date": on create it is compared with today's date, on update with the shipment's `createdAt` date, in the zone set by `APP_TIMEZONE` (default UTC).
- Tracking-number uniqueness is enforced by a database unique constraint (the real guarantee when two requests race) plus a service-level check for a friendly `409`.
- Text fields are trimmed before validation. Tracking numbers are case-sensitive.
- The schema is owned by Flyway (`ddl-auto=none`). The list is sorted newest first.
- The frontend calls the API directly from the browser at `VITE_API_BASE_URL`; nginx only serves the static build (with an SPA fallback so refreshing `/shipments/1` works).
- Backend tests use H2 in-memory so they never depend on a local PostgreSQL.

## Known limitations

- No authentication, pagination or sorting controls (out of scope / optional).
- Repository tests run on H2, not on real PostgreSQL (Testcontainers would be the next improvement).
- No concurrent-edit protection (last write wins); optimistic locking with a `@Version` column would fix this.

## Troubleshooting

- **Port already in use (3000 / 8080 / 5432):** stop the other program, or change the left-hand side of the `ports:` mapping in `compose.yaml`. If you change the backend or frontend port, update `VITE_API_BASE_URL` / `CORS_ALLOWED_ORIGINS` to match and rebuild.
- **Frontend shows "Unable to reach the server":** check `docker compose ps` and `docker compose logs backend`. Make sure the backend finished starting.
- **Backend keeps restarting:** run `docker compose logs db backend`. After changing DB credentials in `.env`, run `docker compose down -v` once, because the old volume still holds the old password.
- **Changed `VITE_API_BASE_URL` but nothing changed:** it is baked in at build time; run `docker compose up --build`.
- **Docker build fails while downloading:** check your internet connection / proxy and retry.

## AI assistance disclosure

I used an AI assistant (Claude by Anthropic) to help generate the initial code for this project from the assignment specification. It helped with the Spring Boot backend, the React frontend, the automated tests, the Docker configuration and this README.

How I verified it:

- Ran the full application with `docker compose up --build` and tested create, view, edit, filter and delete in the browser
- Tested the duplicate tracking number and past delivery date errors in the UI and directly against the API
- Ran the backend tests (`mvn test`, 27 tests passing) and the frontend tests (`npm test`, 10 tests passing)
- Read through the code so I can explain the request flow, the use of DTOs and the service layer, and how duplicate tracking numbers are prevented
